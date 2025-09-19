package vn.fs.dto;

import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CartUpdateResponse {
    private boolean success;
    private String message;

    private Long productId;
    private int itemQty;            // qty hiện tại của sản phẩm trong giỏ
    private int remainingStock;     // tồn còn lại (sau khi trừ trong giỏ)
    private double unitPrice;       // đơn giá sau discount
    private double lineTotal;       // thành tiền của dòng

    private int totalCartItems;     // tổng số lượng tất cả dòng (sum qty)
    private double cartTotal;       // tổng tiền giỏ

    // Render nhanh cho client
    private String unitPriceText;
    private String lineTotalText;
    private String cartTotalText;

    // (optional) để re-render mini cart
    @Data
    public static class MiniItem {
        private Long productId;
        private String name;
        private String image;
        private int qty;
        private String lineTotalText;
    }
    private List<MiniItem> mini; // có thể null
}
