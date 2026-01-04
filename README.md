# JobMatcher

JobMatcher è un'applicazione mobile open source per il **matching intelligente tra CV e offerte di lavoro**, con:

- parsing automatico del CV (PDF/immagine) tramite OCR,
- analisi semantica CV ↔ job description con modelli NLP leggeri,
- ranking basato su **compatibilità** e **distanza geografica**,
- notifiche di nuove offerte compatibili.

L’obiettivo è costruire un **MVP reale ma didattico**, ideale sia come progetto personale che come esempio di architettura moderna nel portfolio.

---

## 🧱 Architettura ad alto livello

Il sistema è composto da tre servizi principali:

- **`jobmatcher-app`** – app mobile Flutter (Android/iOS)
- **`jobmatcher_api`** – backend principale in Spring Boot (API REST, business logic, accesso al DB)
- **`jobmatcher_ai`** – microservizio Python (FastAPI) per OCR, parsing CV e embedding NLP
- **PostgreSQL** – database relazionale per utenti, CV, offerte e risultati di matching

Schema logico:

```text
[ Utente (mobile) ]
        │
        ▼
 [ jobmatcher-app ]
        │  (HTTP/JSON)
        ▼
 [ jobmatcher_api ]  ─────►  [ PostgreSQL ]
        │
        │ (HTTP interno)
        ▼
 [ jobmatcher_ai ]
   (OCR + NLP)
````

---

## 📂 Struttura della repo

```text
jobmatcher/
├─ jobmatcher-app/      # Flutter frontend (mobile)
├─ jobmatcher_api/      # Spring Boot backend REST + DB
├─ jobmatcher_ai/       # FastAPI OCR/NLP microservice
├─ docs/                # Documentazione (architettura, API, ecc.)
├─ docker-compose.yml   # Orchestrazione servizi in locale (DB + backend)
├─ README.md
├─ LICENSE
└─ .gitignore
```

Ogni sottoprogetto contiene un proprio `README.md` con istruzioni di setup più specifiche.

---

## 🧰 Tecnologie utilizzate

**Mobile**

* [Flutter](https://flutter.dev/) (Dart)
* State management: (es. Riverpod o Bloc – da definire nel sottoprogetto)
* Librerie previste:

  * `dio` per le chiamate HTTP
  * `flutter_secure_storage` per token JWT
  * `geolocator` per geolocalizzazione
  * `flutter_map` + OpenStreetMap per mappe (no lock-in su servizi a pagamento)

**Backend API**

* Java 17/21
* Spring Boot 3.x

  * `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-data-jpa`
  * `spring-boot-starter-validation`
  * `springdoc-openapi` per Swagger / OpenAPI
* PostgreSQL (+ opzionale PostGIS)
* Flyway per migrazioni DB

**Backend AI**

* Python 3.11+
* FastAPI
* OCR: `pytesseract` + Tesseract OCR
* NLP / embedding: `sentence-transformers` (es. `bge-m3`)
* Eventuale uso di `spaCy` per parsing avanzato

**DevOps**

* Docker + docker-compose
* GitHub Actions (in futuro) per CI/CD

---

## ▶️ Avvio in locale (dev)

> ⚠️ Nota: i comandi sono indicativi. Alcuni step richiedono che i sottoprogetti siano già inizializzati.

1. **Clona la repo**

```bash
git clone https://github.com/bnb010001/jobmatcher.git
cd jobmatcher
```

2. **Avvia il database + backend via Docker**

Assicurati che `docker-compose.yml` sia presente nella root, poi:

```bash
docker-compose up -d
```

Questo comando avvierà:

* `jobmatcher-db` (PostgreSQL)
* `jobmatcher_api` (Spring Boot, porta 8080)
* `jobmatcher_ai` (FastAPI, porta 8000)

3. **Avvia l’app mobile**

```bash
cd jobmatcher-app
flutter pub get
flutter run
```

L’app si collegherà al backend `jobmatcher_api` esposto su `http://localhost:8080` (o l’URL configurato).

---

## 🎯 Funzionalità previste (MVP)

* Registrazione/login con JWT (candidati e aziende)
* Profilo candidato con:

  * dati personali
  * caricamento CV (PDF/immagine)
* Profilo azienda con:

  * gestione offerte (crea / modifica / chiudi)
* Parsing CV:

  * OCR → testo
  * estrazione competenze/esperienze/formazione
  * embedding semantici
* Matching intelligente:

  * similarity tra CV e offerte
  * ranking pesato per distanza geografica
* Geolocalizzazione:

  * salvataggio posizione candidato
  * geocoding sede offerta → lat/long
* Notifiche (fasi successive):

  * notifiche push per nuove offerte compatibili

---

## 🗺️ Roadmap (alto livello)

1. **Fase 0 – Setup progetto**

   * Struttura repo, Docker, documentazione base
2. **Fase 1 – Auth & profili**

   * Spring Security + JWT, profili base candidato/azienda, schermate Flutter
3. **Fase 2 – Gestione offerte**

   * CRUD offerte lato backend + UI base
4. **Fase 3 – Upload CV & OCR/parsing**

   * integrazione con `jobmatcher_ai`, salvataggio testo/skill
5. **Fase 4 – Matching semantico & geolocalizzazione**

   * calcolo score compatibilità + distanza
6. **Fase 5 – Notifiche & UX**

   * FCM + raffinamento UI/UX
7. **Fase 6 – Test & deploy**

   * Test automatici, pipeline CI/CD

Dettagli aggiuntivi sono disponibili in `docs/architecture.md` e `docs/roadmap.md`.

---

## 📄 Licenza

Questo progetto è rilasciato sotto licenza **MIT** (o altra licenza a tua scelta – da aggiornare in `LICENSE`).

---

## 🤝 Contributi

Per ora il progetto è sviluppato a scopo personale/formativo, ma contributi, issue e suggerimenti sono benvenuti.

````

---

