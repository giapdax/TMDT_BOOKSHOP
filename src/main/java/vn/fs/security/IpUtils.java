package vn.fs.security;

import javax.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils(){}

    public static String getClientIp(HttpServletRequest req){
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        h = req.getHeader("X-Real-IP");
        if (h != null && !h.isBlank()) return h.trim();
        return req.getRemoteAddr();
    }
}
