package com.recargapay.wallet.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Comparator;

@Configuration
public class OpenApiConfig {
    public static final String OPENAPI_DESCRIPTION = """
            API that allows you to create digital wallets,
            perform withdrawals, transfer funds between users,
            and check the current or historical balance of an account.
            Designed for simple, secure, and efficient wallet management within the RecargaPay platform.
            """;
    public static final String VERSION = "1.0.0";

    @Bean
    public OpenAPI springOpenAPI(@Value("${spring.application.name}") String appName) {
        return new OpenAPI()
                .info(new Info()
                        .title(appName)
                        .description(OPENAPI_DESCRIPTION)
                        .version(VERSION));
    }

    @Bean
    public ModelResolver modelResolver(@Qualifier("om") ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("wallet-service")
                .pathsToMatch("/**")
                .addOpenApiCustomizer(sortSchemasAlphabetically())
                .build();
    }

    private OpenApiCustomizer sortSchemasAlphabetically() {
        return openApi -> {
            if (openApi.getTags() != null) {
                openApi.setTags(openApi.getTags().stream()
                        .sorted(Comparator.comparing(tag -> StringUtils.stripAccents(tag.getName())))
                        .toList());
            }
        };
    }
}
