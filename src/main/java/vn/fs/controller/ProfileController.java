package vn.fs.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import vn.fs.commom.CommomDataService;
import vn.fs.entities.Order;
import vn.fs.entities.OrderDetail;
import vn.fs.entities.User;
import vn.fs.repository.OrderDetailRepository;
import vn.fs.repository.OrderRepository;
import vn.fs.repository.UserRepository;

@Controller
public class ProfileController extends CommomController {

    @Autowired
    UserRepository userRepository;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderDetailRepository orderDetailRepository;

    @Autowired
    CommomDataService commomDataService;

    // ===================== Helpers =====================
    /** Lấy User theo principal: chấp nhận login id là username HOẶC email */
    private User resolveCurrentUser(Principal principal) {
        if (principal == null) return null;
        String login = principal.getName();
        // Thử username trước, không có thì rẽ sang email
        User u = Optional.ofNullable(userRepository.findByUsername(login))
                .orElseGet(() -> userRepository.findByEmail(login));
        return u;
    }

    // ===================== /profile =====================
    @GetMapping(value = "/profile")
    public String profile(Model model,
                          Principal principal,
                          Pageable pageable,
                          @RequestParam("page") Optional<Integer> page,
                          @RequestParam("size") Optional<Integer> size) {

        // Bắt buộc đăng nhập
        User current = resolveCurrentUser(principal);
        if (current == null) {
            return "redirect:/login";
        }

        // Đặt user lên model (an toàn dù CommomController cũng đã bơm)
        model.addAttribute("user", current);

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(6);

        Page<Order> orderPage = findPaginated(PageRequest.of(currentPage - 1, pageSize), current.getUserId());

        int totalPages = orderPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        // Bơm dữ liệu chung (header/cart/favorite...) – null-safe
        commomDataService.commonData(model, current);

        model.addAttribute("orderByUser", orderPage);
        return "web/profile";
    }

    public Page<Order> findPaginated(Pageable pageable, Long userId) {
        List<Order> orderList = (userId == null)
                ? Collections.emptyList()
                : orderRepository.findOrderByUserId(userId);

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<Order> list;
        if (orderList.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, orderList.size());
            list = orderList.subList(startItem, toIndex);
        }

        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), orderList.size());
    }

    // ===================== /order/detail/{id} =====================
    @GetMapping("/order/detail/{order_id}")
    public ModelAndView detail(Model model,
                               Principal principal,
                               @PathVariable("order_id") Long id) {
        User current = resolveCurrentUser(principal);
        if (current == null) {
            return new ModelAndView("redirect:/login");
        }

        model.addAttribute("user", current);

        List<OrderDetail> listO = orderDetailRepository.findByOrderId(id);
        model.addAttribute("orderDetail", listO);

        commomDataService.commonData(model, current);
        return new ModelAndView("web/historyOrderDetail");
    }

    // ===================== /order/cancel/{id} =====================
    @RequestMapping("/order/cancel/{order_id}")
    public ModelAndView cancel(ModelMap model, @PathVariable("order_id") Long id) {
        Optional<Order> o = orderRepository.findById(id);
        if (o.isEmpty()) {
            return new ModelAndView("redirect:/profile", model);
        }
        Order oReal = o.get();
        oReal.setStatus((short) 3);
        orderRepository.save(oReal);

        return new ModelAndView("redirect:/profile", model);
    }
}
