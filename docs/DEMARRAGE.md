7# Démarrage — Afya Platform (multi-services)

## Prérequis

- Java 21
- Maven (wrapper inclus : `./mvnw`)
- Podman ou Docker (base PostgreSQL)

## 1. Base identity-service

```bash
cd ~/IdeaProjects/afya
podman compose up -d
```

PostgreSQL écoute sur **localhost:5433** (base `afya_identity`, user/mot de passe `afya`).

## 2. Lancer les services

**identity-service** (port 8081) :

```bash
./mvnw -pl identity-service spring-boot:run
```

**catalog-service** (port 8082) :

```bash
./mvnw -pl catalog-service spring-boot:run
```

**patient-service** (port 8083) :

```bash
./mvnw -pl patient-service spring-boot:run
```

- Identity : **http://localhost:8081**
- Catalog : **http://localhost:8082**
- Patient : **http://localhost:8083**
- Care-entry : **http://localhost:8084**
- Stay : **http://localhost:8085**
- Clinical : **http://localhost:8086**
- Audit : **http://localhost:8087**

**care-entry-service** (nécessite patient + catalog démarrés pour les appels inter-services) :

```bash
./mvnw -pl care-entry-service spring-boot:run
```
- Santé : **http://localhost:8081/actuator/health**
- Login : `POST http://localhost:8081/api/v1/auth/login`

Compte bootstrap (premier démarrage) :

| Champ | Valeur |
|-------|--------|
| Utilisateur | `admin` |
| Mot de passe | `Admin@Afya2026!` |

Variables JWT optionnelles : `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET` (≥ 64 caractères UTF-8).

## 3. Tests

```bash
./mvnw test
```

## 4. Exemple catalog (avec JWT admin)

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@Afya2026!"}' | jq -r .accessToken)

curl -s http://localhost:8082/api/v1/hospital-services \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Départements et services (UI + BFF)

Hiérarchie : **département** (ex. `MED` — Médecine) → **service hospitalier** (ex. Médecine interne) → **lits**.

| Action | Où | Rôle |
|--------|-----|------|
| Créer / modifier **département** et **services** | UI **Organisation** (`/hospital-services`) — une page par département avec ses services | **ADMIN** (écriture) ; **réception** : lecture seule |
| API directe | `POST/PUT/DELETE /api/v1/departments`, `/api/v1/hospital-services` via gateway **8090** | **ADMIN** |
| Lister | `GET /api/v1/departments`, `GET /api/v1/hospital-services` | Authentifié |

L’ancienne route `/departments` redirige vers `/hospital-services`.

**Erreur « Impossible de créer le département »** : le frontend appelle `POST /api/v1/departments` via le BFF. Après ajout de cette API, **redémarrer obligatoirement** :

```bash
./mvnw -pl catalog-service spring-boot:run    # 8082 — génération auto du code
./mvnw -pl afya-bff spring-boot:run           # 8080 — proxy /api/v1/departments
```

Vérification :

```bash
TOKEN=$(curl -s -X POST http://127.0.0.1:8090/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@Afya2026!"}' | jq -r .accessToken)
curl -s http://127.0.0.1:8090/api/v1/departments -H "Authorization: Bearer $TOKEN" | jq .
curl -s -X POST http://127.0.0.1:8090/api/v1/departments \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Pédiatrie","active":true}' | jq .
```

Si `GET /departments` renvoie **401** alors que `GET /hospital-services` fonctionne, le BFF tourne encore avec une **ancienne version** (sans `DepartmentBffController`).

Exemple création département (via gateway **8090**) :

```bash
curl -s -X POST http://localhost:8090/api/v1/departments \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Pédiatrie","active":true}' | jq .
```

Le **code** est généré automatiquement à partir du nom (`Pédiatrie` → `PEDIATRIE`). Vous pouvez encore envoyer `"code":"PEDIA"` pour forcer un code précis.

