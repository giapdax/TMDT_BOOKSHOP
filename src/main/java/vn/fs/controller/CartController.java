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
import org.springframework.web.bind.annotation.*;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

import vn.fs.commom.CommomDataService;
import vn.fs.config.PaypalPaymentIntent;
import vn.fs.config.PaypalPaymentMethod;
import vn.fs.entities.CartItem;
import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.OrderDetailRepository;
import vn.fs.repository.OrderRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository; // dùng Optional/IgnoreCase
import vn.fs.service.PaypalService;
import vn.fs.service.ShoppingCartService;
import vn.fs.service.impl.ExchangeRateService;
import vn.fs.util.Utils;

/**
 * CartController - Checkout flow sạch với Spring Security:
 *  - Lấy currentUser() từ SecurityContext + UserRepository Optional.
 *  - Lưu "order draft" vào session khi đi PayPal, hoàn tất ở /pay/success.
 *  - Tuyệt đối không update entity User trong checkout (tránh vi phạm Bean Validation).
 */
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

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String URL_PAYPAL_SUCCESS = "pay/success";
    public static final String URL_PAYPAL_CANCEL  = "pay/cancel";

    // ============================ Helpers ============================

    /** Lấy user đăng nhập từ Spring Security + Optional/IgnoreCase. */
    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String login = auth.getName(); // có thể là email hoặc username

        // Thử theo thứ tự: usernameIgnoreCase, emailIgnoreCase, combo OR
        return userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .or(() -> userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login))
                .orElse(null);
    }

    /** Tính tổng tiền (đã trừ discount) từ cart. */
    private double calcTotal(Collection<CartItem> cartItems) {
        double total = 0.0;
        if (cartItems == null) return 0.0;
        for (CartItem ci : cartItems) {
            double line = ci.getQuantity() * ci.getProduct().getPrice();
            total += line - (line * ci.getProduct().getDiscount() / 100.0);
        }
        return total;
    }

    /** Bind info giỏ hàng lên model. */
    private void bindCartToModel(Model model) {
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", shoppingCartService.getAmount());
        model.addAttribute("totalPrice", calcTotal(cartItems));
        model.addAttribute("totalCartItems", shoppingCartService.getCount());
    }

    // ============================ Pages ============================

    @GetMapping("/shoppingCart_checkout")
    public String shoppingCart(Model model) {
        bindCartToModel(model);
        return "web/shoppingCart_checkout";
    }
    @Autowired
    private ProductRepository productRepository;

    // add cartItem
    @GetMapping("/addToCart")
    public String add(@RequestParam("productId") Long productId, HttpServletRequest request, Model model) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            CartItem item = new CartItem();
            BeanUtils.copyProperties(product, item);
            item.setQuantity(1);
            item.setProduct(product);
            item.setId(productId);
            shoppingCartService.add(item);
        }
        session = request.getSession();
        session.setAttribute("cartItems", shoppingCartService.getCartItems());
        model.addAttribute("totalCartItems", shoppingCartService.getCount());
        return "redirect:/products";
    }

    // delete cartItem
    @GetMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id, Model model) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            CartItem item = new CartItem();
            BeanUtils.copyProperties(product, item);
            item.setProduct(product);
            item.setId(id);
            shoppingCartService.remove(item);
        }
        model.addAttribute("totalCartItems", shoppingCartService.getCount());
        return "redirect:/checkout";
    }

    // show check out
    @GetMapping("/checkout")
    public String checkOut(Model model) {
        model.addAttribute("order", new Order()); // form binding cho địa chỉ/ghi chú...
        bindCartToModel(model);
        commomDataService.commonData(model, currentUser());
        return "web/shoppingCart_checkout";
    }

    // submit checkout (COD hoặc chuyển hướng PayPal)
    @PostMapping("/checkout")
    @Transactional
    public String checkedOut(Model model, Order order, HttpServletRequest request) throws MessagingException {
        String checkOut = request.getParameter("checkOut");
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();

        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/products";
        }

        double totalPrice = calcTotal(cartItems);

        if (StringUtils.equalsIgnoreCase(checkOut, "paypal")) {
            // Lưu "order draft" vào session để callback success lấy ra hoàn tất
            Order draft = new Order();
            BeanUtils.copyProperties(order, draft);
            session.setAttribute("orderDraft", draft);

            String cancelUrl  = Utils.getBaseURL(request) + "/" + URL_PAYPAL_CANCEL;
            String successUrl = Utils.getBaseURL(request) + "/" + URL_PAYPAL_SUCCESS;
            try {
                double rateVNDToUSD = exchangeRateService.getVNDExchangeRate();
                double usdAmount = totalPrice / rateVNDToUSD;

                log.info("Creating payment with amount: {} USD", usdAmount);
                Payment payment = paypalService.createPayment(
                        usdAmount,
                        "USD",
                        PaypalPaymentMethod.paypal,
                        PaypalPaymentIntent.sale,
                        "Thanh toán bằng Paypal",
                        cancelUrl, successUrl
                );

                for (Links links : payment.getLinks()) {
                    if ("approval_url".equalsIgnoreCase(links.getRel())) {
                        return "redirect:" + links.getHref();
                    }
                }
            } catch (PayPalRESTException e) {
                log.error("PayPal createPayment error", e);
                // Tùy bạn: cho quay lại checkout hiển thị thông báo
                // return "redirect:/checkout";
            }
        }

        // === COD: tạo Order & OrderDetail, KHÔNG update User ===
        User cur = currentUser();
        if (cur == null) {
            return "redirect:/login";
        }

        order.setOrderDate(new Date());
        order.setStatus(0);            // pending
        order.setAmount(totalPrice);
        order.setUser(cur);

        orderRepository.save(order);

        for (CartItem ci : cartItems) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(order);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice()); // nếu muốn lưu giá sau discount thì chỉnh ở đây
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

    // paypal success callback
    @GetMapping(URL_PAYPAL_SUCCESS)
    public String successPay(@RequestParam("paymentId") String paymentId,
                             @RequestParam("PayerID") String payerId,
                             HttpServletRequest request, Model model) throws MessagingException {

        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        double totalPrice = calcTotal(cartItems);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", shoppingCartService.getAmount());
        model.addAttribute("totalPrice", totalPrice);
        model.addAttribute("totalCartItems", shoppingCartService.getCount());

        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);
            if ("approved".equalsIgnoreCase(payment.getState())) {
                User cur = currentUser();
                if (cur == null) return "redirect:/login";

                // Lấy order draft từ session
                Order draft = (Order) session.getAttribute("orderDraft");
                if (draft == null) {
                    draft = new Order(); // fallback cho chắc
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

    // done checkout ship cod
    @GetMapping("/checkout_success")
    public String checkoutSuccess(Model model) {
        commomDataService.commonData(model, currentUser());
        return "web/checkout_success";
    }

    // done checkout paypal
    @GetMapping("/checkout_paypal_success")
    public String paypalSuccess(Model model) {
        commomDataService.commonData(model, currentUser());
        return "web/checkout_paypal_success";
    }

    @GetMapping("/pay/cancel")
    public String cancelPayment(@RequestParam("token") String token, Model model) {
        commomDataService.commonData(model, currentUser());
        model.addAttribute("message", "Bạn đã hủy thanh toán qua PayPal.");
        return "web/payment_cancel";
    }
}
