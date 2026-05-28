# II.2.2. Dictionnaire des données — Afya

Un dictionnaire de données représente l’ensemble des données manipulées par le système. Il permet à l’équipe de développement d’avoir accès à la même information.

Pour une base de données, c’est une collection de données qui sert à la conception d’une base relationnelle. Le dictionnaire est obtenu suite à l’étude des besoins du client. Il est intéressant de dresser la liste des données que le système va utiliser dans un tableau. Pour chaque donnée, on décrit : le **nom**, la **description** (utile lorsque le nom n’est pas significatif), le **type** (selon le SGBD ou général), les **contraintes**, et un **commentaire** le cas échéant.

Le présent document couvre **deux réalisations** du projet Afya :

| Version | Dépôt | Architecture | SGBD |
|---------|-------|--------------|------|
| **Afya Health System** | `afya-health-system` | Monolithe modulaire (Spring Boot) | Oracle |
| **Afya Platform** | `afya` | Microservices + BFF | PostgreSQL (base par service) |

**Légende des types** : types logiques ; `CLOB` / `TEXT` pour textes longs ; `ENUM` = valeur texte contrôlée en application.

---

## A. Afya Health System (monolithe — Oracle)

Schéma unique géré par Flyway (`afya-server/src/main/resources/db/migration/oracle/`). Package racine : `com.afya.afya_health_system`.

### A.1 Patients et numérotation

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `patients.id` | Identifiant technique du patient | NUMBER(19) | PK, auto-incrément, NOT NULL | Clé primaire |
| `patients.first_name` | Prénom | VARCHAR2(80) | NOT NULL | |
| `patients.last_name` | Nom de famille | VARCHAR2(80) | NOT NULL | |
| `patients.dossier_number` | Numéro de dossier hospitalier | VARCHAR2(40) | NOT NULL, UNIQUE | Généré via séquence annuelle |
| `patients.birth_date` | Date de naissance | DATE | NOT NULL | |
| `patients.sex` | Sexe (ex. M, F) | VARCHAR2(10) | NOT NULL | |
| `patients.phone` | Téléphone | VARCHAR2(120) | NULL | |
| `patients.email` | Courriel | VARCHAR2(120) | NULL | |
| `patients.address` | Adresse | VARCHAR2(255) | NULL | |
| `patients.post_name` | Post-nom | VARCHAR2(120) | NULL | Usage administratif congolais |
| `patients.employer` | Employeur | VARCHAR2(120) | NULL | |
| `patients.employee_id` | Matricule employeur | VARCHAR2(80) | NULL | |
| `patients.profession` | Profession | VARCHAR2(120) | NULL | |
| `patients.spouse_name` | Nom du conjoint | VARCHAR2(120) | NULL | |
| `patients.spouse_profession` | Profession du conjoint | VARCHAR2(120) | NULL | |
| `patients.deceased_at` | Date/heure du décès enregistré | TIMESTAMP | NULL | Verrouille certaines opérations métier |
| `patient_dossier_sequences.id` | Identifiant technique | NUMBER(19) | PK, NOT NULL | |
| `patient_dossier_sequences.sequence_year` | Année de la séquence | NUMBER(4) | NOT NULL, UNIQUE | Une ligne par année |
| `patient_dossier_sequences.letter_block` | Bloc alphabétique du numéro | VARCHAR2(4) | NOT NULL | |
| `patient_dossier_sequences.sequence_number` | Compteur séquentiel | NUMBER(10) | NOT NULL | Incrément atomique |

