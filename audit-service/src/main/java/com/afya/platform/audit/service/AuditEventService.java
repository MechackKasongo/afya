package com.afya.platform.audit.service;

import com.afya.platform.audit.dto.ActivityCountItem;
import com.afya.platform.audit.dto.ActivityReportResponse;
import com.afya.platform.audit.dto.AuditEventCreateRequest;
import com.afya.platform.audit.dto.AuditEventResponse;
import com.afya.platform.audit.model.AuditEvent;
import com.afya.platform.audit.repository.AuditEventRepository;
import com.afya.platform.audit.repository.AuditEventSpecifications;
import com.afya.platform.shared.exception.BadRequestException;
import com.afya.platform.shared.exception.ConflictException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEventResponse record(AuditEventCreateRequest request) {
        UUID eventId = request.eventId() != null ? request.eventId() : UUID.randomUUID();
        if (auditEventRepository.findByEventId(eventId).isPresent()) {
            throw new ConflictException("Événement d'audit déjà enregistré: " + eventId);
        }

        String actor = resolveActor(request.actorUsername());
        Instant occurredAt = request.occurredAt() != null ? request.occurredAt() : Instant.now();

        AuditEvent event = new AuditEvent();
        event.setEventId(eventId);
        event.setOccurredAt(occurredAt);
        event.setActorUsername(actor);
        event.setAction(request.action().trim());
        event.setResourceType(request.resourceType().trim());
        event.setResourceId(blankToNull(request.resourceId()));
        event.setSourceService(request.sourceService().trim());
        event.setMetadataJson(blankToNull(request.metadataJson()));

        return toResponse(auditEventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> search(
            String action,
            String resourceType,
            String actorUsername,
            String sourceService,
            String resource,
            Instant from,
            Instant to,
            String sortBy,
            String sortDir,
            Integer page,
            Integer size) {
        if (from != null && to != null) {
            validateRange(from, to);
        }
        int safePage = page == null || page < 0 ? 0 : page;
        int safeSize = size == null || size <= 0 || size > 100 ? 20 : size;
        PageRequest pageable = PageRequest.of(safePage, safeSize, resolveSort(sortBy, sortDir));
        return auditEventRepository.findAll(
                        AuditEventSpecifications.search(
                                blankToNull(action),
                                blankToNull(resourceType),
                                blankToNull(actorUsername),
                                blankToNull(sourceService),
                                blankToNull(resource),
                                from,
                                to),
                        pageable)
                .map(this::toResponse);
    }

    private static Sort resolveSort(String sortBy, String sortDir) {
        String property = switch (sortBy == null ? "" : sortBy.strip()) {
            case "actor", "actorUsername" -> "actorUsername";
            case "action" -> "action";
            case "object", "resourceType" -> "resourceType";
            case "service", "sourceService" -> "sourceService";
            default -> "occurredAt";
        };
        Direction direction = "asc".equalsIgnoreCase(sortDir) ? Direction.ASC : Direction.DESC;
        return Sort.by(direction, property);
    }

    @Transactional(readOnly = true)
    public ActivityReportResponse activityReport(Instant from, Instant to) {
        Instant rangeFrom = defaultFrom(from);
        Instant rangeTo = defaultTo(to);
        validateRange(rangeFrom, rangeTo);
        long total = auditEventRepository.countInRange(rangeFrom, rangeTo);
        List<ActivityCountItem> byAction = mapCounts(auditEventRepository.countByAction(rangeFrom, rangeTo));
        List<ActivityCountItem> bySource = mapCounts(auditEventRepository.countBySourceService(rangeFrom, rangeTo));
        List<ActivityCountItem> topActors = mapCounts(auditEventRepository.countByActor(rangeFrom, rangeTo))
                .stream()
                .limit(10)
                .toList();
        List<ActivityCountItem> byDay = auditEventRepository.countByDay(rangeFrom, rangeTo).stream()
                .map(row -> new ActivityCountItem(dayKey(row[0]), ((Number) row[1]).longValue()))
                .toList();
        return new ActivityReportResponse(rangeFrom, rangeTo, total, byAction, bySource, topActors, byDay, false, null);
    }

    private String resolveActor(String requestedActor) {
        if (requestedActor != null && !requestedActor.isBlank()) {
            return requestedActor.trim();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String username) {
            return username;
        }
        throw new BadRequestException("actorUsername requis pour l'ingestion d'événement");
    }

    private static Instant defaultFrom(Instant from) {
        return from != null ? from : Instant.EPOCH;
    }

    private static Instant defaultTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    private static void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new BadRequestException("La borne 'from' doit être antérieure ou égale à 'to'");
        }
    }

    private static List<ActivityCountItem> mapCounts(List<Object[]> rows) {
        return rows.stream()
                .map(row -> new ActivityCountItem(String.valueOf(row[0]), (Long) row[1]))
                .toList();
    }

    private static String dayKey(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate.toString();
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate().toString();
        }
        return String.valueOf(value);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getEventId(),
                event.getOccurredAt(),
                event.getActorUsername(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getSourceService(),
                event.getMetadataJson(),
                event.getCreatedAt()
        );
    }
}
