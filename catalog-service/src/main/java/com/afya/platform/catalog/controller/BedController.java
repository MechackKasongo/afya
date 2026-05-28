package com.afya.platform.catalog.controller;

import com.afya.platform.catalog.dto.BedOccupancyRequest;
import com.afya.platform.catalog.dto.BedRequest;
import com.afya.platform.catalog.dto.BedResponse;
import com.afya.platform.catalog.service.BedService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/hospital-services/{serviceId}/beds")
public class BedController {

    private final BedService bedService;

    public BedController(BedService bedService) {
        this.bedService = bedService;
    }

    @GetMapping
    public List<BedResponse> list(@PathVariable Long serviceId) {
        return bedService.listByService(serviceId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BedResponse create(@PathVariable Long serviceId, @Valid @RequestBody BedRequest request) {
        return bedService.create(serviceId, request);
    }

    @PostMapping("/provision")
    public int provision(@PathVariable Long serviceId) {
        return bedService.provisionMissingBeds(serviceId);
    }

    @PostMapping("/realign")
    public int realign(@PathVariable Long serviceId) {
        return bedService.realignFreeBeds(serviceId);
    }

    @PatchMapping("/occupancy")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateOccupancy(@PathVariable Long serviceId, @Valid @RequestBody BedOccupancyRequest request) {
        bedService.updateOccupancy(serviceId, request);
    }

    @DeleteMapping("/{bedId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long serviceId, @PathVariable Long bedId) {
        bedService.delete(serviceId, bedId);
    }
}
