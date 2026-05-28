package com.afya.platform.bff.support;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.data.domain.Page;

import java.util.List;

/** Références de type explicites pour RestClient (évite ParameterizedTypeReference<> vide). */
public final class RestClientTypes {

    private RestClientTypes() {
    }

    public static <T> ParameterizedTypeReference<Page<T>> page(Class<T> contentType) {
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(Page.class, contentType).getType());
    }

    public static <T> ParameterizedTypeReference<List<T>> list(Class<T> elementType) {
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(List.class, elementType).getType());
    }
}
