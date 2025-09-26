package vn.fs.security;

import javax.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils(){}

    public static String getClientIp(HttpServletRequest req){
        // 1. Thử lấy header "X-Forwarded-For" (được set bởi proxy)
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank())
            return h.split(",")[0].trim(); // Nếu có nhiều IP thì lấy cái đầu tiên (IP của client thật sự)

        // 2. Nếu không có, thử lấy header "X-Real-IP" (cũng thường do proxy thêm vào)
        h = req.getHeader("X-Real-IP");
        if (h != null && !h.isBlank())
            return h.trim();

        // 3. Nếu không có gì cả, fallback về IP trực tiếp từ request
        return req.getRemoteAddr();
    }

}