Départements seedés par défaut (Flyway catalog V2) : `MED`, `CHIR`, `URG`.

## 5. Exemple patient (JWT réceptionniste ou admin)

Créer un utilisateur `RECEPTION` via identity (ou utiliser admin) puis :

```bash
curl -s -X POST http://localhost:8083/api/v1/patients \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"firstName":"Jean","lastName":"Mukendi","birthDate":"1985-03-20","sex":"M"}' | jq .
```

## 6. Exemple admission

```bash
curl -s -X POST http://localhost:8084/api/v1/admissions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"patientId":1,"hospitalServiceId":1}' | jq .
```

## 7. Exemple séjour (après admission)

```bash
curl -s -X POST http://localhost:8085/api/v1/stays \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"admissionId":1,"patientId":1,"roomLabel":"A12","bedLabel":"LIT-1"}' | jq .
```

Lors d'une **sortie** (`POST .../admissions/{id}/discharge`), care-entry appelle automatiquement la clôture du séjour.

## 8. Exemple dossier clinique

```bash
curl -s http://localhost:8086/api/v1/patients/1/medical-record \
  -H "Authorization: Bearer $TOKEN" | jq .

curl -s -X POST http://localhost:8086/api/v1/patients/1/prescriptions \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"drugName":"Paracétamol","dosage":"500mg","frequency":"3x/jour","startDate":"2026-05-15"}' | jq .
```

### Consultations, diagnostics et catalogue maladies

Migrations Flyway **clinical-record** : `V6__consultation_event_disease_type.sql` (`consultation_events.disease_type`), `V7__disease_catalog.sql` (`consultation_events.disease_name`, table `disease_catalog`).

Sur une **fiche consultation**, un diagnostic exige `diseaseType` et `diseaseName` (détails optionnels dans `content`). Chaque saisie incrémente le catalogue ; après **5** utilisations identiques (même type + libellé normalisé), la maladie apparaît dans la liste sélectionnable.

```bash
# Créer une consultation (médecin ou admin)
curl -s -X POST http://localhost:8080/api/v1/consultations \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"patientId":1,"admissionId":1,"doctorName":"Dr Dupont","reason":"Suivi"}' | jq .

# Ajouter un diagnostic (type + maladie obligatoires)
curl -s -X POST http://localhost:8080/api/v1/consultations/1/diagnostics \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"diseaseType":"Infectieuse","diseaseName":"Paludisme","content":"Suspicion"}' | jq .

# Maladies proposées après 5 saisies (même type)
curl -s "http://localhost:8080/api/v1/disease-catalog?diseaseType=Infectieuse" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Chronologie d'une consultation
curl -s http://localhost:8080/api/v1/consultations/1/events \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**UI** : menu **Consultations** — chronologie (Date, Type d’événement, Type de maladie, Maladie, Contenu), prescriptions sur la fiche consultation (pas dans le dossier médical global). Les types de maladie sont une liste fixe côté frontend ; le catalogue ne couvre que les **noms** de maladie.

> Distinct du diagnostic **dossier patient** (`POST /api/v1/patients/{id}/medical-record/diagnoses`, code CIM) — pas encore exposé dans l’UI.

Les documents stockent une clé `objectStorageKey` (MinIO/S3 à brancher ultérieurement).

## 9. Audit (journal et rapports admin)

```bash
./mvnw -pl audit-service spring-boot:run
```

Ingestion (JWT utilisateur — `actorUsername` optionnel, déduit du token) :

```bash
curl -s -X POST http://localhost:8087/api/v1/audit/events \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"action":"ADMISSION_CREATED","resourceType":"ADMISSION","resourceId":"10","sourceService":"care-entry-service"}' | jq .
```

Le publisher d’audit est centralisé dans **`afya-shared`** (`AuditEventPublisher`, config auto si `app.audit.enabled`).

En dev, les services publient vers l’audit (clé `dev-audit-ingestion-key`, désactivable via `AUDIT_ENABLED=false`) :
- **identity** : `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT_SUCCESS` ; admin utilisateurs : `USER_CREATED`, `USER_UPDATED`, `USER_ACTIVATED`, `USER_DEACTIVATED`, `USER_DELETED`
- **patient** : `PATIENT_CREATED`, `PATIENT_UPDATED`, `PATIENT_CONTACTS_UPDATED`, `APPOINTMENT_CREATED`, `APPOINTMENT_CANCELLED`
- **care-entry** : `ADMISSION_CREATED`, `ADMISSION_TRANSFERRED`, `ADMISSION_DISCHARGED`, `ADMISSION_CANCELLED`, `EMERGENCY_VISIT_CREATED`, `EMERGENCY_VISIT_CLOSED`
- **stay** : `STAY_OPENED`, `STAY_CLOSED`, `HOSPITALIZATION_FORM_UPDATED`
- **clinical** : `MEDICAL_RECORD_OPENED`, `CLINICAL_NOTE_ADDED`, `DIAGNOSIS_ADDED`, `PRESCRIPTION_CREATED`, `MEDICATION_ADMINISTERED`, `NURSING_CARE_RECORDED`, `CLINICAL_DOCUMENT_ADDED`
- **catalog** (admin) : `DEPARTMENT_*`, `HOSPITAL_SERVICE_*`, `BED_CREATED`, `BED_DELETED`

Ingestion machine-à-machine (clé `AUDIT_INGESTION_KEY`, header `X-Audit-Ingestion-Key`) :

```bash
curl -s -X POST http://localhost:8087/api/v1/audit/events \
  -H "X-Audit-Ingestion-Key: $AUDIT_INGESTION_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"actorUsername":"identity-job","action":"LOGIN_SUCCESS","resourceType":"USER","resourceId":"admin","sourceService":"identity-service"}' | jq .
