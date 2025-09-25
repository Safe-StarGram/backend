package com.github.token;

import com.github.jwt.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwt;

    private static final AntPathMatcher matcher = new AntPathMatcher();

    private static final String[] PUBLIC_PATTERNS = {
            "/auth/**", "/uploads/**", "/error"
    };


    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        final String uri = req.getRequestURI();
        for (String p : PUBLIC_PATTERNS) {
            if (matcher.match(p, uri)) {
                chain.doFilter(req, res);
                return;
            }
        }
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }
        String token = header.substring(7);
        try {
            if (jwt.validate(token)) {
                String userId = String.valueOf(jwt.getUserId(token));
                String role = jwt.getRole(token);
                String authority = (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role;
                
                System.out.println("JwtAuthFilter - UserId: " + userId);
                System.out.println("JwtAuthFilter - JWT Role: " + role);
                System.out.println("JwtAuthFilter - Spring Authority: " + authority);
                
                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        role != null ? List.of(new SimpleGrantedAuthority(authority)) : List.of()
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
                chain.doFilter(req, res);
                return;
            }
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"잘못된 또는 만료된 토큰\"}");
        } catch (JwtException ex) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"토큰 검증 실패\"}");
        }
    }

}
