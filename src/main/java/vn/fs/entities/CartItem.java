package vn.fs.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dùng cho tầng controller/view.
 * Không có JPA annotation, KHÔNG map DB.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CartItem {

    private Long id;          // productId
    private String name;      // fallback nếu product null
    private double unitPrice; // fallback nếu product null
    private int quantity;
    private Product product;  // để view lấy ảnh/tên,... cho tiện

    public double getUnitPriceAfterDiscount() {
        if (product == null) return unitPrice;
        double price = product.getPrice();
        double discount = product.getDiscount() / 100.0;
        return price * (1.0 - discount);
    }

    public double getLineTotal() {
        return getUnitPriceAfterDiscount() * Math.max(1, quantity);
    }

    public String getDisplayName() {
        if (product != null && product.getProductName() != null) return product.getProductName();
        return name;
    }
}
