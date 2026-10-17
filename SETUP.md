# TalentMatch – KI-gestütztes HR-Recruiting

Angular-21-Frontend + Spring-Boot-4-Backend (Spring AI, Spring Security/JWT, JPA) nach `TalentMatch.pdf`
und den UI-Mockups in `UI_Mockups/`.

## Schnellstart (Windows)

```bat
start.bat   :: startet Backend (Port 8080) und Frontend (Port 4200) und öffnet den Browser
stop.bat    :: beendet beide
```

Voraussetzungen: JDK 21+ (getestet mit 26), Node.js 20+ (getestet mit 22). Maven wird über den
Wrapper (`backend/mvnw.cmd`) geladen, Frontend-Abhängigkeiten installiert `start.bat` beim ersten Start.

**Anmeldung:** Beim ersten Start werden Demo-Benutzer angelegt. Die Test-Zugangsdaten stehen in
`backend/src/main/resources/application.yml` unter `talentmatch.seed` (HR-Manager:in und Bewerber:in).
Passwort ändern: Avatar oben rechts → „Passwort ändern“.

## Bewerber-Portal

Bewerber:innen registrieren sich selbst („Als Bewerber:in registrieren“ auf der Login-Seite) und
erhalten immer die Rolle Bewerber:in. Im Portal können sie:

- **Offene Stellen** ansehen und sich mit einem PDF-Lebenslauf bewerben (eine Bewerbung pro Stelle),
- unter **Meine Bewerbungen** den Status verfolgen: Eingegangen → Einladung zum Interview → Zusage/Absage.

Bewerber:innen sehen bewusst **keinen Match-Score** und keine KI-Bewertung; der interne Schritt
„KI-analysiert“ erscheint für sie als „Eingegangen“. Nach dem Absenden startet die KI-Analyse
automatisch im Hintergrund – schlägt sie fehl (z. B. ohne API-Key), kann HR sie in der Pipeline
manuell starten. Das Demo-Bewerberkonto hat bereits zwei Bewerbungen.

## KI-Analyse (OpenAI)

Die Analyse neuer Lebensläufe nutzt Spring AI mit OpenAI `gpt-4o`. Vor dem Start setzen:

```bat
set OPENAI_API_KEY=sk-...
```

Ohne Key startet die App trotzdem: alle Demo-Bewerbungen haben fertige Analysen; Upload und
Text-Extraktion funktionieren, die KI-Analyse meldet dann „Analyse fehlgeschlagen – erneut versuchen“.
Modell änderbar über `OPENAI_MODEL`.

## Architektur

| Bereich | Umsetzung |
|---|---|
| Backend | `backend/` – Controller → Service → Repository, DTOs (Records), `@RestControllerAdvice` mit ProblemDetail |
| KI-Workflows | `ai/RecruitingAiService`: Resume Parsing und Match-Analyse via `BeanOutputConverter` (Structured Output) |
| Matching | `analysis/SkillMatcher` (regelbasiert) + LLM-Bewertung; Score = 50 % Skills, 30 % Erfahrung, 10 % Ausbildung, 10 % Sprachen |
| PDF | Apache Tika über `spring-ai-tika-document-reader`; Original-PDF wird für die Vorschau gespeichert |
| Sicherheit | JWT (HS256), Rollen `HR_MANAGER` / `APPLICANT`; Recruiting-Endpunkte nur für HR, `/api/applicant/**` nur für Bewerber:innen |
| Datenbank | Lokal H2 (Datei `backend/data/`, PostgreSQL-Modus); Profil `postgres` für PostgreSQL |
| Frontend | `frontend/` – Standalone Components, Signals-Stores, Reactive Forms, Route Guards, CDK Drag & Drop |

## Entwicklung

```bat
cd backend  && .\mvnw.cmd clean test && .\mvnw.cmd checkstyle:check
cd frontend && npm run build && npm run test -- --watch=false && npm run lint && npm run format
```

## Docker (PostgreSQL)

```bat
docker compose up --build
```

Danach unter http://localhost:8081 erreichbar (Frontend über nginx, `/api` → Backend, Datenbank PostgreSQL).
