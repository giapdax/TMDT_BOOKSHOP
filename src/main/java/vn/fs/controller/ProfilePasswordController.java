package vn.fs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.fs.dto.ChangePasswordInProfile;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfilePasswordController {

    @Autowired private UserRepository userRepository;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/change-password")
    public String showChangePassword(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ChangePasswordInProfile());
        }
        return "web/profile-change-password";
    }

    @PostMapping("/change-password")
    @Transactional
    public String doChangePassword(@Valid @ModelAttribute("form") ChangePasswordInProfile form,
                                   BindingResult br,
                                   Model model,
                                   HttpSession httpSession) {

        if (br.hasErrors()) {
            return "web/profile-change-password";
        }

        // Lấy user hiện tại (có thể là username hoặc email)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loginId = (auth == null) ? "" : auth.getName();

        Optional<User> opt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginId, loginId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy tài khoản hiện tại.");
            return "web/profile-change-password";
        }

        User me = opt.get();

        // Verify mật khẩu hiện tại
        if (!passwordEncoder.matches(form.getCurrentPassword(), me.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "web/profile-change-password";
        }

        // Verify confirm
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "Xác nhận mật khẩu không khớp.");
            return "web/profile-change-password";
        }

        // (optional) không cho new == current
        if (passwordEncoder.matches(form.getNewPassword(), me.getPassword())) {
            model.addAttribute("error", "Mật khẩu mới phải khác mật khẩu hiện tại.");
            return "web/profile-change-password";
        }

        // Cập nhật bằng JPQL field-level để KHÔNG trigger validate toàn entity (tránh lỗi phone NotBlank,…)
        String hashed = passwordEncoder.encode(form.getNewPassword());
        int updated = userRepository.updatePasswordByEmail(me.getEmail(), hashed);
        if (updated <= 0) {
            model.addAttribute("error", "Cập nhật mật khẩu thất bại. Vui lòng thử lại.");
            return "web/profile-change-password";
        }

        // Đổi thành công: gợi ý đăng nhập lại
        try { SecurityContextHolder.clearContext(); } catch (Exception ignored) {}
        try { httpSession.invalidate(); } catch (Exception ignored) {}

        model.addAttribute("message", "Đổi mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
        model.addAttribute("showLoginLink", true);
        model.addAttribute("form", new ChangePasswordInProfile());
        return "web/profile-change-password";
    }
}
