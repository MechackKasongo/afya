package com.afya.platform.audit.repository;

import com.afya.platform.audit.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

    Optional<AuditEvent> findByEventId(UUID eventId);

    @Query("""
            SELECT e.action, COUNT(e)
            FROM AuditEvent e
            WHERE e.occurredAt >= :from AND e.occurredAt <= :to
            GROUP BY e.action
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countByAction(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT e.sourceService, COUNT(e)
            FROM AuditEvent e
            WHERE e.occurredAt >= :from AND e.occurredAt <= :to
            GROUP BY e.sourceService
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countBySourceService(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT e.actorUsername, COUNT(e)
            FROM AuditEvent e
            WHERE e.occurredAt >= :from AND e.occurredAt <= :to
            GROUP BY e.actorUsername
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> countByActor(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
            SELECT CAST(occurred_at AS DATE) AS event_day, COUNT(*)
            FROM audit_events
            WHERE occurred_at >= :from AND occurred_at <= :to
            GROUP BY CAST(occurred_at AS DATE)
            ORDER BY event_day DESC
            """, nativeQuery = true)
    List<Object[]> countByDay(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT COUNT(e) FROM AuditEvent e
            WHERE e.occurredAt >= :from AND e.occurredAt <= :to
            """)
    long countInRange(@Param("from") Instant from, @Param("to") Instant to);
}
