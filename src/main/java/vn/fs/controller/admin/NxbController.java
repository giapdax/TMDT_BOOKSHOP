package vn.fs.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.entities.NXB;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.ProductRepository;

import javax.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class NxbController {

    private final NxbRepository nxbRepository;
    private final ProductRepository productRepository;

    /* ===== LIST ===== */
    @GetMapping("/nxbs")
    public String list(Model model) {
        model.addAttribute("nxbs", nxbRepository.findAll());
        model.addAttribute("nxb", new NXB());
        return "admin/nxbs";
    }

    /* ===== CREATE ===== */
    @PostMapping("/addNxb")
    public String add(@Valid @ModelAttribute("nxb") NXB nxb,
                      BindingResult result, RedirectAttributes ra) {
        if (result.hasErrors()) {
            ra.addFlashAttribute("message", "Dữ liệu không hợp lệ!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }
        if (nxb.getStatus() == null) nxb.setStatus(true);
        nxbRepository.save(nxb);
        ra.addFlashAttribute("message", "Thêm NXB thành công!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/nxbs";
    }

    /* ===== EDIT PAGE ===== */
    @GetMapping("/editNxb/{id}")
    public String editPage(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        Optional<NXB> opt = nxbRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy NXB!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }
        model.addAttribute("nxb", opt.get());
        return "admin/editNxb";
    }

    /* ===== UPDATE ===== */
    @PostMapping("/editNxb/{id}")
    public String update(@PathVariable("id") Long id,
                         @ModelAttribute("nxb") NXB form,
                         RedirectAttributes ra) {
        Optional<NXB> opt = nxbRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy NXB!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }
        NXB n = opt.get();
        n.setName(form.getName());
        if (form.getStatus() != null) n.setStatus(form.getStatus());
        nxbRepository.save(n);
        ra.addFlashAttribute("message", "Cập nhật NXB thành công!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/nxbs";
    }

    /* ===== DELETE (soft nếu còn sản phẩm) ===== */
    @GetMapping("/deleteNxb/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<NXB> opt = nxbRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy NXB!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }
        NXB n = opt.get();

        long cntActive = productRepository.countActiveByNxb(id); // ✅ dùng đúng theo NXB
        if (cntActive > 0) {
            n.setStatus(false); // ngừng bán thay vì xóa cứng
            nxbRepository.save(n);
            ra.addFlashAttribute("message", "NXB còn sản phẩm đang bán → đã NGỪNG bán (ẩn).");
            ra.addFlashAttribute("alertType", "warning");
            return "redirect:/admin/nxbs";
        }

        try {
            nxbRepository.deleteById(id);
            ra.addFlashAttribute("message", "Xóa NXB thành công.");
            ra.addFlashAttribute("alertType", "success");
        } catch (Exception e) {
            n.setStatus(false);
            nxbRepository.save(n);
            ra.addFlashAttribute("message", "Không thể xóa do ràng buộc. Đã NGỪNG bán (ẩn).");
            ra.addFlashAttribute("alertType", "warning");
        }
        return "redirect:/admin/nxbs";
    }

    /* ===== RESTORE ===== */
    // nhận cả GET và POST, path /admin/restoreNxb/{id}
    @RequestMapping(value = "/restoreNxb/{id}", method = {RequestMethod.GET, RequestMethod.POST})
    public String restoreNxb(@PathVariable("id") Long id, RedirectAttributes ra) {
        nxbRepository.findById(id).ifPresent(n -> {
            n.setStatus(true);
            nxbRepository.save(n);
        });
        ra.addFlashAttribute("message", "Đã HIỂN THỊ lại NXB!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/nxbs";
    }


}
