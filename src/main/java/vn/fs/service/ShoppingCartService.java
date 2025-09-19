package vn.fs.service;

import org.springframework.stereotype.Service;
import vn.fs.entities.CartItem;
import vn.fs.entities.Product;

import java.util.Collection;

@Service
public interface ShoppingCartService {

    /** Tổng SỐ LƯỢNG (sum qty) của tất cả sản phẩm trong giỏ của USER hiện tại */
    int getCount();

    /** Tổng TIỀN giỏ của USER hiện tại */
    double getAmount();

    /** Xoá toàn bộ giỏ của USER hiện tại */
    void clear();

    /** Lấy các CartItem của USER hiện tại */
    Collection<CartItem> getCartItems();

    /** Thêm mới / cộng dồn */
    void add(CartItem item);

    /** Xoá theo CartItem */
    void remove(CartItem item);

    /** Xoá theo Product */
    void remove(Product product);

    /* Helpers cho thao tác nhanh */
    CartItem getItem(Long productId);
    void updateQuantity(Long productId, int quantity);
    void increase(Long productId, int step);
    void decrease(Long productId, int step);
}
