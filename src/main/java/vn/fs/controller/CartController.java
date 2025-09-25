package vn.fs.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
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
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.service.CheckoutService;
import vn.fs.service.PaypalService;
import vn.fs.service.ShoppingCartService;
import vn.fs.service.impl.ExchangeRateService;
import vn.fs.util.Utils;

@Controller
@RequiredArgsConstructor
public class CartController extends CommomController {

    private final HttpSession session;

    private final CommomDataService commomDataService;
    private final ShoppingCartService shoppingCartService;
    private final PaypalService paypalService;
    private final ExchangeRateService exchangeRateService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CheckoutService checkoutService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String URL_PAYPAL_SUCCESS = "pay/success";
    public static final String URL_PAYPAL_CANCEL  = "pay/cancel";

    // lấy user đăng nhập (email)
    private User me() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || a instanceof AnonymousAuthenticationToken) return null;
        return userRepository.findByEmail(a.getName());
    }

    // bind giỏ hàng
    private void bindCart(Model model) {
        Collection<CartItem> items = shoppingCartService.getCartItems();
        double amount = shoppingCartService.getAmount();
        model.addAttribute("cartItems", items);
        model.addAttribute("total", amount);
        model.addAttribute("totalPrice", amount);
        model.addAttribute("totalCartItems", shoppingCartService.getDistinctCount());
        model.addAttribute("totalCartQtySum", shoppingCartService.getQuantitySum());
    }

    /* ===== PAGES ===== */
    @GetMapping("/shoppingCart_checkout")
    public String shoppingCart(Model model) {
        bindCart(model);
        User u = me();
        commomDataService.commonData(model, u);

        if (!model.containsAttribute("checkout")) {
            CheckoutAddressDTO dto = new CheckoutAddressDTO();
            if (u != null) {
                dto.setEmail(u.getEmail());
                dto.setFullName(u.getName());
                dto.setPhone(u.getPhone());
            }
            model.addAttribute("checkout", dto);
        }
        return "web/shoppingCart_checkout";
    }

    @GetMapping("/checkout")
    public String checkOut(Model model) { return shoppingCart(model); }

    /* ===== CART OPS ===== */
    @GetMapping("/addToCart")
    public String add(@RequestParam("productId") Long productId,
                      @RequestParam(value = "qty", defaultValue = "1") int qty) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p != null) shoppingCartService.addOrIncrease(productId, Math.max(1, qty));
        return "redirect:/products";
    }

    @GetMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id) {
        Product p = productRepository.findById(id).orElse(null);
        if (p != null) {
            CartItem it = new CartItem();
            BeanUtils.copyProperties(p, it);
            it.setProduct(p);
            it.setId(id);
            shoppingCartService.remove(it);
        }
        return "redirect:/checkout";
    }

    @GetMapping("/cart/inc")
    public String inc(@RequestParam Long productId,
                      @RequestParam(required=false) String redirect,
                      @RequestParam(required=false) String anchor) {
        shoppingCartService.increase(productId, 1);
        if ("checkout".equalsIgnoreCase(redirect)) return "redirect:/checkout" + (anchor != null ? "#" + anchor : "");
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
        if ("checkout".equalsIgnoreCase(redirect)) return "redirect:/checkout" + (anchor != null ? "#" + anchor : "");
        return "redirect:/products";
    }

    /* ===== CHECKOUT ===== */
    @PostMapping("/checkout")
    @Transactional
    public String checkedOut(@Valid @ModelAttribute("checkout") CheckoutAddressDTO checkout,
                             BindingResult br,
                             Model model,
                             HttpServletRequest request) throws MessagingException {

        Collection<CartItem> items = shoppingCartService.getCartItems();
        if (items == null || items.isEmpty()) return "redirect:/products";

        bindCart(model);
        commomDataService.commonData(model, me());
        if (br.hasErrors()) return "web/shoppingCart_checkout";

        double totalVnd = shoppingCartService.getAmount();
        String method = checkout.getPaymentMethod() == null ? "cod" : checkout.getPaymentMethod().trim().toLowerCase();

        // paypal → tạo payment và redirect
        if (StringUtils.equals(method, "paypal")) {
            Order draft = new Order();
            draft.setAddress(checkout.getAddress());
            draft.setPhone(checkout.getPhone());
            session.setAttribute("orderDraft", draft);

            String cancelUrl  = Utils.getBaseURL(request) + URL_PAYPAL_CANCEL;
            String successUrl = Utils.getBaseURL(request) + URL_PAYPAL_SUCCESS;
            try {
                double rate = exchangeRateService.getVNDExchangeRate(); // VND/1USD
                double usd = BigDecimal.valueOf(totalVnd / rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
                Payment payment = paypalService.createPayment(
                        usd, "USD",
                        PaypalPaymentMethod.paypal,
                        PaypalPaymentIntent.sale,
                        "Thanh toán bằng Paypal",
                        cancelUrl, successUrl
                );
                for (Links l : payment.getLinks()) {
                    if ("approval_url".equalsIgnoreCase(l.getRel())) return "redirect:" + l.getHref();
                }
                log.warn("approval_url not found");
                return "redirect:/checkout";
            } catch (PayPalRESTException e) {
                log.error("createPayment error", e);
                return "redirect:/checkout";
            }
        }

        // cod
        User u = me(); if (u == null) return "redirect:/login";

        Order order = checkoutService.createCOD(u, checkout, items, totalVnd);
        checkoutService.sendMail(u, items, totalVnd, order);

        shoppingCartService.clear();
        session.removeAttribute("cartItems");
        model.addAttribute("orderId", order.getOrderId());
        return "redirect:/checkout_success";
    }

    /* ===== PAYPAL RETURN ===== */
    @GetMapping(URL_PAYPAL_SUCCESS)
    public String successPay(@RequestParam("paymentId") String paymentId,
                             @RequestParam("PayerID") String payerId,
                             HttpServletRequest request, Model model) throws MessagingException {
        Collection<CartItem> items = shoppingCartService.getCartItems();
        double total = shoppingCartService.getAmount();
        bindCart(model);

        try {
            Payment payment = paypalService.executePayment(paymentId, payerId);
            if ("approved".equalsIgnoreCase(payment.getState())) {
                User u = me(); if (u == null) return "redirect:/login";
                Order draft = (Order) session.getAttribute("orderDraft");
                if (draft == null) draft = new Order();
                Order order = checkoutService.finalizePaypal(u, draft, items, total);
                checkoutService.sendMail(u, items, total, order);

                shoppingCartService.clear();
                session.removeAttribute("cartItems");
                session.removeAttribute("orderDraft");
                model.addAttribute("orderId", order.getOrderId());
                return "redirect:/checkout_paypal_success";
            }
        } catch (PayPalRESTException e) {
            log.error("executePayment error", e);
        }
        return "redirect:/";
    }

    /* ===== AFTER PAGES ===== */
    @GetMapping("/checkout_success")
    public String checkoutSuccess(Model model) {
        commomDataService.commonData(model, me());
        return "web/checkout_success";
    }

    @GetMapping("/checkout_paypal_success")
    public String paypalSuccess(Model model) {
        commomDataService.commonData(model, me());
        return "web/checkout_paypal_success";
    }

    @GetMapping("/pay/cancel")
    public String cancelPayment(@RequestParam(name = "token", required = false) String token, Model model) {
        commomDataService.commonData(model, me());
        model.addAttribute("message", "Bạn đã hủy thanh toán qua PayPal.");
        return "web/payment_cancel";
    }
}
