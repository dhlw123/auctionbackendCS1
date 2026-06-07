package com.auction.config;

import com.auction.auth.jwtools.JwtSecurityFilter;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cấu hình bảo mật hệ thống Spring Security.
 * Thiết lập các quy tắc phân quyền (Authorization), mã hóa mật khẩu, cấu hình CORS và cơ chế JWT.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtSecurityFilter jwtSecurityFilter;

    public SecurityConfig(JwtSecurityFilter jwtSecurityFilter) {
        this.jwtSecurityFilter = jwtSecurityFilter;
    }

    /**
     * Cấu hình PasswordEncoder sử dụng thuật toán BCrypt để mã hóa mật khẩu người dùng.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Cấu hình CORS (Cross-Origin Resource Sharing) cho phép frontend (React chạy trên cổng 5173 hoặc 3000)
     * có thể gọi API từ backend này.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Cho phép các nguồn gốc (Origin) truy cập
        configuration.setAllowedOrigins(
                Arrays.asList("http://localhost:5173", "http://localhost:3000")
        );
        // Cho phép các phương thức HTTP
        configuration.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        // Cho phép các Headers yêu cầu từ phía client
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "content-type", "x-auth-token")
        );
        // Expose header để client có thể đọc x-auth-token
        configuration.setExposedHeaders(Arrays.asList("x-auth-token"));
        // Cho phép truyền kèm thông tin xác thực (Credentials như Cookies, Authorization headers)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Cấu hình bộ lọc bảo mật chính (SecurityFilterChain) định nghĩa cách kiểm soát các request HTTP gửi đến.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults()) // Áp dụng cấu hình CORS ở trên
                .csrf(csrf -> csrf.disable())   // Vô hiệu hóa CSRF vì ứng dụng sử dụng Stateless JWT
                .sessionManagement(s ->
                        s.sessionCreationPolicy(
                                org.springframework.security.config.http.SessionCreationPolicy.STATELESS // Không lưu session phía server
                        )
                )
                .authorizeHttpRequests(auth ->
                        auth
                                // Cấu hình các đường dẫn công khai (không cần đăng nhập)
                                .requestMatchers(
                                        "/users/login",
                                        "/swagger-ui/**",
                                        "/swagger.json",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/register",
                                        "/login",
                                        "/refresh"
                                )
                                .permitAll()
                                // Cho phép tất cả mọi người đọc thông tin sản phẩm và trạng thái sản phẩm (GET)
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/items/**",
                                        "/item/status/**"
                                )
                                .permitAll()
                                // Các API quản trị bắt buộc người dùng có quyền (Role) là ADMIN
                                .requestMatchers("/admin/**")
                                .hasRole("ADMIN")
                                // Tất cả các yêu cầu khác đều bắt buộc phải xác thực (đăng nhập)
                                .anyRequest()
                                .authenticated()
                );

        // Thêm bộ lọc xác thực JwtSecurityFilter vào trước UsernamePasswordAuthenticationFilter
        http.addFilterBefore(
                jwtSecurityFilter,
                UsernamePasswordAuthenticationFilter.class
        );
        http.logout(logout -> logout.disable());
        return http.build();
    }
}
