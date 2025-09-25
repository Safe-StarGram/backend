package com.github.config;

import com.github.jwt.JwtTokenProvider;
import com.github.token.JwtAuthFilter;
import com.github.token.OAuth2LoginSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenProvider jwt;

    @Autowired(required = false)
    private OAuth2LoginSuccessHandler oauth2SuccessHandler;

    @Autowired(required = false)
    private ClientRegistrationRepository oauth2ClientRepo;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authorizeHttpRequests(reg -> reg
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/join", "/auth/login",
                        "/auth/auto-refresh", "/auth/logout").permitAll()
                .requestMatchers(HttpMethod.GET, "/auth/debug-cookies").permitAll()
                .requestMatchers(HttpMethod.GET, "/departments/**", "/positions/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/profiles/**").permitAll()  // 프로필 조회는 허용
                .requestMatchers(HttpMethod.PATCH, "/profiles/**").authenticated()  // 프로필 수정은 인증 필요
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/areas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/areas/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/stats/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()  // 게시글 조회는 인증 없이 허용
                .requestMatchers("/api/comment/**").permitAll()
                .requestMatchers("/notifications/**").permitAll()
                .requestMatchers("/debug/**").permitAll()  // 디버그 엔드포인트 허용
                .requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")  // 관리자 API는 ROLE_ADMIN 권한 필요
                .requestMatchers(HttpMethod.PATCH, "/api/posts/manager-risk/**").authenticated()  // 관리자 위험성 평가는 인증 필요
                .anyRequest().authenticated()
        );

        if (oauth2ClientRepo != null) {
            http.oauth2Login(oauth -> {
                if (oauth2SuccessHandler != null) {
                    oauth.successHandler(oauth2SuccessHandler);
                }
            });
        }

        http.addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);

        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다.\"}");
                })
                .accessDeniedHandler((req, res, e) -> {
                    res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"status\":403,\"error\":\"Forbidden\",\"message\":\"접근 권한이 없습니다.\"}");
                })
        );
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경과 프로덕션 환경 모두 지원
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:5173",
            "http://localhost:3001",
            "http://localhost:8080",
            "http://127.0.0.1:3000",
            "http://127.0.0.1:3001",
            "https://safe-stargram.vercel.app",
            "https://chan23.duckdns.org",
            "https://www.chan23.duckdns.org"
        ));
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Set-Cookie"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}

