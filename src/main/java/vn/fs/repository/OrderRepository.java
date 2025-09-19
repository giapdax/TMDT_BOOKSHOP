package vn.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fs.entities.Order;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = "select * from orders where user_id = ?1", nativeQuery = true)
    List<Order> findOrderByUserId(Long userId);

    /* ===== DỮ LIỆU DASHBOARD (tuỳ status “đã chốt” của bạn, mình giả định 1,2,3) ===== */

    // Tổng doanh thu theo ngày từ một mốc
    @Query(value =
            "select date(o.order_date) as d, coalesce(sum(o.amount),0) as v " +
                    "from orders o " +
                    "where o.order_date >= ?1 and o.status in (1,2,3) " +
                    "group by date(o.order_date) " +
                    "order by d", nativeQuery = true)
    List<Object[]> incomeByDateFrom(LocalDate from);

    // Số đơn theo ngày từ một mốc
    @Query(value =
            "select date(o.order_date) as d, count(*) as v " +
                    "from orders o " +
                    "where o.order_date >= ?1 and o.status in (1,2,3) " +
                    "group by date(o.order_date) " +
                    "order by d", nativeQuery = true)
    List<Object[]> ordersCountByDateFrom(LocalDate from);

    // Tổng số đơn từ một mốc (ví dụ 7 ngày)
    @Query(value =
            "select count(*) from orders o " +
                    "where o.order_date >= ?1 and o.status in (1,2,3)", nativeQuery = true)
    long countOrdersFrom(LocalDate from);
}
