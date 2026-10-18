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

**Passwort vergessen:** Link auf der Login-Seite. Die App schickt einen Einmal-Link per E-Mail
(60 Minuten gültig, `talentmatch.security.password-reset-validity`); gespeichert wird nur ein Hash des Links.
Ob es zur Adresse ein Konto gibt, verrät die Seite nicht. Ohne Mailserver liegt die Mail im Postausgang –
dort kann HR den Link sehen, für echten Betrieb deshalb SMTP einrichten (siehe unten).

**Schutz gegen Passwort-Raten:** Nach 5 Fehlversuchen in Folge ist die Anmeldung für die E-Mail-Adresse
15 Minuten gesperrt (`talentmatch.security.max-login-failures` / `login-lock-duration`). Ein über den Link
zurückgesetztes Passwort hebt die Sperre sofort auf. Die Zähler liegen im Speicher und sind nach einem
Neustart zurückgesetzt.

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

## Evidenz (Talentpool)

In der Bewerber-Detailansicht sendet **„In Evidenz halten“** eine Absage mit der Bitte um Zustimmung,
die Unterlagen aufzubewahren (Text vor dem Versand anpassbar). Die Bewerber:in antwortet über einen
persönlichen Link ohne Anmeldung („Ja, gerne“ / „Nein, danke“; Portal-Nutzer:innen auch unter „Meine
Bewerbungen“).

- Ohne Antwort: Erinnerung nach 7 Tagen, Löschung nach 14 Tagen.
- „Nein, danke“ oder Widerruf: sofortige Löschung.
- Mit Zustimmung: Aufbewahrung höchstens 24 Monate, danach automatische Löschung.
- Die Fristen prüft die App stündlich und beim Start (`talentmatch.talent-pool` in `application.yml`).

Der Menüpunkt **„Evidenz“** listet alle Einträge über alle Stellen. **„Bewerbung löschen“** entfernt eine
Bewerbung endgültig, inklusive Unterlagen, KI-Analyse und E-Mails.

## Datenschutz

- **Datenschutzhinweise** unter `/datenschutz` (öffentlich, verlinkt bei Login und Registrierung sowie im
  Konto-Menü der Bewerber:innen). Bei der Registrierung müssen sie bestätigt werden; der Zeitpunkt wird am
  Konto gespeichert. Der Text ist ein Muster und vor produktivem Einsatz um verantwortliche Stelle,
  Datenschutzbeauftragte und Rechtsgrundlagen zu ergänzen.
- **Bewerbung zurückziehen:** unter „Meine Bewerbungen“ – die Bewerbung wird mit Unterlagen, KI-Analyse und
  E-Mails sofort gelöscht; danach ist eine neue Bewerbung auf die Stelle möglich.
- **Konto löschen:** Konto-Menü → „Konto löschen“ (mit Passwort-Bestätigung) – löscht das Konto und alle
  Bewerbungen des Kontos. Von HR hochgeladene Bewerbungen ohne Kontoverknüpfung bleiben davon unberührt.
- **Automatische Löschung:** Absagen ohne Evidenz werden nach 6 Monaten gelöscht
  (`talentmatch.privacy.rejected-retention`, geprüft stündlich und beim Start). Die Frist beginnt mit der
  Absage; wird eine Bewerbung aus „Absage“ zurückgeholt, entfällt sie. Für bereits bestehende Absagen beginnt
  sie beim ersten Start dieser Version.

## E-Mails und Postausgang

Bewerber:innen **mit Portal-Konto** erhalten automatisch E-Mails:

- eine **Eingangsbestätigung**, sobald sie sich beworben haben,
- bei **jeder für sie sichtbaren Statusänderung**: Einladung zum Interview, Zusage, Absage oder „wieder in
  Prüfung“. Der interne Schritt „KI-analysiert“ löst keine Mail aus; „In Evidenz halten“ verschickt nur die
  Evidenz-Mail, keine zusätzliche Absage.

Von HR hochgeladene Bewerbungen ohne Portal-Konto erhalten keine automatischen Mails.

Alle E-Mails der App stehen im **Postausgang** (Avatar-Menü → „Postausgang“). Ohne Mailserver werden sie
**nur dort abgelegt**, nicht versendet – die Links darin funktionieren trotzdem. Für echten Versand vor dem
Start einen SMTP-Server setzen:

```bat
set SPRING_MAIL_HOST=smtp.example.com
set SPRING_MAIL_PORT=587
set SPRING_MAIL_USERNAME=...
set SPRING_MAIL_PASSWORD=...
set SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
set SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
set TALENTMATCH_MAIL_FROM=recruiting@example.com
set TALENTMATCH_APP_BASE_URL=https://talentmatch.example.com
```

`TALENTMATCH_APP_BASE_URL` ist die Adresse, unter der Bewerber:innen die Antwortseite erreichen.

## KI-Analyse (OpenAI)

Die Analyse neuer Lebensläufe nutzt Spring AI mit OpenAI `gpt-4o`. Vor dem Start setzen:

```bat
set OPENAI_API_KEY=sk-...
```

Ein dauerhaft in Windows gesetzter `OPENAI_API_KEY` (Benutzer- oder Systemvariable) wird von `start.bat`
auch dann übernommen, wenn das aufrufende Fenster vor dem Setzen geöffnet wurde. Nach dem Ändern des Keys
die App mit `stop.bat` / `start.bat` neu starten.

**Bild-PDFs:** Enthält ein PDF keinen Text (eingescannt oder über „Drucken → Microsoft Print to PDF“
erstellt), rendert die App bis zu 5 Seiten und lässt GPT-4o den Text auslesen (dauert etwa 10 Sekunden).
Ohne Key erscheint stattdessen eine Meldung, wie sich das PDF mit Text speichern lässt.

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
