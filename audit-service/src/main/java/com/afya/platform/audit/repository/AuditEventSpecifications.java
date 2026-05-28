package com.afya.platform.audit.repository;

import com.afya.platform.audit.model.AuditEvent;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;

public final class AuditEventSpecifications {

    private AuditEventSpecifications() {
    }

    public static Specification<AuditEvent> search(
            String action,
            String resourceType,
            String actorUsername,
            String sourceService,
            String resource,
            Instant from,
            Instant to) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            addContains(predicates, cb, root.get("action"), action);
            addContains(predicates, cb, root.get("resourceType"), resourceType);
            addContains(predicates, cb, root.get("actorUsername"), actorUsername);
            addContains(predicates, cb, root.get("sourceService"), sourceService);
            if (resource != null && !resource.isBlank()) {
                String pattern = "%" + resource.strip().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("resourceType")), pattern),
                        cb.and(
                                cb.isNotNull(root.get("resourceId")),
                                cb.like(cb.lower(root.get("resourceId")), pattern))));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addContains(
            ArrayList<Predicate> predicates,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            jakarta.persistence.criteria.Path<String> path,
            String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String pattern = "%" + value.strip().toLowerCase(Locale.ROOT) + "%";
        predicates.add(cb.like(cb.lower(path), pattern));
    }
}
