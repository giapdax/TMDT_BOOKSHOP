package vn.fs.service;

import com.paypal.api.payments.Links;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.PayPalRESTException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.commom.CommomDataService;
import vn.fs.dto.CheckoutAddressDTO;
import vn.fs.entities.CartItem;
import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;
import vn.fs.entities.User;
import vn.fs.repository.OrderDetailRepository;
import vn.fs.repository.OrderRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.service.impl.ExchangeRateService;
import vn.fs.util.Utils;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ShoppingCartService cart;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final ExchangeRateService exchangeRateService;
    private final PaypalService paypalService;
    private final CommomDataService commomDataService;

    /* ==================== PUBLIC API ==================== */

    /** Tạo thanh toán PayPal, return approval URL */
    public String beginPaypalCheckout(User user, CheckoutAddressDTO dto, HttpServletRequest req, HttpSession session)
            throws PayPalRESTException {
        // Lưu draft (địa chỉ, phone) vào session để dùng sau khi approve
        session.setAttribute("orderDraft", draftFrom(dto));

        double totalVnd = cart.getAmount();
        double rate = exchangeRateService.getVNDExchangeRate();
        double usdAmount = new BigDecimal(totalVnd / rate).setScale(2, RoundingMode.HALF_UP).doubleValue();

        String base = Utils.getBaseURL(req);
        String cancelUrl = base + "pay/cancel";
        String successUrl = base + "pay/success";

        Payment payment = paypalService.createPayment(
                usdAmount,
                "USD",
                vn.fs.config.PaypalPaymentMethod.paypal,
                vn.fs.config.PaypalPaymentIntent.sale,
                "Thanh toán bằng Paypal",
                cancelUrl, successUrl
        );

        for (Links l : payment.getLinks()) {
            if ("approval_url".equalsIgnoreCase(l.getRel())) {
                return l.getHref();
            }
        }
        return null;
    }

    /** Hoàn tất PayPal sau khi user approve */
    @Transactional
    public Long finalizePaypal(User user, String paymentId, String payerId, HttpSession session) throws Exception {
        Payment payment = paypalService.executePayment(paymentId, payerId);
        if (!"approved".equalsIgnoreCase(payment.getState())) {
            throw new IllegalStateException("Payment not approved");
        }
        return persistOrderAfterPaid(user, session, /*paidStatus*/ (short) 2);
    }

    /** Đặt hàng COD (pending), rollback nếu thiếu kho (nếu muốn) */
    @Transactional
    public Long placeOrderCOD(User user, CheckoutAddressDTO dto, HttpSession session) throws MessagingException {
        // Tạo order pending + persist item + (tuỳ bạn) trừ stock sau khi xác nhận giao
        Order order = createOrderSkeleton(user, dto, (short) 0, cart.getAmount());
        persistOrderDetailsAndDecreaseStockOrThrow(order); // Nếu muốn trừ kho ngay, dùng hàm này
        afterPersistSuccess(user, order, session);
        return order.getOrderId();
    }

    /* =============== INTERNAL BUSINESS HELPERS =============== */

    private Long persistOrderAfterPaid(User user, HttpSession session, short status) throws MessagingException {
        Collection<CartItem> items = cart.getCartItems();
        double total = cart.getAmount();

        Order draft = (Order) session.getAttribute("orderDraft");
        if (draft == null) draft = new Order();

        draft.setOrderDate(new Date());
        draft.setStatus(status);      // 2 = paid
        draft.setUser(user);
        draft.setAmount(total);
        orderRepository.save(draft);

        // ghi OrderDetail + trừ stock theo từng item (conditional)
        for (CartItem ci : items) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(draft);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);

            int updated = productRepository.decreaseStock(
                    ci.getProduct().getProductId(), ci.getQuantity());
            if (updated == 0) {
                // Thiếu kho -> throw để rollback toàn bộ giao dịch
                throw new IllegalStateException(
                        "Không đủ tồn kho cho productId=" + ci.getProduct().getProductId());
            }
        }

        afterPersistSuccess(user, draft, session);
        return draft.getOrderId();
    }

    private void afterPersistSuccess(User user, Order order, HttpSession session) throws MessagingException {
        Collection<CartItem> items = cart.getCartItems();
        double total = cart.getAmount();

        commomDataService.sendSimpleEmail(
                user.getEmail(),
                "Book-Shop Xác Nhận Đơn hàng",
                "CONFIRM",
                items,
                total,
                order
        );

        cart.clear();
        session.removeAttribute("cartItems");
        session.removeAttribute("orderDraft");
    }

    private Order createOrderSkeleton(User user, CheckoutAddressDTO dto, short status, double total) {
        Order order = new Order();
        order.setOrderDate(new Date());
        order.setStatus(status);
        order.setAmount(total);
        order.setUser(user);
        order.setAddress(dto.getAddress());
        order.setPhone(dto.getPhone());
        return orderRepository.save(order);
    }

    /** Ghi chi tiết & trừ stock; thiếu kho -> throw để rollback */
    private void persistOrderDetailsAndDecreaseStockOrThrow(Order order) {
        for (CartItem ci : cart.getCartItems()) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(order);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);

            int updated = productRepository.decreaseStock(
                    ci.getProduct().getProductId(), ci.getQuantity());
            if (updated == 0) {
                throw new IllegalStateException(
                        "Không đủ tồn kho cho productId=" + ci.getProduct().getProductId());
            }
        }
    }

    private Order draftFrom(CheckoutAddressDTO dto) {
        Order o = new Order();
        o.setAddress(dto.getAddress());
        o.setPhone(dto.getPhone());
        return o;
    }
}
