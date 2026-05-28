package com.afya.platform.bff.support;

import com.afya.platform.bff.client.StayClient;
import com.afya.platform.bff.dto.AdmissionResponse;
import com.afya.platform.bff.dto.StayResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class AdmissionUiEnricher {

    private final StayClient stayClient;

    public AdmissionUiEnricher(StayClient stayClient) {
        this.stayClient = stayClient;
    }

    public AdmissionCompatMapper.AdmissionUiResponse enrich(AdmissionResponse raw, String authorizationHeader) {
        return AdmissionCompatMapper.toUi(raw, findStay(raw.id(), authorizationHeader));
    }

    public Page<AdmissionCompatMapper.AdmissionUiResponse> enrichPage(
            Page<AdmissionResponse> page,
            String authorizationHeader
    ) {
        return page.map(raw -> enrich(raw, authorizationHeader));
    }

    private StayResponse findStay(Long admissionId, String authorizationHeader) {
        try {
            return stayClient.getByAdmission(admissionId, authorizationHeader);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }
}
