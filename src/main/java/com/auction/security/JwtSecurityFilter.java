package com.auction.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    private JwtUtil jwtUtil;
    private CustomUserDetailsService userDetailsService;

    public JwtSecurityFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;

    }

    /**
     * Filters incoming requests to validate the JWT token and set the authentication context.
     */
    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String encodedToken = parseJwt(request);
        // allow unauthenticated requests to proceed, let controller-level security handle authorization
        if (encodedToken != null && jwtUtil.validateJwtToken(encodedToken)) {
            String username = jwtUtil.getUserFromToken(encodedToken);
            UserDetailsImpl userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        }
        filterChain.doFilter(request, response);
    }

    /**
     * Parses the JWT token from the Authorization header.
     *
     * @param request The HTTP request.
     * @return The JWT token string, or null if not found.
     */
    public String parseJwt(HttpServletRequest request) {
        String authenticationHeader = request.getHeader("Authorization");

        if (authenticationHeader != null && authenticationHeader.startsWith("Bearer ")) {
            return authenticationHeader.substring(7);
        }
        return null;

    }

    /**
     * Specifies which paths should not be filtered by this filter.
     *
     * @param request The HTTP request.
     * @return True if the path should not be filtered, false otherwise.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        return path.equals("/users/login") || path.equals("/users/register") || path.equals("/users/refresh");
    }
}
