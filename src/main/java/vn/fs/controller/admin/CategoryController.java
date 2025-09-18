package vn.fs.controller.admin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.fs.entities.Category;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.UserRepository;

@Controller
@RequestMapping("/admin")
public class CategoryController {

    @Value("${upload.path}")
    private String uploadDir; // ví dụ: upload/images

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    UserRepository userRepository;

    /* ---------- COMMON USER IN MODEL ---------- */
    @ModelAttribute(value = "user")
    public User user(Model model, Principal principal, User user) {
        if (principal != null) {
            model.addAttribute("user", new User());
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
    @GetMapping(value = "/categories")
    public String categories(Model model) {
        model.addAttribute("category", new Category());
        return "admin/categories";
    }

    /* ---------- ADD (tùy chọn có ảnh) ---------- */
    @PostMapping(value = "/addCategory")
    public String addCategory(@Validated @ModelAttribute("category") Category category,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              RedirectAttributes ra) {
        try {
            // tạo thư mục nếu chưa có
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            if (file != null && !file.isEmpty()) {
                String original = StringUtils.cleanPath(file.getOriginalFilename());
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot).toLowerCase(Locale.ROOT);
                String newName = "cat_new_" + System.currentTimeMillis() + ext;

                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, uploadPath.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
                }
                category.setCategoryImage(newName);
            }

            categoryRepository.save(category);
            ra.addFlashAttribute("message", "Thêm thể loại thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi thêm thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    /* ---------- EDIT (GET) ---------- */
    @GetMapping(value = "/editCategory/{id}")
    public String editCategory(@PathVariable("id") Long id, ModelMap model) {
        Category category = categoryRepository.findById(id).orElse(null);
        model.addAttribute("category", category);
        return "admin/editCategory";
    }

    /* ---------- EDIT (POST) – ĐỔI ẢNH AN TOÀN ---------- */
    @PostMapping("/editCategory/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @ModelAttribute("category") Category form,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(value = "existingImage", required = false) String existingImage,
                                 RedirectAttributes ra) {
        try {
            Category cat = categoryRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));

            // cập nhật tên
            cat.setCategoryName(form.getCategoryName() == null ? null : form.getCategoryName().trim());

            // đảm bảo thư mục tồn tại
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            if (file != null && !file.isEmpty()) {
                // tạo tên file mới an toàn
                String original = StringUtils.cleanPath(file.getOriginalFilename());
                String ext = "";
                int dot = original.lastIndexOf('.');
                if (dot >= 0) ext = original.substring(dot).toLowerCase(Locale.ROOT);
                String newName = "cat_" + id + "_" + System.currentTimeMillis() + ext;

                // lưu file mới
                try (InputStream in = file.getInputStream()) {
                    Files.copy(in, uploadPath.resolve(newName), StandardCopyOption.REPLACE_EXISTING);
                }

                // xóa file cũ nếu có
                if (existingImage != null && !existingImage.isBlank()) {
                    try { Files.deleteIfExists(uploadPath.resolve(existingImage)); } catch (Exception ignore) {}
                }

                // cập nhật DB
                cat.setCategoryImage(newName);
            } else {
                // không upload -> giữ ảnh cũ
                cat.setCategoryImage(existingImage);
            }

            categoryRepository.save(cat);
            ra.addFlashAttribute("message", "Cập nhật thể loại thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi cập nhật: " + e.getMessage());
        }
        return "redirect:/admin/editCategory/" + id;
    }

    /* ---------- DELETE (có xóa ảnh) ---------- */
    @GetMapping("/delete/{id}")
    public String delCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        try {
            categoryRepository.findById(id).ifPresent(cat -> {
                // xóa ảnh
                if (cat.getCategoryImage() != null && !cat.getCategoryImage().isBlank()) {
                    try { Files.deleteIfExists(Paths.get(uploadDir).resolve(cat.getCategoryImage())); } catch (Exception ignore) {}
                }
                categoryRepository.deleteById(id);
            });
            ra.addFlashAttribute("message", "Xóa thể loại thành công!");
        } catch (Exception e) {
            ra.addFlashAttribute("message", "Lỗi xóa: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}
