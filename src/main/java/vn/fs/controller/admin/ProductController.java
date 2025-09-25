package vn.fs.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.dto.ProductDTO;
import vn.fs.entities.Category;
import vn.fs.entities.NXB;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.repository.OrderDetailRepository;

import javax.validation.Valid;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final NxbRepository nxbRepository;
    private final UserRepository userRepository;
    private final OrderDetailRepository orderDetailRepository;

    /** Thư mục upload ảnh (có thể override bằng -Dupload.path hoặc ENV) */
    private final String pathUploadImage = System.getProperty(
            "upload.path",
            System.getProperty("user.dir") + File.separator + "upload" + File.separator + "images"
    );

    private static final String TMP_PREFIX = "__tmp__";

    /* ===================== COMMON BINDINGS ===================== */

    @ModelAttribute("user")
    public User bindUser(Model model, Principal principal) {
        if (principal == null) return null;
        User u = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", u);
        return u;
    }

    @ModelAttribute("products")
    public List<Product> products(Model model) {
        List<Product> list = productRepository.findAll();
        model.addAttribute("products", list);
        return list;
    }

    // Dropdown: Add (only active)
    @ModelAttribute("categoryList")
    public List<Category> categoryActiveList() {
        return categoryRepository.findByStatusTrueOrderByCategoryNameAsc();
    }
    @ModelAttribute("nxbList")
    public List<NXB> nxbActiveList() {
        return nxbRepository.findByStatusTrueOrderByNameAsc();
    }

    // Dropdown: Edit (full)
    @ModelAttribute("categoryAllList")
    public List<Category> categoryAllList() {
        return categoryRepository.findAllForDropdown();
    }
    @ModelAttribute("nxbAllList")
    public List<NXB> nxbAllList() {
        return nxbRepository.findAllForDropdown();
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(true);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(sdf, true));
    }

    /* ===================== PAGES ===================== */

    @GetMapping("/products")
    public String productsPage(Model model) {
        if (!model.containsAttribute("product")) {
            ProductDTO dto = ProductDTO.builder()
                    .status(true)
                    .discount(0)
                    .quantity(0)
                    .enteredDate(new Date())
                    .build();
            model.addAttribute("product", dto);
        }
        return "admin/products";
    }

    @GetMapping("/editProduct/{id}")
    public String editPage(@PathVariable("id") Long id, ModelMap model, RedirectAttributes ra) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        ProductDTO dto = toDTO(product);
        model.addAttribute("product", dto);
        return "admin/editProduct";
    }

    /* ===================== CREATE ===================== */

    @PostMapping("/addProduct")
    public String addProduct(@Valid @ModelAttribute("product") ProductDTO dto,
                             BindingResult br,
                             @RequestParam(value = "file", required = false) MultipartFile file,
                             Model model) {

        dto.normalize();

        // Lưu ảnh TẠM nếu người dùng vừa chọn file (để không mất khi lỗi)
        if (file != null && !file.isEmpty()) {
            String tmp = saveTempImageIfPresent(file);
            if (tmp == null) {
                br.rejectValue("productId", null, "Vui lòng chọn ảnh hợp lệ (jpg, jpeg, png, webp).");
            } else {
                dto.setProductImage(tmp); // giữ tên ảnh tạm trong DTO
            }
        }

        // Validate field
        if (br.hasErrors()) {
            model.addAttribute("openAddModal", true);
            return "admin/products";
        }

        // Ảnh là bắt buộc với ADD: nếu chưa có ảnh tạm hoặc ảnh cũ -> báo lỗi
        if (StringUtils.isBlank(dto.getProductImage())) {
            br.rejectValue("productId", null, "Vui lòng chọn ảnh sản phẩm.");
            model.addAttribute("openAddModal", true);
            return "admin/products";
        }

        // Trùng tên => lỗi luôn
        if (productRepository.existsByProductNameIgnoreCase(dto.getProductName())) {
            br.rejectValue("productName", null, "Tên sản phẩm đã tồn tại.");
            model.addAttribute("openAddModal", true);
            return "admin/products";
        }

        // Promote ảnh tạm -> ảnh chính
        String finalName = promoteIfTempOrKeep(dto.getProductImage());
        if (finalName == null) {
            br.rejectValue("productId", null, "Không thể lưu ảnh sản phẩm. Vui lòng thử lại.");
            model.addAttribute("openAddModal", true);
            return "admin/products";
        }

        Product entity = new Product();
        applyDTOToEntity(dto, entity);
        entity.setProductImage(finalName);

        productRepository.save(entity);

        model.addAttribute("message", "Thêm sản phẩm thành công.");
        model.addAttribute("alertType", "success");

        // Reset form DTO (xoá ảnh tạm khỏi DTO)
        model.addAttribute("product", ProductDTO.builder()
                .status(true).discount(0).quantity(0).enteredDate(new Date()).build());

        return "admin/products";
    }

    /* ===================== UPDATE ===================== */

    @PostMapping("/editProduct/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @Valid @ModelAttribute("product") ProductDTO dto,
                                BindingResult br,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                @RequestParam(value = "existingImage", required = false) String existingImage,
                                Model model,
                                RedirectAttributes ra) {

        Product p = productRepository.findById(id).orElse(null);
        if (p == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }

        dto.normalize();

        // Nếu user chọn ảnh mới -> lưu TẠM để không mất khi lỗi
        if (file != null && !file.isEmpty()) {
            String tmp = saveTempImageIfPresent(file);
            if (tmp == null) {
                br.rejectValue("productId", null, "Ảnh tải lên không hợp lệ (jpg, jpeg, png, webp).");
            } else {
                dto.setProductImage(tmp); // ưu tiên hiển thị ảnh tạm
            }
        } else {
            // Không upload mới: nếu DTO chưa có productImage thì dùng ảnh hiện có
            if (StringUtils.isBlank(dto.getProductImage())) {
                dto.setProductImage(existingImage);
            }
        }

        if (br.hasErrors()) {
            return "admin/editProduct";
        }

        // Trùng tên (exclude chính nó)
        if (productRepository.existsByProductNameIgnoreCaseAndProductIdNot(dto.getProductName(), id)) {
            br.rejectValue("productName", null, "Tên sản phẩm đã tồn tại.");
            return "admin/editProduct";
        }

        // Xác định ảnh cuối cùng
        String finalName;
        if (StringUtils.isNotBlank(dto.getProductImage())) {
            finalName = isTemp(dto.getProductImage()) ? promoteIfTempOrKeep(dto.getProductImage()) : dto.getProductImage();
            if (finalName == null) {
                br.rejectValue("productId", null, "Không thể lưu ảnh sản phẩm. Vui lòng thử lại.");
                return "admin/editProduct";
            }
        } else {
            // Cho phép edit không cần ảnh (giữ existingImage)
            finalName = existingImage;
        }

        // Nếu dùng ảnh mới khác ảnh cũ -> xóa ảnh cũ an toàn
        String oldImage = p.getProductImage();
        if (StringUtils.isNotBlank(finalName) && !StringUtils.equals(finalName, oldImage)) {
            deleteImageQuietly(oldImage);
        }

        applyDTOToEntity(dto, p);
        p.setProductImage(finalName);

        productRepository.save(p);

        ra.addFlashAttribute("message", "Cập nhật sản phẩm thành công.");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/products";
    }

    /* ===================== DELETE / RESTORE ===================== */

    @RequestMapping(value = "/deleteProduct/{id}", method = {RequestMethod.POST, RequestMethod.DELETE, RequestMethod.GET})
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        Product p = opt.get();

        boolean referenced = orderDetailRepository.existsRefByProductId(id);

        if (referenced) {
            p.setStatus(false);
            productRepository.save(p);
            ra.addFlashAttribute("message", "Đã xóa mềm (ẨN) do sản phẩm đã phát sinh đơn hàng.");
            ra.addFlashAttribute("alertType", "success");
            return "redirect:/admin/products";
        }

        try {
            String img = p.getProductImage();
            productRepository.deleteById(id);
            deleteImageQuietly(img);
            ra.addFlashAttribute("message", "Xóa vĩnh viễn sản phẩm thành công.");
            ra.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            p.setStatus(false);
            productRepository.save(p);
            ra.addFlashAttribute("message", "Không xóa được vĩnh viễn. Đã chuyển sang trạng thái ẨN.");
            ra.addFlashAttribute("alertType", "warning");
        }
        return "redirect:/admin/products";
    }

    @RequestMapping(value = "/restoreProduct/{id}", method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.GET})
    public String restoreProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        Product p = opt.get();

        p.setStatus(true);
        productRepository.save(p);

        ra.addFlashAttribute("message", "Đã mở bán lại sản phẩm.");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/products";
    }

    /* ===================== HELPERS ===================== */

    private ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
                .productId(p.getProductId())
                .productName(StringUtils.defaultString(p.getProductName()))
                .categoryId(p.getCategory() != null ? p.getCategory().getCategoryId() : null)
                .nxbId(p.getNxb() != null ? p.getNxb().getId() : null)
                .price(p.getPrice())
                .quantity(p.getQuantity())
                .discount(p.getDiscount())
                .description(StringUtils.defaultString(p.getDescription()))
                .enteredDate(p.getEnteredDate())
                .status(p.getStatus() == null ? Boolean.TRUE : p.getStatus())
                .productImage(p.getProductImage()) // để hiển thị ảnh hiện tại
                .build();
    }

    private void applyDTOToEntity(ProductDTO dto, Product p) {
        p.setProductName(dto.getProductName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setQuantity(dto.getQuantity());
        p.setDiscount(dto.getDiscount());
        p.setEnteredDate(dto.getEnteredDate());
        p.setStatus(dto.getStatus());

        if (dto.getCategoryId() != null) {
            categoryRepository.findById(dto.getCategoryId()).ifPresent(p::setCategory);
        } else {
            p.setCategory(null);
        }

        if (dto.getNxbId() != null) {
            nxbRepository.findById(dto.getNxbId()).ifPresent(p::setNxb);
        } else {
            p.setNxb(null);
        }
    }

    /** Lưu ảnh TẠM (prefix __tmp__) để không mất khi form lỗi */
    private String saveTempImageIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String original = StringUtils.defaultString(file.getOriginalFilename()).trim();
        String ext = getExtension(original).toLowerCase(Locale.ROOT);
        if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
            log.warn("Invalid image extension: {}", ext);
            return null;
        }
        ensureUploadDir();
        String tempName = TMP_PREFIX + UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File target = new File(pathUploadImage, tempName);
        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(file.getBytes());
            return tempName;
        } catch (IOException e) {
            log.error("Save temp image error", e);
            return null;
        }
    }

    /** Nếu là file tạm (__tmp__...) thì đổi tên sang file chính; nếu không thì giữ nguyên */
    private String promoteIfTempOrKeep(String name) {
        if (StringUtils.isBlank(name)) return null;
        if (!isTemp(name)) return name;

        String ext = getExtension(name);
        String finalName = UUID.randomUUID().toString().replace("-", "") + "." + ext;

        File tempFile = new File(pathUploadImage, name);
        File finalFile = new File(pathUploadImage, finalName);
        try {
            ensureUploadDir();
            Files.move(tempFile.toPath(), finalFile.toPath());
            return finalName;
        } catch (Exception e) {
            log.error("Promote temp image failed: {}", name, e);
            return null;
        }
    }

    private boolean isTemp(String name) {
        return StringUtils.startsWith(name, TMP_PREFIX);
    }

    private void ensureUploadDir() {
        File dir = new File(pathUploadImage);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Cannot create upload dir: {}", pathUploadImage);
        }
    }

    private void deleteImageQuietly(String name) {
        if (StringUtils.isBlank(name)) return;
        // Không xóa file tạm ở đây để tránh race-condition nếu người dùng đang dùng lại.
        if (isTemp(name)) return;
        try {
            File f = new File(pathUploadImage, name);
            if (f.exists() && f.isFile() && !f.delete()) {
                log.warn("Cannot delete old image: {}", f.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Delete old image error: {}", name, e);
        }
    }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0 && i < filename.length() - 1) ? filename.substring(i + 1) : "";
    }
}
