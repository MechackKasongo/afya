package com.afya.platform.clinical.service;

import com.afya.platform.clinical.model.DiseaseCatalog;
import com.afya.platform.clinical.repository.DiseaseCatalogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiseaseCatalogServiceTest {

    @Mock
    private DiseaseCatalogRepository diseaseCatalogRepository;

    @InjectMocks
    private DiseaseCatalogService diseaseCatalogService;

    @Test
    void recordUsageIncrementsExistingEntry() {
        DiseaseCatalog existing = new DiseaseCatalog();
        existing.setDiseaseType("Chronique");
        existing.setLabel("Diabète");
        existing.setLabelNormalized("diabète type 2");
        existing.setUsageCount(4);

        when(diseaseCatalogRepository.findByDiseaseTypeAndLabelNormalized(
                        eq("Chronique"), eq("diabète type 2")))
                .thenReturn(Optional.of(existing));
        when(diseaseCatalogRepository.save(any(DiseaseCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

        diseaseCatalogService.recordUsage("Chronique", "Diabète type 2");

        ArgumentCaptor<DiseaseCatalog> captor = ArgumentCaptor.forClass(DiseaseCatalog.class);
        verify(diseaseCatalogRepository).save(captor.capture());
        assertThat(captor.getValue().getUsageCount()).isEqualTo(5);
        assertThat(captor.getValue().isSelectable()).isTrue();
    }

    @Test
    void recordUsageCreatesNewEntryOnFirstUse() {
        when(diseaseCatalogRepository.findByDiseaseTypeAndLabelNormalized(eq("Infectieuse"), eq("paludisme")))
                .thenReturn(Optional.empty());
        when(diseaseCatalogRepository.save(any(DiseaseCatalog.class))).thenAnswer(inv -> inv.getArgument(0));

        diseaseCatalogService.recordUsage("Infectieuse", "Paludisme");

        verify(diseaseCatalogRepository).save(any(DiseaseCatalog.class));
    }

    @Test
    void normalizeLabelCollapsesSpacesAndLowercases() {
        assertThat(DiseaseCatalogService.normalizeLabel("  Paludisme   grave "))
                .isEqualTo("paludisme grave");
    }
}
