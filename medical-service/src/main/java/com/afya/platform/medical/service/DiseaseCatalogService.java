package com.afya.platform.medical.service;

import com.afya.platform.medical.dto.DiseaseCatalogResponse;
import com.afya.platform.medical.model.DiseaseCatalog;
import com.afya.platform.medical.repository.DiseaseCatalogRepository;
import com.afya.platform.shared.exception.BadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class DiseaseCatalogService {

    private final DiseaseCatalogRepository diseaseCatalogRepository;

    public DiseaseCatalogService(DiseaseCatalogRepository diseaseCatalogRepository) {
        this.diseaseCatalogRepository = diseaseCatalogRepository;
    }

    @Transactional(readOnly = true)
    public List<DiseaseCatalogResponse> listSelectable(String diseaseType) {
        String normalizedType = requireDiseaseType(diseaseType);
        return diseaseCatalogRepository
                .findByDiseaseTypeAndUsageCountGreaterThanEqualOrderByUsageCountDescLabelAsc(
                        normalizedType,
                        DiseaseCatalog.MIN_USAGE_FOR_SELECTION)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void recordUsage(String diseaseType, String diseaseName) {
        String normalizedType = requireDiseaseType(diseaseType);
        String label = requireDiseaseName(diseaseName);
        String normalizedLabel = normalizeLabel(label);
        Instant now = Instant.now();

        DiseaseCatalog entry = diseaseCatalogRepository
                .findByDiseaseTypeAndLabelNormalized(normalizedType, normalizedLabel)
                .orElseGet(() -> {
                    DiseaseCatalog created = new DiseaseCatalog();
                    created.setDiseaseType(normalizedType);
                    created.setLabel(label);
                    created.setLabelNormalized(normalizedLabel);
                    created.setUsageCount(0);
                    created.setFirstUsedAt(now);
                    created.setLastUsedAt(now);
                    return created;
                });

        entry.setLabel(label);
        entry.setUsageCount(entry.getUsageCount() + 1);
        entry.setLastUsedAt(now);
        diseaseCatalogRepository.save(entry);
    }

    static String normalizeLabel(String label) {
        return label.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private static String requireDiseaseType(String diseaseType) {
        if (diseaseType == null || diseaseType.isBlank()) {
            throw new BadRequestException("Le type de maladie est obligatoire.");
        }
        return diseaseType.strip();
    }

    private static String requireDiseaseName(String diseaseName) {
        if (diseaseName == null || diseaseName.isBlank()) {
            throw new BadRequestException("Le nom de la maladie est obligatoire.");
        }
        return diseaseName.strip();
    }

    private DiseaseCatalogResponse toResponse(DiseaseCatalog entry) {
        return new DiseaseCatalogResponse(
                entry.getId(),
                entry.getDiseaseType(),
                entry.getLabel(),
                entry.getUsageCount(),
                entry.isSelectable()
        );
    }
}
