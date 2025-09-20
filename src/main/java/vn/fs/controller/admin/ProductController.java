package vn.fs.controller.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.entities.Category;
import vn.fs.entities.NXB;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.repository.OrderDetailRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

    // ======= DROPDOWN DATA (CHIA RIÊNG CHO ADD vs EDIT) =======

    // Dùng ở trang THÊM sản phẩm: chỉ hiện danh sách đang hiển thị (status = true)
    @ModelAttribute("categoryActiveList")
    public List<Category> categoryActiveList() {
        return categoryRepository.findByStatusTrueOrderByCategoryNameAsc();
    }
    @ModelAttribute("nxbActiveList")
    public List<NXB> nxbActiveList() {
        return nxbRepository.findByStatusTrueOrderByNameAsc();
    }

    // Dùng ở trang SỬA sản phẩm: hiện FULL, kể cả ẩn/chưa có SP
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
        model.addAttribute("product", new Product()); // form thêm
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
        model.addAttribute("product", product);
        return "admin/editProduct";
    }

    /* ===================== CREATE ===================== */

    @PostMapping("/addProduct")
    public String addProduct(@ModelAttribute("product") Product product,
                             @RequestParam("file") MultipartFile file,
                             @RequestParam(value = "nxb.id", required = false) Long nxbId,
                             RedirectAttributes ra) {

        if (StringUtils.isBlank(product.getProductName())) {
            ra.addFlashAttribute("message", "Tên sản phẩm không được để trống!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        if (product.getCategory() == null || product.getCategory().getCategoryId() == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn thể loại!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }

        if (nxbId != null) nxbRepository.findById(nxbId).ifPresent(product::setNxb);
        else product.setNxb(null);

        if (product.getEnteredDate() == null) product.setEnteredDate(new Date());
        if (product.getDiscount() < 0) product.setDiscount(0);
        if (product.getQuantity() < 0) product.setQuantity(0);
        if (product.getPrice() < 0) product.setPrice(0.0);
        if (product.getStatus() == null) product.setStatus(true); // mặc định hiển thị

        String savedName = saveImageIfPresent(file);
        if (savedName == null) {
            ra.addFlashAttribute("message", "Vui lòng chọn ảnh hợp lệ (jpg, jpeg, png, webp)!");
            ra.addFlashAttribute("alertType", "warning");
            return "redirect:/admin/products";
        }
        product.setProductImage(savedName);

        productRepository.save(product);
        ra.addFlashAttribute("message", "Thêm sản phẩm thành công");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/products";
    }

    /* ===================== UPDATE ===================== */

    @PostMapping("/editProduct/{id}")
    public String updateProduct(@PathVariable("id") Long id,
                                @ModelAttribute("product") Product form,
                                @RequestParam(value = "file", required = false) MultipartFile file,
                                @RequestParam(value = "existingImage", required = false) String existingImage,
                                @RequestParam(value = "nxb.id", required = false) Long nxbId,
                                RedirectAttributes ra) {

        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        Product p = opt.get();

        p.setProductName(form.getProductName());
        p.setDescription(form.getDescription());
        p.setPrice(Math.max(0.0, form.getPrice()));
        p.setQuantity(Math.max(0, form.getQuantity()));
        p.setDiscount(Math.max(0, form.getDiscount()));
        p.setEnteredDate(form.getEnteredDate() != null ? form.getEnteredDate() : p.getEnteredDate());
        p.setStatus(form.getStatus() != null ? form.getStatus() : p.getStatus());

        if (form.getCategory() != null && form.getCategory().getCategoryId() != null) {
            categoryRepository.findById(form.getCategory().getCategoryId()).ifPresent(p::setCategory);
        }

        if (nxbId != null) nxbRepository.findById(nxbId).ifPresent(p::setNxb);
        else p.setNxb(null);

        String oldImage = p.getProductImage();
        if (file != null && !file.isEmpty()) {
            String newName = saveImageIfPresent(file);
            if (newName == null) {
                ra.addFlashAttribute("message", "Ảnh tải lên không hợp lệ!");
                ra.addFlashAttribute("alertType", "warning");
                return "redirect:/admin/editProduct/" + id;
            }
            p.setProductImage(newName);
            deleteImageQuietly(oldImage);
        } else {
            p.setProductImage(existingImage);
        }

        productRepository.save(p);
        ra.addFlashAttribute("message", "Cập nhật sản phẩm thành công");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/products";
    }

    /* ===================== DELETE / RESTORE ===================== */

    @RequestMapping(value = "/deleteProduct/{id}", method = {RequestMethod.POST, RequestMethod.DELETE})
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        return doDeleteProduct(id, ra);
    }
    @GetMapping("/deleteProduct/{id}")
    public String deleteProductGet(@PathVariable("id") Long id, RedirectAttributes ra) {
        return doDeleteProduct(id, ra);
    }

    private String doDeleteProduct(Long id, RedirectAttributes ra) {
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
            ra.addFlashAttribute("message", "Đã xóa thành công và cập nhật trạng thái (đã ẨN).");
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
            ra.addFlashAttribute("message", "Đã xóa thành công và cập nhật trạng thái (đã ẨN).");
            ra.addFlashAttribute("alertType", "warning");
        }
        return "redirect:/admin/products";
    }

    @RequestMapping(value = "/restoreProduct/{id}", method = {RequestMethod.POST, RequestMethod.PUT})
    public String restoreProduct(@PathVariable("id") Long id, RedirectAttributes ra) {
        return doRestoreProduct(id, ra);
    }
    @GetMapping("/restoreProduct/{id}")
    public String restoreProductGet(@PathVariable("id") Long id, RedirectAttributes ra) {
        return doRestoreProduct(id, ra);
    }

    private String doRestoreProduct(Long id, RedirectAttributes ra) {
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

    private String saveImageIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String original = StringUtils.defaultString(file.getOriginalFilename()).trim();
        String ext = getExtension(original).toLowerCase(Locale.ROOT);
        if (!(ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp"))) {
            log.warn("Invalid image extension: {}", ext);
            return null;
        }

        ensureUploadDir();
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        File target = new File(pathUploadImage, uniqueName);

        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(file.getBytes());
            return uniqueName;
        } catch (IOException e) {
            log.error("Save image error", e);
            return null;
        }
    }

    private void ensureUploadDir() {
        File dir = new File(pathUploadImage);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("Cannot create upload dir: {}", pathUploadImage);
        }
    }

    private void deleteImageQuietly(String name) {
        if (StringUtils.isBlank(name)) return;
        try {
            File f = new File(pathUploadImage, name);
            if (f.exists() && isFileDeletable(f) && !f.delete()) {
                log.warn("Cannot delete old image: {}", f.getAbsolutePath());
            }
        } catch (Exception e) {
            log.warn("Delete old image error: {}", name, e);
        }
    }

    private boolean isFileDeletable(File f) { return f.isFile(); }

    private String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return (i >= 0 && i < filename.length() - 1) ? filename.substring(i + 1) : "";
    }
}
