package vn.fs.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import vn.fs.service.IpLockService;
import vn.fs.service.UserLockService;

import javax.servlet.ServletException;               // <--- chắc chắn import javax (Boot 2.x)
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Autowired
    private UserLockService userLockService;

    @Autowired
    private IpLockService ipLockService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {   // <--- thêm ServletException ở đây

        String loginId = request.getParameter("username");
        String ip = IpUtils.getClientIp(request);

        if (loginId != null && !loginId.trim().isEmpty()) {
            userLockService.onFailure(loginId.trim());
        }
        ipLockService.onFailure(ip);

        super.setDefaultFailureUrl("/login?error");
        super.onAuthenticationFailure(request, response, exception); // method này có thể ném ServletException
    }
}
