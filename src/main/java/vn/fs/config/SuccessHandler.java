package vn.fs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.fs.dto.SessionUser;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;
import vn.fs.security.IpUtils;
import vn.fs.service.IpLockService;
import vn.fs.service.UserLockService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired private UserLockService userLockService;
    @Autowired private IpLockService   ipLockService;

    // Lấy user theo username/email để đẩy vào session
    @Autowired private UserRepository  userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth)
            throws IOException, ServletException {

        // Reset counters khi login thành công
        userLockService.onSuccess(auth.getName());
        ipLockService.onSuccess(IpUtils.getClientIp(req));

        // Đặt "customer" vào session dưới dạng DTO an toàn
        String loginId = auth.getName();
        User me = userRepository.findByUsername(loginId);
        if (me == null) {
            me = userRepository.findByEmail(loginId);
        }
        if (me != null) {
            SessionUser su = SessionUser.from(me);
            req.getSession().setAttribute("customer", su);
        }

        // Điều hướng theo role
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
        boolean isStaff = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_STAFF"::equals);

        if (isAdmin || isStaff) {
            getRedirectStrategy().sendRedirect(req, res, "/admin/home");
        } else {
            // Về trang đã request trước đó hoặc "/"
            super.onAuthenticationSuccess(req, res, auth);
        }
    }
}
