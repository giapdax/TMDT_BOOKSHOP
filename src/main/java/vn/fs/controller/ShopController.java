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
public class ShopController {

    @Autowired private ProductRepository productRepository;
    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private CommomDataService commomDataService;
    @Autowired private UserRepository userRepository;

    private User getCurrentUser(HttpSession session) {
        Object customer = (session != null) ? session.getAttribute("customer") : null;
        if (customer instanceof User) return (User) customer;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String loginId = auth.getName();
            User byUsername = userRepository.findByUsername(loginId);
            return (byUsername != null) ? byUsername : userRepository.findByEmail(loginId);
        }
        return null;
    }

    @GetMapping(value = "/products")
    public String shop(Model model,
                       Pageable pageable,
                       @RequestParam("page") Optional<Integer> page,
                       @RequestParam("size") Optional<Integer> size,
                       HttpSession session) {
        User current = getCurrentUser(session);
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(12);

        Page<Product> productPage = findPaginated(PageRequest.of(currentPage - 1, pageSize));
        markFavorite(productPage.getContent(), current);
        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
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
    @GetMapping(value = "/searchProduct")
    public String showsearch(Model model,
                             Pageable pageable,
                             @RequestParam("keyword") String keyword,
                             @RequestParam("size") Optional<Integer> size,
                             @RequestParam("page") Optional<Integer> page,
                             HttpSession session) {
        User current = getCurrentUser(session);
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(12);

        Page<Product> productPage = findPaginatSearch(PageRequest.of(currentPage - 1, pageSize), keyword);
        markFavorite(productPage.getContent(), current);
        int totalPages = productPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }
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
//------------
            Favorite save = favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
            if (save != null) {
                copy.favorite = true;
            }
            else {
                copy.favorite = false;
            }
//------------------
//            boolean isFav = false;
//            if (currentUserId != null) {
//                Favorite save = favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
//                isFav = (save != null);
//            }
//            copy.favorite = isFav;

            listProductNew.add(copy);
        }

        model.addAttribute("products", listProductNew);
        commomDataService.commonData(model, current);
        return "web/shop";
    }

    /* ========== NEW: lọc theo Nhà xuất bản ========== */
    @GetMapping(value = "/productByPublisher")
    public String listProductByPublisher(Model model,
                                         @RequestParam("id") Long nxbId,
                                         HttpSession session) {
        User current = getCurrentUser(session);
        Long currentUserId = (current != null) ? current.getUserId() : null;

        List<Product> products = productRepository.listProductByNxb(nxbId);
        List<Product> listView = new ArrayList<>();

        for (Product p : products) {
            Product copy = new Product();
            BeanUtils.copyProperties(p, copy);

//------------
            Favorite save = favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
            if (save != null) {
                copy.favorite = true;
            }
            else {
                copy.favorite = false;
            }
//------------------
//            boolean isFav = false;
//            if (currentUserId != null) {
//                Favorite save = favoriteRepository.selectSaves(copy.getProductId(), currentUserId);
//                isFav = (save != null);
//            }
//            copy.favorite = isFav;

            listView.add(copy);
        }

        model.addAttribute("products", listView);
        commomDataService.commonData(model, current);
        return "web/shop";
    }
    private void markFavorite(List<Product> products, User user) {
        if(user == null)return;
        for (Product p : products) {
            Favorite f = favoriteRepository.selectSaves(p.getProductId(), user.getUserId());
            p.favorite = (f!=null);
        }
    }
}
