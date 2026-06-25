package com.afya.platform.medical.controller;

import com.afya.platform.medical.dto.DiseaseCatalogResponse;
import com.afya.platform.medical.service.DiseaseCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/disease-catalog")
public class DiseaseCatalogController {

    private final DiseaseCatalogService diseaseCatalogService;

    public DiseaseCatalogController(DiseaseCatalogService diseaseCatalogService) {
        this.diseaseCatalogService = diseaseCatalogService;
    }

    /** Maladies proposées à la sélection (saisies au moins 5 fois pour ce type). */
    @GetMapping
    public List<DiseaseCatalogResponse> listSelectable(@RequestParam String diseaseType) {
        return diseaseCatalogService.listSelectable(diseaseType);
    }
}
