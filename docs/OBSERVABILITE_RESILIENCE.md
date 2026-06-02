# Observabilité de la résilience — Afya

Ce guide regroupe les commandes utiles pour suivre la résilience HTTP (retry, erreurs réseau, circuit breaker) introduite dans les services Afya.

---

## 1. Endpoints Actuator à connaître

Services exposant `health,info,metrics` :

- `afya-bff` : `http://localhost:8080/actuator`
- `identity-service` : `http://localhost:8081/actuator`
- `catalog-service` : `http://localhost:8082/actuator`
- `patient-service` : `http://localhost:8083/actuator`
- `care-entry-service` : `http://localhost:8084/actuator`
- `stay-service` : `http://localhost:8085/actuator`
- `clinical-record-service` : `http://localhost:8086/actuator`
- `audit-service` : `http://localhost:8087/actuator`

---

## 2. Vérification rapide en ligne de commande

### 2.1 Lister les métriques d’un service

```bash
curl -s http://localhost:8080/actuator/metrics | jq
```

### 2.2 Lire les métriques de résilience

```bash
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.retry_total" | jq
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.downstream_5xx_total" | jq
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.downstream_io_failure_total" | jq
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.circuit_open_total" | jq
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.circuit_reject_total" | jq
```

### 2.3 Filtrer par cible (`target`)

Exemple sur la cible `http://localhost:8086` :

```bash
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.retry_total?tag=target:http://localhost:8086" | jq
```

---

## 3. Signification des métriques

| Métrique | Signification |
|----------|---------------|
| `afya.http.resilience.retry_total` | Nombre de retries déclenchés |
| `afya.http.resilience.downstream_5xx_total` | Réponses 5xx reçues du service aval |
| `afya.http.resilience.downstream_io_failure_total` | Échecs I/O (timeout, refus connexion, etc.) |
| `afya.http.resilience.circuit_open_total` | Nombre d’ouvertures de circuit |
| `afya.http.resilience.circuit_reject_total` | Requêtes rejetées car circuit ouvert |

Tag principal :

- `target` : service cible (ex: `http://localhost:8086`)

---

## 4. Requêtes Prometheus recommandées

> Nom Prometheus typique : points remplacés par underscore.

### 4.1 Vue trafic de retry (5 min)

```promql
sum(rate(afya_http_resilience_retry_total[5m])) by (target)
```

### 4.2 Erreurs 5xx downstream (5 min)

```promql
sum(rate(afya_http_resilience_downstream_5xx_total[5m])) by (target)
```

### 4.3 Erreurs réseau / timeout (5 min)

```promql
sum(rate(afya_http_resilience_downstream_io_failure_total[5m])) by (target)
```

### 4.4 Ouverture de circuit (15 min)

```promql
sum(increase(afya_http_resilience_circuit_open_total[15m])) by (target)
```

### 4.5 Rejets pour circuit ouvert (5 min)

```promql
sum(rate(afya_http_resilience_circuit_reject_total[5m])) by (target)
```

---

## 5. Seuils d’alerte de base

### Alerte A — Instabilité réseau

- **Condition** : `sum(rate(...downstream_io_failure_total[5m])) by (target) > 0.1`
- **Durée** : 5 minutes
- **Action** : vérifier disponibilité réseau/containers du service `target`

### Alerte B — Circuit qui s’ouvre

- **Condition** : `increase(...circuit_open_total[10m]) > 0`
- **Durée** : immédiat
- **Action** : identifier la cause (5xx ou I/O) avant saturation

### Alerte C — Circuit ouvert persistant

- **Condition** : `sum(rate(...circuit_reject_total[5m])) by (target) > 0`
- **Durée** : 5 minutes
- **Action** : incident en cours sur service aval, basculer plan de continuité

### Alerte D — Erreurs applicatives aval

- **Condition** : `sum(rate(...downstream_5xx_total[5m])) by (target) > 0.05`
- **Durée** : 10 minutes
- **Action** : analyser logs applicatifs du service cible

---

## 6. Checklist exploitation

- Vérifier `health` de tous les services (`8080..8087`)
- Vérifier `retry_total` et `io_failure_total` après un test de charge
- Vérifier que `circuit_open_total` reste stable en nominal
- Corréler alertes métriques avec logs (`corr:<correlationId>`)

---

## 7. Références projet

- Résilience HTTP : `afya-shared/src/main/java/com/afya/platform/shared/http/HttpResilienceInterceptor.java`
- Correlation ID : `afya-shared/src/main/java/com/afya/platform/shared/web/CorrelationIdFilter.java`
- Gestion erreurs globale : `afya-shared/src/main/java/com/afya/platform/shared/web/GlobalExceptionHandler.java`
