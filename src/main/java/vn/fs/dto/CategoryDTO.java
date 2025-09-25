package vn.fs.dto;

import lombok.*;
import javax.validation.constraints.*;

/**
 * DTO cho Category:
 * - Chứa toàn bộ validate.
 * - Giữ tên ảnh (kể cả file tạm __tmp__) để không mất ảnh khi form lỗi.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {

    private Long categoryId;

    @NotBlank(message = "Tên thể loại không được để trống.")
    @Size(min = 2, max = 255, message = "Tên thể loại phải từ 2 đến 255 ký tự.")
    private String categoryName;

    /** tên file ảnh (có thể là ảnh tạm __tmp__...) */
    private String categoryImage;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private Boolean status;

    /** Chuẩn hoá chuỗi để tránh lỗi trùng do khoảng trắng */
    public void normalize() {
        if (categoryName != null) {
            categoryName = categoryName.replaceAll("\\s+", " ").trim();
        }
    }
}
