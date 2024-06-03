package test.ticket.tickettools.web.controller;

import org.springframework.web.bind.annotation.ModelAttribute;
import test.ticket.tickettools.utils.JwtUtil;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public abstract class BaseController {

    protected Map<String, String> headers = new HashMap<>();
    protected String currentUser;

    @ModelAttribute
    public void setHeaders(HttpServletRequest request) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            String headerValue = request.getHeader(headerName);
            headers.put(headerName, headerValue);
        }
        parseToken(headers.get("authorization"));
    }

    private void parseToken(String token) {
        // 假设 token 是 "Bearer <token>"
        if (token != null && token.startsWith("Bearer ")) {
            String actualToken = token.substring(7);
            // 解析 token 获取当前用户信息
            // 这里你可以调用你自己的解析逻辑
            this.currentUser = JwtUtil.getUsernameFromToken(actualToken);
        }
    }

}

