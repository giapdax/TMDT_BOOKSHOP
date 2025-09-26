package vn.fs.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;
import vn.fs.entities.Product;
import vn.fs.repository.OrderDetailRepository;
import vn.fs.repository.OrderRepository;
import vn.fs.repository.ProductRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    // list
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    // chi tiết
    public List<OrderDetail> detailsOf(Long orderId) {
        return orderDetailRepository.findByOrderId(orderId);
    }

    // tổng tiền
    public Double amountOf(Long orderId) {
        return orderRepository.findById(orderId).map(Order::getAmount).orElse(0d);
    }

    // tìm đơn
    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    // huỷ
    @Transactional
    public boolean cancel(Long id) {
        return orderRepository.findById(id).map(o -> {
            o.setStatus(3); // huỷ
            orderRepository.save(o);
            return true;
        }).orElse(false);
    }

    // xác nhận
    @Transactional
    public boolean confirm(Long id) {
        return orderRepository.findById(id).map(o -> {
            o.setStatus(1); // xác nhận
            orderRepository.save(o);
            return true;
        }).orElse(false);
    }

    // đã giao + trừ kho
    @Transactional
    public boolean delivered(Long id) {
        return orderRepository.findById(id).map(o -> {
            o.setStatus(2); // đã giao/đã thanh toán
            orderRepository.save(o);

            List<OrderDetail> items = orderDetailRepository.findByOrderId(id);
            for (OrderDetail od : items) {
                Product p = od.getProduct();
                if (p == null) continue;
                p.setQuantity(p.getQuantity() - od.getQuantity()); // giữ logic cũ
                productRepository.save(p);
            }
            return true;
        }).orElse(false);
    }
}
