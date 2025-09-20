package vn.fs.controller.admin;

import java.io.InputStream;
import java.nio.file.*;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.fs.entities.Category;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;

@Controller
@RequestMapping("/admin")
public class CategoryController {

    @Value("${upload.path}")
    private String uploadDir;

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    /* ---------- COMMON USER IN MODEL ---------- */
    @ModelAttribute("user")
    public User user(Model model, Principal principal, User user) {
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
        return user;
    }

    /* ---------- LIST TO TABLE ---------- */
    @ModelAttribute("categories")
    public List<Category> showCategory(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return categories;
    }

    /* ---------- LIST PAGE ---------- */
    @GetMapping("/categories")
    public String categories(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories";
    }

    /* ---------- ADD (optional image) ---------- */
    @PostMapping("/addCategory")
    public String addCategory(@Valid @ModelAttribute("category") Category category,
                              BindingResult result,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("message", "Dữ liệu không hợp lệ!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }
        try {
            Files.createDirectories(Paths.get(uploadDir));

            if (file != null && !file.isEmpty()) {
                String original = StringUtils.cleanPath(file.getOriginalFilename());
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot).toLowerCase(Locale.ROOT);
                String newName = "cat_new_" + System.currentTimeMillis() + ext;
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, Paths.get(uploadDir).resolve(newName), StandardCopyOption.REPLACE_EXISTING);
                }
                category.setCategoryImage(newName);
            }
            if (category.getStatus() == null) category.setStatus(true);

            categoryRepository.save(category);
            ra.addFlashAttribute("message", "Thêm thể loại thành công!");
            ra.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi thêm thể loại: " + e.getMessage());
            ra.addFlashAttribute("alertType", "danger");
        }
        return "redirect:/admin/categories";
    }

    /* ---------- EDIT (GET) ---------- */
    @GetMapping("/editCategory/{id}")
    public String editCategory(@PathVariable("id") Long id, ModelMap model, RedirectAttributes ra) {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", opt.get());
        return "admin/editCategory";
    }

    /* ---------- EDIT (POST) ---------- */
    @PostMapping("/editCategory/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @ModelAttribute("category") Category form,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(value = "existingImage", required = false) String existingImage,
                                 RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            cat.setCategoryName(form.getCategoryName() == null ? null : form.getCategoryName().trim());
            if (form.getStatus() != null) cat.setStatus(form.getStatus());

            Files.createDirectories(Paths.get(uploadDir));

            if (file != null && !file.isEmpty()) {
                String original = StringUtils.cleanPath(file.getOriginalFilename());
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot).toLowerCase(Locale.ROOT);
                String newName = "cat_" + id + "_" + System.currentTimeMillis() + ext;

                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, Paths.get(uploadDir).resolve(newName), StandardCopyOption.REPLACE_EXISTING);
                }

                if (existingImage != null && !existingImage.isBlank()) {
                    try { Files.deleteIfExists(Paths.get(uploadDir).resolve(existingImage)); } catch (Exception ignore) {}
                }
                cat.setCategoryImage(newName);
            } else {
                cat.setCategoryImage(existingImage);
            }

            categoryRepository.save(cat);
            ra.addFlashAttribute("message", "Cập nhật thể loại thành công!");
            ra.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi cập nhật: " + e.getMessage());
            ra.addFlashAttribute("alertType", "danger");
        }
        return "redirect:/admin/editCategory/" + id;
    }

    /* ---------- HIDE (soft delete nếu còn sản phẩm) ---------- */
    @GetMapping("/delete/{id}")
    public String hideCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }
        Category c = opt.get();

        long cntActive = productRepository.countActiveByCategory(id);
        if (cntActive > 0) {
            c.setStatus(false); // Ẩn nếu còn SP đang bán
            categoryRepository.save(c);
            ra.addFlashAttribute("message", "Thể loại còn sản phẩm đang bán → đã ẨN.");
            ra.addFlashAttribute("alertType", "warning");
            return "redirect:/admin/categories";
        }

        try {
            // không còn SP active → cho phép xóa cứng + xóa ảnh
            if (c.getCategoryImage() != null && !c.getCategoryImage().isBlank()) {
                try { Files.deleteIfExists(Paths.get(uploadDir).resolve(c.getCategoryImage())); } catch (Exception ignore) {}
            }
            categoryRepository.deleteById(id);
            ra.addFlashAttribute("message", "Xóa thể loại thành công.");
            ra.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            c.setStatus(false);
            categoryRepository.save(c);
            ra.addFlashAttribute("message", "Không thể xóa do ràng buộc. Đã ẨN thể loại.");
            ra.addFlashAttribute("alertType", "warning");
        }
        return "redirect:/admin/categories";
    }

    @RequestMapping(value = "/restoreCategory/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String restoreCategory(@PathVariable("id") Long id, RedirectAttributes ra){
        categoryRepository.findById(id).ifPresent(c -> {
            c.setStatus(true);
            categoryRepository.save(c);
        });
        ra.addFlashAttribute("message","Đã HIỂN THỊ lại thể loại!");
        ra.addFlashAttribute("alertType","success");
        return "redirect:/admin/categories";
    }

}
