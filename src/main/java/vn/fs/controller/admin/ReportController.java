package vn.fs.controller.admin;

import java.security.Principal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import vn.fs.repository.OrderDetailRepository;
import vn.fs.repository.UserRepository;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ReportController {

    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;

    /* Helper: bind user hiện tại vào model nếu đăng nhập */
    private void bindCurrentUser(Model model, Principal principal) {
        if (principal == null) return;
        String login = principal.getName(); // có thể là username HOẶC email
        userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login)
                .ifPresent(u -> model.addAttribute("user", u));
    }

    /** Thống kê theo SẢN PHẨM */
    @GetMapping("/reports")
    public String reportByProduct(Model model, Principal principal) {
        bindCurrentUser(model, principal);
        model.addAttribute("title", "Thống kê theo sản phẩm");
        model.addAttribute("listReportCommon", orderDetailRepository.repo());
        return "admin/statistical";
    }

    /** Thống kê theo THỂ LOẠI */
    @GetMapping("/reportCategory")
    public String reportByCategory(Model model, Principal principal) {
        bindCurrentUser(model, principal);
        model.addAttribute("title", "Thống kê theo thể loại");
        model.addAttribute("listReportCommon", orderDetailRepository.repoWhereCategory());
        return "admin/statistical";
    }

    /** Thống kê theo NĂM */
    @GetMapping("/reportYear")
    public String reportByYear(Model model, Principal principal) {
        bindCurrentUser(model, principal);
        model.addAttribute("title", "Thống kê theo năm");
        model.addAttribute("listReportCommon", orderDetailRepository.repoWhereYear());
        return "admin/statistical";
    }

    /** Thống kê theo THÁNG */
    @GetMapping("/reportMonth")
    public String reportByMonth(Model model, Principal principal) {
        bindCurrentUser(model, principal);
        model.addAttribute("title", "Thống kê theo tháng");
        model.addAttribute("listReportCommon", orderDetailRepository.repoWhereMonth());
        return "admin/statistical";
    }

    /** Thống kê theo QUÝ */
    @GetMapping("/reportQuarter")
    public String reportByQuarter(Model model, Principal principal) {
        bindCurrentUser(model, principal);
        model.addAttribute("title", "Thống kê theo quý");
        model.addAttribute("listReportCommon", orderDetailRepository.repoWhereQUARTER());
        return "admin/statistical";
    }

    /** Thống kê theo KHÁCH HÀNG */
    @GetMapping("/reportOrderCustomer")
    public String reportByCustomer(Model model, Principal principal) {
        bindCurrentUser(model, principal);
        model.addAttribute("title", "Thống kê theo khách hàng");
        model.addAttribute("listReportCommon", orderDetailRepository.reportCustomer());
        return "admin/statistical";
    }
}
