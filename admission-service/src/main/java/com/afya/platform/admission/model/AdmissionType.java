package com.afya.platform.admission.model;

/**
 * Type d'admission — MD-05 (Admission du mémoire).
 *
 * <ul>
 *   <li>{@link #NORMALE} — admission planifiée via la réception.</li>
 *   <li>{@link #URGENCE} — admission directe depuis les urgences ({@code EmergencyVisit}).</li>
 * </ul>
 */
public enum AdmissionType {

    /** Admission standard passant par la réception. */
    NORMALE,

    /** Admission issue d'un passage aux urgences. */
    URGENCE
}
