package com.afya.platform.catalog.service;

import com.afya.platform.catalog.model.Bed;
import com.afya.platform.catalog.model.BedAssignmentPolicy;
import com.afya.platform.catalog.model.HospitalService;
import com.afya.platform.catalog.support.BedLabelSupport;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class BedAssignmentSelector {

    public Optional<Bed> pickFreeBed(HospitalService service, List<Bed> freeBeds) {
        if (freeBeds.isEmpty()) {
            return Optional.empty();
        }
        Comparator<Bed> byRoom = Comparator
                .comparingLong((Bed b) -> BedLabelSupport.roomOrderKey(b.getLabel()))
                .thenComparingLong(b -> BedLabelSupport.bedOrderKey(b.getLabel()));
        Comparator<Bed> byLongestIdle = Comparator
                .comparing(Bed::getLastFreedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(byRoom);
        Comparator<Bed> policy = service.getBedAssignmentPolicy() == BedAssignmentPolicy.LONGEST_IDLE
                ? byLongestIdle
                : byRoom;
        return freeBeds.stream().min(policy);
    }
}
