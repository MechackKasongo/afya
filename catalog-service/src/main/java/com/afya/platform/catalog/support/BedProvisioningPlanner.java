package com.afya.platform.catalog.support;

import java.util.ArrayList;
import java.util.List;

/**
 * Planifie les libellés chambre-lit jusqu'à la capacité totale du service.
 * Ex. capacité 10, 2 lits/chambre, préfixe A → A1-01, A1-02, A2-01, … (10 lits, 5 chambres).
 */
public final class BedProvisioningPlanner {

    private BedProvisioningPlanner() {
    }

    public static List<String> plannedLabels(int bedCapacity, int bedsPerRoom, char roomLetter) {
        if (bedCapacity <= 0 || bedsPerRoom <= 0) {
            return List.of();
        }
        List<String> labels = new ArrayList<>(bedCapacity);
        int room = 1;
        int added = 0;
        while (added < bedCapacity) {
            for (int bedIndex = 0; bedIndex < bedsPerRoom && added < bedCapacity; bedIndex++) {
                labels.add(BedLabelSupport.formatSequentialLabel(
                        roomLetter, room, BedLabelSupport.bedCodeFromIndex(bedIndex)));
                added++;
            }
            room++;
        }
        return labels;
    }
}
