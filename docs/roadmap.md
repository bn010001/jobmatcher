# Roadmap di sviluppo – JobMatcher

Questa roadmap definisce le fasi di sviluppo del progetto JobMatcher e i principali task per ogni fase.

---

## Fase 0 – Setup progetto e infrastruttura

**Obiettivo:** avere la repo strutturata, Docker pronto e un ambiente di sviluppo coerente.

- [ ] Creare la repo `jobmatcher` su GitHub
- [ ] Creare le cartelle:
  - [ ] `jobmatcher-app/`
  - [ ] `jobmatcher_api/`
  - [ ] `jobmatcher_ai/`
  - [ ] `docs/`
- [ ] Aggiungere file di base:
  - [ ] `README.md` (root)
  - [ ] `docs/architecture.md`
  - [ ] `docs/roadmap.md`
  - [ ] `.gitignore`
  - [ ] `LICENSE` (es. MIT o Apache 2.0)
  - [ ] `docker-compose.yml` con:
    - [ ] servizio `postgres`
    - [ ] placeholder servizi `jobmatcher_api` e `jobmatcher_ai`
- [ ] Verificare che `docker-compose up -d postgres` avvii correttamente il DB

---

## Fase 1 – Auth & profili base

**Obiettivo:** registrazione/login funzionanti, profili candidato/azienda gestibili.

### Backend (`jobmatcher_api`)

- [ ] Inizializzare progetto Spring Boot 3 (Gradle o Maven)
- [ ] Configurare connessione a PostgreSQL (profilo `dev`)
- [ ] Definire entità:
  - [ ] `User` (email, password_hash, role, timestamps)
  - [ ] `Candidate` (FK user, dati base, posizione)
  - [ ] `Company` (FK user, dati azienda)
- [ ] Configurare Spring Security:
  - [ ] Autenticazione con JWT
  - [ ] Endpoint `/auth/login` e `/auth/register`
  - [ ] Ruoli: `CANDIDATE`, `COMPANY`, `ADMIN`
- [ ] Endpoint profilo:
  - [ ] `GET /candidates/me`
  - [ ] `PUT /candidates/me`
  - [ ] `GET /companies/me`
  - [ ] `PUT /companies/me`

### Mobile (`jobmatcher-app`)

- [ ] Inizializzare progetto Flutter
- [ ] Configurare base:
  - [ ] librerie: HTTP client (`dio`), state management (es. `riverpod`/`bloc`)
- [ ] Schermate:
  - [ ] Login
  - [ ] Registrazione (candidato/azienda)
  - [ ] Home base dopo login
  - [ ] Pagina “Profilo” per candidato
  - [ ] Pagina “Profilo” per azienda

---

## Fase 2 – Gestione offerte di lavoro

**Obiettivo:** aziende possono creare offerte, candidati possono visualizzarle.

### Backend (`jobmatcher_api`)

- [ ] Entità `Job`:
  - [ ] FK verso `Company`
  - [ ] campi: titolo, descrizione, requisiti, tipo contratto, sede, lat, long, stato, timestamps
- [ ] Endpoint:
  - [ ] `POST /jobs` (creazione offerta – solo azienda)
  - [ ] `GET /jobs/mine` (offerte di una azienda)
  - [ ] `GET /jobs/{id}`
  - [ ] `PATCH /jobs/{id}` (modifica parziale)
  - [ ] `GET /jobs` (lista paginata per candidato – senza matching per ora)
- [ ] Geocoding sede offerta (temporaneamente opzionale o mock)

### Mobile (`jobmatcher-app`)

- [ ] Lato azienda:
  - [ ] Schermata “Le mie offerte”
  - [ ] Form “Crea offerta”
  - [ ] Dettaglio offerta
- [ ] Lato candidato:
  - [ ] Lista offerte generica
  - [ ] Dettaglio offerta

---

## Fase 3 – Upload CV, OCR & parsing

**Obiettivo:** candidato può caricare il CV, che viene processato dal servizio AI.

### Backend AI (`jobmatcher_ai`)

