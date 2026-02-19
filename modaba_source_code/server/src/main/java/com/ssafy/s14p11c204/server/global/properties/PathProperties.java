package com.ssafy.s14p11c204.server.global.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "spring.paths")
public record PathProperties(
        @NotNull List<String> publicPaths,
        @NotNull List<String> protectedPaths
) {
}
