package com.afya.platform.auth.model;

/**
 * Résultat d'une tentative de connexion — MD-01 (JournalConnexion du mémoire).
 */
public enum LoginOutcome {

    /** Connexion réussie — token émis. */
    SUCCESS,

    /** Mot de passe incorrect. */
    FAILURE_BAD_PASSWORD,

    /** Compte introuvable pour ce username. */
    FAILURE_USER_NOT_FOUND,

    /** Compte bloqué (trop de tentatives). */
    FAILURE_ACCOUNT_LOCKED,

    /** Compte inactif (désactivé par un admin). */
    FAILURE_ACCOUNT_INACTIVE
}
