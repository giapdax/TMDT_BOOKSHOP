package vn.fs.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.fs.commom.CommomDataService;
import vn.fs.entities.Favorite;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.FavoriteRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;

import javax.servlet.http.HttpSession;

@Controller
public class ShopController { // không extends gì để tránh side-effect không cần thiết

    @Autowired private ProductRepository productRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private CommomDataService commomDataService;
    @Autowired private UserRepository userRepository;

    // ========================== Helpers ==========================
    private User getCurrentUser(HttpSession session) {
        // Ưu tiên session attribute "customer" nếu có
        Object customer = (session != null) ? session.getAttribute("customer") : null;
        if (customer instanceof User) {
            return (User) customer;
        }
        // Nếu chưa, lấy theo SecurityContext (đã login)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String loginId = auth.getName();
            User byUsername = userRepository.findByUsername(loginId);
            return (byUsername != null) ? byUsername : userRepository.findByEmail(loginId);
        }
        return null; // anonymous
    }

    // ========================== All products (paging) ==========================
    @GetMapping(value = "/products")
    public String shop(Model model,
                       Pageable pageable,
                       @RequestParam("page") Optional<Integer> page,
                       @RequestParam("size") Optional<Integer> size,
                       HttpSession session) {

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(12);

        Page<Product> productPage = findPaginated(PageRequest.of(currentPage - 1, pageSize));

        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        // Bơm dữ liệu common (an toàn kể cả khi user == null)
        User current = getCurrentUser(session);
        commomDataService.commonData(model, current);

        model.addAttribute("products", productPage);
        return "web/shop";
    }

    public Page<Product> findPaginated(Pageable pageable) {
        List<Product> all = productRepository.findAll();

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<Product> list;
        if (all.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, all.size());
            list = all.subList(startItem, toIndex);
        }
        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), all.size());
    }

    // ========================== Search ==========================
    @GetMapping(value = "/searchProduct")
    public String showsearch(Model model,
                             Pageable pageable,
                             @RequestParam("keyword") String keyword,
                             @RequestParam("size") Optional<Integer> size,
                             @RequestParam("page") Optional<Integer> page,
                             HttpSession session) {

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(12);

        Page<Product> productPage = findPaginatSearch(PageRequest.of(currentPage - 1, pageSize), keyword);

        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        User current = getCurrentUser(session);
        commomDataService.commonData(model, current);

        model.addAttribute("products", productPage);
        model.addAttribute("keyword", keyword);
        return "web/shop";
    }

    public Page<Product> findPaginatSearch(Pageable pageable, String keyword) {
        List<Product> all = productRepository.searchProduct(keyword);

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<Product> list;
        if (all.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, all.size());
            list = all.subList(startItem, toIndex);
        }
        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), all.size());
    }

    // ========================== By Category ==========================
    @GetMapping(value = "/productByCategory")
    public String listProductbyid(Model model,
                                  @RequestParam("id") Long categoryId,
                                  HttpSession session) {

        User current = getCurrentUser(session);
        Long currentUserId = (current != null) ? current.getUserId() : null;

        List<Product> products = productRepository.listProductByCategory(categoryId);
        List<Product> listProductNew = new ArrayList<>();

        for (Product product : products) {
            Product copy = new Product();
            BeanUtils.copyProperties(product, copy);

            // Chỉ check favorite khi đã đăng nhập (userId != null)
            boolean isFav = false;
            if (currentUserId != null) {
                Favorite save = favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
                isFav = (save != null);
            }
            copy.favorite = isFav;

            listProductNew.add(copy);
        }

        model.addAttribute("products", listProductNew);

        // bơm dữ liệu common cho header/sidebar
        commomDataService.commonData(model, current);
        return "web/shop";
    }
}
