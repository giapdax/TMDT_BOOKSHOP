package vn.fs.entities;

import javax.persistence.*;
import javax.validation.constraints.*;
import javax.validation.constraints.AssertTrue;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;

@SuppressWarnings("serial")
@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_user_email", columnNames = "email"),
                @UniqueConstraint(name = "ux_user_username", columnNames = "username")
        }
)
public class User implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    // Username: dưới 50 ký tự (tối đa 49), chỉ chữ & số, không khoảng trắng/ký tự đặc biệt
    @NotBlank(message = "Vui lòng nhập username.")
    @Size(min = 1, max = 49, message = "Username tối đa 49 ký tự.")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Username chỉ gồm chữ và số, không khoảng trắng/ký tự đặc biệt.")
    @Column(length = 50)
    private String username;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String email;

    // Họ tên: chỉ chữ (có dấu) + khoảng trắng
    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(min = 2, max = 50, message = "Tên từ 2-50 ký tự.")
    @Pattern(regexp = "^[A-Za-zÀ-ỹĐđ ]{2,50}$", message = "Tên chỉ chứa chữ cái (có dấu) và khoảng trắng.")
    @Column(length = 50)
    private String name;

    /** Mật khẩu (BCrypt). ≥12 ký tự, có chữ cái, số, ký tự đặc biệt, không khoảng trắng */
    @NotBlank
    @Size(min = 12, max = 128, message = "Mật khẩu tối thiểu 12 ký tự.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,128}$",
            message = "Mật khẩu phải có chữ cái, số và ký tự đặc biệt, không khoảng trắng."
    )
    @Column(nullable = false)
    private String password;

    // Xác nhận mật khẩu: chỉ dùng để validate, không lưu DB
    @Transient
    private String confirmPassword;

    // SĐT Việt Nam (di động): 0|+84 + (3|5|7|8|9) + 8 số
    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(
            regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$",
            message = "Số điện thoại VN không hợp lệ. Ví dụ: 090xxxxxxx hoặc +843xxxxxxx"
    )
    @Column(length = 20)
    private String phone;

    private String avatar;

    @Temporal(TemporalType.DATE)
    private Date registerDate;

    private Boolean status;

    @Column(nullable = false)
    private int failedAttempt = 0;

    private LocalDateTime lockedUntil;
    private LocalDateTime lastLoginAt;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
            name = "users_roles",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "userId"),
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")
    )
    private Collection<Role> roles;

    public User() {}

    // ===== Cross-field validation: password == confirmPassword =====
    @AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp.")
    public boolean isPasswordConfirmed() {
        // Nếu một trong hai trống thì để controller xử lý; ở đây chỉ check khi cả hai có giá trị
        if (password == null || confirmPassword == null) return true;
        return password.equals(confirmPassword);
    }

    // ===== Getters/Setters =====
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = (username == null) ? null : username.trim(); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = (email == null) ? null : email.trim().toLowerCase(); }

    public String getName() { return name; }
    public void setName(String name) { this.name = (name == null) ? null : name.trim(); }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = (phone == null) ? null : phone.trim(); }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    public Date getRegisterDate() { return registerDate; }
    public void setRegisterDate(Date registerDate) { this.registerDate = registerDate; }

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public int getFailedAttempt() { return failedAttempt; }
    public void setFailedAttempt(int failedAttempt) { this.failedAttempt = failedAttempt; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Collection<Role> getRoles() { return roles; }
    public void setRoles(Collection<Role> roles) { this.roles = roles; }
}
