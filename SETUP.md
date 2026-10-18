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

**Lebensläufe hochladen (HR):** Mehrere PDFs auf einmal ablegen – die App erkennt Lebenslauf, Skill-Profil und
Anschreiben und fasst Unterlagen derselben Person zusammen (gleiche E-Mail, Telefonnummer oder gleicher Name;
reine Namens-Treffer sind zur Prüfung markiert). In der Vorschau lassen sich Dokumentart und Zuordnung ändern.
Doppelte Bewerbungen derselben Person **auf derselben Stelle** zeigt die Detailansicht als „Mögliches Duplikat“ an
und führt sie auf Wunsch zusammen. Stellenübergreifend oder zwischen verschiedenen Bewerber-Konten ist das nicht
möglich.

**Stellen verwalten (HR):** In der Pipeline einer Stelle über „Bearbeiten“ bzw. „Löschen“. Das Löschen
entfernt nach Bestätigung auch alle Bewerbungen der Stelle inklusive Unterlagen und KI-Analysen.
Beschreibungen dürfen bis zu 20.000 Zeichen lang sein; `**Text**` wird im Bewerber-Portal fett dargestellt
(im Formular per „F“-Button oder Strg+B, mit Vorschau). Bestehende Datenbanken erweitert die App beim Start
automatisch (`SchemaUpgrade`).

## Bewerber-Portal

Bewerber:innen registrieren sich selbst („Als Bewerber:in registrieren“ auf der Login-Seite) und
erhalten immer die Rolle Bewerber:in. Im Portal können sie:

- **Offene Stellen** ansehen (mit eigener Detailseite pro Stelle: vollständige Beschreibung und gesuchte
  Kenntnisse) und sich mit einem PDF-Lebenslauf bewerben (eine Bewerbung pro Stelle) –
  optional zusätzlich mit **Skill-Profil** und **Anschreiben** (ebenfalls PDF, je max. 10 MB),
- unter **Meine Bewerbungen** den Status verfolgen: Eingegangen → Einladung zum Interview → Zusage/Absage.

Die Zusatzunterlagen fließen in die KI-Analyse ein:

- **Skill-Profil:** Die darin genannten Skills zählen im Skill-Abgleich mit. Skills, die nur das Skill-Profil
  belegt, sind für HR als „laut Skill-Profil“ markiert; die KI schlägt dazu Prüffragen fürs Interview vor.
- **Anschreiben:** fließt nur qualitativ ein (Summary, Risikofaktoren, Interviewfragen), nicht in den Score.

HR sieht alle Unterlagen in der Bewerber-Detailansicht unter „PDF-Vorschau“.

Neue Portal-Bewerbungen tragen in Pipeline und Bewerberliste ein **„Neu“**-Etikett und stehen oben in
ihrer Pipeline-Spalte, bis HR sie öffnet oder verschiebt. Der Menüpunkt „Pipeline“ öffnet immer die zuletzt
angesehene Stelle (auch nach erneutem Anmelden).

Bewerber:innen sehen bewusst **keinen Match-Score** und keine KI-Bewertung; der interne Schritt
„KI-analysiert“ erscheint für sie als „Eingegangen“. Nach dem Absenden startet die KI-Analyse
automatisch im Hintergrund – schlägt sie fehl (z. B. ohne API-Key), kann HR sie in der Pipeline
manuell starten. Das Demo-Bewerberkonto hat bereits zwei Bewerbungen.

## KI-Analyse (OpenAI)

Die Analyse neuer Lebensläufe nutzt Spring AI mit OpenAI `gpt-4o`. Vor dem Start setzen:

```bat
set OPENAI_API_KEY=sk-...
```

Ein dauerhaft in Windows gesetzter `OPENAI_API_KEY` (Benutzer- oder Systemvariable) wird von `start.bat`
auch dann übernommen, wenn das aufrufende Fenster vor dem Setzen geöffnet wurde. Nach dem Ändern des Keys
die App mit `stop.bat` / `start.bat` neu starten.

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
