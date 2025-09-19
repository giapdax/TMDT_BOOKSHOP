package vn.fs.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.fs.entities.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {

    @Query(value = "select * from order_details where order_id = ?1", nativeQuery = true)
    List<OrderDetail> findByOrderId(Long orderId);

    // ===== THỐNG KÊ THEO SẢN PHẨM =====
    @Query(value =
            "SELECT p.product_name, " +
                    "       SUM(o.quantity)           AS quantity, " +
                    "       SUM(o.quantity * o.price) AS sum, " +
                    "       AVG(o.price)              AS avg, " +
                    "       MIN(o.price)              AS min, " +
                    "       MAX(o.price)              AS max " +
                    "FROM order_details o " +
                    "JOIN products p ON o.product_id = p.product_id " +
                    "GROUP BY p.product_name", nativeQuery = true)
    List<Object[]> repo();

    // ===== THỐNG KÊ THEO THỂ LOẠI =====
    @Query(value =
            "SELECT c.category_name, " +
                    "       SUM(o.quantity)           AS quantity, " +
                    "       SUM(o.quantity * o.price) AS sum, " +
                    "       AVG(o.price)              AS avg, " +
                    "       MIN(o.price)              AS min, " +
                    "       MAX(o.price)              AS max " +
                    "FROM order_details o " +
                    "JOIN products p   ON o.product_id = p.product_id " +
                    "JOIN categories c ON p.category_id = c.category_id " +
                    "GROUP BY c.category_name", nativeQuery = true)
    List<Object[]> repoWhereCategory();

    // ===== THỐNG KÊ THEO NĂM =====
    @Query(value =
            "SELECT YEAR(od.order_date)       AS y, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "GROUP BY YEAR(od.order_date)", nativeQuery = true)
    List<Object[]> repoWhereYear();

    // ===== THỐNG KÊ THEO THÁNG =====
    @Query(value =
            "SELECT MONTH(od.order_date)      AS m, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "GROUP BY MONTH(od.order_date)", nativeQuery = true)
    List<Object[]> repoWhereMonth();

    // ===== THỐNG KÊ THEO QUÝ =====
    @Query(value =
            "SELECT QUARTER(od.order_date)    AS q, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "GROUP BY QUARTER(od.order_date)", nativeQuery = true)
    List<Object[]> repoWhereQUARTER();

    // ===== THỐNG KÊ THEO KHÁCH HÀNG =====
    @Query(value =
            "SELECT u.user_id, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "JOIN user   u  ON od.user_id   = u.user_id " +
                    "GROUP BY u.user_id", nativeQuery = true)
    List<Object[]> reportCustomer();

    // ===== PHỤ TRỢ DASHBOARD =====

    @Query(value =
            "SELECT COALESCE(SUM(od.quantity), 0) " +
                    "FROM order_details od " +
                    "JOIN orders o ON o.order_id = od.order_id " +
                    "WHERE o.order_date >= :from " +
                    "  AND o.status IN (1,2,3)",
            nativeQuery = true)
    long totalSoldQtyFrom(@Param("from") LocalDate from);

    @Query(value =
            "SELECT DATE(o.order_date) AS d, COALESCE(SUM(od.quantity), 0) AS qty " +
                    "FROM order_details od " +
                    "JOIN orders o ON o.order_id = od.order_id " +
                    "WHERE o.order_date >= :from " +
                    "  AND o.status IN (1,2,3) " +
                    "GROUP BY DATE(o.order_date) " +
                    "ORDER BY d",
            nativeQuery = true)
    List<Object[]> soldQtyByDateFrom(@Param("from") LocalDate from);
}
