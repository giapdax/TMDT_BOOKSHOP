package vn.fs.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartUpdateResponse {
    private boolean success;
    private String message;

    private Long productId;
    private int itemQty;             // số lượng của item này trong giỏ sau cập nhật
    private Integer remainingStock;  // tồn còn lại (optional)

    private Double unitPrice;        // đơn giá sau giảm
    private Double lineTotal;        // thành tiền dòng
    private Double cartTotal;        // tổng giỏ

    private Integer totalCartItems;  // số dòng trong giỏ (không phải tổng qty)
    private String unitPriceText;    // text VND
    private String lineTotalText;    // text VND
    private String cartTotalText;    // text VND

    private List<MiniItem> mini;     // danh sách rút gọn để render mini-cart

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniItem {
        private Long productId;
        private String name;
        private String image;
        private int qty;
        private String lineTotalText;
        private String remainingStockText; // optional
    }
}
