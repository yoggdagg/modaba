package com.ssafy.s14p11c204.server.global.config;

import com.ssafy.s14p11c204.server.global.properties.PathProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Configuration
public class SwaggerConfig {
    private final List<PathPattern> publicPatterns;
    private final List<PathPattern> protectedPatterns;

    public SwaggerConfig(PathProperties pathProperties) {
        this.publicPatterns = pathProperties.publicPaths().stream().map(PathPatternParser.defaultInstance::parse)
                .toList();
        this.protectedPatterns = pathProperties.protectedPaths().stream().map(PathPatternParser.defaultInstance::parse)
                .toList();
    }

    @Bean
    public OpenApiCustomizer securityResponseCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {
            PathContainer pathContainer = PathContainer.parsePath(path);
            boolean matchProtected = protectedPatterns.stream().anyMatch(pattern -> pattern.matches(pathContainer));
            boolean matchPublic = publicPatterns.stream().anyMatch(pattern -> pattern.matches(pathContainer));

            if (matchProtected && !matchPublic) {
                pathItem.readOperations().forEach(operation -> {
                    if (!operation.getResponses().containsKey("401")) {
                        operation.getResponses().addApiResponse("401",
                                new ApiResponse().description("UNAUTHORIZED : 인증 자격 증명이 유효하지 않거나 누락되었습니다."));
                    }
                });
            }
        });
    }

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("경도 프로젝트 API 명세서")
                .description("경도 프로젝트의 API 명세서입니다.")
                .version("v0");

        SecurityScheme securityScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        SecurityRequirement securityRequirement = new SecurityRequirement().addList("bearerAuth");

        return new OpenAPI()
                .components(new Components().addSecuritySchemes("bearerAuth", securityScheme))
                .info(info)
                .addSecurityItem(securityRequirement);
    }

    @Bean
    public GroupedOpenApi authOpenApi() {
        return GroupedOpenApi.builder()
                .group("Auth API")
                .pathsToMatch("/api/v0/auth/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi accountOpenApi() {
        return GroupedOpenApi.builder()
                .group("Account API")
                .pathsToMatch("/api/v0/users/**") // users로 수정 (기존 코드 참고)
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi roomOpenApi() {
        return GroupedOpenApi.builder()
                .group("Room API")
                .pathsToMatch("/api/v0/rooms/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi gameOpenApi() {
        return GroupedOpenApi.builder()
                .group("Game API")
                .pathsToMatch("/api/v0/game/**", "/api/v0/games/**", "/api/v0/test/game/**") // test 경로 추가
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi friendOpenApi() {
        return GroupedOpenApi.builder()
                .group("Friend API")
                .pathsToMatch("/api/v0/friends/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi regionOpenApi() {
        return GroupedOpenApi.builder()
                .group("Region API")
                .pathsToMatch("/api/v0/regions/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }
// OAuth와 Chat API 모두를 포함하도록 정리했습니다.
    @Bean
    public GroupedOpenApi oauthOpenApi() {
        return GroupedOpenApi.builder()
                .group("OAuth API")
                .pathsToMatch("/api/v0/oauth/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi chatOpenApi() {
        return GroupedOpenApi.builder()
                .group("Chat API")
                .pathsToMatch("/api/v0/chat/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi aiOpenApi() {
        return GroupedOpenApi.builder()
                .group("AI API")
                .pathsToMatch("/api/v0/ai/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi activityOpenApi() {
        return GroupedOpenApi.builder()
                .group("Activity API")
                .pathsToMatch("/api/v0/activities/**")
                .addOpenApiCustomizer(securityResponseCustomizer())
                .build();
    }
}