    package com.auction.auth.jwtools;

    import java.io.IOException;
    import java.time.Instant;
    import java.util.Date;
    import java.util.List;
    import java.util.Optional;

    import org.springframework.beans.factory.annotation.Qualifier;
    import org.springframework.http.HttpMethod;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
    import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
    import org.springframework.security.web.util.matcher.RequestMatcher;
    import org.springframework.stereotype.Component;
    import org.springframework.web.filter.OncePerRequestFilter;
    import org.springframework.web.servlet.HandlerExceptionResolver;

    import com.auction.auth.RevokedToken;
    import com.auction.auth.RevokedTokenRepository;
    import com.auction.auth.exceptions.JwtExpiredException;
    import com.auction.users.UserService;

    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;

    /**
     * Bộ lọc bảo mật JWT (JwtSecurityFilter) kế thừa OncePerRequestFilter.
     * Đảm bảo chỉ được kích hoạt một lần duy nhất cho mỗi yêu cầu HTTP gửi đến.
     * Xác thực token JWT, kiểm tra thu hồi/cấm người dùng, và thiết lập Security Context.
     */
    @Component
    public class JwtSecurityFilter extends OncePerRequestFilter {

        private final JwtUtil jwtUtil;
        private final RevokedTokenRepository revokedTokenRepository;
        private final UserService userService;
        private final HandlerExceptionResolver resolver;
        private final List<RequestMatcher> publicMatchers;

        public JwtSecurityFilter(
            JwtUtil jwtUtil,
            UserService userService,
            RevokedTokenRepository revokedTokenRepository,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
        ) {
            this.jwtUtil = jwtUtil;
            this.userService = userService;
            this.revokedTokenRepository = revokedTokenRepository;
            this.resolver = resolver;
            
            // Danh sách các mẫu đường dẫn công khai được phép truy cập tự do mà không cần kiểm tra JWT
            this.publicMatchers = List.of(
                PathPatternRequestMatcher.pathPattern("/users/login"),
                PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
                PathPatternRequestMatcher.pathPattern("/swagger.json"),
                PathPatternRequestMatcher.pathPattern("/swagger-ui.html"),
                PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
                PathPatternRequestMatcher.pathPattern("/register"),
                PathPatternRequestMatcher.pathPattern("/login"),
                PathPatternRequestMatcher.pathPattern("/refresh"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/items/**"),
                PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/item/status/**")
            );
        }

        /**
         * Logic lọc chính, trích xuất và xác thực token JWT, nạp thông tin người dùng vào Security Context.
         */
        @Override
        public void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {
            String encodedToken = parseJwt(request);

            if (encodedToken == null) {
                filterChain.doFilter(request, response);
                return;
            }

            boolean isTokenValidated;

            try {
                isTokenValidated = jwtUtil.validateJwtToken(encodedToken);
            } catch (JwtExpiredException e) {
                resolver.resolveException(request, response, null, e);
                return;
            }

            if (isTokenValidated) {
                String username = jwtUtil.getUserFromToken(encodedToken);
                Date issuedAt = jwtUtil.getIssuedAtFromToken(encodedToken);

                // Kiểm tra xem tài khoản này có nằm trong danh sách bị thu hồi token / bị cấm hay không
                Optional<RevokedToken> revoked = revokedTokenRepository.findById(username);

                if (revoked.isPresent()) {
                    // Nếu thời gian phát hành token (issuedAt) xảy ra trước thời điểm bị cấm (bannedAt) -> Không xác thực
                    if (
                        !issuedAt
                            .toInstant()
                            .isAfter(
                                Instant.ofEpochMilli(revoked.get().getBannedAt())
                            )
                    ) {
                        filterChain.doFilter(request, response);
                        return;
                    }
                    // Nếu thời gian phát hành sau thời điểm cấm (người dùng đã được unbanned và đăng nhập lại), xóa bản ghi cấm khỏi DB
                    revokedTokenRepository.delete(revoked.get());
                }

                // Nạp thông tin tài khoản người dùng từ DB
                UserDetailsImpl userDetails = UserDetailsImpl.JPAtoUserDetails(
                    userService.getUserByUsername(username)
                );

                // Tạo đối tượng xác thực đại diện cho người dùng
                UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                    );
                authenticationToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );
                
                // Lưu đối tượng xác thực vào Security Context của thread hiện tại
                SecurityContextHolder.getContext().setAuthentication(
                    authenticationToken
                );
            }
            
            // Tiếp tục chuỗi lọc (filter chain)
            filterChain.doFilter(request, response);
        }

        /**
         * Xác định xem request hiện tại có cần chạy qua bộ lọc JWT hay không.
         * Bỏ qua bộ lọc nếu đường dẫn thuộc danh sách publicMatchers.
         */
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
            return publicMatchers.stream().anyMatch(m -> m.matches(request));
        }

        /**
         * Trích xuất mã token JWT từ Header "Authorization" trong request gửi tới.
         * 
         * @param request HTTP request nhận được
         * @return Chuỗi mã JWT sau khi loại bỏ tiền tố "Bearer ", hoặc null nếu không hợp lệ
         */
        public String parseJwt(HttpServletRequest request) {
            String authenticationHeader = request.getHeader("Authorization");

            if (
                authenticationHeader != null &&
                authenticationHeader.startsWith("Bearer ")
            ) {
                return authenticationHeader.substring(7);
            }
            return null;
        }
    }
