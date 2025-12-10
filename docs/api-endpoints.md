# API principali – jobmatcher_api (Spring Boot)

Questo documento elenca gli endpoint REST principali del backend `jobmatcher_api`.

Base URL (dev): `http://localhost:8080/api`

---

## 1. Autenticazione

### POST `/api/auth/register`

Registra un nuovo utente candidato o azienda.

**Body (esempio candidato)**

```json
{
  "role": "CANDIDATE",
  "email": "user@example.com",
  "password": "password123",
  "firstName": "Mario",
  "lastName": "Rossi"
}

**Body (esempio azienda)**

{
  "role": "COMPANY",
  "email": "hr@azienda.it",
  "password": "password123",
  "companyName": "Azienda S.p.A."
}

