# Architettura di JobMatcher

Questo documento descrive l’architettura logica e tecnica del progetto **JobMatcher**.

## 1. Panoramica

JobMatcher è composto da tre servizi principali e un database:

1. **jobmatcher-app** – app mobile Flutter (Android/iOS)
2. **jobmatcher_api** – backend principale (Spring Boot)
3. **jobmatcher_ai** – microservizio Python (FastAPI)
4. **PostgreSQL** – database relazionale

L’utente interagisce solo con **jobmatcher-app**, che comunica con **jobmatcher_api** tramite API REST.  
Il backend Java, quando necessario, delega operazioni di OCR e NLP al microservizio **jobmatcher_ai**.

---

## 2. Componenti

### 2.1 jobmatcher-app (Flutter)

Responsabilità principali:

- Interfaccia utente per candidati e aziende
- Gestione flussi:
  - registrazione e login
  - gestione profili
  - caricamento CV
  - visualizzazione offerte
  - schermate di matching (offerte suggerite)
- Gestione geolocalizzazione lato client (lettura posizione GPS)
- Chiamate REST verso `jobmatcher_api` (gestione token JWT)

Non accede direttamente al database né al servizio AI.

---

### 2.2 jobmatcher_api (Spring Boot)

È il **backend “core”** dell’applicazione.

Responsabilità principali:

- Autenticazione e autorizzazione (JWT, ruoli candidato/azienda/admin)
- Gestione utenti:
  - registrazione, login, refresh token
  - profili candidati e aziende
- Gestione offerte di lavoro:
  - creazione, modifica, chiusura
  - salvataggio dati (titolo, descrizione, requisiti, sede, tipo contratto...)
- Gestione CV:
  - upload file (PDF/immagine)
  - associazione CV ad un candidato
  - memorizzazione risultati di parsing (testo, competenze, embedding…)
- Matching:
  - calcolo compatibilità tra CV e job (usando embedding ricevuti da `jobmatcher_ai`)
  - integrazione distanza geografica (Haversine o PostGIS)
  - API per restituire lista di offerte ordinate per score
- Notifiche:
  - gestione token device (per notifiche push)
  - logica per decidere quando inviare notifiche (implementazione successiva)

`jobmatcher_api` è responsabile **dell’integrità dei dati** e della logica di business.

jobmatcher_api/
├─ src/
│  ├─ main/
│  │  ├─ java/com/jobmatcher/api/
│  │  │   ├─ JobmatcherApiApplication.java
│  │  │   ├─ config/
│  │  │   │    └─ AiClientProperties.java
│  │  │   ├─ controller/
│  │  │   ├─ domain/
│  │  │   │    ├─ user/
│  │  │   │    ├─ candidate/
│  │  │   │    ├─ company/
│  │  │   │    ├─ job/
│  │  │   │    └─ curriculum/
│  │  │   ├─ service/
│  │  │   └─ repository/
│  │  └─ resources/
│  │       ├─ application.yml
│  │       └─ db/migration/   # per Flyway
│  └─ test/
│       └─ java/com/jobmatcher/api/
├─ pom.xml
└─ README.md


### 2.3 jobmatcher_ai (FastAPI)

È un microservizio dedicato alle funzionalità “intelligenti”:

- **OCR**
  - Riconoscimento testo da CV in formato immagine/PDF scansionato
  - Basato su Tesseract (`pytesseract`)
- **Parsing CV**
  - Pulizia del testo (remove rumore, normalizzazione)
  - Estrazione di:
    - esperienze lavorative
    - formazione
    - competenze (skills)
    - lingue
  - Restituzione di un JSON strutturato
- **NLP / embedding**
  - Generazione di embedding vettoriali del CV (e delle offerte)
  - Modelli tipo Sentence Transformers (es. `all-MiniLM-L6-v2`)
  - Calcolo similarità semantica (es. cosine similarity – lato Java o Python)

Espone endpoint REST consumati da `jobmatcher_api`. Non accede direttamente al database.

jobmatcher_ai/
├─ app/
│  ├─ __init__.py
│  ├─ main.py
│  ├─ api/
│  │   ├─ __init__.py
│  │   ├─ health.py
│  │   ├─ ocr.py
│  │   └─ cv.py
│  └─ services/
│      ├─ __init__.py
│      ├─ ocr_service.py
│      └─ nlp_service.py
├─ requirements.txt
└─ README.md


### 2.4 Database (PostgreSQL)

Tabelle principali previste (semplificate):

- `users`
  - id, email, password_hash, role, created_at, updated_at
- `candidates`
  - id (FK user), nome, cognome, data_nascita, posizione (lat/long, città, ecc.)
- `companies`
  - id (FK user), ragione_sociale, sede_legale, settore, ecc.
- `jobs`
  - id, company_id, titolo, descrizione, requisiti, tipo_contratto, sede, lat, long, stato, created_at