```

Consultation (rôle **ADMIN** uniquement) :

```bash
curl -s "http://localhost:8087/api/v1/audit/events?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq .

curl -s http://localhost:8087/api/v1/reports/activity \
  -H "Authorization: Bearer $TOKEN" | jq .
```

## 10. API Gateway + BFF

### API Gateway (phase E — port **8090**)

Point d’entrée HTTP **sans logique métier** : routage, en-têtes de sécurité, limitation de débit (~30 req/s par IP), corps max 12 Mo. Tout le trafic `/api/` est proxifié vers le **BFF** (port **8080** sur l’hôte en dev Maven).

```bash
# BFF doit déjà tourner sur 8080, puis :
podman compose up -d api

curl -s http://127.0.0.1:8090/gateway/health
./scripts/smoke-api.sh
```

Configuration : `infra/gateway/` (Nginx + `BFF_UPSTREAM`, défaut `http://host.docker.internal:8080`). TLS : `./scripts/generate-tls-certs.sh` puis port **8443**. Variables : voir [.env.example](../.env.example).

Le **front Vite** proxifie `/api` vers **8090** par défaut. Sans gateway :

```bash
VITE_API_PROXY_TARGET=http://127.0.0.1:8080 npm run dev
```

En conteneur (SPA Nginx), `frontend/docker/nginx-default.conf` envoie `/api/` vers le service compose **`api`** (gateway).

### BFF (agrégation métier — port **8080**)

Le module **`afya-bff`** agrège auth, patients, admissions, urgences, séjours, dossier clinique (notes, diagnostics, prescriptions, soins), catalogue, **administration des utilisateurs** (proxy identity) et rapport d’activité / journal audit admin.

```bash
./mvnw -pl afya-bff spring-boot:run
```

Prérequis pour le tableau de bord : **identity**, **patient**, **catalog**, **care-entry**, **stay**, **clinical-record** (et **audit** pour le rapport admin).

Login (via la **gateway**, recommandé) :

```bash
curl -s -X POST http://localhost:8090/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@Afya2026!"}' | jq .
```

