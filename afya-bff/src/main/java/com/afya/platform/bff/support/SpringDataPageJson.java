package com.afya.platform.bff.support;

import java.util.List;

/** Forme JSON des réponses {@code Page} renvoyées par les microservices Spring Data. */
record SpringDataPageJson<T>(
        List<T> content,
        long totalElements,
        int number,
        int size
) {
}
