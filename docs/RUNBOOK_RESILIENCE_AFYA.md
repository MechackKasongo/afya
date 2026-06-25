# Runbook — incidents de résilience HTTP (Afya)

Ce runbook couvre les incidents liés aux métriques :

- `afya_http_resilience_downstream_io_failure_total`
- `afya_http_resilience_downstream_5xx_total`
- `afya_http_resilience_circuit_open_total`
- `afya_http_resilience_circuit_reject_total`
- `afya_http_resilience_retry_total`

---

## 1. Tri rapide (5 minutes)

1. Identifier la `target` impactée via Grafana/Prometheus.
2. Vérifier l’état des services :
   - `GET /actuator/health` du service appelant
   - `GET /actuator/health` du service ciblé
3. Vérifier si le problème est :
   - **réseau/I-O** (`io_failure_total`)
   - **applicatif** (`downstream_5xx_total`)
   - **saturation/protection** (`circuit_open_total` + `circuit_reject_total`)

---

## 2. Scénarios et actions

### A) IO failures élevés

**Symptôme**
- `rate(...io_failure_total[5m])` élevé
- parfois `retry_total` en hausse

**Actions**
1. Vérifier que le service cible tourne (`podman compose ps`).
2. Vérifier DNS/URL/port dans `application*.properties`.
3. Vérifier latence réseau et saturation machine.
4. Si incident infra : escalader SRE/infra.

**Validation**
- baisse de `io_failure_total`
- retour du trafic nominal

---

### B) 5xx downstream élevés

**Symptôme**
- `rate(...downstream_5xx_total[5m])` élevé

**Actions**
1. Lire les logs du service cible avec `corr:<correlationId>`.
2. Rechercher les erreurs dominantes (DB, validation, null, timeout interne).
3. Corriger le bug ou réduire la charge.
4. Redéployer si nécessaire.

**Validation**
- 5xx en baisse
- plus d’ouverture de circuit

---

### C) Circuit s’ouvre et rejette

**Symptôme**
- `increase(...circuit_open_total[10m]) > 0`
- `rate(...circuit_reject_total[5m]) > 0` persistant

**Actions**
1. Traiter d’abord la cause racine (IO/5xx).
2. Vérifier la durée d’ouverture du circuit (`app.http.client.circuit.open-duration`).
3. Vérifier seuil d’ouverture (`app.http.client.circuit.failure-threshold`).
4. Ajuster prudemment les seuils uniquement après RCA.

**Validation**
- circuit ne s’ouvre plus
- rejet à 0 sur fenêtre glissante

---

### D) Retry élevé sans erreurs majeures

**Symptôme**
- `retry_total` haut mais 5xx/IO modérés

**Actions**
1. Vérifier pics de latence.
2. Vérifier timeouts trop agressifs.
3. Ajuster timeout/read timeout sur service appelant.

**Validation**
- retry revient à un niveau bas

---

## 3. Commandes utiles

### Health checks

```bash
curl -s http://localhost:8080/actuator/health | jq
curl -s http://localhost:8085/actuator/health | jq
```

### Vérification métrique précise

```bash
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.circuit_reject_total" | jq
curl -s "http://localhost:8080/actuator/metrics/afya.http.resilience.downstream_io_failure_total?tag=target:http://localhost:8085" | jq
```

---

## 4. Critères de sortie d’incident

- Alertes rétablies (`green`) pendant au moins 15 minutes
- `circuit_reject_total` à 0
- 5xx/IO revenue sous seuil
- cause racine documentée

---

## 5. Post-mortem minimal

- Service appelant
- Service cible (`target`)
- Début/fin incident
- Symptôme principal (I/O, 5xx, circuit)
- Cause racine
- Correctif
- Action préventive