Login direct sur le BFF (debug) : remplacer `8090` par `8080`.

Même secret JWT que identity : `JWT_ACCESS_SECRET` (≥ 64 caractères).

Profil courant (noms de services résolus via catalog) :

```bash
TOKEN=$(curl -s -X POST http://localhost:8090/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin@Afya2026!"}' | jq -r .accessToken)

curl -s http://localhost:8090/api/v1/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Réponse attendue : `roles` (ex. `ROLE_ADMIN`), `hospitalServiceIds`, `hospitalServiceNames`.

### Administration utilisateurs (rôle ADMIN)

Exposé par le BFF (`/api/v1/users/**` → **identity-service**). Réservé au rôle **ADMIN** (côté BFF et identity).

```bash
# Liste paginée
curl -s "http://localhost:8080/api/v1/users?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" | jq .

# Rôles disponibles
curl -s http://localhost:8080/api/v1/users/roles \
  -H "Authorization: Bearer $TOKEN" | jq .

# Création (mot de passe généré si omis ; journal local côté identity)
curl -s -X POST http://localhost:8080/api/v1/users \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{
    "fullName":"Marie Kabila",
    "email":"marie.kabila@afya.local",
    "firstName":"Marie",
    "lastName":"Kabila",
    "role":"RECEPTION",
    "hospitalServiceIds":[1]
  }' | jq .
```

Journal des identifiants créés (fichier `./data/credentials-log.txt` dans le répertoire de travail d’**identity-service**) :

- Aperçu : `GET /api/v1/users/credentials-log/preview`
- Téléchargement : `GET /api/v1/users/credentials-log` ou `.csv`

### Périmètre par service hospitalier (JWT)

Chaque utilisateur peut être affecté à un ou plusieurs **services hospitaliers** (`hospitalServiceIds` en base identity, claim JWT `hospitalServiceIds`).

| Comportement | Détail |
|--------------|--------|
| **ADMIN** | Voit toutes les admissions (care-entry) ; accès admin utilisateurs, audit, catalogue complet. |
| **Autres rôles** | Liste des admissions filtrée sur les `hospitalServiceIds` du token ; sans affectation → liste vide. |
| **Profil `/auth/me`** | IDs + noms de services (noms enrichis par le BFF via catalog). |

À la création ou modification d’un utilisateur, renseigner `hospitalServiceIds` (IDs du catalog). L’UI **Utilisateurs** (`/users`) permet de gérer ces affectations lorsque `usersAdmin: true` dans `frontend/src/config/features.ts`.

### Urgences, occupation et suggestion de lit (phase C)

Liste paginée des passages (filtres `status`, `priority`, tri) :

```bash
curl -s "http://localhost:8080/api/v1/urgences?page=0&size=20&sortBy=createdAt&sortDir=desc" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Triage et orientation (care-entry, proxifiés si besoin côté BFF ultérieurement) :

```bash
curl -s -X POST "http://localhost:8084/api/v1/urgences/1/triage" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"triageLevel":"2","details":"Douleur thoracique"}' | jq .

curl -s -X POST "http://localhost:8084/api/v1/urgences/1/orientation" \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"orientation":"Cardiologie"}' | jq .
```

KPI occupation (admin, tableau de bord) :

```bash
curl -s http://localhost:8080/api/v1/stats/occupancy \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### Lits, chambres et admissions

Pour chaque **service hospitalier** :

- **`bed_capacity`** = nombre total de **lits** (chaque lit compte : admission, occupation, statistiques).
- **`beds_per_room`** = lits par chambre (ex. `2` → chambre `001` a `001-A` et `001-B`).
- **Chambres** = `ceil(bed_capacity / beds_per_room)` (ex. 10 lits, 2/chambre → 5 chambres, 10 lignes `beds`).

Le catalog crée automatiquement une ligne `beds` par lit. Modèle : **chambre** = lettre + numéro **sans zéros initiaux** (ex. `A1`, `A2`, lettre configurable) + **lit** = numéro (ex. `01`, `02`). Plusieurs lits par chambre : `A3-01`, `A3-02`. Libellé interne `A1-01`. Si des lits manquent en base, **Générer lits** ou une mise à jour de capacité complète jusqu’à `bed_capacity`.

À **chaque admission** d’un patient (capacité > 0) :

1. Si chambre/lit ne sont pas saisis, le système propose le **premier lit libre** (chambres dans l’ordre A1, A2, …).
2. Ces valeurs sont enregistrées sur le **séjour** (stay-service).
3. Le lit catalog passe à **occupé** ; à la **sortie**, `last_freed_at` est mis à jour et le lit redevient libre.

L’interface remplit chambre/lit dès que vous choisissez le service ; l’attribution est aussi garantie côté **care-entry** et **BFF** si les champs sont vides.

Suggestion de lit libre pour un service (nom catalog) :

```bash
curl -s "http://localhost:8080/api/v1/admissions/suggestions/bed?serviceName=Médecine%20interne" \
  -H "Authorization: Bearer $TOKEN" | jq .
```

Admin : **Organisation** → service → **Voir lits** / **Générer lits** si besoin. Redémarrer `catalog-service`, `care-entry-service` et `afya-bff` après mise à jour du code.

### Documents cliniques et statistiques volumes (phase D)

**MinIO** (optionnel, recommandé en dev) :

```bash
podman compose up -d minio
# Console : http://localhost:9001 — identifiants minio / minio123
```

Activer MinIO côté **clinical-record-service** :

```bash
export STORAGE_PROVIDER=minio
export MINIO_ENDPOINT=http://127.0.0.1:9000
export MINIO_ACCESS_KEY=minio
export MINIO_SECRET_KEY=minio123
export MINIO_BUCKET=afya-clinical
./mvnw -pl clinical-record-service spring-boot:run
```

Sans MinIO, le service utilise par défaut le **filesystem** (`./data/clinical-object-store`).

Upload via le BFF :

```bash
curl -s -X POST "http://localhost:8080/api/v1/patients/1/documents/upload" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/chemin/vers/radio.pdf" \
  -F "title=Radio thorax" | jq .

curl -s -O -J "http://localhost:8080/api/v1/patients/1/documents/1/download" \
  -H "Authorization: Bearer $TOKEN"
```

Volumes agrégés (admin) :

```bash
curl -s http://localhost:8080/api/v1/stats/volumes \
  -H "Authorization: Bearer $TOKEN" | jq .
```

## 11. Front React (Vite)

Le dossier **`frontend/`** est l’interface Afya Santé (React 18, thèmes clair / sombre / contraste). Toutes les requêtes passent par le **BFF** — pas d’appel direct aux microservices depuis le navigateur.

```bash
cd frontend
npm install
npm run dev
```

| Élément | Valeur |
|--------|--------|
| UI dev | **http://localhost:5173** |
| Proxy Vite | `/api` → **http://127.0.0.1:8080** (BFF) |
| Build prod | `npm run build` puis fichiers dans `frontend/dist/` |
| Compte bootstrap | `admin` / `Admin@Afya2026!` |

Configuration proxy : `frontend/vite.config.ts` (cible `127.0.0.1`, pas `localhost`, pour éviter `ECONNREFUSED` si le BFF n’écoute qu’en IPv4 sur Fedora).

### Modules disponibles dans l’UI (via BFF)

| Écran | Statut |
|-------|--------|
| Connexion, tableau de bord (KPI partiels) | OK |
| Patients (liste, création, fiche, modification) | OK |
| Admissions (liste, création par nom de service, transfert, sortie) | OK |
| Urgences (liste paginée, filtres statut/priorité, création, clôture) | OK (triage/orientation API ; timeline UI masquée) |
| KPI occupation lits (admin, tableau de bord) | OK |
| Suggestion chambre/lit à l’admission ou au transfert | OK (catalog + lits libres) |
| Dossiers médicaux (notes, diagnostics, prescriptions, **documents** upload/téléchargement) | OK |
| Services hospitaliers (CRUD) | OK |
| Reporting admin (rapport d’activité audit + journal `/api/v1/audit/events`) | OK |
| Formulaire clinique d’hospitalisation (`/api/v1/admissions/{id}/clinical-form` → stay) | OK |
| Administration utilisateurs (`/api/v1/users`, affectations services) | OK (ADMIN) |
| Consultations (fiche, chronologie, diagnostics type+maladie, catalogue, prescriptions) | OK — menu **Consultations** ; BFF `/api/v1/consultations`, `/api/v1/disease-catalog` |
| Signes vitaux admission, déclaration décès, fil d’événements urgences | Masqués — voir `frontend/src/config/features.ts` |

**Backend (phase A)** : à l’admission, **care-entry** ouvre automatiquement un **séjour** (stay-service). Chambre/lit optionnels via `roomLabel` / `bedLabel` à la création d’admission.

**Backend (phase B)** : CRUD utilisateurs et rôles dans **identity-service** ; proxy BFF + profil enrichi (`hospitalServiceNames`) ; filtrage des **admissions** par `hospitalServiceIds` du JWT pour les non-admins (`afya-shared` : `JwtAuthDetails`, `HospitalScopeSupport`).

**Backend (phase C)** : **care-entry** — urgences enrichies (`priority`, `triageLevel`, `orientation`, statuts `EN_ATTENTE_TRIAGE` / `EN_COURS` / `ORIENTE` / `CLOTURE`, liste paginée, `POST .../triage`, `POST .../orientation`) ; **catalog** — `GET /api/v1/stats/occupancy`, `GET /api/v1/hospital-services/{id}/bed-suggestion` ; **BFF** — `GET /api/v1/stats/occupancy`, `GET /api/v1/admissions/suggestions/bed?serviceName=`.

**Backend (phase D)** : **MinIO** (docker-compose port 9000) ou stockage fichier local (`STORAGE_PROVIDER=filesystem`) ; **clinical-record** — `POST .../documents/upload` (multipart), `GET .../documents/{id}/download` ; **BFF** — mêmes routes + `GET /api/v1/stats/volumes` (agrégat admin : patients, admissions, urgences, séjours ouverts, documents).

**Backend (phase E)** : **API Gateway** Nginx (`infra/gateway`, service compose **`api`**, port **8090**) devant le BFF ; `server.forward-headers-strategy=framework` sur le BFF ; scripts `scripts/smoke-api.sh`, `.env.example` ; observabilité BFF : `actuator/health`, `metrics`.

Les rôles API / JWT utilisent le préfixe `ROLE_` (ex. `ROLE_ADMIN`, `ROLE_RECEPTION`). Le front normalise les variantes sans préfixe via `frontend/src/auth/roles.ts`.

Depuis une fiche admission, le lien **Dossier clinique** pointe vers `/medical-records/{patientId}` (API `GET /api/v1/patients/{id}/medical-record`).

### Dépannage : « Identifiants invalides ou serveur indisponible » à la connexion (UI **8088**)

Souvent un **502** de la gateway vers le BFF (conteneur `bff` recréé, IP mise en cache par Nginx) — pas un mauvais mot de passe.

1. Vérifier le login via la gateway :
   ```bash
   curl -s -X POST http://127.0.0.1:8090/api/v1/auth/login \
     -H 'Content-Type: application/json' \
     -d '{"username":"admin","password":"Admin@Afya2026!"}' | head -c 80
   ```
   Si la réponse commence par `{"accessToken"`, les identifiants sont bons.
2. Corriger sans rebuild : `podman exec afya-api-1 nginx -s reload`
3. Reconstruire la gateway (résolution DNS dynamique du BFF) : `podman compose -f docker-compose.stack.yml up -d --build api`

Compte bootstrap : **`admin`** / **`Admin@Afya2026!`** — UI : http://127.0.0.1:8088/login

### Dépannage : `vite http proxy error ECONNREFUSED` sur `/api/...`

1. La **gateway** (8090) et le **BFF** (8080) doivent être joignables :
   ```bash
   podman compose up -d api
   curl -s http://127.0.0.1:8090/gateway/health
   curl -s http://127.0.0.1:8080/actuator/health
   ```
2. Vérifier **identity** (login via gateway) :
   ```bash
   ./scripts/smoke-api.sh
   ```
3. Si la gateway répond mais pas le front : redémarrer Vite (`vite.config.ts` cible **8090** par défaut).
4. Sans gateway : `VITE_API_PROXY_TARGET=http://127.0.0.1:8080 npm run dev`.
5. BFF sur un autre port : `BFF_UPSTREAM=http://host.docker.internal:8088` avant `podman compose up -d api`.

### Nginx (prod / conteneur web)

Fichier `frontend/docker/nginx-default.conf` : `/api/` → service compose **`api`** (gateway), fichiers statiques pour le SPA.

## 12. Démarrage minimal (dev UI)

**Règle : un terminal (ou une fenêtre tmux) par microservice.** Ne pas lancer tous les services dans une seule commande Maven — les logs et les redémarrages deviennent illisibles.

### Étape 0 — Infra (1 terminal)

```bash
cd ~/IdeaProjects/afya
podman compose up -d
```

Attendre que les bases Postgres soient **healthy**, puis lancer la gateway **après** le BFF (étape 2) :

```bash
podman compose up -d api    # gateway → BFF sur l'hôte (port 8090)
```

### Étape 1 — Microservices (1 terminal chacun)

Ordre recommandé (dépendances inter-services) :

| Terminal | Service | Port | Commande |
|----------|---------|------|----------|
| 1 | identity-service | 8081 | `./mvnw -pl identity-service spring-boot:run` |
| 2 | catalog-service | 8082 | `./mvnw -pl catalog-service spring-boot:run` |
| 3 | patient-service | 8083 | `./mvnw -pl patient-service spring-boot:run` |
| 4 | stay-service | 8085 | `./mvnw -pl stay-service spring-boot:run` |
| 5 | care-entry-service | 8084 | `./mvnw -pl care-entry-service spring-boot:run` |
| 6 | clinical-record-service | 8086 | `./mvnw -pl clinical-record-service spring-boot:run` |
| 7 | audit-service | 8087 | `./mvnw -pl audit-service spring-boot:run` |

Liste complète à copier-coller : `./scripts/dev-print-terminals.sh`

**care-entry** appelle patient + catalog : démarrer les terminaux 2 et 3 avant le 5.

### Étape 2 — BFF (1 terminal)

Quand identity et catalog répondent (8081, 8082) :

```bash
./mvnw -pl afya-bff spring-boot:run    # port 8080
```

Puis activer la gateway si ce n'est pas déjà fait :

```bash
podman compose up -d api
```

### Étape 3 — Frontend (1 terminal)

```bash
cd frontend
npm install   # première fois uniquement
npm run dev   # port 5173
```

Ouvrir **http://localhost:5173**, se connecter avec `admin` / `Admin@Afya2026!`.

### Vérification

```bash
./scripts/check-ports.sh   # 5173, 8090, 8080–8087, 9000
./scripts/smoke-api.sh     # gateway + login
```

### Alternative tmux (fenêtres au lieu de terminaux séparés)

Même principe — **une fenêtre par service** :

```bash
./scripts/dev-tmux.sh
```

Raccourcis : `Ctrl+b` puis `n` (fenêtre suivante), `p` (précédente), `d` (détacher sans arrêter).

Arrêter la session : `tmux kill-session -t afya-platform`

### TLS auto-signé (gateway)

Certificats de développement (non approuvés par le navigateur) :

```bash
./scripts/generate-tls-certs.sh
podman compose up -d --build api   # monte infra/gateway/certs → HTTPS 8443
```

| Point d'accès | URL |
|---------------|-----|
| API HTTP | http://127.0.0.1:8090 |
| API HTTPS | https://127.0.0.1:8443 |

Smoke HTTPS :

```bash
GATEWAY_BASE=https://127.0.0.1:8443 ./scripts/smoke-api.sh
```

(`curl -k` automatique pour les URLs `https://`.)

### Stack full conteneurisée

Tous les microservices, le BFF, la gateway (TLS) et l’UI Nginx dans un seul compose — **sans Maven sur l’hôte** :

```bash
./scripts/stack-up.sh
# chemin relatif obligatoire (pas /scripts/stack-up.sh)
# ou : ./scripts/generate-tls-certs.sh && podman compose -f docker-compose.stack.yml up -d --build
```

| Service | Port hôte | Rôle |
|---------|-----------|------|
| `web` | **8088** | SPA React (Nginx) |
| `api` | **8090** / **8443** | Gateway → `bff:8080` |
| `minio` | 9000 / 9001 | Stockage documents (clinical) |
| Bases Postgres | *(internes)* | Volumes `stack_*` dédiés |

Compte : `admin` / `Admin@Afya2026!` — UI : http://127.0.0.1:8088

Profil Spring **`docker`** : JDBC sur les conteneurs `*-db`, URLs inter-services par nom DNS compose (`http://patient:8083`, etc.). Fichiers `application-docker.properties` par module.

Arrêt :

```bash
podman compose -f docker-compose.stack.yml down
```

La première construction Maven dans les images peut prendre **10–20 minutes** (téléchargement des dépendances).

**Scripts utiles :**

| Script | Rôle |
|--------|------|
| `scripts/pull-stack-images.sh` | Télécharge les images de base une par une |
| `scripts/stack-wait.sh` | Attend gateway + BFF (stack) avant smoke |
| `scripts/stack-up.sh` | Certificats + pull + build + wait (stack conteneurisée) |
| `scripts/dev-print-terminals.sh` | Liste des commandes — un terminal par microservice |
| `scripts/dev-tmux.sh` | Même chose en fenêtres tmux (identity … audit, bff, web) |

**Dépannage stack**

| Symptôme | Cause / action |
|----------|----------------|
| `registry-1.docker.io` / `connection reset by peer` | Réseau ou Docker Hub indisponible — relancer `./scripts/pull-stack-images.sh` puis `./scripts/stack-up.sh` |
| `apk add bash` / TLS Alpine dans le build | Images Spring passées en **Debian** (`eclipse-temurin:21-jdk`, pas `-alpine`) — refaire `pull-stack-images.sh` puis `stack-up.sh` |
| Contexte build ~450 Mo | Normal sans `.dockerignore` ; avec `.dockerignore` à la racine, le contexte est bien plus petit |
| Smoke 502 après échec `stack-up` | Seule la gateway **dev** (`docker-compose.yml`) tourne encore → BFF sur l’hôte absent ; soit relancer la stack, soit `./mvnw -pl identity-service,afya-bff spring-boot:run` |
| `COMPOSE_PARALLEL_LIMIT=1 ./scripts/stack-up.sh` | Builds Maven un par un (connexion instable) |

Ne lancer le smoke **qu’après** `stack-wait` OK ou `podman compose -f docker-compose.stack.yml ps` montrant `bff` et `api` healthy.

### TLS / production

En production : remplacer les certificats auto-signés par des certificats émis (Let’s Encrypt, PKI interne) et ne pas exposer les microservices ni le BFF directement sur Internet — uniquement la gateway (ou un Ingress) en entrée.

Voir aussi [ARCHITECTURE_SERVICES.md](ARCHITECTURE_SERVICES.md) et [infra/gateway/README.md](../infra/gateway/README.md).
