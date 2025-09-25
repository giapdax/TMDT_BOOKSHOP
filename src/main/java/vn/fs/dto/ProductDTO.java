package vn.fs.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.*;
import java.util.Date;

/**
 * DTO cho thao tác thêm/sửa Product.
 * - Tất cả validate đặt ở đây (KHÔNG validate trong Entity).
 * - Lưu tên ảnh (kể cả ảnh tạm __tmp__) trong productImage để không mất ảnh khi form lỗi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long productId; // dùng khi edit

    @NotBlank(message = "Tên sản phẩm không được để trống.")
    @Size(min = 2, max = 255, message = "Tên sản phẩm phải từ 2 đến 255 ký tự.")
    private String productName;

    @NotNull(message = "Vui lòng chọn thể loại.")
    private Long categoryId;

    // Có thể null (không bắt buộc)
    private Long nxbId;

    @NotNull(message = "Vui lòng nhập đơn giá.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Đơn giá không được âm.")
    @Digits(integer = 14, fraction = 2, message = "Đơn giá tối đa 14 số nguyên và 2 số lẻ.")
    private Double price;

    @NotNull(message = "Vui lòng nhập số lượng.")
    @Min(value = 0, message = "Số lượng không được âm.")
    private Integer quantity;

    @NotNull(message = "Vui lòng nhập mức giảm giá.")
    @Min(value = 0, message = "Giảm giá không được âm.")
    @Max(value = 90, message = "Giảm giá tối đa 90%.")
    private Integer discount;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự.")
    private String description;

    @NotNull(message = "Vui lòng chọn ngày nhập.")
    @PastOrPresent(message = "Ngày nhập không được vượt quá hôm nay.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date enteredDate;

    @NotNull(message = "Vui lòng chọn trạng thái hiển thị.")
    private Boolean status;

    /** Giữ tên ảnh hiện tại hoặc ảnh tạm (__tmp__...) để không bị mất khi form lỗi */
    private String productImage;

    /** Chuẩn hoá chuỗi để hạn chế trùng do khoảng trắng */
    public void normalize() {
        if (productName != null) productName = productName.replaceAll("\\s+", " ").trim();
        if (description != null) description = description.trim();
    }
}