### A.2 Identité et sécurité

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `app_users.id` | Identifiant utilisateur | NUMBER(19) | PK, NOT NULL | |
| `app_users.username` | Identifiant de connexion | VARCHAR2(80) | NOT NULL, UNIQUE | |
| `app_users.email` | Adresse e-mail | VARCHAR2(160) | UNIQUE, NULL | Connexion possible par e-mail |
| `app_users.full_name` | Nom complet affiché | VARCHAR2(120) | NOT NULL | |
| `app_users.password_hash` | Mot de passe haché (BCrypt) | VARCHAR2(255) | NOT NULL | Jamais en clair |
| `app_users.active` | Compte actif | NUMBER(1) / BOOLEAN | NOT NULL, défaut vrai | |
| `app_users.failed_login_attempts` | Échecs de connexion consécutifs | NUMBER(10) | NOT NULL | Anti brute-force |
| `app_users.locked_until` | Fin de verrouillage temporaire | TIMESTAMP | NULL | |
| `roles.id` | Identifiant rôle | NUMBER(19) | PK, NOT NULL | |
| `roles.code` | Code rôle (ex. ADMIN, MEDECIN) | VARCHAR2(64) | NOT NULL, UNIQUE | Injecté dans le JWT |
| `roles.label` | Libellé affiché du rôle | VARCHAR2(120) | NOT NULL | |
| `user_roles.user_id` | Utilisateur | NUMBER(19) | PK composite, FK → `app_users` | Association N-N |
| `user_roles.role_id` | Rôle | NUMBER(19) | PK composite, FK → `roles` | |
| `user_hospital_services.user_id` | Utilisateur | NUMBER(19) | PK composite, FK → `app_users` | |
| `user_hospital_services.hospital_service_id` | Service hospitalier autorisé | NUMBER(19) | PK composite | Périmètre admissions / urgences |
| `refresh_tokens.id` | Identifiant ligne refresh | NUMBER(19) | PK, NOT NULL | |
| `refresh_tokens.token` | Valeur du refresh token | VARCHAR2(2000) | NOT NULL, UNIQUE | Rotation à chaque refresh |
| `refresh_tokens.username` | Utilisateur propriétaire | VARCHAR2(80) | NOT NULL | |
| `refresh_tokens.expires_at` | Expiration | TIMESTAMP | NOT NULL | |
| `refresh_tokens.revoked` | Jeton révoqué | NUMBER(1) | NOT NULL | Logout / rotation |
| `revoked_access_jti.jti` | Identifiant unique du JWT d’accès | VARCHAR2(128) | PK, NOT NULL | Claim `jti` |
| `revoked_access_jti.expires_at` | Expiration naturelle du JWT | TIMESTAMP | NOT NULL | Purge automatique possible |
| `revoked_access_jti.username` | Utilisateur concerné | VARCHAR2(80) | NOT NULL | |

### A.3 Catalogue hospitalier

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `hospital_services.id` | Identifiant service | NUMBER(19) | PK, NOT NULL | |
| `hospital_services.name` | Nom du service | VARCHAR2(120) | NOT NULL, UNIQUE | Référence textuelle dans admissions |
| `hospital_services.bed_capacity` | Capacité lits | NUMBER(10) | NOT NULL | |
| `hospital_services.active` | Service actif au catalogue | NUMBER(1) | NOT NULL | |

