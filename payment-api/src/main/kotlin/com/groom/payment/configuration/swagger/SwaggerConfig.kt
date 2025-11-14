package com.groom.payment.configuration.swagger

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig(
    @param:Value("\${spring.application.name:e-commerce-service-api}")
    private val applicationName: String,
) {
    companion object {
        const val SECURITY_SCHEME_NAME = "Bearer Authentication"
    }

    @Bean
    fun ecommerceOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title(applicationName)
                    .description("E-commerce REST API documentation")
                    .version("v1.0.0")
                    .license(License().name("Apache 2.0")),
            ).externalDocs(
                ExternalDocumentation()
                    .description("Spring Boot & SpringDoc reference")
                    .url("https://springdoc.org/"),
            ).components(
                Components()
                    .addSecuritySchemes(
                        SECURITY_SCHEME_NAME,
                        SecurityScheme()
                            .name(SECURITY_SCHEME_NAME)
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT 토큰을 입력하세요. (Bearer 접두사는 자동으로 추가됩니다)"),
                    ),
            ).addSecurityItem(
                SecurityRequirement().addList(SECURITY_SCHEME_NAME),
            )
}
