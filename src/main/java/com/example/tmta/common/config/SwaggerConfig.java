package com.example.tmta.common.config;

import com.example.tmta.common.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.media.MediaType;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
public class SwaggerConfig {
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("\\b[A-Z]{1,2}\\d{3}\\b");
    private static final String ERROR_RESPONSE_REF = "#/components/schemas/ErrorResponse";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local"),
                        new Server()
                                .url("https://api.tm-ta.com")
                                .description("Production")
                ))
                .info(apiInfo());
    }

    @Bean
    public OpenApiCustomizer errorResponseExamplesCustomizer() {
        final Map<String, ErrorCode> errorCodeByCode = new LinkedHashMap<>();
        for (ErrorCode errorCode : ErrorCode.values()) {
            errorCodeByCode.put(errorCode.getCode(), errorCode);
        }

        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem == null || pathItem.readOperations() == null) {
                    return;
                }
                for (Operation operation : pathItem.readOperations()) {
                    if (operation.getResponses() == null) {
                        continue;
                    }
                    for (ApiResponse apiResponse : operation.getResponses().values()) {
                        Content content = apiResponse.getContent();
                        if (content == null || content.isEmpty()) {
                            continue;
                        }

                        Set<String> codesInDescription = extractErrorCodes(apiResponse.getDescription());
                        if (codesInDescription.isEmpty()) {
                            continue;
                        }

                        content.forEach((mediaTypeKey, mediaType) -> {
                            if (!isErrorResponseSchema(mediaType)) {
                                return;
                            }

                            Map<String, Example> examples = new LinkedHashMap<>();
                            for (String code : codesInDescription) {
                                ErrorCode errorCode = errorCodeByCode.get(code);
                                if (errorCode == null) {
                                    continue;
                                }
                                Example example = new Example();
                                example.setSummary(errorCode.name());
                                example.setValue(Map.of(
                                        "code", errorCode.getCode(),
                                        "message", errorCode.getMessage(),
                                        "timestamp", Instant.parse("2026-02-21T10:15:30Z").toString()
                                ));
                                examples.put(errorCode.getCode(), example);
                            }

                            if (!examples.isEmpty()) {
                                mediaType.setExamples(examples);
                            }
                        });
                    }
                }
            });
        };
    }

    @Bean
    public OpenApiCustomizer commonErrorResponsesCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem == null || pathItem.readOperations() == null) {
                    return;
                }
                for (Operation operation : pathItem.readOperations()) {
                    if (operation.getResponses() == null) {
                        continue;
                    }
                    addErrorResponseIfAbsent(operation, "405", "지원하지 않는 메서드입니다. (C002)");
                    addErrorResponseIfAbsent(operation, "415", "지원하지 않는 Content-Type 입니다. (C008)");
                    addErrorResponseIfAbsent(operation, "406", "지원하지 않는 응답 형식입니다. (C009)");
                }
            });
        };
    }

    private static boolean isErrorResponseSchema(io.swagger.v3.oas.models.media.MediaType mediaType) {
        if (mediaType == null || mediaType.getSchema() == null) {
            return false;
        }

        Schema<?> schema = mediaType.getSchema();
        if (ERROR_RESPONSE_REF.equals(schema.get$ref())) {
            return true;
        }

        if (schema.getAllOf() != null) {
            for (Schema<?> inner : schema.getAllOf()) {
                if (ERROR_RESPONSE_REF.equals(inner.get$ref())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> extractErrorCodes(String description) {
        Set<String> codes = new LinkedHashSet<>();
        if (description == null || description.isBlank()) {
            return codes;
        }

        Matcher matcher = ERROR_CODE_PATTERN.matcher(description);
        while (matcher.find()) {
            codes.add(matcher.group());
        }
        return codes;
    }

    private static void addErrorResponseIfAbsent(Operation operation, String statusCode, String description) {
        if (operation.getResponses().containsKey(statusCode)) {
            return;
        }

        ApiResponse response = new ApiResponse();
        response.setDescription(description);

        MediaType mediaType = new MediaType();
        Schema<?> schema = new Schema<>();
        schema.set$ref(ERROR_RESPONSE_REF);
        mediaType.setSchema(schema);

        Content content = new Content();
        content.addMediaType("application/json", mediaType);
        response.setContent(content);

        operation.getResponses().addApiResponse(statusCode, response);
    }

    private Info apiInfo() {
        return new Info()
                .title("TMTA API")
                .description("API for TMTA project")
                .version("1.0.0");
    }
}