### A.4 Hospitalisation

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `admissions.id` | Identifiant séjour | NUMBER(19) | PK, NOT NULL | |
| `admissions.patient_id` | Patient hospitalisé | NUMBER(19) | NOT NULL, FK → `patients` | |
| `admissions.service_name` | Service d’hébergement | VARCHAR2(80) | NOT NULL | Copie du nom catalogue |
| `admissions.room` | Chambre | VARCHAR2(20) | NULL | |
| `admissions.bed` | Lit | VARCHAR2(20) | NULL | |
| `admissions.reason` | Motif d’admission | VARCHAR2(255) | NULL | |
| `admissions.admission_date_time` | Date/heure d’entrée | TIMESTAMP | NOT NULL | |
| `admissions.discharge_date_time` | Date/heure de sortie | TIMESTAMP | NULL | |
| `admissions.status` | État du séjour | VARCHAR2(20) | NOT NULL, ENUM | `EN_COURS`, `TRANSFERE`, `SORTI`, `DECEDE` |
| `admission_movements.id` | Identifiant mouvement | NUMBER(19) | PK, NOT NULL | |
| `admission_movements.admission_id` | Séjour concerné | NUMBER(19) | NOT NULL, FK | |
| `admission_movements.type` | Type (transfert, etc.) | VARCHAR2(20) | NOT NULL | |
| `admission_movements.from_service` | Service d’origine | VARCHAR2(80) | NULL | |
| `admission_movements.to_service` | Service de destination | VARCHAR2(80) | NULL | |
| `admission_movements.created_at` | Horodatage | TIMESTAMP | NOT NULL | |
| `admission_movements.note` | Commentaire | VARCHAR2(255) | NULL | |
| `admission_clinical_forms.id` | Identifiant formulaire | NUMBER(19) | PK, NOT NULL | |
| `admission_clinical_forms.admission_id` | Séjour | NUMBER(19) | NOT NULL, UNIQUE | Un formulaire par admission |
| `admission_clinical_forms.antecedents_text` | Antécédents | CLOB | NULL | |
| `admission_clinical_forms.anamnesis_text` | Anamnèse | CLOB | NULL | |
| `admission_clinical_forms.physical_exam_pulmonary_text` | Examen pulmonaire | CLOB | NULL | |
| `admission_clinical_forms.physical_exam_cardiac_text` | Examen cardiaque | CLOB | NULL | |
| `admission_clinical_forms.physical_exam_abdominal_text` | Examen abdominal | CLOB | NULL | |
| `admission_clinical_forms.physical_exam_neurological_text` | Examen neurologique | CLOB | NULL | |
| `admission_clinical_forms.physical_exam_misc_text` | Examen divers | CLOB | NULL | |
| `admission_clinical_forms.paraclinical_text` | Bilan paraclinique | CLOB | NULL | |
| `admission_clinical_forms.conclusion_text` | Conclusion | CLOB | NULL | |
| `prescription_lines.id` | Identifiant ligne | NUMBER(19) | PK, NOT NULL | |
| `prescription_lines.admission_id` | Séjour | NUMBER(19) | NOT NULL, FK | Index |
| `prescription_lines.medication_name` | Médicament | VARCHAR2(500) | NOT NULL | |
| `prescription_lines.dosage_text` | Posologie | VARCHAR2(500) | NULL | |
| `prescription_lines.frequency_text` | Fréquence | VARCHAR2(255) | NULL | |
| `prescription_lines.instructions_text` | Consignes | CLOB | NULL | |
| `prescription_lines.prescriber_name` | Prescripteur | VARCHAR2(120) | NULL | |
| `prescription_lines.start_date` | Début traitement | DATE | NOT NULL | |
| `prescription_lines.end_date` | Fin traitement | DATE | NULL | |
| `prescription_lines.active` | Ligne active | NUMBER(1) | NOT NULL | |
| `prescription_lines.created_at` | Création | TIMESTAMP | NOT NULL | |
| `medication_administrations.id` | Identifiant | NUMBER(19) | PK, NOT NULL | |
| `medication_administrations.prescription_line_id` | Ligne de prescription | NUMBER(19) | NOT NULL, FK | |
| `medication_administrations.administration_date` | Date d’administration | DATE | NOT NULL | |
| `medication_administrations.slot` | Créneau | VARCHAR2(20) | NOT NULL, ENUM | `MATIN`, `SOIR`, `JOURNEE` |
| `medication_administrations.administered` | Dose administrée | NUMBER(1) | NOT NULL | UNIQUE (ligne, date, slot) |
| `vital_sign_readings.id` | Identifiant relevé | NUMBER(19) | PK, NOT NULL | |
| `vital_sign_readings.admission_id` | Séjour | NUMBER(19) | NOT NULL, FK | Index (admission, recorded_at) |
| `vital_sign_readings.recorded_at` | Date/heure du relevé | TIMESTAMP | NOT NULL | |
| `vital_sign_readings.slot` | Créneau | VARCHAR2(20) | NULL, ENUM | Matin / soir / journée |
| `vital_sign_readings.systolic_bp` | Tension systolique (mmHg) | NUMBER(10) | NULL | |
| `vital_sign_readings.diastolic_bp` | Tension diastolique | NUMBER(10) | NULL | |
| `vital_sign_readings.pulse_bpm` | Pouls (bpm) | NUMBER(10) | NULL | |
| `vital_sign_readings.temperature_celsius` | Température (°C) | NUMBER(5,2) | NULL | |
| `vital_sign_readings.weight_kg` | Poids (kg) | NUMBER(6,2) | NULL | |
| `vital_sign_readings.diuresis_ml` | Diurèse (ml) | NUMBER(10) | NULL | |
| `vital_sign_readings.stools_note` | Selles / transit | VARCHAR2(500) | NULL | |

