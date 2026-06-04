# Diagrammes de composants — par application / service

Un fichier PlantUML par **application déployable** de la plateforme Afya.

| Application | Port | Fichier |
|-------------|------|---------|
| API Gateway | 8090 / 8443 | [COMPOSANTS_GATEWAY.puml](COMPOSANTS_GATEWAY.puml) |
| afya-bff | 8080 | [COMPOSANTS_BFF.puml](COMPOSANTS_BFF.puml) |
| identity-service | 8081 | [COMPOSANTS_IDENTITY_SERVICE.puml](COMPOSANTS_IDENTITY_SERVICE.puml) |
| catalog-service | 8082 | [COMPOSANTS_CATALOG_SERVICE.puml](COMPOSANTS_CATALOG_SERVICE.puml) |
| patient-service | 8083 | [COMPOSANTS_PATIENT_SERVICE.puml](COMPOSANTS_PATIENT_SERVICE.puml) |
| care-entry-service | 8084 | [COMPOSANTS_CARE_ENTRY_SERVICE.puml](COMPOSANTS_CARE_ENTRY_SERVICE.puml) |
| stay-service | 8085 | [COMPOSANTS_STAY_SERVICE.puml](COMPOSANTS_STAY_SERVICE.puml) |
| clinical-record-service | 8086 | [COMPOSANTS_CLINICAL_RECORD_SERVICE.puml](COMPOSANTS_CLINICAL_RECORD_SERVICE.puml) |
| audit-service | 8087 | [COMPOSANTS_AUDIT_SERVICE.puml](COMPOSANTS_AUDIT_SERVICE.puml) |

Vue **globale** (tous les services) : [../COMPOSANTS_AFYA.puml](../COMPOSANTS_AFYA.puml)

## Rendu

```bash
plantuml docs/plantuml/composants/*.puml
```

Extension Cursor : **PlantUML** → ouvrir un fichier → **Alt+D**.

## Légende commune

| Stéréotype | Rôle |
|------------|------|
| `<<REST>>` | Contrôleur `/api/v1` |
| `<<service>>` | Logique métier |
| `<<repository>>` | Accès JPA |
| `<<integration>>` | Client HTTP vers autre microservice |
| `<<client>>` | Client HTTP BFF |
| `<<config>>` | Sécurité, RestClient, bootstrap |
