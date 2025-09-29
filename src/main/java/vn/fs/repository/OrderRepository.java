package vn.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fs.entities.Order;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query(value = "select * from orders where user_id = ?1", nativeQuery = true)
    List<Order> findOrderByUserId(Long userId);

    // ====== Dashboard reports (giữ nguyên) ======
    @Query(value =
            "select date(o.order_date) as d, coalesce(sum(o.amount),0) as v " +
                    "from orders o " +
                    "where o.order_date >= ?1 and o.status in (1,2,3) " +
                    "group by date(o.order_date) " +
                    "order by d", nativeQuery = true)
    List<Object[]> incomeByDateFrom(LocalDate from);

    @Query(value =
            "select date(o.order_date) as d, count(*) as v " +
                    "from orders o " +
                    "where o.order_date >= ?1 and o.status in (1,2,3) " +
                    "group by date(o.order_date) " +
                    "order by d", nativeQuery = true)
    List<Object[]> ordersCountByDateFrom(LocalDate from);

    @Query(value =
            "select count(*) from orders o " +
                    "where o.order_date >= ?1 and o.status in (1,2,3)", nativeQuery = true)
    long countOrdersFrom(LocalDate from);

    // ====== NEW: Lấy tất cả đơn theo ngày mới -> cũ (cho trang mặc định) ======
    List<Order> findAllByOrderByOrderDateDesc();

    // ====== NEW: Lọc hiện đại (status, from/to, q, payment) và sort mới -> cũ ======
    // payment: null (tất cả) | 'PAYPAL' | 'COD'
    @Query(value =
            "SELECT o.* " +
                    "FROM orders o " +
                    "JOIN user u ON u.user_id = o.user_id " +
                    "WHERE (:status IS NULL OR o.status = :status) " +
                    "  AND (:from IS NULL OR DATE(o.order_date) >= :from) " +
                    "  AND (:to   IS NULL OR DATE(o.order_date) <= :to) " +
                    "  AND ( " +
                    "       :q IS NULL OR :q = '' OR " +
                    "       LOWER(COALESCE(u.name, ''))   LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "       LOWER(COALESCE(u.email, ''))  LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "       LOWER(COALESCE(o.phone, ''))  LIKE LOWER(CONCAT('%', :q, '%')) OR " +
                    "       LOWER(COALESCE(o.address,'')) LIKE LOWER(CONCAT('%', :q, '%')) " +
                    "  ) " +
                    "  AND ( " +
                    "       :payment IS NULL OR " +
                    "       (:payment = 'PAYPAL' AND o.paypal_capture_id IS NOT NULL) OR " +
                    "       (:payment = 'COD'    AND o.paypal_capture_id IS NULL) " +
                    "  ) " +
                    "ORDER BY o.order_date DESC",
            nativeQuery = true)
    List<Order> findAllFiltered(
            @Param("status") Integer status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("q") String q,
            @Param("payment") String payment
    );
}
