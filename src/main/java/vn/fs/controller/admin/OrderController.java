package vn.fs.controller.admin;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import vn.fs.dto.OrderExcelExporter;
import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;
import vn.fs.service.OrderAdminService;

@Controller
@RequestMapping("/admin")
public class OrderController {

    @Autowired private OrderAdminService orderService;
    @Autowired private UserRepository userRepository;

    @ModelAttribute("user")
    public User user(Model model, Principal principal, User user) {
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
        return user;
    }

    // list + filters (auto-apply)
    @GetMapping("/orders")
    public String orders(Model model,
                         @RequestParam(required = false) Integer status,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                         @RequestParam(required = false) String q,
                         @RequestParam(required = false) String payment) {

        List<Order> orders = orderService.listAllFiltered(status, from, to, q, payment);
        model.addAttribute("orderDetails", orders);

        // giữ lại filter trên UI
        model.addAttribute("f_status", status);
        model.addAttribute("f_from", from);
        model.addAttribute("f_to", to);
        model.addAttribute("f_q", q);
        model.addAttribute("f_payment", payment);

        return "admin/orders";
    }

    @GetMapping("/order/detail/{order_id}")
    public String detail(ModelMap model, @PathVariable("order_id") Long id) {
        List<OrderDetail> list = orderService.detailsOf(id);
        double amount = orderService.amountOf(id).orElse(0d);

        model.addAttribute("amount", amount);
        model.addAttribute("orderDetail", list);
        model.addAttribute("orderId", id);
        model.addAttribute("menuO", "menu");
        return "admin/editOrder";
    }

    @RequestMapping("/order/cancel/{order_id}")
    public String cancel(@PathVariable("order_id") Long id) {
        orderService.cancel(id);
        return "forward:/admin/orders";
    }

    @RequestMapping("/order/confirm/{order_id}")
    public String confirm(@PathVariable("order_id") Long id) {
        orderService.confirm(id);
        return "forward:/admin/orders";
    }

    @RequestMapping("/order/delivered/{order_id}")
    public String delivered(@PathVariable("order_id") Long id) {
        orderService.deliveredAndDecreaseStock(id);
        return "forward:/admin/orders";
    }

    @GetMapping("/export")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=orders.xlsx");
        List<Order> listOrders = orderService.listAll();
        new OrderExcelExporter(listOrders).export(response);
    }
}
