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

import vn.fs.dto.CategoryDTO;
import vn.fs.entities.Category;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;

@Controller
@RequestMapping("/admin")
public class CategoryController {

    // Thư mục upload. Có default nếu chưa set trong application.properties
    @Value("${upload.path:${user.dir}/upload/images}")
    private String uploadDir;

    // Prefix file tạm để giữ ảnh khi form lỗi
    private static final String TMP_PREFIX = "__tmp__";

    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;

    @ModelAttribute("user")
    public User user(Model model, Principal principal, User user) {
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
        return user;
    }

    @ModelAttribute("categories")
    public List<Category> showCategory(Model model) {
        List<Category> categories = categoryRepository.findAll();
        model.addAttribute("categories", categories);
        return categories;
    }

    /* ====================== PAGES ====================== */
    @GetMapping("/categories")
    public String categories(Model model) {
        if (!model.containsAttribute("category")) {
            CategoryDTO dto = CategoryDTO.builder().status(Boolean.TRUE).build();
            model.addAttribute("category", dto);
        }
        return "admin/categories";
    }

    @GetMapping("/editCategory/{id}")
    public String editCategory(@PathVariable("id") Long id, ModelMap model, RedirectAttributes ra) {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }
        Category c = opt.get();
        CategoryDTO dto = CategoryDTO.builder()
                .categoryId(c.getCategoryId())
                .categoryName(nullToEmpty(c.getCategoryName()))
                .categoryImage(c.getCategoryImage())
                .status(c.getStatus() == null ? Boolean.TRUE : c.getStatus())
                .build();
        model.addAttribute("category", dto);
        return "admin/editCategory";
    }

    @PostMapping("/addCategory")
    public String addCategory(@Valid @ModelAttribute("category") CategoryDTO dto,
                              BindingResult br,
                              @RequestParam(value = "file", required = false) MultipartFile file,
                              Model model) {
        dto.normalize();

        // Lưu ảnh TẠM để không mất khi lỗi
        if (file != null && !file.isEmpty()) {
            String tmp = saveTempIfValid(file);
            if (tmp == null) {
                br.rejectValue("categoryId", null, "Ảnh không hợp lệ (jpg, jpeg, png, webp).");
            } else {
                dto.setCategoryImage(tmp);
            }
        }

        if (br.hasErrors()) {
            model.addAttribute("openAddModal", true);
            return "admin/categories";
        }

        // Trùng tên -> lỗi
        if (categoryRepository.existsByCategoryNameIgnoreCase(dto.getCategoryName())) {
            br.rejectValue("categoryName", null, "Tên thể loại đã tồn tại.");
            model.addAttribute("openAddModal", true);
            return "admin/categories";
        }

        // Promote ảnh tạm -> chính (nếu có)
        String finalImg = promoteIfTempOrKeep(dto.getCategoryImage());

        Category cat = new Category();
        cat.setCategoryName(dto.getCategoryName());
        cat.setStatus(dto.getStatus() != null ? dto.getStatus() : Boolean.TRUE);
        cat.setCategoryImage(finalImg);

        categoryRepository.save(cat);

        model.addAttribute("message", "Thêm thể loại thành công!");
        model.addAttribute("alertType", "success");
        model.addAttribute("category", CategoryDTO.builder().status(Boolean.TRUE).build());
        return "admin/categories";
    }

    @PostMapping("/editCategory/{id}")
    public String updateCategory(@PathVariable("id") Long id,
                                 @Valid @ModelAttribute("category") CategoryDTO dto,
                                 BindingResult br,
                                 @RequestParam(value = "file", required = false) MultipartFile file,
                                 @RequestParam(value = "existingImage", required = false) String existingImage,
                                 Model model,
                                 RedirectAttributes ra) {
        Category cat = categoryRepository.findById(id).orElse(null);
        if (cat == null) {
            ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }

        dto.normalize();

        // Nếu chọn ảnh mới -> lưu tạm
        if (file != null && !file.isEmpty()) {
            String tmp = saveTempIfValid(file);
            if (tmp == null) {
                br.rejectValue("categoryId", null, "Ảnh không hợp lệ (jpg, jpeg, png, webp).");
            } else {
                dto.setCategoryImage(tmp);
            }
        } else {
            if (isBlank(dto.getCategoryImage())) {
                dto.setCategoryImage(existingImage);
            }
        }

        if (br.hasErrors()) {
            return "admin/editCategory";
        }

        // Check trùng tên (exclude chính nó)
        if (categoryRepository.existsByCategoryNameIgnoreCaseAndCategoryIdNot(dto.getCategoryName(), id)) {
            br.rejectValue("categoryName", null, "Tên thể loại đã tồn tại.");
            return "admin/editCategory";
        }

        // Ảnh cuối
        String finalName = dto.getCategoryImage();
        if (isTemp(finalName)) {
            finalName = promoteIfTempOrKeep(finalName);
            if (finalName == null) {
                br.rejectValue("categoryId", null, "Không thể lưu ảnh. Vui lòng thử lại.");
                return "admin/editCategory";
            }
        }

        // Xóa ảnh cũ nếu thay đổi
        String oldImg = cat.getCategoryImage();
        if (!isBlank(finalName) && !equalsStr(finalName, oldImg)) {
            deleteImageQuietly(oldImg);
        }

        // Apply DTO -> entity
        cat.setCategoryName(dto.getCategoryName());
        cat.setStatus(dto.getStatus() != null ? dto.getStatus() : cat.getStatus());
        cat.setCategoryImage(finalName);

        categoryRepository.save(cat);

        ra.addFlashAttribute("message", "Cập nhật thể loại thành công!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/categories";
    }

    @GetMapping("/delete/{id}")
    public String hideCategory(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<Category> opt = categoryRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }
        Category c = opt.get();

        // Ẩn toàn bộ sản phẩm thuộc category này
        productRepository.hideByCategory(id);

        // Ẩn category (soft hide)
        c.setStatus(false);
        categoryRepository.save(c);

        // Nếu KHÔNG có bất kỳ sản phẩm nào tham chiếu category này (kể cả đã ẩn) thì cho xóa cứng
        long totalRef = productRepository.countByCategory_CategoryId(id);
        if (totalRef == 0) {
            try {
                deleteImageQuietly(c.getCategoryImage());
                categoryRepository.deleteById(id);
                ra.addFlashAttribute("message", "Đã ẨN sản phẩm & XÓA vĩnh viễn thể loại (do không còn sản phẩm tham chiếu).");
                ra.addFlashAttribute("alertType", "success");
                return "redirect:/admin/categories";
            } catch (Exception ignore) {
                // Nếu FK vẫn giữ ở DB, vẫn coi như soft-hide là đủ
            }
        }

        ra.addFlashAttribute("message", "Đã ẨN thể loại và ẨN toàn bộ sản phẩm thuộc thể loại này.");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/categories";
    }

    @RequestMapping(value = "/restoreCategory/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String restoreCategory(@PathVariable("id") Long id, RedirectAttributes ra){
        categoryRepository.findById(id).ifPresent(c -> {
            c.setStatus(true);
            categoryRepository.save(c);
        });
        // Không auto-mở sản phẩm con — admin tự mở cái cần bán
        ra.addFlashAttribute("message","Đã HIỂN THỊ lại thể loại (sản phẩm vẫn ẨN).");
        ra.addFlashAttribute("alertType","success");
        return "redirect:/admin/categories";
    }

    private String saveTempIfValid(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String original = StringUtils.cleanPath(file.getOriginalFilename());
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot).toLowerCase(Locale.ROOT);
            if (!(ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".webp"))) {
                return null;
            }
            String newName = TMP_PREFIX + System.currentTimeMillis() + "_" + Math.abs(original.hashCode()) + ext;
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, Paths.get(uploadDir).resolve(newName), StandardCopyOption.REPLACE_EXISTING);
            }
            return newName;
        } catch (Exception e) {
            return null;
        }
    }

    private String promoteIfTempOrKeep(String name) {
        if (isBlank(name)) return null;
        if (!isTemp(name)) return name;
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String ext = "";
            int dot = name.lastIndexOf('.');
            if (dot >= 0) ext = name.substring(dot);
            String finalName = "cat_" + System.currentTimeMillis() + ext;
            Path src = Paths.get(uploadDir).resolve(name);
            Path dst = Paths.get(uploadDir).resolve(finalName);
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
            return finalName;
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isTemp(String name) {
        return name != null && name.startsWith(TMP_PREFIX);
    }

    private void deleteImageQuietly(String name) {
        try {
            if (isBlank(name) || isTemp(name)) return; // không xoá file tạm ở đây
            Files.deleteIfExists(Paths.get(uploadDir).resolve(name));
        } catch (Exception ignore) {}
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
    private boolean equalsStr(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
