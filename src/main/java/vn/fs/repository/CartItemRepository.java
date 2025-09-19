package vn.fs.repository;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.fs.entities.Cart;
import vn.fs.entities.CartItemEntity;
import vn.fs.entities.Product;

import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {

    List<CartItemEntity> findByCart(Cart cart);

    Optional<CartItemEntity> findByCartAndProduct(Cart cart, Product product);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItemEntity i WHERE i.lastTouch < :before")
    int deleteOldItems(@Param("before") Date before);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM CartItemEntity i WHERE i.cart = :cart")
    int deleteByCart(@Param("cart") Cart cart);
}
