package com.afya.platform.bff.support;

import com.afya.platform.bff.client.HospitalClient;
import com.afya.platform.bff.dto.HospitalServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class HospitalServiceResolver {

    private final HospitalClient hospitalClient;

    public HospitalServiceResolver(HospitalClient hospitalClient) {
        this.hospitalClient = hospitalClient;
    }

    public Long resolveIdByName(String serviceName, String authorizationHeader) {
        String normalized = serviceName == null ? "" : serviceName.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Le nom du service est obligatoire");
        }
        Page<HospitalServiceResponse> page = hospitalClient.listHospitalServices(true, 0, 200, authorizationHeader);
        return page.getContent().stream()
                .filter(s -> s.name().equalsIgnoreCase(normalized))
                .map(HospitalServiceResponse::id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Service hospitalier introuvable : " + normalized));
    }
}
