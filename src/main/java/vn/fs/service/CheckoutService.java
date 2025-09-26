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
    private final ExchangeRateService exchangeRateService;
    private final PaypalService paypalService;
    private final CommomDataService commomDataService;

    // COD
    @Transactional
    public Long placeOrderCOD(User user, CheckoutAddressDTO dto, HttpSession session) throws MessagingException {
        Collection<CartItem> items = cart.getCartItems();
        double total = cart.getAmount();

        Order order = new Order();
        order.setOrderDate(new Date());
        order.setStatus(0);
        order.setAmount(total);
        order.setUser(user);
        order.setAddress(dto.getAddress());
        order.setPhone(dto.getPhone());
        orderRepository.save(order);

        for (CartItem ci : items) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(order);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);
        }

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
        return order.getOrderId();
    }

    // tạo thanh toán PayPal, trả về approval url
    public String startPaypal(User user, CheckoutAddressDTO dto, HttpServletRequest req, HttpSession session) throws PayPalRESTException {
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

    // hoàn tất PayPal (approved)
    @Transactional
    public Long finalizePaypal(User user, String paymentId, String payerId, HttpSession session) throws Exception {
        Payment payment = paypalService.executePayment(paymentId, payerId);
        if (!"approved".equalsIgnoreCase(payment.getState())) {
            throw new IllegalStateException("Payment not approved");
        }

        Collection<CartItem> items = cart.getCartItems();
        double total = cart.getAmount();

        Order draft = (Order) session.getAttribute("orderDraft");
        if (draft == null) draft = new Order();
        draft.setOrderDate(new Date());
        draft.setStatus(2);
        draft.setUser(user);
        draft.setAmount(total);
        orderRepository.save(draft);

        for (CartItem ci : items) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(draft);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);
        }

        commomDataService.sendSimpleEmail(
                user.getEmail(),
                "Book-Shop Xác Nhận Đơn hàng",
                "CONFIRM",
                items,
                total,
                draft
        );

        cart.clear();
        session.removeAttribute("cartItems");
        session.removeAttribute("orderDraft");
        return draft.getOrderId();
    }

    // tạo draft từ DTO
    private Order draftFrom(CheckoutAddressDTO dto) {
        Order o = new Order();
        o.setAddress(dto.getAddress());
        o.setPhone(dto.getPhone());
        return o;
    }
}
