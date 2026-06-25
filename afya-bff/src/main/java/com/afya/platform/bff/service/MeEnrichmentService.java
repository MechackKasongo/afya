package com.afya.platform.bff.service;

import com.afya.platform.bff.client.HospitalClient;
import com.afya.platform.bff.dto.HospitalServiceResponse;
import com.afya.platform.bff.dto.MeResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MeEnrichmentService {

    private final HospitalClient hospitalClient;

    public MeEnrichmentService(HospitalClient hospitalClient) {
        this.hospitalClient = hospitalClient;
    }

    public MeResponse enrich(MeResponse me, String authorizationHeader) {
        List<Long> ids = me.hospitalServiceIds() == null ? List.of() : me.hospitalServiceIds();
        List<String> names = resolveHospitalServiceNames(ids, authorizationHeader);
        return new MeResponse(
                me.id(),
                me.username(),
                me.fullName(),
                me.roles(),
                ids,
                names);
    }

    public com.afya.platform.bff.dto.TokenResponse enrichToken(
            com.afya.platform.bff.dto.TokenResponse tokens,
            String authorizationHeader
    ) {
        MeResponse enriched = enrich(tokens.me(), authorizationHeader);
        return new com.afya.platform.bff.dto.TokenResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                tokens.tokenType(),
                tokens.expiresInSeconds(),
                enriched);
    }

    private List<String> resolveHospitalServiceNames(List<Long> ids, String authorizationHeader) {
        if (ids.isEmpty()) {
            return List.of();
        }
        try {
            Page<HospitalServiceResponse> page =
                    hospitalClient.listHospitalServices(null, 0, 500, authorizationHeader);
            Map<Long, String> namesById = page.getContent().stream()
                    .collect(Collectors.toMap(HospitalServiceResponse::id, HospitalServiceResponse::name, (a, b) -> a));
            return ids.stream()
                    .map(id -> namesById.getOrDefault(id, "Service #" + id))
                    .toList();
        } catch (Exception ignored) {
            return ids.stream().map(id -> "Service #" + id).toList();
        }
    }
}
