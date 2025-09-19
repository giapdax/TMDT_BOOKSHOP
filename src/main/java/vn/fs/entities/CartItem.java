package vn.fs.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * CartItem lưu trong bộ nhớ (per-user) – không map JPA.
 * Dùng Product để đọc tên/giá/discount cho đúng dữ liệu hiện tại.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;         // = productId
    private int quantity;    // số lượng trong giỏ
    private Product product; // tham chiếu nhanh

    /* ======= Helpers cho Thymeleaf/JSON ======= */

    public String getName() {
        return product != null ? product.getProductName() : null;
    }

    /** Đơn giá sau discount (nếu có) */
    public double getUnitPrice() {
        if (product == null) return 0.0;
        double p = product.getPrice();
        double discount = product.getDiscount() / 100.0;
        return p * (1 - discount);
    }

    /** Thành tiền dòng */
    public double getLineTotal() {
        return getUnitPrice() * Math.max(1, quantity);
    }
}
