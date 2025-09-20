package vn.fs.service;

import vn.fs.entities.CartItem;
import vn.fs.entities.Product;

import java.util.Collection;

public interface ShoppingCartService {

    /** Tổng QUANTITY (cộng dồn) toàn giỏ */
    int getQuantitySum();

    /** Số lượng SẢN PHẨM KHÁC NHAU (distinct) trong giỏ */
    int getDistinctCount();

    /** Tổng tiền */
    double getAmount();

    /** Danh sách CartItem (DTO) để render UI */
    Collection<CartItem> getCartItems();

    /** Thêm (hoặc tăng) với kiểm tra tồn kho. Trả về số lượng thực tế sau khi thêm. */
    int addOrIncrease(Long productId, int addQty);

    /** Cập nhật quantity tuyệt đối với kiểm tra tồn kho. Trả về quantity thực tế sau khi set. */
    int updateQuantity(Long productId, int quantity);

    /** Tăng/giảm theo step với kiểm tra tồn kho. Trả về quantity thực tế sau khi tăng/giảm. */
    int increase(Long productId, int step);
    int decrease(Long productId, int step);

    /** Xoá theo product hoặc theo DTO */
    void remove(Product product);
    void remove(CartItem item);

    /** Lấy item theo productId (DTO) */
    CartItem getItem(Long productId);

    /** Xoá sạch giỏ */
    void clear();
    int decreaseOrRemove(Long productId, int step);
}
