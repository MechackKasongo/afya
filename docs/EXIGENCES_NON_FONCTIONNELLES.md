# Besoins non fonctionnels — mise en œuvre par l’architecture

## Sécurité

| Exigence | Mise en œuvre |
|----------|----------------|
| Protection des données | HTTPS partout ; chiffrement au repos (BDD, stockage objet) ; pas de secrets dans le code. |
| Accès selon les rôles | **identity-service** : rôles Admin, Réceptionniste, Médecin, Infirmier ; claims JWT (`roles`, périmètre `hospitalServiceIds`). Chaque service applique des règles (ex. infirmier : lecture prescription, écriture soins uniquement). |
| Traçabilité | **audit-service** : qui a accédé ou modifié quoi (sans dupliquer le contenu clinique dans les logs). |

## Disponibilité et fiabilité

| Exigence | Mise en œuvre |
|----------|----------------|
| Accès continu en heures de service | Déploiement conteneurisé ; health checks par service ; redémarrage automatique. |
| Intégrité des données | Transaction locale par service ; pas de transaction distribuée 2PC ; sagas ou compensation pour flux multi-services. |
| Défaillance technique | Un service down n’effondre pas tout le site si le **BFF** dégrade (message clair) ; audit asynchrone pour ne pas bloquer le métier. |

## Accessibilité et compatibilité

| Exigence | Mise en œuvre |
|----------|----------------|
| Solution web multi-OS | Front SPA (React/Vue) + API REST ; pas de client lourd. |
| Navigateur | Cibles modernes (Chrome, Firefox, Edge) ; tests responsive. |
| Interface simple, intuitive, responsive | **BFF** + UI unique ; design system commun (mobile / tablette / desktop). |

## Qualité technique transversale

- **Versioning API** : `/api/v1/` par service.  
- **Observabilité** : logs structurés, corrélation `traceId` entre BFF et services.  
- **Contrats** : tests d’intégration et contrats consumer-driven entre services critiques (patient ↔ admission).  
