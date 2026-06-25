package com.afya.platform.hospital.model;

public enum BedAssignmentPolicy {
    /** Premier lit libre selon l’ordre numérique des chambres (001, 002, …). */
    ROOM_ORDER_ASC,
    /** Lit libre depuis le plus longtemps (dernière libération la plus ancienne). */
    LONGEST_IDLE
}
