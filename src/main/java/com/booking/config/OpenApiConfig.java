package com.booking.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Resource Booking API")
                        .version("1.0.0")
                        .description("Booking API for shared resources (rooms, vehicles, equipment) "
                                + "with JWT auth and ADMIN/USER roles. Log in via POST /auth/login and paste "
                                + "the token into Authorize to try the protected endpoints."))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
