package vn.fs.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.entities.Role;
import vn.fs.entities.User;
import vn.fs.repository.RoleRepository;
import vn.fs.repository.UserRepository;
import vn.fs.service.UserAdminService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<User> listAll() {
        return userRepository.findAll();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Set<Long> roleIdsOf(User user) {
        if (user == null || user.getRoles() == null) return Set.of();
        Set<Long> ids = new HashSet<>();
        user.getRoles().forEach(r -> ids.add(r.getId()));
        return ids;
    }

    @Override
    @Transactional
    public void updateRolesAndStatus(Long userId, List<Long> roleIds, boolean enabled) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) return;
        u.setStatus(enabled);
        Set<Role> newRoles = new HashSet<>();
        if (roleIds != null) {
            for (Long rid : roleIds) {
                roleRepository.findById(rid).ifPresent(newRoles::add);
            }
        }
        u.setRoles(newRoles);
        userRepository.save(u);
    }
}
