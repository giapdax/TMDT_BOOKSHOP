package vn.fs.controller;

import java.util.*;
import java.util.stream.Collectors;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;

import vn.fs.commom.CommomDataService;
import vn.fs.config.PaypalPaymentIntent;
import vn.fs.config.PaypalPaymentMethod;
import vn.fs.dto.CartUpdateResponse;
import vn.fs.entities.*;
import vn.fs.repository.*;
import vn.fs.service.PaypalService;
import vn.fs.service.ShoppingCartService;
import vn.fs.service.impl.ExchangeRateService;
import vn.fs.util.Utils;

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

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        String login = auth.getName();
        return userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .or(() -> userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login))
                .orElse(null);
    }

    private double unitPrice(Product p) {
        double price = p.getPrice();
        return price - (price * p.getDiscount() / 100.0);
    }

    private String vnd(double v) {
        return java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("vi", "VN")).format(v);
    }

    private double calcTotal(Collection<CartItem> cartItems) {
        return cartItems == null ? 0.0 : cartItems.stream().mapToDouble(CartItem::getLineTotal).sum();
    }

    private void bindCartToModel(Model model) {
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", shoppingCartService.getAmount());
        model.addAttribute("totalPrice", calcTotal(cartItems));
        model.addAttribute("totalCartItems", shoppingCartService.getCount());
    }

    private List<CartUpdateResponse.MiniItem> buildMini() {
        return shoppingCartService.getCartItems().stream().map(ci -> {
            CartUpdateResponse.MiniItem m = new CartUpdateResponse.MiniItem();
            m.setProductId(ci.getId());
            m.setName(ci.getName());
            m.setImage(ci.getProduct().getProductImage());
            m.setQty(ci.getQuantity());
            m.setLineTotalText(vnd(ci.getLineTotal()));
            return m;
        }).collect(Collectors.toList());
    }

    /* ============================ Pages ============================ */

    @GetMapping("/shoppingCart_checkout")
    public String shoppingCart(Model model, @RequestParam(value = "err", required = false) String err) {
        bindCartToModel(model);
        if (StringUtils.isNotBlank(err)) model.addAttribute("cartError", err);
        return "web/shoppingCart_checkout";
    }

    /* ====================== Redirect Cart Actions (fallback) ====================== */

    @GetMapping("/addToCart")
    public String add(@RequestParam("productId") Long productId,
                      @RequestParam(value = "qty", required = false, defaultValue = "1") int qty,
                      HttpServletRequest request,
                      RedirectAttributes ra) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || Boolean.FALSE.equals(product.getStatus())) {
            ra.addAttribute("err", "Sản phẩm không khả dụng.");
            return "redirect:/shoppingCart_checkout";
        }
        if (qty < 1) qty = 1;

        CartItem existed = shoppingCartService.getItem(productId);
        int existedQty = existed == null ? 0 : existed.getQuantity();
        if (product.getQuantity() < existedQty + qty) {
            ra.addAttribute("err", "Vượt tồn kho! Hiện còn: " + product.getQuantity());
            return "redirect:/shoppingCart_checkout";
        }

        CartItem item = new CartItem();
        BeanUtils.copyProperties(product, item);
        item.setQuantity(qty);
        item.setProduct(product);
        item.setId(productId);
        shoppingCartService.add(item);

        session = request.getSession();
        session.setAttribute("cartItems", shoppingCartService.getCartItems());
        ra.addAttribute("err", "");
        return "redirect:/shoppingCart_checkout";
    }

    @GetMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            CartItem item = new CartItem();
            BeanUtils.copyProperties(product, item);
            item.setProduct(product);
            item.setId(id);
            shoppingCartService.remove(item);
        }
        ra.addAttribute("err", "");
        return "redirect:/shoppingCart_checkout";
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@RequestParam("productId") Long productId,
                                 @RequestParam("quantity") int quantity,
                                 RedirectAttributes ra) {
        if (quantity < 1) quantity = 1;
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) {
            ra.addAttribute("err", "Không tìm thấy sản phẩm.");
            return "redirect:/shoppingCart_checkout";
        }
        if (quantity > p.getQuantity()) {
            ra.addAttribute("err", "Vượt tồn kho! Hiện còn: " + p.getQuantity());
            return "redirect:/shoppingCart_checkout";
        }
        shoppingCartService.updateQuantity(productId, quantity);
        ra.addAttribute("err", "");
        return "redirect:/shoppingCart_checkout";
    }

    @PostMapping("/cart/increase")
    public String increase(@RequestParam("productId") Long productId, RedirectAttributes ra) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) {
            ra.addAttribute("err", "Không tìm thấy sản phẩm.");
            return "redirect:/shoppingCart_checkout";
        }
        CartItem ci = shoppingCartService.getItem(productId);
        int next = (ci == null ? 1 : ci.getQuantity() + 1);
        if (next > p.getQuantity()) {
            ra.addAttribute("err", "Vượt tồn kho! Hiện còn: " + p.getQuantity());
            return "redirect:/shoppingCart_checkout";
        }
        shoppingCartService.increase(productId, 1);
        ra.addAttribute("err", "");
        return "redirect:/shoppingCart_checkout";
    }

    @PostMapping("/cart/decrease")
    public String decrease(@RequestParam("productId") Long productId, RedirectAttributes ra) {
        shoppingCartService.decrease(productId, 1);
        ra.addAttribute("err", "");
        return "redirect:/shoppingCart_checkout";
    }

    /* ============================ AJAX CART API ============================ */

    @PostMapping(value="/api/cart/add", produces="application/json")
    @ResponseBody
    public ResponseEntity<CartUpdateResponse> apiAdd(@RequestParam Long productId,
                                                     @RequestParam(defaultValue="1") int qty) {
        User user = currentUser();
        if (user == null) {
            return ResponseEntity.ok(CartUpdateResponse.builder()
                    .success(false).message("Vui lòng đăng nhập để thêm giỏ hàng.")
                    .build());
        }
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null || Boolean.FALSE.equals(p.getStatus())) {
            return ResponseEntity.ok(CartUpdateResponse.builder()
                    .success(false).message("Sản phẩm không khả dụng.").build());
        }
        if (qty < 1) qty = 1;

        CartItem existed = shoppingCartService.getItem(productId);
        int had = existed == null ? 0 : existed.getQuantity();
        if (p.getQuantity() < had + qty) {
            return ResponseEntity.ok(CartUpdateResponse.builder()
                    .success(false)
                    .message("Vượt tồn kho! Hiện còn: " + p.getQuantity())
                    .productId(productId)
                    .itemQty(had)
                    .remainingStock(Math.max(0, p.getQuantity() - had))
                    .unitPrice(unitPrice(p))
                    .lineTotal(existed == null ? 0 : existed.getLineTotal())
                    .totalCartItems(shoppingCartService.getCount())
                    .cartTotal(shoppingCartService.getAmount())
                    .cartTotalText(vnd(shoppingCartService.getAmount()))
                    .build());
        }

        CartItem item = new CartItem();
        BeanUtils.copyProperties(p, item);
        item.setId(productId);
        item.setProduct(p);
        item.setQuantity(qty);
        shoppingCartService.add(item);

        CartItem now = shoppingCartService.getItem(productId);
        double total = shoppingCartService.getAmount();

        CartUpdateResponse res = CartUpdateResponse.builder()
                .success(true)
                .message("Đã thêm vào giỏ!")
                .productId(productId)
                .itemQty(now.getQuantity())
                .remainingStock(p.getQuantity() - now.getQuantity())
                .unitPrice(unitPrice(p))
                .lineTotal(now.getLineTotal())
                .totalCartItems(shoppingCartService.getCount())
                .cartTotal(total)
                .unitPriceText(vnd(unitPrice(p)))
                .lineTotalText(vnd(now.getLineTotal()))
                .cartTotalText(vnd(total))
                .mini(buildMini())
                .build();
        return ResponseEntity.ok(res);
    }

    @PostMapping(value="/api/cart/increase", produces="application/json")
    @ResponseBody
    public ResponseEntity<CartUpdateResponse> apiIncrease(@RequestParam Long productId) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) return ResponseEntity.ok(CartUpdateResponse.builder().success(false).message("Không tìm thấy sản phẩm.").build());

        CartItem ci = shoppingCartService.getItem(productId);
        int next = (ci == null ? 1 : ci.getQuantity() + 1);
        if (next > p.getQuantity()) {
            return ResponseEntity.ok(CartUpdateResponse.builder()
                    .success(false).message("Vượt tồn kho! Hiện còn: " + p.getQuantity())
                    .productId(productId)
                    .itemQty(ci == null ? 0 : ci.getQuantity())
                    .remainingStock(Math.max(0, p.getQuantity() - (ci == null ? 0 : ci.getQuantity())))
                    .build());
        }
        shoppingCartService.increase(productId, 1);
        CartItem now = shoppingCartService.getItem(productId);
        double total = shoppingCartService.getAmount();
        return ResponseEntity.ok(CartUpdateResponse.builder()
                .success(true)
                .productId(productId)
                .itemQty(now.getQuantity())
                .remainingStock(p.getQuantity() - now.getQuantity())
                .unitPrice(unitPrice(p))
                .lineTotal(now.getLineTotal())
                .totalCartItems(shoppingCartService.getCount())
                .cartTotal(total)
                .unitPriceText(vnd(unitPrice(p)))
                .lineTotalText(vnd(now.getLineTotal()))
                .cartTotalText(vnd(total))
                .build());
    }

    @PostMapping(value="/api/cart/decrease", produces="application/json")
    @ResponseBody
    public ResponseEntity<CartUpdateResponse> apiDecrease(@RequestParam Long productId) {
        CartItem ci = shoppingCartService.getItem(productId);
        if (ci == null) return ResponseEntity.ok(CartUpdateResponse.builder().success(false).message("Không có trong giỏ.").build());
        shoppingCartService.decrease(productId, 1);
        CartItem now = shoppingCartService.getItem(productId);
        Product p = ci.getProduct();
        double total = shoppingCartService.getAmount();
        return ResponseEntity.ok(CartUpdateResponse.builder()
                .success(true)
                .productId(productId)
                .itemQty(now.getQuantity())
                .remainingStock(p.getQuantity() - now.getQuantity())
                .unitPrice(unitPrice(p))
                .lineTotal(now.getLineTotal())
                .totalCartItems(shoppingCartService.getCount())
                .cartTotal(total)
                .unitPriceText(vnd(unitPrice(p)))
                .lineTotalText(vnd(now.getLineTotal()))
                .cartTotalText(vnd(total))
                .build());
    }

    @PostMapping(value="/api/cart/set", produces="application/json")
    @ResponseBody
    public ResponseEntity<CartUpdateResponse> apiSet(@RequestParam Long productId,
                                                     @RequestParam int qty) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) return ResponseEntity.ok(CartUpdateResponse.builder().success(false).message("Không tìm thấy sản phẩm.").build());
        if (qty < 1) qty = 1;
        if (qty > p.getQuantity()) {
            CartItem ci = shoppingCartService.getItem(productId);
            int cur = ci == null ? 0 : ci.getQuantity();
            return ResponseEntity.ok(CartUpdateResponse.builder()
                    .success(false).message("Vượt tồn kho! Hiện còn: " + p.getQuantity())
                    .productId(productId)
                    .itemQty(cur)
                    .remainingStock(Math.max(0, p.getQuantity() - cur))
                    .build());
        }
        shoppingCartService.updateQuantity(productId, qty);
        CartItem now = shoppingCartService.getItem(productId);
        double total = shoppingCartService.getAmount();
        return ResponseEntity.ok(CartUpdateResponse.builder()
                .success(true)
                .productId(productId)
                .itemQty(now.getQuantity())
                .remainingStock(p.getQuantity() - now.getQuantity())
                .unitPrice(unitPrice(p))
                .lineTotal(now.getLineTotal())
                .totalCartItems(shoppingCartService.getCount())
                .cartTotal(total)
                .unitPriceText(vnd(unitPrice(p)))
                .lineTotalText(vnd(now.getLineTotal()))
                .cartTotalText(vnd(total))
                .build());
    }

    @PostMapping(value="/api/cart/remove", produces="application/json")
    @ResponseBody
    public ResponseEntity<CartUpdateResponse> apiRemove(@RequestParam Long productId) {
        CartItem ci = shoppingCartService.getItem(productId);
        if (ci != null) shoppingCartService.remove(ci);
        double total = shoppingCartService.getAmount();
        return ResponseEntity.ok(CartUpdateResponse.builder()
                .success(true)
                .productId(productId)
                .itemQty(0)
                .totalCartItems(shoppingCartService.getCount())
                .cartTotal(total)
                .cartTotalText(vnd(total))
                .mini(buildMini())
                .build());
    }

    @GetMapping(value="/api/cart/mini", produces="application/json")
    @ResponseBody
    public ResponseEntity<CartUpdateResponse> apiMini() {
        double total = shoppingCartService.getAmount();
        return ResponseEntity.ok(CartUpdateResponse.builder()
                .success(true)
                .totalCartItems(shoppingCartService.getCount())
                .cartTotal(total)
                .cartTotalText(vnd(total))
                .mini(buildMini())
                .build());
    }

    /* ============================ Checkout ============================ */

    @GetMapping("/checkout")
    public String checkOut(Model model, @RequestParam(value = "err", required = false) String err) {
        model.addAttribute("order", new Order());
        bindCartToModel(model);
        if (StringUtils.isNotBlank(err)) model.addAttribute("cartError", err);
        commomDataService.commonData(model, currentUser());
        return "web/shoppingCart_checkout";
    }

    @PostMapping("/checkout")
    @Transactional
    public String checkedOut(Model model, Order order, HttpServletRequest request, RedirectAttributes ra)
            throws MessagingException {
        String checkOut = request.getParameter("checkOut");
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/products";
        }

        // Validate tồn lần cuối trước khi chốt
        for (CartItem ci : cartItems) {
            Product p = productRepository.findById(ci.getProduct().getProductId()).orElse(null);
            if (p == null || ci.getQuantity() > p.getQuantity()) {
                ra.addAttribute("err", "Sản phẩm '" + (p != null ? p.getProductName() : "#") +
                        "' không đủ tồn. Còn: " + (p == null ? 0 : p.getQuantity()));
                return "redirect:/shoppingCart_checkout";
            }
        }

        double totalPrice = calcTotal(cartItems);

        if (StringUtils.equalsIgnoreCase(checkOut, "paypal")) {
            // Lưu order draft chờ callback
            Order draft = new Order();
            BeanUtils.copyProperties(order, draft);
            session.setAttribute("orderDraft", draft);

            String cancelUrl  = Utils.getBaseURL(request) + "/" + URL_PAYPAL_CANCEL;
            String successUrl = Utils.getBaseURL(request) + "/" + URL_PAYPAL_SUCCESS;
            try {
                double rateVNDToUSD = exchangeRateService.getVNDExchangeRate();
                double usdAmount = totalPrice / rateVNDToUSD;

                Payment payment = paypalService.createPayment(
                        usdAmount, "USD",
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
                ra.addAttribute("err", "Không tạo được phiên thanh toán PayPal.");
                return "redirect:/shoppingCart_checkout";
            }
        }

        // === COD flow ===
        User cur = currentUser();
        if (cur == null) return "redirect:/login";

        order.setOrderDate(new Date());
        order.setStatus(0); // pending
        order.setAmount(totalPrice);
        order.setUser(cur);
        orderRepository.save(order);

        for (CartItem ci : cartItems) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(order);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);
        }

        // Trừ tồn kho (atomic), thiếu -> rollback
        reduceInventoryOrThrow(cartItems);

        // Email + dọn giỏ
        commomDataService.sendSimpleEmail(
                cur.getEmail(), "Book-Shop Xác Nhận Đơn hàng",
                "CONFIRM", cartItems, totalPrice, order
        );

        shoppingCartService.clear();
        session.removeAttribute("cartItems");
        model.addAttribute("orderId", order.getOrderId());
        return "redirect:/checkout_success";
    }

    @GetMapping(URL_PAYPAL_SUCCESS)
    @Transactional
    public String successPay(@RequestParam("paymentId") String paymentId,
                             @RequestParam("PayerID") String payerId,
                             HttpServletRequest request, Model model,
                             RedirectAttributes ra) throws MessagingException {

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

                Order draft = (Order) session.getAttribute("orderDraft");
                if (draft == null) draft = new Order();

                draft.setOrderDate(new Date());
                draft.setStatus(2); // paid
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

                // Trừ tồn kho (atomic), thiếu -> rollback
                reduceInventoryOrThrow(cartItems);

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
        } catch (Exception e) {
            log.error("PayPal success/stock error", e);
            ra.addAttribute("err", "Thanh toán thành công nhưng trừ kho thất bại. Vui lòng liên hệ CSKH.");
            return "redirect:/shoppingCart_checkout";
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
    public String cancelPayment(@RequestParam("token") String token, Model model) {
        commomDataService.commonData(model, currentUser());
        model.addAttribute("message", "Bạn đã hủy thanh toán qua PayPal.");
        return "web/payment_cancel";
    }

    /* ============================ STOCK CORE ============================ */

    /** Trừ kho atomic bằng UPDATE ... WHERE quantity >= ?; thiếu -> throw để rollback. */
    private void reduceInventoryOrThrow(Collection<CartItem> cartItems) {
        for (CartItem ci : cartItems) {
            Long pid = ci.getProduct().getProductId();
            int need = ci.getQuantity();
            int updated = productRepository.decreaseStock(pid, need);
            if (updated == 0) {
                throw new IllegalStateException("Sản phẩm '" + ci.getProduct().getProductName()
                        + "' không đủ tồn khi chốt đơn.");
            }
        }
    }
}
