package vn.fs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.fs.entities.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // List product by category
    @Query(value = "SELECT * FROM products WHERE category_id = ?", nativeQuery = true)
    List<Product> listProductByCategory(Long categoryId);

    // Top 10 product by category
    @Query(value = "SELECT * FROM products AS b WHERE b.category_id = ?;", nativeQuery = true)
    List<Product> listProductByCategory10(Long categoryId);

    // NEW ARRIVALS: 20 sp mới nhất
    @Query(value = "SELECT * FROM products ORDER BY entered_date DESC LIMIT 20;", nativeQuery = true)
    List<Product> listProductNew20();

    // Search Product
    @Query(value = "SELECT * FROM products WHERE product_name LIKE %?1%", nativeQuery = true)
    List<Product> searchProduct(String productName);

    // Đếm sp theo thể loại
    @Query(value = ""
            + "SELECT c.category_id, c.category_name, c.category_image, "
            + "       COUNT(p.product_id) AS so_luong "
            + "FROM categories c "
            + "LEFT JOIN products p ON p.category_id = c.category_id "
            + "GROUP BY c.category_id, c.category_name, c.category_image "
            + "ORDER BY c.category_name", nativeQuery = true)
    List<Object[]> listCategoryByProductName();

    // BEST SELLERS: trả (product_id, count) top 20
    @Query(value = "SELECT od.product_id, COUNT(*) AS SoLuong "
            + "FROM order_details od "
            + "JOIN products p ON od.product_id = p.product_id "
            + "GROUP BY od.product_id "
            + "ORDER BY SoLuong DESC LIMIT 20;", nativeQuery = true)
    List<Object[]> bestSaleProduct20();

    // FIX: dùng Long thay vì Integer
    @Query(value = "SELECT * FROM products WHERE product_id IN :ids", nativeQuery = true)
    List<Product> findByInventoryIds(@Param("ids") List<Long> listProductId);

    /* ========== Lọc theo NXB ========== */
    @Query(value = "SELECT * FROM products WHERE nxb_id = :nxbId", nativeQuery = true)
    List<Product> listProductByNxb(@Param("nxbId") Long nxbId);

    @Query(value = "SELECT * FROM products WHERE nxb_id = :nxbId LIMIT 10", nativeQuery = true)
    List<Product> listProductByNxb10(@Param("nxbId") Long nxbId);

    /* ========== STOCK ========== */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Product p SET p.quantity = p.quantity - :qty "
            + "WHERE p.productId = :productId AND p.quantity >= :qty")
    int decreaseStock(@Param("productId") Long productId, @Param("qty") int qty);

    /* ========== FEATURED: chọn theo % giảm giá cao nhất ========== */
    @Query(value = "SELECT * FROM products ORDER BY discount DESC, entered_date DESC LIMIT 20", nativeQuery = true)
    List<Product> topDiscount20();
}
