package com.afya.platform.admission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "admission_notifications")
public class AdmissionNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admission_id", nullable = false)
    private Long admissionId;

    @Column(name = "recipient_user_id")
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private AdmissionNotificationType notificationType;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt = Instant.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdmissionNotificationStatus status = AdmissionNotificationStatus.ENVOYEE;

    @Column(name = "read_at")
    private Instant readAt;

    public Long getId() {
        return id;
    }

    public Long getAdmissionId() {
        return admissionId;
    }

    public void setAdmissionId(Long admissionId) {
        this.admissionId = admissionId;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public void setRecipientUserId(Long recipientUserId) {
        this.recipientUserId = recipientUserId;
    }

    public AdmissionNotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(AdmissionNotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public AdmissionNotificationStatus getStatus() {
        return status;
    }

    public void setStatus(AdmissionNotificationStatus status) {
        this.status = status;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }
}
