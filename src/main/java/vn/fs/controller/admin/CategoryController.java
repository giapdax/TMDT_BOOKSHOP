package vn.fs.controller.admin;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.dto.CategoryDTO;
import vn.fs.entities.User;
import vn.fs.service.CategoryService;
import vn.fs.service.ImageStorageService;
import vn.fs.repository.UserRepository;

import javax.validation.Valid;
import java.security.Principal;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final ImageStorageService image;   // dùng check file hợp lệ
    private final UserRepository userRepository;

    /* bind user cho header */
    @ModelAttribute("user")
    public User user(Model model, Principal principal) {
        if (principal == null) return null;
        User u = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", u);
        return u;
    }

    /* bind list category cho bảng */
    @ModelAttribute("categories")
    public java.util.List<vn.fs.entities.Category> categories(Model model) {
        var list = categoryService.findAll();
        model.addAttribute("categories", list);
        return list;
    }

    /* trang danh sách */
    @GetMapping("/categories")
    public String page(Model model) {
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", categoryService.newDefaultDTO());
        }
        return "admin/categories";
    }

    /* mở trang sửa */
    @GetMapping("/editCategory/{id}")
    public String editPage(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        return categoryService.findById(id)
                .map(c -> { model.addAttribute("category", categoryService.toDTO(c)); return "admin/editCategory"; })
                .orElseGet(() -> {
                    ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
                    ra.addFlashAttribute("alertType", "danger");
                    return "redirect:/admin/categories";
                });
    }

    /* thêm mới */
    @PostMapping("/addCategory")
    public String add(@Valid @ModelAttribute("category") CategoryDTO dto,
                      BindingResult br,
                      @RequestParam(value = "file", required = false) MultipartFile file,
                      Model model) {

        dto.normalize(); // gộp khoảng trắng, trim

        // kiểm tra trùng tên
        if (categoryService.nameExists(dto.getCategoryName())) {
            br.rejectValue("categoryName", null, "Tên thể loại đã tồn tại.");
        }

        // nếu có file thì thử lưu tạm để check hợp lệ (jpg, jpeg, png, webp)
        if (file != null && !file.isEmpty() && image.saveTemp(file) == null) {
            br.rejectValue("categoryId", null, "Ảnh không hợp lệ (jpg, jpeg, png, webp).");
        }

        // có lỗi -> mở lại modal
        if (br.hasErrors()) {
            model.addAttribute("openAddModal", true);
            return "admin/categories";
        }

        // ok -> service lo tạo + promote ảnh
        categoryService.create(dto, file);

        model.addAttribute("message", "Thêm thể loại thành công!");
        model.addAttribute("alertType", "success");
        model.addAttribute("category", categoryService.newDefaultDTO());
//        return "admin/categories";
        return "redirect:/admin/categories";
    }

    /* cập nhật */
    @PostMapping("/editCategory/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("category") CategoryDTO dto,
                         BindingResult br,
                         @RequestParam(value = "file", required = false) MultipartFile file,
                         @RequestParam(value = "existingImage", required = false) String existingImage,
                         RedirectAttributes ra) {

        dto.normalize();

        // trùng tên (trừ chính nó)
        if (categoryService.nameExistsOther(dto.getCategoryName(), id)) {
            br.rejectValue("categoryName", null, "Tên thể loại đã tồn tại.");
        }

        // có file thì check hợp lệ bằng cách save tạm
        if (file != null && !file.isEmpty() && image.saveTemp(file) == null) {
            br.rejectValue("categoryId", null, "Ảnh không hợp lệ (jpg, jpeg, png, webp).");
        }

        if (br.hasErrors()) return "admin/editCategory";

        categoryService.update(id, dto, file, existingImage);

        ra.addFlashAttribute("message", "Cập nhật thể loại thành công!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/categories";
    }

    /* ẩn category + ẩn toàn bộ sản phẩm con; nếu không còn sp tham chiếu thì xóa cứng
       giữ nguyên path /delete/{id} để không vỡ link cũ */
    @GetMapping("/delete/{id}")
    public String hide(@PathVariable("id") Long id, RedirectAttributes ra) {
        var opt = categoryService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/categories";
        }

        boolean hard = categoryService.hideAndCascade(id);
        if (hard) {
            ra.addFlashAttribute("message", "Đã ẨN sản phẩm & XÓA vĩnh viễn thể loại (không còn sản phẩm tham chiếu).");
            ra.addFlashAttribute("alertType", "success");
        } else {
            ra.addFlashAttribute("message", "Đã ẨN thể loại và ẨN toàn bộ sản phẩm thuộc thể loại này.");
            ra.addFlashAttribute("alertType", "success");
        }
        return "redirect:/admin/categories";
    }

    /* khôi phục category (không tự mở sp con) */
    @RequestMapping(value = "/restoreCategory/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String restore(@PathVariable("id") Long id, RedirectAttributes ra) {
        categoryService.restore(id);
        ra.addFlashAttribute("message", "Đã HIỂN THỊ lại thể loại (sản phẩm vẫn ẨN).");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/categories";
    }
}