### A.5 Urgences

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `urgences.id` | Identifiant passage | NUMBER(19) | PK, NOT NULL | |
| `urgences.patient_id` | Patient | NUMBER(19) | NOT NULL, FK | |
| `urgences.motif` | Motif de venue | VARCHAR2(255) | NULL | |
| `urgences.priority` | Priorité | VARCHAR2(40) | NULL | |
| `urgences.triage_level` | Niveau de triage | VARCHAR2(40) | NULL | |
| `urgences.orientation` | Orientation | VARCHAR2(80) | NULL | |
| `urgences.status` | État du passage | VARCHAR2(30) | NOT NULL, ENUM | `EN_ATTENTE_TRIAGE`, `EN_COURS`, `ORIENTE`, `CLOTURE` |
| `urgences.created_at` | Ouverture | TIMESTAMP | NOT NULL | |
| `urgences.closed_at` | Clôture | TIMESTAMP | NULL | |
| `urgence_timeline_events.id` | Identifiant événement | NUMBER(19) | PK, NOT NULL | |
| `urgence_timeline_events.urgence_id` | Passage aux urgences | NUMBER(19) | NOT NULL, FK | |
| `urgence_timeline_events.type` | Type d’événement | VARCHAR2(40) | NOT NULL | |
| `urgence_timeline_events.details` | Détail | VARCHAR2(255) | NULL | |
| `urgence_timeline_events.created_at` | Horodatage | TIMESTAMP | NOT NULL | |

### A.6 Dossier médical et consultations

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `medical_records.id` | Identifiant dossier | NUMBER(19) | PK, NOT NULL | |
| `medical_records.patient_id` | Patient | NUMBER(19) | NOT NULL, UNIQUE | Un dossier par patient |
| `medical_records.allergies` | Allergies | CLOB | NULL | |
| `medical_records.antecedents` | Antécédents généraux | CLOB | NULL | |
| `medical_records.created_at` | Création | TIMESTAMP | NOT NULL | |
| `medical_records.updated_at` | Dernière mise à jour | TIMESTAMP | NOT NULL | |
| `medical_record_entries.id` | Identifiant entrée | NUMBER(19) | PK, NOT NULL | |
| `medical_record_entries.patient_id` | Patient | NUMBER(19) | NOT NULL, FK | |
| `medical_record_entries.type` | Type d’entrée | VARCHAR2(40) | NOT NULL | |
| `medical_record_entries.content` | Contenu clinique | CLOB | NOT NULL | |
| `medical_record_entries.created_at` | Horodatage | TIMESTAMP | NOT NULL | |
| `consultations.id` | Identifiant consultation | NUMBER(19) | PK, NOT NULL | |
| `consultations.patient_id` | Patient | NUMBER(19) | NOT NULL, FK | |
| `consultations.admission_id` | Séjour lié | NUMBER(19) | NOT NULL, FK | |
| `consultations.doctor_name` | Médecin | VARCHAR2(80) | NOT NULL | |
| `consultations.reason` | Motif | VARCHAR2(255) | NULL | |
| `consultations.consultation_date_time` | Date/heure | TIMESTAMP | NOT NULL | |
| `consultation_events.id` | Identifiant | NUMBER(19) | PK, NOT NULL | |
| `consultation_events.consultation_id` | Consultation | NUMBER(19) | NOT NULL, FK | |
| `consultation_events.patient_id` | Patient (dénormalisé) | NUMBER(19) | NOT NULL | |
| `consultation_events.type` | Type d’événement | VARCHAR2(40) | NOT NULL | |
| `consultation_events.content` | Contenu | CLOB | NOT NULL | |
| `consultation_events.created_at` | Horodatage | TIMESTAMP | NOT NULL | |

### A.7 Données transitoires (non persistées en table dédiée)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `access_token` | Jeton JWT d’accès | Chaîne (JWT) | HS512, claims `jti`, `roles`, `fullName` | Durée configurable (env.) |
| `refresh_token` (API) | Jeton de renouvellement | Chaîne (JWT) | Stocké aussi en `refresh_tokens` | |
| Indicateurs reporting | Agrégats (occupation, séjours, etc.) | DTO / requêtes SQL | Lecture seule | Module `afya-reporting` |

---

## B. Afya Platform (microservices — PostgreSQL)

