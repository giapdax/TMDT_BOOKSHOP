package vn.fs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.fs.entities.Role;
import vn.fs.entities.User;
import vn.fs.repository.RoleRepository;
import vn.fs.repository.UserRepository;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.*;

@Controller
public class RegisterController {

    private static final String SESSION_PENDING_USER = "PENDING_USER";
    private static final String SESSION_OTP = "REGISTER_OTP";
    private static final String SESSION_OTP_EXPIRE = "REGISTER_OTP_EXPIRE";

    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // DÙNG CHÍNH JavaMailSender sẵn có trong dự án (không tạo service mới)
    @Autowired(required = false)
    private JavaMailSender mailSender;

    // ============ GET /register ============
    @GetMapping("/register")
    public String showRegister(Model model) {
        if (!model.containsAttribute("user")) {
            model.addAttribute("user", new User());
        }
        return "web/register";
    }

    // ============ POST /register ============
    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("user") User form,
                             BindingResult br,
                             Model model,
                             HttpSession session) {

        // Lỗi validate entity (email/username/name/phone/password/confirm)
        if (br.hasErrors()) {
            return "web/register";
        }

        // Check trùng email/username nếu repo có method (dùng reflection để khỏi lệ thuộc chữ ký)
        if (emailExists(form.getEmail())) {
            model.addAttribute("error", "Email đã tồn tại.");
            return "web/register";
        }
        if (usernameExists(form.getUsername())) {
            model.addAttribute("error", "Username đã tồn tại.");
            return "web/register";
        }

        // Tạo OTP 6 số + hạn 5 phút
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        long expireAt = Instant.now().plusSeconds(5 * 60).toEpochMilli();

        // LƯU TOÀN BỘ DỮ LIỆU FORM VÀO SESSION (CHƯA encode password)
        session.setAttribute(SESSION_PENDING_USER, form);
        session.setAttribute(SESSION_OTP, otp);
        session.setAttribute(SESSION_OTP_EXPIRE, expireAt);

        // GỬI EMAIL OTP (vẫn theo cơ chế cũ bằng JavaMailSender)
        try {
            if (mailSender == null) throw new IllegalStateException("MailSender bean not available");
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(form.getEmail());
            msg.setSubject("Mã xác nhận đăng ký (OTP) - BookShop");
            msg.setText("Xin chào,\n\nMã OTP của bạn là: " + otp + "\nHiệu lực trong 5 phút.\n\nTrân trọng,\nBookShop");
            mailSender.send(msg);
            model.addAttribute("message", "Mã xác nhận đã được gửi tới email của bạn.");
        } catch (Exception ex) {
            // Nếu gửi fail, dọn session và báo lỗi – tránh user kẹt 404
            clearRegisterSession(session);
            model.addAttribute("user", form);
            model.addAttribute("error", "Không thể gửi email OTP. Vui lòng kiểm tra cấu hình mail và thử lại!");
            return "web/register";
        }

        model.addAttribute("email", form.getEmail());
        return "web/confirmOtpRegister"; // trang nhập OTP
    }

    // ============ GET /confirmOtpRegister ============
    @GetMapping("/confirmOtpRegister")
    public String showConfirmOtp(Model model, HttpSession session) {
        User pending = (User) session.getAttribute(SESSION_PENDING_USER);
        if (pending == null) {
            model.addAttribute("user", new User());
            model.addAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "web/register";
        }
        model.addAttribute("email", pending.getEmail());
        return "web/confirmOtpRegister";
    }

    // ============ POST /confirmOtpRegister ============
    @PostMapping("/confirmOtpRegister")
    public String confirmRegister(@RequestParam("otp") String otpInput,
                                  Model model,
                                  HttpSession session) {

        User pending = (User) session.getAttribute(SESSION_PENDING_USER);
        String otp = (String) session.getAttribute(SESSION_OTP);
        Long expireAt = (Long) session.getAttribute(SESSION_OTP_EXPIRE);

        // Mất session -> quay lại form, tránh 404
        if (pending == null || otp == null || expireAt == null) {
            clearRegisterSession(session);
            model.addAttribute("user", new User());
            model.addAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "web/register";
        }

        // Hết hạn OTP
        if (Instant.now().toEpochMilli() > expireAt) {
            clearRegisterSession(session);
            model.addAttribute("user", new User());
            model.addAttribute("error", "Mã OTP đã hết hạn. Vui lòng đăng ký lại.");
            return "web/register";
        }

        // Sai OTP
        if (!otp.equals(otpInput.trim())) {
            model.addAttribute("email", pending.getEmail());
            model.addAttribute("error", "Mã OTP không chính xác!");
            return "web/confirmOtpRegister";
        }

        // OK -> encode password, gán role, lưu DB
        pending.setPassword(passwordEncoder.encode(pending.getPassword()));
        pending.setConfirmPassword(null); // tránh validate lại với chuỗi đã mã hoá
        pending.setRegisterDate(new Date());
        pending.setStatus(true);

        // ==> Avatar mặc định nếu chưa có
        if (pending.getAvatar() == null || pending.getAvatar().trim().isEmpty()) {
            pending.setAvatar("user.png"); // hình mặc định
        }

        // ==> ROLE mặc định = ROLE_USER
        Role roleUser = resolveUserRoleSafely();
        if (roleUser != null) {
            pending.setRoles(Collections.singletonList(roleUser));
        }

        userRepository.save(pending);
        clearRegisterSession(session);

        model.addAttribute("message", "Đăng ký & kích hoạt thành công. Hãy đăng nhập!");
        return "web/login";
    }

    // ----------------- Helpers -----------------

    private void clearRegisterSession(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_USER);
        session.removeAttribute(SESSION_OTP);
        session.removeAttribute(SESSION_OTP_EXPIRE);
    }

    /**
     * Tìm/tạo ROLE_USER nhưng KHÔNG phụ thuộc chữ ký repo:
     * 1) findByName("ROLE_USER")
     * 2) findByName("USER")
     * 3) findById(2) / findById(1)
     * 4) TẠO MỚI "ROLE_USER" nếu vẫn không có
     */
    @SuppressWarnings("unchecked")
    private Role resolveUserRoleSafely() {
        // 1) Ưu tiên "ROLE_USER"
        Role role = invokeRoleByName("ROLE_USER");
        if (role != null) return role;

        // 2) Thử "USER" (nếu DB bạn đang đặt thế)
        role = invokeRoleByName("USER");
        if (role != null) return role;

        // 3) Fallback theo id thông dụng
        try {
            Method m = roleRepository.getClass().getMethod("findById", Long.class);
            // thử id=2
            Object res = m.invoke(roleRepository, 2L);
            if (res instanceof Optional) {
                Optional<Role> o = (Optional<Role>) res;
                if (o.isPresent()) return o.get();
            } else if (res instanceof Role) {
                return (Role) res;
            }
            // thử id=1
            res = m.invoke(roleRepository, 1L);
            if (res instanceof Optional) {
                Optional<Role> o = (Optional<Role>) res;
                if (o.isPresent()) return o.get();
            } else if (res instanceof Role) {
                return (Role) res;
            }
        } catch (Throwable ignored) {}

        // 4) Không có thì TẠO MỚI "ROLE_USER"
        try {
            Role newRole = new Role();
            newRole.setName("ROLE_USER");
            return roleRepository.save(newRole);
        } catch (Throwable ignored) {}

        return null;
    }

    @SuppressWarnings("unchecked")
    private Role invokeRoleByName(String name) {
        try {
            Method m = roleRepository.getClass().getMethod("findByName", String.class);
            Object res = m.invoke(roleRepository, name);
            if (res == null) return null;
            if (res instanceof Optional) return ((Optional<Role>) res).orElse(null);
            return (Role) res;
        } catch (Throwable ignored) { return null; }
    }

    // Kiểm tra tồn tại email theo nhiều chữ ký repo thường gặp
    private boolean emailExists(String email) {
        try {
            Method exists = userRepository.getClass().getMethod("existsByEmailIgnoreCase", String.class);
            Object r = exists.invoke(userRepository, email);
            if (r instanceof Boolean && (Boolean) r) return true;
        } catch (Throwable ignored) {}
        try {
            Method exists = userRepository.getClass().getMethod("existsByEmail", String.class);
            Object r = exists.invoke(userRepository, email);
            if (r instanceof Boolean && (Boolean) r) return true;
        } catch (Throwable ignored) {}
        try {
            Method find = userRepository.getClass().getMethod("findByEmailIgnoreCase", String.class);
            Object r = find.invoke(userRepository, email);
            if (r instanceof Optional) return ((Optional<?>) r).isPresent();
            return r != null;
        } catch (Throwable ignored) {}
        try {
            Method find = userRepository.getClass().getMethod("findByEmail", String.class);
            Object r = find.invoke(userRepository, email);
            if (r instanceof Optional) return ((Optional<?>) r).isPresent();
            return r != null;
        } catch (Throwable ignored) {}
        return false;
    }

    // Kiểm tra tồn tại username theo nhiều chữ ký repo thường gặp
    private boolean usernameExists(String username) {
        try {
            Method exists = userRepository.getClass().getMethod("existsByUsernameIgnoreCase", String.class);
            Object r = exists.invoke(userRepository, username);
            if (r instanceof Boolean && (Boolean) r) return true;
        } catch (Throwable ignored) {}
        try {
            Method exists = userRepository.getClass().getMethod("existsByUsername", String.class);
            Object r = exists.invoke(userRepository, username);
            if (r instanceof Boolean && (Boolean) r) return true;
        } catch (Throwable ignored) {}
        try {
            Method find = userRepository.getClass().getMethod("findByUsernameIgnoreCase", String.class);
            Object r = find.invoke(userRepository, username);
            if (r instanceof Optional) return ((Optional<?>) r).isPresent();
            return r != null;
        } catch (Throwable ignored) {}
        try {
            Method find = userRepository.getClass().getMethod("findByUsername", String.class);
            Object r = find.invoke(userRepository, username);
            if (r instanceof Optional) return ((Optional<?>) r).isPresent();
            return r != null;
        } catch (Throwable ignored) {}
        return false;
    }
}
