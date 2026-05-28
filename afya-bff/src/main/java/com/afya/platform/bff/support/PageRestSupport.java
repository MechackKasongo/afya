package com.afya.platform.bff.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;

/** Désérialise les réponses {@code Page} des microservices (JSON Spring Data). */
public final class PageRestSupport {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private PageRestSupport() {
    }

    public static <T> Page<T> getPage(
            RestClient client,
            Function<UriBuilder, URI> uri,
            Class<T> elementType,
            Consumer<HttpHeaders> headers
    ) {
        String json = client.get()
                .uri(uri)
                .headers(headers)
                .retrieve()
                .body(String.class);
        try {
            JavaType type = MAPPER.getTypeFactory()
                    .constructParametricType(SpringDataPageJson.class, elementType);
            SpringDataPageJson<T> body = MAPPER.readValue(json, type);
            var content = body.content() != null ? body.content() : Collections.<T>emptyList();
            var pageable = PageRequest.of(body.number(), Math.max(body.size(), 1));
            return new PageImpl<>(content, pageable, body.totalElements());
        } catch (Exception e) {
            throw new IllegalStateException("Réponse paginée invalide depuis le service en aval", e);
        }
    }
}
