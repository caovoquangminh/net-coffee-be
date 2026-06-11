package com.netcoffee.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Trả về HTTP 401 (thay vì 403 mặc định của Spring Security) khi request không được xác thực: thiếu
 * token, token sai hoặc token đã HẾT HẠN.
 *
 * <p>Nhờ vậy frontend phân biệt rõ:
 *
 * <ul>
 *   <li>401 = "cần đăng nhập lại" → interceptor tự logout + đưa về màn hình login.
 *   <li>403 = "đã đăng nhập nhưng không đủ quyền" → giữ nguyên phiên.
 * </ul>
 *
 * <p>Khắc phục lỗi: sau khi tắt/mở lại máy ngày hôm sau, token 24h đã hết hạn nên mọi request trả
 * 403; frontend (chỉ bắt 401) bị kẹt ở màn hình trống. Với entry point này, token hết hạn → 401 →
 * app tự đưa user về đăng nhập thay vì trống.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("message", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
        body.put("timestamp", LocalDateTime.now().toString());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
