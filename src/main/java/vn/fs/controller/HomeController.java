package vn.fs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.fs.commom.CommomDataService;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;
import vn.fs.util.SessionUtils;

import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private CommomDataService commomDataService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping({"/", "/home"})
    public String home(Model model, HttpSession session) {
        // Lấy user hiện tại (entity) nếu có
        User current = null;
        Long uid = SessionUtils.getUserId(session);
        if (uid == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String loginId = auth.getName();
                current = Optional.ofNullable(userRepository.findByUsername(loginId))
                        .orElseGet(() -> userRepository.findByEmail(loginId));
            }
        } else {
            current = userRepository.findById(uid).orElse(null);
        }

        // Bơm dữ liệu dùng cho header/footer/menu
        commomDataService.commonData(model, current);

        // Dữ liệu section trang chủ
        bestSaleProduct20(model, current);

        // Đảm bảo default cho các list th:each trong home.html
        if (!model.containsAttribute("countProductByCategory")) {
            model.addAttribute("countProductByCategory", Collections.emptyList());
        }
        if (!model.containsAttribute("productList")) {
            model.addAttribute("productList", Collections.emptyList());
        }
        if (!model.containsAttribute("bestSaleProduct20")) {
            model.addAttribute("bestSaleProduct20", Collections.emptyList());
        }

        return "web/home";
    }

    @SuppressWarnings("unchecked")
    private void bestSaleProduct20(Model model, User currentUser) {
        List<Product> result = new ArrayList<>();

        Object svc = null;
        try {
            svc = applicationContext.getBean("productService");
        } catch (Exception e1) {
            try {
                svc = applicationContext.getBean("productServiceImpl");
            } catch (Exception e2) {
                model.addAttribute("bestSaleProduct20", Collections.emptyList());
                return;
            }
        }

        Class<?> c = svc.getClass();
        String[] noArg = {
                "getBestSaleTop20", "findBestSaleTop20", "top20BestSale",
                "getTop20BestSale", "bestSaleTop20"
        };
        String[] withUser = {
                "getBestSaleTop20ForUser", "findBestSaleTop20ForUser",
                "top20BestSaleForUser", "getTop20BestSaleForUser",
                "bestSaleTop20ForUser"
        };

        try {
            if (currentUser != null && currentUser.getUserId() != null) {
                for (String m : withUser) {
                    try {
                        Object data = c.getMethod(m, Long.class).invoke(svc, currentUser.getUserId());
                        if (data instanceof List) { result = (List<Product>) data; break; }
                    } catch (NoSuchMethodException ignored) {}
                }
            }
            if (result.isEmpty()) {
                for (String m : noArg) {
                    try {
                        Object data = c.getMethod(m).invoke(svc);
                        if (data instanceof List) { result = (List<Product>) data; break; }
                    } catch (NoSuchMethodException ignored) {}
                }
            }
        } catch (Exception e) {
            result = Collections.emptyList();
        }

        model.addAttribute("bestSaleProduct20", result == null ? Collections.emptyList() : result);
    }
}