Bases **séparées** par microservice. Les identifiants inter-services (`patient_id`, `hospital_service_id`, etc.) sont des références **logiques** (sans FK physique entre bases).

| Service | Base PostgreSQL (ex.) | Tables principales |
|---------|----------------------|-------------------|
| `catalog-service` | `afya_catalog` | `departments`, `hospital_services`, `beds` |
| `identity-service` | `afya_identity` | `roles`, `app_users`, `user_roles`, `refresh_tokens`, `revoked_access_jti` |
| `patient-service` | `afya_patient` | `patients`, `patient_dossier_sequences` |
| `care-entry-service` | `afya_care_entry` | `admissions`, `transfer_requests`, `emergency_visits` |
| `stay-service` | `afya_stay` | `stays`, `hospitalization_forms` |
| `clinical-record-service` | `afya_clinical` | `medical_records`, `clinical_notes`, `diagnoses`, `prescription_lines`, `medication_administrations`, `nursing_care_records`, `clinical_documents` |
| `audit-service` | `afya_audit` | `audit_events` |

### B.1 Catalogue (`catalog-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `departments.id` | Identifiant département | BIGINT | PK, identity, NOT NULL | |
| `departments.code` | Code court (ex. MED, CHIR) | VARCHAR(40) | NOT NULL, UNIQUE | |
| `departments.name` | Libellé département | VARCHAR(120) | NOT NULL | |
| `departments.active` | Département actif | BOOLEAN | NOT NULL, défaut TRUE | |
| `departments.created_at` | Date de création | TIMESTAMP | NOT NULL | |
| `hospital_services.id` | Identifiant service hospitalier | BIGINT | PK, NOT NULL | |
| `hospital_services.department_id` | Département parent | BIGINT | NOT NULL, FK → `departments` | Index |
| `hospital_services.name` | Nom du service | VARCHAR(120) | NOT NULL, UNIQUE | |
| `hospital_services.bed_capacity` | Capacité lits (théorique) | INT | NOT NULL, défaut 0 | |
| `hospital_services.active` | Service actif | BOOLEAN | NOT NULL, défaut TRUE | |
| `hospital_services.created_at` | Création | TIMESTAMP | NOT NULL | |
| `beds.id` | Identifiant lit | BIGINT | PK, NOT NULL | |
| `beds.hospital_service_id` | Service d’appartenance | BIGINT | NOT NULL, FK, ON DELETE CASCADE | |
| `beds.label` | Libellé lit (ex. L-01) | VARCHAR(40) | NOT NULL | UNIQUE (service, label) |
| `beds.occupied` | Lit occupé | BOOLEAN | NOT NULL, défaut FALSE | |

### B.2 Identité (`identity-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `roles.id` | Identifiant rôle | BIGINT | PK, NOT NULL | |
| `roles.code` | Code rôle | VARCHAR(40) | NOT NULL, UNIQUE | ADMIN, RECEPTION, MEDECIN, INFIRMIER |
| `roles.label` | Libellé affiché | VARCHAR(120) | NOT NULL | |
| `app_users.id` | Identifiant utilisateur | BIGINT | PK, NOT NULL | |
| `app_users.username` | Login | VARCHAR(80) | NOT NULL, UNIQUE | |
| `app_users.email` | E-mail | VARCHAR(160) | UNIQUE, NULL | |
| `app_users.full_name` | Nom complet | VARCHAR(120) | NOT NULL | Claim JWT |
| `app_users.password_hash` | Mot de passe BCrypt | VARCHAR(255) | NOT NULL | |
| `app_users.active` | Compte actif | BOOLEAN | NOT NULL, défaut TRUE | |
| `app_users.created_at` | Création compte | TIMESTAMP | NOT NULL | |
| `user_roles.user_id` | Utilisateur | BIGINT | PK composite, FK | Association N-N |
| `user_roles.role_id` | Rôle | BIGINT | PK composite, FK | |
| `refresh_tokens.id` | Identifiant ligne | BIGINT | PK, NOT NULL | |
| `refresh_tokens.user_id` | Utilisateur | BIGINT | NOT NULL, FK | |
| `refresh_tokens.token_hash` | Empreinte du refresh token | VARCHAR(128) | NOT NULL, UNIQUE | Pas le JWT en clair |
| `refresh_tokens.expires_at` | Expiration | TIMESTAMP | NOT NULL | |
| `refresh_tokens.revoked` | Révoqué | BOOLEAN | NOT NULL, défaut FALSE | |
| `refresh_tokens.created_at` | Création | TIMESTAMP | NOT NULL | |
| `revoked_access_jti.jti` | ID unique JWT d’accès | VARCHAR(64) | PK, NOT NULL | Logout |
| `revoked_access_jti.expires_at` | Expiration naturelle JWT | TIMESTAMP | NOT NULL | |
| `revoked_access_jti.revoked_at` | Date de révocation | TIMESTAMP | NOT NULL | |

