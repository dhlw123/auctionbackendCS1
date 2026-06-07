package com.auction.config.swag;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình Swagger/OpenAPI để hỗ trợ xác thực Bearer Token (JWT). Giúp kiểm thử (test) các API yêu
 * cầu đăng nhập trực tiếp trên giao diện Swagger UI.
 */
@Configuration
public class SwaggerCustomConfig {

  /** Tùy biến đối tượng OpenAPI để khai báo cơ chế bảo mật JWT Bearer Token. */
  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";
    return new OpenAPI()
        // Áp dụng quy tắc bảo mật JWT toàn cục cho các API tài liệu
        .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
        // Định nghĩa cấu hình Security Schemes cho Swagger
        .components(
            new Components()
                .addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
