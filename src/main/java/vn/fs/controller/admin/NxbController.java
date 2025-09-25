package vn.fs.controller.admin;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.dto.NxbDTO;
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
        if (!model.containsAttribute("nxb")) {
            model.addAttribute("nxb", NxbDTO.builder().status(true).build());
        }
        return "admin/nxbs";
    }

    /* ===== CREATE ===== */
    @PostMapping("/addNxb")
    public String add(@Valid @ModelAttribute("nxb") NxbDTO dto,
                      BindingResult br,
                      Model model,
                      RedirectAttributes ra) {

        dto.normalize();

        // Validate trùng tên
        if (StringUtils.isNotBlank(dto.getName())
                && nxbRepository.existsByNameIgnoreCase(dto.getName())) {
            br.rejectValue("name", null, "Tên NXB đã tồn tại.");
        }

        if (br.hasErrors()) {
            // ở lại trang list và mở lại modal
            model.addAttribute("nxbs", nxbRepository.findAll());
            model.addAttribute("openAddModal", true);
            return "admin/nxbs";
        }

        NXB entity = new NXB();
        applyDTOToEntity(dto, entity);
        nxbRepository.save(entity);

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
        model.addAttribute("nxb", toDTO(opt.get()));
        return "admin/editNxb";
    }

    /* ===== UPDATE ===== */
    @PostMapping("/editNxb/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("nxb") NxbDTO dto,
                         BindingResult br,
                         RedirectAttributes ra,
                         Model model) {

        Optional<NXB> opt = nxbRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy NXB!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }

        dto.normalize();

        // Check trùng tên (trừ chính nó)
        if (StringUtils.isNotBlank(dto.getName())
                && nxbRepository.existsByNameIgnoreCaseAndIdNot(dto.getName(), id)) {
            br.rejectValue("name", null, "Tên NXB đã tồn tại.");
        }

        if (br.hasErrors()) {
            return "admin/editNxb";
        }

        NXB entity = opt.get();
        applyDTOToEntity(dto, entity);
        nxbRepository.save(entity);

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

        long cntActive = productRepository.countActiveByNxb(id);
        if (cntActive > 0) {
            n.setStatus(false);
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

    /* ===== Helpers ===== */
    private NxbDTO toDTO(NXB n) {
        return NxbDTO.builder()
                .id(n.getId())
                .name(StringUtils.defaultString(n.getName()))
                .status(n.getStatus() == null ? Boolean.TRUE : n.getStatus())
                .build();
    }

    private void applyDTOToEntity(NxbDTO dto, NXB n) {
        n.setName(dto.getName());
        n.setStatus(dto.getStatus() == null ? Boolean.TRUE : dto.getStatus());
    }
}