### B.3 Patients (`patient-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `patients.id` | Identifiant patient | BIGINT | PK, NOT NULL | Référencé par autres services |
| `patients.first_name` | Prénom | VARCHAR(80) | NOT NULL | Index (nom, prénom) |
| `patients.last_name` | Nom | VARCHAR(80) | NOT NULL | |
| `patients.post_name` | Post-nom | VARCHAR(120) | NULL | |
| `patients.dossier_number` | N° dossier hospitalier | VARCHAR(40) | NOT NULL, UNIQUE | |
| `patients.birth_date` | Date de naissance | DATE | NOT NULL | |
| `patients.sex` | Sexe | VARCHAR(10) | NOT NULL | |
| `patients.phone` | Téléphone | VARCHAR(120) | NULL | |
| `patients.email` | E-mail | VARCHAR(120) | NULL | |
| `patients.address` | Adresse | VARCHAR(255) | NULL | |
| `patients.created_at` | Enregistrement | TIMESTAMP | NOT NULL | |
| `patient_dossier_sequences.sequence_year` | Année de numérotation | INT | PK, NOT NULL | |
| `patient_dossier_sequences.letter_block` | Bloc alphabétique | VARCHAR(4) | NOT NULL | |
| `patient_dossier_sequences.sequence_number` | Compteur | INT | NOT NULL | |

> **Rendez-vous** : hors périmètre Afya Platform. La table `appointments` peut subsister dans le schéma Flyway sans être documentée ni exposée (pas de BFF/front).

### B.4 Entrées de soins (`care-entry-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `admissions.id` | Identifiant admission | BIGINT | PK, NOT NULL | Lié à `stays.admission_id` |
| `admissions.patient_id` | Patient | BIGINT | NOT NULL | FK logique → patient-service |
| `admissions.hospital_service_id` | Service cible | BIGINT | NOT NULL | FK logique → catalog-service |
| `admissions.admitted_at` | Date/heure d’admission | TIMESTAMP | NOT NULL | |
| `admissions.discharged_at` | Date/heure de sortie | TIMESTAMP | NULL | |
| `admissions.status` | État admission | VARCHAR(20) | NOT NULL, ENUM | `OUVERTE`, `TRANSFEREE`, `SORTIE`, `ANNULEE` |
| `admissions.discharge_reason` | Motif de sortie | VARCHAR(255) | NULL | |
| `admissions.created_at` | Création | TIMESTAMP | NOT NULL | |
| `transfer_requests.id` | Identifiant demande | BIGINT | PK, NOT NULL | |
| `transfer_requests.admission_id` | Admission concernée | BIGINT | NOT NULL, FK | |
| `transfer_requests.from_service_id` | Service d’origine | BIGINT | NOT NULL | ID catalogue |
| `transfer_requests.to_service_id` | Service de destination | BIGINT | NOT NULL | ID catalogue |
| `transfer_requests.requested_at` | Date de demande | TIMESTAMP | NOT NULL | |
| `transfer_requests.reason` | Motif transfert | VARCHAR(255) | NULL | |
| `emergency_visits.id` | Identifiant passage urgences | BIGINT | PK, NOT NULL | |
| `emergency_visits.patient_id` | Patient | BIGINT | NOT NULL | |
| `emergency_visits.arrived_at` | Arrivée | TIMESTAMP | NOT NULL | |
| `emergency_visits.ended_at` | Fin de passage | TIMESTAMP | NULL | |
| `emergency_visits.status` | État | VARCHAR(20) | NOT NULL, ENUM | `EN_COURS`, `SORTIE`, `ADMIS` |
| `emergency_visits.triage_notes` | Notes de triage | VARCHAR(500) | NULL | |
| `emergency_visits.created_at` | Création | TIMESTAMP | NOT NULL | |

