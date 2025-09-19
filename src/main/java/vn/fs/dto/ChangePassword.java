package vn.fs.dto;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangePassword {

    @NotEmpty
    @Size(min = 12, max = 128, message = "Mật khẩu tối thiểu 12 ký tự.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,128}$",
            message = "Phải có chữ cái, số, ký tự đặc biệt và không khoảng trắng."
    )
    private String newPassword;

    @NotEmpty
    @Size(min = 12, max = 128, message = "Mật khẩu tối thiểu 12 ký tự.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,128}$",
            message = "Phải có chữ cái, số, ký tự đặc biệt và không khoảng trắng."
    )
    private String confirmPassword;
}
