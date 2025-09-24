package vn.fs.controller;

import java.util.Collection;
import java.util.Date;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

import vn.fs.commom.CommomDataService;
import vn.fs.config.PaypalPaymentIntent;
import vn.fs.config.PaypalPaymentMethod;
import vn.fs.dto.CheckoutAddressDTO;
import vn.fs.entities.CartItem;
import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.OrderDetailRepository;
import vn.fs.repository.OrderRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.service.PaypalService;
import vn.fs.service.ShoppingCartService;
import vn.fs.service.impl.ExchangeRateService;
import vn.fs.util.Utils;

import javax.validation.Valid;

@Controller
public class CartController extends CommomController {

    @Autowired private HttpSession session;

    @Autowired private CommomDataService commomDataService;
    @Autowired private ShoppingCartService shoppingCartService;
    @Autowired private PaypalService paypalService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderDetailRepository orderDetailRepository;
    @Autowired private ExchangeRateService exchangeRateService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String URL_PAYPAL_SUCCESS = "pay/success";
    public static final String URL_PAYPAL_CANCEL  = "pay/cancel";

    /* ============================ Helpers ============================ */

    /** Lấy user đăng nhập từ Spring Security + Optional/IgnoreCase. */
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String login = auth.getName(); // có thể là email hoặc username
        return userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .or(() -> userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login))
                .orElse(null);
    }

    /** Bind info giỏ hàng lên model theo service mới. */
    private void bindCartToModel(Model model) {
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", shoppingCartService.getAmount());            // tổng tiền (đã trừ discount)
        model.addAttribute("totalPrice", shoppingCartService.getAmount());      // đồng bộ UI cũ
        model.addAttribute("totalCartItems", shoppingCartService.getDistinctCount()); // badge ở header
        model.addAttribute("totalCartQtySum", shoppingCartService.getQuantitySum());  // tổng qty
    }

    /* ============================ Pages ============================ */

    @GetMapping("/shoppingCart_checkout")
    public String shoppingCart(Model model) {
        bindCartToModel(model);
        User me = currentUser();
        commomDataService.commonData(model, me);

        // prefills
        if (!model.containsAttribute("checkout")) {
            CheckoutAddressDTO dto = new CheckoutAddressDTO();
            if (me != null) {
                dto.setEmail(me.getEmail());
                dto.setFullName(me.getName());
                dto.setPhone(me.getPhone());
            }
            model.addAttribute("checkout", dto);
        }
        return "web/shoppingCart_checkout";
    }

    // alias
    @GetMapping("/checkout")
    public String checkOut(Model model) {
        return shoppingCart(model);
    }

    /* ====================== Cart actions ====================== */

    @GetMapping("/addToCart")
    public String add(@RequestParam("productId") Long productId,
                      @RequestParam(value = "qty", defaultValue = "1") int qty) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            int desired = Math.max(1, qty);
            shoppingCartService.addOrIncrease(productId, desired);
        }
        return "redirect:/products";
    }

    @GetMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            CartItem item = new CartItem();
            BeanUtils.copyProperties(product, item);
            item.setProduct(product);
            item.setId(id);
            shoppingCartService.remove(item);
        }
        return "redirect:/checkout";
    }

    /* ====================== Submit checkout ====================== */

    @PostMapping("/checkout")
    @Transactional
    public String checkedOut(@Valid @ModelAttribute("checkout") CheckoutAddressDTO checkout,
                             BindingResult br,
                             Model model,
                             HttpServletRequest request) throws MessagingException {

        // cart empty?
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/products";
        }

        // fill header/nav + cart data again if errors
        bindCartToModel(model);
        commomDataService.commonData(model, currentUser());

        if (br.hasErrors()) {
            // trả lại view + lỗi
            return "web/shoppingCart_checkout";
        }

        double totalPrice = shoppingCartService.getAmount();
        String method = (checkout.getPaymentMethod() == null) ? "cod" : checkout.getPaymentMethod().trim().toLowerCase();

        if (StringUtils.equals(method, "paypal")) {
            // Lưu "order draft" vào session để callback success lấy ra hoàn tất
            Order draft = new Order();
            draft.setAddress(checkout.getAddress());
            draft.setPhone(checkout.getPhone());
            // (note không lưu trong entity hiện tại — nếu muốn, thêm cột rồi map)
            session.setAttribute("orderDraft", draft);

            String cancelUrl  = Utils.getBaseURL(request) + URL_PAYPAL_CANCEL;
            String successUrl = Utils.getBaseURL(request) + URL_PAYPAL_SUCCESS;
            try {
                double rateVNDToUSD = exchangeRateService.getVNDExchangeRate();
                java.math.BigDecimal usd = new java.math.BigDecimal(totalPrice / rateVNDToUSD)
                        .setScale(2, java.math.RoundingMode.HALF_UP);
                double usdAmount = usd.doubleValue();

                log.info("Creating PayPal payment (rounded): {} USD", usdAmount);
                Payment payment = paypalService.createPayment(
                        usdAmount,
                        "USD",
                        PaypalPaymentMethod.paypal,
                        PaypalPaymentIntent.sale,
                        "Thanh toán bằng Paypal",
                        cancelUrl, successUrl
                );

                for (Links l : payment.getLinks()) {
                    if ("approval_url".equalsIgnoreCase(l.getRel())) {
                        return "redirect:" + l.getHref();
                    }
                }
                log.warn("No approval_url returned by PayPal.");
                return "redirect:/checkout";

            } catch (PayPalRESTException e) {
                log.error("PayPal createPayment error", e);
                return "redirect:/checkout";
            }
        }

        // === COD flow ===
        User cur = currentUser();
        if (cur == null) return "redirect:/login";

        Order order = new Order();
        order.setOrderDate(new Date());
        order.setStatus(0);            // pending
        order.setAmount(totalPrice);
        order.setUser(cur);
        order.setAddress(checkout.getAddress());
        order.setPhone(checkout.getPhone());
        orderRepository.save(order);

        for (CartItem ci : cartItems) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(order);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice()); // nếu muốn dùng giá đã discount thì thay tại đây
            orderDetailRepository.save(od);
        }

        // send mail
        commomDataService.sendSimpleEmail(
                cur.getEmail(),
                "Book-Shop Xác Nhận Đơn hàng",
                "CONFIRM",
                cartItems,
                totalPrice,
                order
        );

        shoppingCartService.clear();
        session.removeAttribute("cartItems");
        model.addAttribute("orderId", order.getOrderId());

        return "redirect:/checkout_success";
    }

    /* ====================== PayPal callbacks ====================== */

    @GetMapping(URL_PAYPAL_SUCCESS)
    public String successPay(@RequestParam("paymentId") String paymentId,
                             @RequestParam("PayerID") String payerId,
                             HttpServletRequest request, Model model) throws MessagingException {

        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        double totalPrice = shoppingCartService.getAmount();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", totalPrice);
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("totalCartItems", shoppingCartService.getDistinctCount());

        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);
            if ("approved".equalsIgnoreCase(payment.getState())) {
                User cur = currentUser();
                if (cur == null) return "redirect:/login";

                // Lấy order draft từ session
                Order draft = (Order) session.getAttribute("orderDraft");
                if (draft == null) {
                    draft = new Order();
                }

                draft.setOrderDate(new Date());
                draft.setStatus(2);        // paid
                draft.setUser(cur);
                draft.setAmount(totalPrice);
                orderRepository.save(draft);

                for (CartItem ci : cartItems) {
                    OrderDetail od = new OrderDetail();
                    od.setQuantity(ci.getQuantity());
                    od.setOrder(draft);
                    od.setProduct(ci.getProduct());
                    od.setPrice(ci.getProduct().getPrice());
                    orderDetailRepository.save(od);
                }

                commomDataService.sendSimpleEmail(
                        cur.getEmail(),
                        "Book-Shop Xác Nhận Đơn hàng",
                        "CONFIRM",
                        cartItems,
                        totalPrice,
                        draft
                );

                shoppingCartService.clear();
                session.removeAttribute("cartItems");
                session.removeAttribute("orderDraft");
                model.addAttribute("orderId", draft.getOrderId());

                return "redirect:/checkout_paypal_success";
            }
        } catch (PayPalRESTException e) {
            log.error("PayPal executePayment error", e);
        }
        return "redirect:/";
    }

    @GetMapping("/checkout_success")
    public String checkoutSuccess(Model model) {
        commomDataService.commonData(model, currentUser());
        return "web/checkout_success";
    }

    @GetMapping("/checkout_paypal_success")
    public String paypalSuccess(Model model) {
        commomDataService.commonData(model, currentUser());
        return "web/checkout_paypal_success";
    }

    @GetMapping("/pay/cancel")
    public String cancelPayment(@RequestParam(name = "token", required = false) String token, Model model) {
        commomDataService.commonData(model, currentUser());
        model.addAttribute("message", "Bạn đã hủy thanh toán qua PayPal.");
        return "web/payment_cancel";
    }

    @GetMapping("/cart/inc")
    public String inc(@RequestParam Long productId,
                      @RequestParam(required=false) String redirect,
                      @RequestParam(required=false) String anchor) {
        shoppingCartService.increase(productId, 1);
        if ("checkout".equalsIgnoreCase(redirect)) {
            return "redirect:/checkout" + (anchor != null ? "#" + anchor : "");
        }
        return "redirect:/products";
    }

    @GetMapping("/cart/dec")
    public String dec(@RequestParam Long productId,
                      @RequestParam(required=false) String redirect,
                      @RequestParam(required=false) String anchor) {
        int after = shoppingCartService.decrease(productId, 1);
        if (after <= 0) {
            CartItem ci = shoppingCartService.getItem(productId);
            if (ci != null) shoppingCartService.remove(ci);
        }
        if ("checkout".equalsIgnoreCase(redirect)) {
            return "redirect:/checkout" + (anchor != null ? "#" + anchor : "");
        }
        return "redirect:/products";
    }
}