### B.5 Séjours (`stay-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `stays.id` | Identifiant séjour | BIGINT | PK, NOT NULL | |
| `stays.patient_id` | Patient | BIGINT | NOT NULL | |
| `stays.admission_id` | Admission liée | BIGINT | NOT NULL, UNIQUE | 1 séjour ↔ 1 admission |
| `stays.check_in_at` | Entrée effective (lit) | TIMESTAMP | NOT NULL | |
| `stays.check_out_at` | Sortie effective | TIMESTAMP | NULL | |
| `stays.room_label` | Chambre | VARCHAR(40) | NULL | |
| `stays.bed_label` | Lit | VARCHAR(40) | NULL | |
| `stays.status` | État séjour | VARCHAR(20) | NOT NULL, ENUM | `PLANIFIE`, `EN_COURS`, `SUSPENDU`, `CLOTURE`, `ANNULE` |
| `stays.created_at` | Création | TIMESTAMP | NOT NULL | |
| `hospitalization_forms.stay_id` | Séjour (clé = PK) | BIGINT | PK, FK → `stays` | Formulaire 1:1 |
| `hospitalization_forms.chief_complaint` | Motif principal | VARCHAR(500) | NULL | |
| `hospitalization_forms.history_text` | Histoire / anamnèse | VARCHAR(4000) | NULL | |
| `hospitalization_forms.allergies` | Allergies | VARCHAR(500) | NULL | |
| `hospitalization_forms.updated_at` | Dernière MAJ | TIMESTAMP | NOT NULL | |

### B.6 Dossier clinique (`clinical-record-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `medical_records.id` | Identifiant dossier | BIGINT | PK, NOT NULL | |
| `medical_records.patient_id` | Patient | BIGINT | NOT NULL, UNIQUE | Un dossier par patient |
| `medical_records.opened_at` | Ouverture dossier | TIMESTAMP | NOT NULL | |
| `clinical_notes.id` | Identifiant note | BIGINT | PK, NOT NULL | |
| `clinical_notes.medical_record_id` | Dossier | BIGINT | NOT NULL, FK | |
| `clinical_notes.authored_at` | Rédaction | TIMESTAMP | NOT NULL | |
| `clinical_notes.author_username` | Auteur | VARCHAR(80) | NOT NULL | |
| `clinical_notes.narrative` | Texte clinique | VARCHAR(4000) | NOT NULL | |
| `diagnoses.id` | Identifiant diagnostic | BIGINT | PK, NOT NULL | |
| `diagnoses.medical_record_id` | Dossier | BIGINT | NOT NULL, FK | |
| `diagnoses.code` | Code (ex. CIM) | VARCHAR(40) | NULL | |
| `diagnoses.label` | Libellé diagnostic | VARCHAR(255) | NOT NULL | |
| `diagnoses.recorded_at` | Enregistrement | TIMESTAMP | NOT NULL | |
| `diagnoses.author_username` | Auteur | VARCHAR(80) | NOT NULL | |
| `prescription_lines.id` | Ligne prescription | BIGINT | PK, NOT NULL | |
| `prescription_lines.medical_record_id` | Dossier | BIGINT | NOT NULL, FK | |
| `prescription_lines.drug_name` | Médicament | VARCHAR(120) | NOT NULL | |
| `prescription_lines.dosage` | Posologie | VARCHAR(80) | NOT NULL | |
| `prescription_lines.frequency` | Fréquence | VARCHAR(80) | NOT NULL | |
| `prescription_lines.start_date` | Début | DATE | NOT NULL | |
| `prescription_lines.end_date` | Fin | DATE | NULL | |
| `prescription_lines.status` | État | VARCHAR(20) | NOT NULL, ENUM | `ACTIVE`, `COMPLETED`, `CANCELLED` |
| `prescription_lines.prescribed_by` | Prescripteur | VARCHAR(80) | NOT NULL | |
| `prescription_lines.created_at` | Création | TIMESTAMP | NOT NULL | |
| `medication_administrations.id` | Administration | BIGINT | PK, NOT NULL | |
| `medication_administrations.prescription_line_id` | Ligne liée | BIGINT | NOT NULL, FK | |
| `medication_administrations.administered_at` | Date/heure | TIMESTAMP | NOT NULL | |
| `medication_administrations.dose_given` | Dose réelle | VARCHAR(80) | NULL | |
| `medication_administrations.nurse_username` | Infirmier(ère) | VARCHAR(80) | NOT NULL | |
| `medication_administrations.notes` | Notes | VARCHAR(500) | NULL | |
| `nursing_care_records.id` | Soin infirmier | BIGINT | PK, NOT NULL | |
| `nursing_care_records.medical_record_id` | Dossier | BIGINT | NOT NULL, FK | |
| `nursing_care_records.care_type` | Type de soin | VARCHAR(80) | NOT NULL | |
| `nursing_care_records.performed_at` | Réalisation | TIMESTAMP | NOT NULL | |
| `nursing_care_records.nurse_username` | Infirmier(ère) | VARCHAR(80) | NOT NULL | |
| `nursing_care_records.description` | Description | VARCHAR(2000) | NOT NULL | |
| `clinical_documents.id` | Document | BIGINT | PK, NOT NULL | |
| `clinical_documents.medical_record_id` | Dossier | BIGINT | NOT NULL, FK | |
| `clinical_documents.title` | Titre | VARCHAR(200) | NOT NULL | |
| `clinical_documents.content_type` | MIME | VARCHAR(100) | NOT NULL | |
| `clinical_documents.object_storage_key` | Clé stockage objet | VARCHAR(500) | NOT NULL | Fichier externe |
| `clinical_documents.uploaded_at` | Téléversement | TIMESTAMP | NOT NULL | |
| `clinical_documents.uploaded_by` | Utilisateur | VARCHAR(80) | NOT NULL | |

