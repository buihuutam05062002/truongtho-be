package com.truongtho.be.config;

import com.truongtho.be.enums.ErrorCode;
import com.truongtho.be.exception.ErrorBody;
import com.truongtho.be.security.FixedWindowLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;

public class RateLimitFilter extends OncePerRequestFilter {
    private static final String AUTH = "/api/v1/auth/";
    private final FixedWindowLimiter limiter = new FixedWindowLimiter(30, Duration.ofMinutes(1));
    private final ObjectMapper objectMapper;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String p = req.getRequestURI();
        boolean target = p.equals(AUTH + "login") || p.equals(AUTH + "register")
                || p.equals(AUTH + "refresh") || p.startsWith(AUTH + "oauth2");
        return !"POST".equals(req.getMethod()) || !target;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (limiter.tryAcquire(req.getRemoteAddr())) {
            chain.doFilter(req, res);
            return;
        }
        ErrorCode c = ErrorCode.TOO_MANY_REQUESTS;
        res.setStatus(c.getStatus().value());
        res.setHeader("Retry-After", String.valueOf(limiter.windowSeconds()));
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(),
                new ErrorBody(new ErrorBody.Err(c.getCode(), c.getMessage(), null)));
    }
}