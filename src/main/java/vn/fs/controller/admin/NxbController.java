package vn.fs.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vn.fs.entities.NXB;
import vn.fs.entities.User;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.UserRepository;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class NxbController {

    @Autowired NxbRepository nxbRepository;
    @Autowired UserRepository userRepository;

    @ModelAttribute("user")
    public User user(Model model, Principal principal, User user) {
        if (principal != null) {
            model.addAttribute("user", new User());
            user = userRepository.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }
        return user;
    }

    @ModelAttribute("nxbs")
    public List<NXB> listNxb(Model model) {
        List<NXB> nxbs = nxbRepository.findAll();
        model.addAttribute("nxbs", nxbs);
        return nxbs;
    }

    @GetMapping("/nxbs")
    public String nxbs(Model model) {
        model.addAttribute("nxb", new NXB());
        return "admin/nxbs"; // sửa đúng tên view
    }

    @PostMapping("/addNxb")
    public String addNxb(@Validated @ModelAttribute("nxb") NXB nxb,
                         BindingResult bindingResult, ModelMap model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Dữ liệu không hợp lệ!");
            return "admin/nxbs";
        }
        nxbRepository.save(nxb);
        model.addAttribute("message", "Thêm NXB thành công!");
        return "redirect:/admin/nxbs";
    }

    @GetMapping("/editNxb/{id}")
    public String editNxb(@PathVariable("id") Long id, ModelMap model) {
        NXB nxb = nxbRepository.findById(id).orElse(null);
        model.addAttribute("nxb", nxb);
        return "admin/editNxb";
    }

    @PostMapping("/editNxb/{id}")
    public String updateNxb(@PathVariable("id") Long id,
                            @Validated @ModelAttribute("nxb") NXB form,
                            BindingResult result, ModelMap model) {
        if (result.hasErrors()) {
            return "admin/editNxb";
        }
        NXB nxb = nxbRepository.findById(id).orElse(null);
        if (nxb == null) return "redirect:/admin/nxbs";
        nxb.setName(form.getName());
        nxbRepository.save(nxb);
        return "redirect:/admin/nxbs";
    }

    @GetMapping("/deleteNxb/{id}")
    public String deleteNxb(@PathVariable("id") Long id, Model model) {
        nxbRepository.deleteById(id);
        model.addAttribute("message", "Xoá NXB thành công!");
        return "redirect:/admin/nxbs";
    }
}