### B.7 Audit (`audit-service`)

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `audit_events.id` | Identifiant technique | BIGSERIAL | PK, NOT NULL | |
| `audit_events.event_id` | UUID événement | UUID | NOT NULL, UNIQUE | Idempotence |
| `audit_events.occurred_at` | Moment métier | TIMESTAMP | NOT NULL | Index DESC |
| `audit_events.actor_username` | Acteur | VARCHAR(80) | NOT NULL | |
| `audit_events.action` | Action (ex. CREATE, LOGIN) | VARCHAR(80) | NOT NULL | |
| `audit_events.resource_type` | Type ressource | VARCHAR(60) | NOT NULL | |
| `audit_events.resource_id` | ID ressource | VARCHAR(80) | NULL | |
| `audit_events.source_service` | Service émetteur | VARCHAR(40) | NOT NULL | |
| `audit_events.metadata_json` | Métadonnées JSON | TEXT | NULL | |
| `audit_events.created_at` | Persistance | TIMESTAMP | NOT NULL | |

### B.8 Données transitoires

| Donnée | Description | Type | Contraintes | Commentaire |
|--------|-------------|------|-------------|-------------|
| `access_token` | JWT d’accès | Chaîne (JWT) | HS512, `jti`, `roles`, `fullName` | Module `afya-shared` |
| `refresh_token` (API) | Jeton de renouvellement | Chaîne + hash en base | `identity-service` | |

---

## C. Synthèse comparative

| Aspect | Afya Health System | Afya Platform |
|--------|-------------------|---------------|
| Architecture | Monolithe modulaire | Microservices + BFF |
| SGBD | Oracle (schéma unique) | PostgreSQL (bases séparées) |
| Catalogue | `hospital_services` | `departments` + `hospital_services` + `beds` |
| Admission / séjour | `admissions` unique | `admissions` + `stays` |
| Urgences | `urgences` + timeline | `emergency_visits` |
| Dossier clinique | CLOB, consultations, formulaire admission détaillé | Notes, diagnostics, documents, soins infirmiers |
| Rendez-vous | Non | Hors périmètre (non retenu) |
| Audit centralisé | Logs applicatifs | `audit_events` |
| Périmètre utilisateur | `user_hospital_services` | À prévoir côté identity (non en V1 Flyway) |

---

*Document généré à partir du code source et des migrations Flyway des dépôts `afya-health-system` et `afya` (Afya Platform).*
