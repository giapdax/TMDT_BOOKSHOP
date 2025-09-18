package vn.fs.controller.admin;

import java.security.Principal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import vn.fs.entities.Role;
import vn.fs.entities.User;
import vn.fs.repository.RoleRepository;
import vn.fs.repository.UserRepository;

@Controller
@RequestMapping("/admin/customers")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    // Danh sách người dùng
    @GetMapping
    public String list(Model model, Principal principal,
                       @RequestParam(value = "updated", required = false) String updated,
                       @RequestParam(value = "notfound", required = false) String notfound) {
        if (principal == null) return "redirect:/login";

        User me = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", me);

        List<User> users = userRepository.findAll();
        model.addAttribute("users", users);

        if (StringUtils.hasText(updated))  model.addAttribute("flash", "Cập nhật thành công!");
        if (StringUtils.hasText(notfound)) model.addAttribute("flashErr", "Không tìm thấy người dùng!");

        return "admin/users";
    }

    // Form chỉnh sửa quyền (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        var me = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", me);

        var target = userRepository.findById(id).orElse(null);
        if (target == null) return "redirect:/admin/customers?notfound=1";

        var allRoles = roleRepository.findAll();
        var selectedRoleIds = target.getRoles() == null ? java.util.Set.<Long>of()
                : target.getRoles().stream().map(r -> r.getId()).collect(java.util.stream.Collectors.toSet());

        model.addAttribute("target", target);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("selectedRoleIds", selectedRoleIds);

        return "admin/user-edit"; // <<< đúng với tên file
    }

    // Lưu quyền + trạng thái (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/edit/{id}")
    public String update(
            @PathVariable("id") Long id,
            @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
            @RequestParam(value = "enabled", required = false) String enabled // "on" nếu tick
    ) {
        User target = userRepository.findById(id).orElse(null);
        if (target == null) {
            return "redirect:/admin/customers?notfound=1";
        }

        // Cập nhật trạng thái: map checkbox -> boolean
        boolean isEnabled = StringUtils.hasText(enabled);
        // Đổi cho đúng field của entity (nếu của fen là enabled/active thì thay ở đây)
        target.setStatus(isEnabled);

        // Cập nhật vai trò
        Set<Role> newRoles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long rid : roleIds) {
                roleRepository.findById(rid).ifPresent(newRoles::add);
            }
        }
        target.setRoles(newRoles);

        userRepository.save(target);
        return "redirect:/admin/customers?updated=1";
    }
}