- `curricula`
  - id, candidate_id, file_path, testo_estratto, json_parsed, embedding (es. array o tabella separata)
- `device_tokens`
  - id, user_id, token, piattaforma, created_at

Le migrazioni DB saranno gestite tramite Flyway (file in `jobmatcher_api/src/main/resources/db/migration`).

---

## 3. Flussi principali

### 3.1 Registrazione e login

1. L’utente usa `jobmatcher-app` per registrarsi come candidato o azienda.
2. L’app invia i dati a `jobmatcher_api` (`POST /auth/register`).
3. Il backend:
   - valida i dati,
   - crea record in `users` e in `candidates` / `companies`,
   - restituisce un token JWT.
4. L’app salva i token in secure storage.

---

### 3.2 Upload e parsing CV

1. Il candidato, da `jobmatcher-app`, seleziona un file CV.
2. L’app invia il file via `multipart/form-data` a `jobmatcher_api` (`POST /candidates/me/cv`).
3. Il backend:
   - salva il file (filesystem o storage S3-like),
   - crea un record in `curricula` in stato `PENDING`,
   - chiama `jobmatcher_ai` (`POST /cv/parse`) passando il file o il testo.
4. `jobmatcher_ai`:
   - esegue OCR se necessario,
   - pulisce e analizza il testo,
   - genera embedding,
   - restituisce JSON con:
     - testo_estratto
     - competenze/esperienze/formazione
     - embedding vettoriale
5. `jobmatcher_api` aggiorna il record `curricula` con i dati ricevuti.

---

### 3.3 Creazione offerte di lavoro

1. Un utente azienda usa `jobmatcher-app` per creare una nuova offerta.
2. L’app invia la richiesta a `jobmatcher_api` (`POST /jobs`).
3. Il backend:
   - valida i dati,
   - geocodifica la sede (indirizzo → lat/long),
   - salva nei `jobs`,
   - chiama `jobmatcher_ai` (`POST /job/embed`) per generare l’embedding della job description,
   - salva embedding associato all’offerta (o in tabella dedicata).

---

### 3.4 Matching offerte per un candidato

1. Il candidato apre la sezione “Offerte suggerite per te” sull’app.
2. L’app chiama `jobmatcher_api` (`GET /candidates/me/matches?radius=50&limit=20`).
3. Il backend:
   - recupera l’ultimo CV valido e l’embedding associato,
   - filtra offerte in base alla distanza (query SQL con Haversine o PostGIS),
   - calcola il punteggio di compatibilità combinando:
     - similarità semantica CV ↔ job,
     - distanza geografica,
   - ordina i risultati per punteggio,
   - restituisce la lista all’app.
4. L’app visualizza le offerte con:
   - titolo, azienda, tipo contratto
   - distanza (in km)
   - percentuale di compatibilità stimata.

---

### 3.5 Notifiche (fase successiva)

1. Alla pubblicazione di una nuova offera, `jobmatcher_api`:
   - calcola i candidati più compatibili (es. score > soglia),
   - recupera i device token da `device_tokens`,
   - invia notifiche push tramite FCM.
2. L’app, ricevendo la notifica, apre la schermata della singola offerta.

---

## 4. Comunicazione tra servizi

### 4.1 API pubbliche (app → backend)

- Base URL (dev): `http://localhost:8080/api`
- Esempi:
  - `POST /auth/register`
  - `POST /auth/login`
  - `GET /candidates/me`
  - `POST /candidates/me/cv`
  - `GET /candidates/me/matches`

### 4.2 API interne (backend → AI)

- Base URL (dev): `http://jobmatcher_ai:8000`
- Esempi:
  - `POST /ocr`
  - `POST /cv/parse`
  - `POST /job/embed`

---

## 5. Deployment (dev)

In sviluppo, i servizi vengono eseguiti tramite `docker-compose`:

- PostgreSQL (`jobmatcher-db`)
- jobmatcher_api (porta 8080)
- jobmatcher_ai (porta 8000)

L’app Flutter viene lanciata localmente e comunica con il backend esposto su `localhost` (o IP/hostname configurato).

In produzione, i servizi potranno essere separati su container/host diversi, mantenendo la stessa architettura logica.
````

---

## 3️⃣ Mini scheletro cartelle per i tre servizi

Non è obbligatorio crearle subito a mano se userai tool (Flutter create / Spring Initializr), ma come riferimento concettuale:

```text
jobmatcher-app/
  lib/
  android/
  ios/
  web/
  pubspec.yaml
  README.md

jobmatcher_api/
  src/
    main/
      java/it/tgn/jobmatcher/...
      resources/
        application.yml
        db/migration/
  build.gradle.kts (o pom.xml)
  README.md

jobmatcher_ai/
  app/
    main.py
    api/
    services/
  pyproject.toml
  README.md
```