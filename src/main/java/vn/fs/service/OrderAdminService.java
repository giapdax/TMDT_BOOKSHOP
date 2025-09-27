package vn.fs.service;

import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface OrderAdminService {
    // list
    List<Order> listAll();

    // amount
    Optional<Double> amountOf(Long orderId);

    // chi tiết
    List<OrderDetail> detailsOf(Long orderId);

    // đổi trạng thái
    void cancel(Long orderId);    // 3
    void confirm(Long orderId);   // 1
    void deliveredAndDecreaseStock(Long orderId); // 2 + trừ kho

}