- [ ] Inizializzare progetto Python (FastAPI)
- [ ] Configurare dipendenze (FastAPI, Uvicorn, pytesseract, sentence-transformers)
- [ ] Configurare Tesseract a livello di sistema
- [ ] Endpoint:
  - [ ] `POST /ocr` → riceve file, restituisce testo estratto
  - [ ] `POST /cv/parse` → riceve file o testo, restituisce:
    - [ ] testo pulito
    - [ ] dati strutturati (skills, esperienze, formazione)
    - [ ] embedding vettoriali

### Backend API (`jobmatcher_api`)

- [ ] Entità `Curriculum`:
  - [ ] FK verso `Candidate`
  - [ ] path file, testo estratto, JSON parsing, embedding, stato
- [ ] Endpoint:
  - [ ] `POST /candidates/me/cv` (upload CV)
  - [ ] `GET /candidates/me/cv` (visualizza info CV)
- [ ] Integrazione con `jobmatcher_ai`:
  - [ ] chiamata a `/cv/parse` dopo upload
  - [ ] salvataggio di testo, JSON e embedding in DB

### Mobile (`jobmatcher-app`)

- [ ] Schermata “Il tuo CV”:
  - [ ] upload file (PDF/immagine)
  - [ ] visualizzazione stato parsing
  - [ ] anteprima competenze estratte (anche solo testo)

---

## Fase 4 – Matching semantico & geolocalizzazione

**Obiettivo:** suggerire le offerte migliori per ogni candidato.

### Backend AI (`jobmatcher_ai`)

- [ ] Endpoint:
  - [ ] `POST /job/embed` → embedding per job description
- [ ] Utility:
  - [ ] funzione di similarity (cosine) – opzionalmente qui o lato Java

### Backend API (`jobmatcher_api`)

- [ ] Salvataggio embedding per `Job`
- [ ] Implementare logica matching:
  - [ ] recupero embedding CV candidato
  - [ ] filtro offerte per distanza (Haversine o PostGIS)
  - [ ] calcolo score: combinazione similarity + distanza
- [ ] Endpoint:
  - [ ] `GET /candidates/me/matches?radius=XX&limit=YY`
- [ ] Eventuali query/indici specifici per ottimizzare performance

### Mobile (`jobmatcher-app`)

- [ ] Schermata “Offerte suggerite per te”:
  - [ ] chiamata a `/candidates/me/matches`
  - [ ] card offerta con:
    - [ ] titolo, azienda, distanza
    - [ ] percentuale compatibilità

---

## Fase 5 – Notifiche & UX refinement

**Obiettivo:** renderlo usabile e più vicino a un prodotto reale.

### Backend (`jobmatcher_api`)

- [ ] Entità `DeviceToken` (user_id, token, piattaforma)
- [ ] Endpoint:
  - [ ] `POST /devices/token` (registrazione token push)
- [ ] Integrazione FCM per notifiche push
- [ ] Logica:
  - [ ] alla creazione di una nuova offerta:
    - [ ] calcolare candidati con score > soglia
    - [ ] inviare notifiche

### Mobile (`jobmatcher-app`)

- [ ] Integrazione `firebase_messaging`:
  - [ ] richiesta permessi
  - [ ] gestione token FCM (invio a backend)
  - [ ] routing dall’evento di notifica al dettaglio offerta
- [ ] Miglioramenti UI/UX:
  - [ ] filtri (raggio, tipo contratto)
  - [ ] skeleton loading, handling errori, ecc.

---

## Fase 6 – Testing, CI/CD e polishing

**Obiettivo:** progetto pulito, testato e presentabile in portfolio.

### Backend

- [ ] Test unitari per servizi principali (auth, CV, jobs, matching)
- [ ] Test di integrazione con DB (Testcontainers)
- [ ] Configurazione profili `dev` e `prod` (o `docker`)

### Backend AI

- [ ] Test su:
  - [ ] qualità OCR minima
  - [ ] funzionamento embedding
  - [ ] parsing base CV

### Mobile

- [ ] Widget test per schermate chiave
- [ ] Test di integrazione base (navigazione principale)

### DevOps

- [ ] Configurare GitHub Actions:
  - [ ] build & test per `jobmatcher_api`
  - [ ] build & test per `jobmatcher_ai`
  - [ ] (opzionale) build per `jobmatcher-app`
- [ ] Documentare in README:
  - [ ] come avviare l’ambiente completo con Docker
  - [ ] come avviare i singoli servizi in dev (approccio ibrido)
