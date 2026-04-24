package com.auction.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtSecurityFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtSecurityFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;

    }

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String encodedToken = parseJwt(request);
            if (jwtUtil.validateJwtToken(encodedToken)) {
                String username = jwtUtil.getUserFromToken(encodedToken);
                UserDetailsImpl userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

            }
        } catch (Exception e) {
            System.err.printf("Can't set user authentication: {%s}", e.getMessage());
        }
        filterChain.doFilter(request, response);
    }

    public String parseJwt(HttpServletRequest request) {
        String authenticationHeader = request.getHeader("Authorization");

        if (authenticationHeader != null && authenticationHeader.startsWith("Bearer ")) {
            return authenticationHeader.substring(7);
        }
        return null;

    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getServletPath();

        return path.equals("/users/login") || path.equals("/users/signin");
    }
}
