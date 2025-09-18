package vn.fs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import vn.fs.entities.Role;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserDetailService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Cho phép đăng nhập bằng username HOẶC email (ignore case).
     * Set principalName về username "chuẩn" để SuccessHandler có thể reset by username.
     */
    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        String key = (identifier == null) ? "" : identifier.trim();

        Optional<User> opt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(key, key);
        User u = opt.orElseThrow(() -> new UsernameNotFoundException("Invalid username or password."));

        var authorities = (u.getRoles() == null ? java.util.List.<Role>of() : u.getRoles())
                .stream()
                .filter(Objects::nonNull)
                .map(Role::getName)
                .filter(Objects::nonNull)
                .map(name -> name.startsWith("ROLE_") ? name : "ROLE_" + name)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        boolean enabled = Boolean.TRUE.equals(u.getStatus());
        boolean accountNonLocked = u.getLockedUntil() == null || !u.getLockedUntil().isAfter(LocalDateTime.now());

        return org.springframework.security.core.userdetails.User
                .withUsername(u.getUsername() != null ? u.getUsername() : key)
                .password(u.getPassword())
                .authorities(authorities)
                .accountLocked(!accountNonLocked)
                .disabled(!enabled)
                .accountExpired(false)
                .credentialsExpired(false)
                .build();
    }
}
