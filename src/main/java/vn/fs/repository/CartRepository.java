package vn.fs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.entities.Cart;
import vn.fs.entities.CartStatus;

import java.util.Date;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    // Lấy giỏ đang ACTIVE của user
    Optional<Cart> findByUser_UserIdAndStatus(Long userId, CartStatus status);

    /**
     * Đánh dấu các giỏ đã quá hạn thành EXPIRED
     */
    @Modifying
    @Transactional
    @Query("update Cart c set c.status = vn.fs.entities.CartStatus.EXPIRED " +
            "where c.expiresAt < :now and c.status = vn.fs.entities.CartStatus.ACTIVE")
    int markExpired(@Param("now") Date now);

    /**
     * Xoá các giỏ đã quá hạn và KHÔNG có item (tránh lỗi FK với cart_items)
     */
    @Modifying
    @Transactional
    @Query("delete from Cart c where c.expiresAt < :now and c.items is empty")
    int deleteExpired(@Param("now") Date now);
}
