package vn.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import vn.fs.commom.CommomDataService;
import vn.fs.dto.SessionUser;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;

import javax.servlet.http.HttpSession;

@Component
@ControllerAdvice
public class GlobalModelAdvice {

    @Autowired private CommomDataService commomDataService;
    @Autowired private UserRepository userRepository;

    @ModelAttribute
    public void injectCommonData(Model model, HttpSession session) {
        User current = null;

        // 1) Ưu tiên lấy từ session "customer" (có thể là User hoặc SessionUser)
        Object customer = (session != null) ? session.getAttribute("customer") : null;
        if (customer instanceof User) {
            current = (User) customer;
        } else if (customer instanceof SessionUser) {
            SessionUser su = (SessionUser) customer;
            // theo id -> username -> email
            if (su.getUserId() != null) {
                current = userRepository.findById(su.getUserId()).orElse(null);
            }
            if (current == null && su.getUsername() != null) {
                current = userRepository.findByUsernameIgnoreCase(su.getUsername()).orElse(null);
            }
            if (current == null && su.getEmail() != null) {
                current = userRepository.findByEmailIgnoreCase(su.getEmail()).orElse(null);
            }
        }

        // 2) Nếu chưa có, lấy qua SecurityContext (support username OR email)
        if (current == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String loginId = auth.getName(); // có thể là username hoặc email
                current = userRepository.findByUsernameIgnoreCase(loginId)
                        .orElseGet(() -> userRepository.findByEmailIgnoreCase(loginId).orElse(null));
            }
        }

        // 3) Bơm dữ liệu chung cho header/footer/menu
        commomDataService.commonData(model, current);

        // 4) QUAN TRỌNG: luôn gán 'user' KHÔNG NULL để Thymeleaf không nổ khi gọi ${user.name}
        model.addAttribute("user", (current != null) ? current : new User());

        // 5) Thêm displayName (nếu bạn muốn dùng ở đâu đó)
        String displayName = "Khách";
        if (current != null) {
            if (current.getName() != null && !current.getName().isBlank())       displayName = current.getName();
            else if (current.getUsername() != null && !current.getUsername().isBlank()) displayName = current.getUsername();
            else if (current.getEmail() != null)                                  displayName = current.getEmail();
        } else if (customer instanceof SessionUser) {
            SessionUser su = (SessionUser) customer;
            if (su.getName() != null && !su.getName().isBlank())                  displayName = su.getName();
            else if (su.getUsername() != null && !su.getUsername().isBlank())     displayName = su.getUsername();
            else if (su.getEmail() != null)                                       displayName = su.getEmail();
        }
        model.addAttribute("displayName", displayName);
    }
}
