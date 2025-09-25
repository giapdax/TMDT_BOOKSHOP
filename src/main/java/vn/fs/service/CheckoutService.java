package vn.fs.service;

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

import javax.mail.MessagingException;
import java.util.Collection;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final CommomDataService commomDataService;

    // tạo đơn COD
    @Transactional
    public Order createCOD(User user, CheckoutAddressDTO dto, Collection<CartItem> items, double total) {
        Order o = new Order();
        o.setOrderDate(new Date());
        o.setStatus(0); // pending
        o.setAmount(total);
        o.setUser(user);
        o.setAddress(dto.getAddress());
        o.setPhone(dto.getPhone());
        orderRepository.save(o);

        for (CartItem ci : items) {
            OrderDetail d = new OrderDetail();
            d.setOrder(o);
            d.setProduct(ci.getProduct());
            d.setQuantity(ci.getQuantity());
            d.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(d);
        }
        return o;
    }

    // hoàn tất PayPal
    @Transactional
    public Order finalizePaypal(User user, Order draft, Collection<CartItem> items, double total) {
        draft.setOrderDate(new Date());
        draft.setStatus(2); // paid
        draft.setUser(user);
        draft.setAmount(total);
        orderRepository.save(draft);

        for (CartItem ci : items) {
            OrderDetail d = new OrderDetail();
            d.setOrder(draft);
            d.setProduct(ci.getProduct());
            d.setQuantity(ci.getQuantity());
            d.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(d);
        }
        return draft;
    }

    // gửi mail
    public void sendMail(User user, Collection<CartItem> items, double total, Order order) throws MessagingException {
        commomDataService.sendSimpleEmail(
                user.getEmail(),
                "Book-Shop Xác Nhận Đơn hàng",
                "CONFIRM",
                items,
                total,
                order
        );
    }
}
