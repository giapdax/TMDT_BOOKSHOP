package vn.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.fs.commom.CommomDataService;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;

import javax.servlet.http.HttpSession;
import java.util.Optional;

/**
 * Bơm dữ liệu dùng chung (categoryList, cart, totalSave, countProductByCategory, ...) cho TẤT CẢ view.
 * Nhờ vậy không controller nào quên gọi nữa.
 */
@Component
@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired private CommomDataService commomDataService;
    @Autowired private UserRepository userRepository;

    @ModelAttribute
    public void injectCommonData(Model model, HttpSession session) {
        User current = null;

        // Lấy từ session attribute "customer" nếu controller khác đã set
        Object customer = (session != null) ? session.getAttribute("customer") : null;
        if (customer instanceof User) {
            current = (User) customer;
        }

        // Nếu chưa có thì lấy qua SecurityContext (đăng nhập chuẩn Spring Security)
        if (current == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String loginId = auth.getName();
                current = Optional.ofNullable(userRepository.findByUsername(loginId))
                        .orElseGet(() -> userRepository.findByEmail(loginId));
            }
        }

        // Bơm dữ liệu chung (an toàn với current = null)
        commomDataService.commonData(model, current);

        // Tùy template cũ đang dùng biến "user", set luôn để không vỡ binding
        model.addAttribute("user", current);
    }
}
