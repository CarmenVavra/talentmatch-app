# Handoff: TalentMatch – KI-gestütztes HR-Recruiting (Web-Frontend)

## Overview
TalentMatch unterstützt HR-Manager:innen bei der Sichtung von Bewerbungen: Lebensläufe (PDF) werden hochgeladen, per LLM strukturiert extrahiert, mit einer Stelle abgeglichen (Match-Score 0–100, gefundene/fehlende Skills, Risiken) und es werden Interviewfragen generiert. Fachliches Konzept, Datenmodell und KI-Workflows: `TalentMatch-Konzept.pdf`.

Dieses Handoff umfasst 4 Views: **Stellenübersicht (Dashboard)**, **Kanban-Pipeline**, **Bewerber-Detail (AI Analytics)**, **Upload-Dialog**. Zielgerät: responsive Web (Desktop + Tablet, ab ca. 768 px). Smartphone ist nicht erforderlich.

## About the Design Files
`TalentMatch.dc.html` ist eine **Design-Referenz in HTML** – ein klickbarer Prototyp, der Aussehen und Verhalten zeigt, **kein Produktionscode**. Aufgabe ist, das Design in der Zielumgebung neu umzusetzen:
**Angular 17+ (Standalone Components, Signals, RxJS, Reactive Forms, Route Guards)** mit `@angular/cdk/drag-drop` für das Kanban-Board. Styling wahlweise Tailwind oder SCSS mit CSS-Custom-Properties (Tokens unten). Angular Material nur verwenden, wenn es auf radius 0 / flache Optik umgestylt wird.

Öffnen: `TalentMatch.dc.html` im Browser (benötigt `support.js` und den `_ds/`-Ordner daneben). Template-Markup steht zwischen `<x-dc>` und `</x-dc>`; Logik und **alle Mockdaten** stehen in der Klasse `Component` im `<script data-dc-script>`-Block (`JOBS`, `COLS`, `CANDS0`, `SCEN`).

## Fidelity
**High-fidelity.** Farben, Typografie, Abstände und Interaktionen sind final. Pixelgenau nachbauen.

---

## Globales Layout
- Seitenhintergrund `#F3F4F5`; Flächen (Panels, Karten) `#FFFFFF`.
- Inhalt: `max-width: 1440px`, zentriert, horizontales Padding `clamp(16px, 3vw, 40px)`, oben `clamp(20px, 3vw, 40px)`, unten 64px.
- **Formensprache (Modernist):** Eckenradius überall **0**. Abschnitte werden durch **2px-Linien** `rgba(43,47,49,.32)` oben getrennt (`border-top: 2px solid`), innere Trennlinien 1px `rgba(43,47,49,.15)`. Alles linksbündig – auch Button-Labels (bei breiten Buttons: Label links, Icon rechts, `justify-content: space-between`).
- Icons: **Lucide** (lucide-angular), Stroke 2, `stroke-linecap/linejoin: round`, 14–16px in UI, 20–28px in Leerzuständen.

### Header (sticky, top 0, z-index 20)
- Weiß, `border-bottom: 2px solid rgba(43,47,49,.32)`, min-height 60px, Flex mit gap 32px, wrap.
- **Logo:** 22×22 Raster aus 4 Quadraten (2×2, gap 2px) in `#4D6D9A`, `#86B3D1`, `#99CED3`, `#EDB5BF` + Wortmarke „TalentMatch“ (Archivo 800, 18px, letter-spacing −0.01em). Klick → Dashboard.
- **Nav:** „Stellen“, „Pipeline“, „Bewerber:innen“ – 14px/600, gap 24px. Inaktiv `#5F6366`; aktiv `#2B2F31` + `border-bottom: 3px solid #4D6D9A` (Unterstrich sitzt bündig auf der Header-Linie).
- **Rechts:** Rollen-Chip „HR-MANAGER“ (11px, uppercase, ls .08em, padding 4×8, bg `#E6EEF6`, text `#34496A`), Name „Katrin Vogel“ 13px/600 + „People & Culture“ 11px `#5F6366`, Avatar 34×34 Quadrat `#4D6D9A` mit Initialen weiß 13px/800.
- Die Rolle kommt aus dem JWT (Rollen HR-Manager vs. Bewerber). Alle hier gezeigten Views sind nur für **HR-Manager** (Route Guard).

---

## Screens / Views

### 1. Stellenübersicht (Route `/jobs`)
**Zweck:** Alle offenen Vakanzen mit Anzahl und Status der Bewerbungen sehen; in die Pipeline einer Stelle springen.

**Aufbau (vertikal, gap 28px):**
1. **Titelzeile** (flex, space-between, align end, wrap): Kicker „DASHBOARD“ (11px, uppercase, ls .1em, 600, `#4D6D9A`), H1 „Offene Stellen“ (Archivo 800, `clamp(30px, 3.4vw, 42px)`, lh 1.12, ls −0.015em). Rechts Primary-Button „Neue Stelle anlegen“ + Plus-Icon, min-width 180px, padding 10×14.
2. **KPI-Leiste:** Grid `repeat(auto-fit, minmax(200px, 1fr))`, 2px-Linie oben und unten, Zellen durch 1px-Linien rechts getrennt, padding 18px 20px (erste Zelle links 0). Pro Zelle: Label 12px `#5F6366`, Wert Archivo 800 34px lh 1.05, Notiz 12px (Farbe semantisch).
   - Offene Stellen · 6 · „2 neu in diesem Monat“ (`#5F6366`)
   - Bewerbungen gesamt · Summe · „+18 seit letzter Woche“ (`#25603F`)
   - Unbearbeitet · Summe Status NEW · „warten auf KI-Analyse“ (`#75560E`)
   - Interviews diese Woche · 9 · „nächstes: Mo, 10:00“
3. **Toolbar** (flex, gap 12, wrap): Suchfeld (flex 1 1 280px, max 420px, Lupe-Icon absolut links 10px, Input padding-left 34px, weißer Hintergrund) · Segmented Control Abteilung „Alle / Engineering / Data / Product / People“ · rechts (margin-left auto) Segmented Control „Kacheln / Tabelle“ mit Icons layout-grid / list.
4. **Kachelansicht:** Grid `repeat(auto-fill, minmax(min(100%, 340px), 1fr))`, **gap 2px auf Hintergrund `rgba(43,47,49,.32)` + 2px Rahmen** → sichtbare Rasterlinien. Kachel: weiß, padding 22px, min-height 280px, flex column gap 14, ganze Kachel klickbar, Hover-bg `#F7F9FB`.
   - Oben: Abteilung (Kicker 11px uppercase `#4D6D9A`), Titel Archivo 800 20px lh 1.15, „Level · Ort“ 13px `#5F6366`. Rechts oben „Ø SCORE“ (10px uppercase) + Score-Badge (15px/800, padding 4×8, Farben nach Score-Ampel).
   - Skill-Tags: 11px, padding 3×10, bg `#E6EEF6`, text `#34496A`, gap 6.
   - Fuß (margin-top auto, 1px-Linie oben, padding-top 14): Zahl Bewerbungen Archivo 800 28px + „Bewerbungen“ 13px; rechts „seit TT.MM.JJJJ“ 12px. Darunter **gestapelter Statusbalken** 8px hoch, gap 2px, Segmente `flex: <Anzahl>` in den Spaltenfarben (siehe Pipeline). Legende 12px mit 8×8-Quadraten: „4 neu · 3 analysiert · 2 Interview …“ (nur Segmente > 0).
5. **Tabellenansicht:** weißer Container, 2px-Linie oben, horizontal scrollbar (Tabelle min-width 860px). Spalten: Stelle (Titel 700 + Ort 12px), Abteilung, Level, Bewerbungen (700), Neu, Interview, Ø Score (Badge), Erstellt (`#5F6366`), Pfeil-Icon `#4D6D9A`. Header 11px uppercase ls .08em, 60% Textfarbe, 2px-Linie unten; Zeilen 1px-Linie, Hover 4% Ink-Tint. Zeile klickbar.
6. **Leerzustand:** „Keine Stellen gefunden. Suche oder Filter anpassen.“ (2px-Linie oben, padding 40px 0, `#5F6366`).

**Filter:** Suche matcht Titel + Abteilung + Skills (case-insensitive). Abteilungsfilter exakt.

### 2. Kanban-Pipeline (Route `/jobs/:jobId/pipeline`)
**Zweck:** Bewerbungen einer Stelle durch die Phasen bewegen.

1. **Kopf:** Breadcrumb „Stellen / Pipeline“ (13px `#5F6366`, „Stellen“ als Link), H1 = Stellentitel (`clamp(28px, 3vw, 38px)`), Metazeile 13px `#5F6366` (Abteilung · Level · Ort · „N Bewerbungen“, gap 16). Rechts Primary „Lebensläufe hochladen“ + upload-Icon, min-width 220px → öffnet Upload-Dialog.
2. **Filterleiste** (2px oben, 1px unten, padding 14px 0): Suche „Name oder Skill“ (max 360px) · Label „Match-Score“ + Segmented „Alle / ≥ 50 / ≥ 75“ · rechts Legende Ampel (10×10-Quadrate: ≥ 75 grün, 50–74 gelb, < 50 rot).
   - Score-Filter blendet Karten mit Score < Schwelle aus; **nicht analysierte Karten (score null) bleiben immer sichtbar.**
3. **Board:** CSS-Grid `grid-auto-flow: column; grid-auto-columns: minmax(260px, 1fr); gap 12px; overflow-x: auto; align-items: start` → auf schmalen Viewports horizontal scrollen.

**Spalten** (Status-Enum aus dem Datenmodell):
| Status | Label | Farbe (4px-Balken oben) |
|---|---|---|
| NEW | Eingegangen | `#5F6366` |
| MATCHED | KI-analysiert | `#86B3D1` |
| INTERVIEW | Interview | `#99CED3` |
| HIRED | Zusage | `#3F8F5F` |
| REJECTED | Absage | `#EDB5BF` |

Spalte: bg `#E9ECEE`, min-height 520px. Kopf padding 12px 12px 10px: Label Archivo 800 15px, Zähler-Chip weiß 12px/700 padding 2×8. Karten-Liste padding 0 8px 10px, gap 8. Leer: gestrichelter 2px-Rahmen `rgba(43,47,49,.2)`, „Karte hierher ziehen“ 12px.
**Drop-Ziel aktiv (dragover):** bg `#E2ECF5` + `outline: 2px solid #4D6D9A; outline-offset: -2px`.
Sortierung je Spalte: Score absteigend, nicht analysierte zuerst.

**Bewerber-Karte:** weiß, padding 12, gap 10, `box-shadow: 0 1px 2px rgba(45,43,43,.14)`, Hover `0 3px 10px rgba(45,43,43,.16)`, cursor grab; beim Ziehen Opacity 0.4 am Original.
- Score-Badge 40×40 Quadrat, Zahl 15px/800, bg/fg aus Ampel, **3px linker Rand in Ampel-Balkenfarbe**. Nicht analysiert: „—“ grau.
- Name 14px/700, darunter aktuelle Position „Rolle · Firma“ 12px `#5F6366`.
- Bis zu 3 Skill-Chips: 11px, padding 2×7, bg `#EEF1F3`, text `#3D4144` (per Tweak „compactCards“ ausblendbar).
- Nur bei score null: Secondary-Button „KI-Analyse starten“ + sparkles-Icon (`#4D6D9A`), 12px; während der Analyse Label „Analyse läuft …“. Klick stoppt Propagation (öffnet nicht die Detailseite).
- Fuß: 1px-Linie, 11px `#5F6366`, links Eingangsdatum „TT.MM.“, rechts Stadt.
- Klick auf Karte → Detail.

**Drag & Drop:** `cdkDropListGroup` über alle Spalten; Drop setzt `Application.status` → `PATCH /api/applications/{id}` (optimistisches Update, bei Fehler zurückrollen).

### 3. Bewerber-Detail / AI Analytics (Route `/applications/:id`)
**Zweck:** Eignung beurteilen, Interview vorbereiten, Status ändern.

1. Breadcrumb „Stellen / {Stellentitel} / {Name}“.
2. **Header-Panel** (weiß, 2px oben), Grid `repeat(auto-fit, minmax(min(100%, 320px), 1fr))`:
   - **Links** (padding 24, gap 12): Status-Tag (11px uppercase 600, padding 4×8; Farben: NEW `#E9ECEE`/`#2F4A6E`, MATCHED `#E2EDF6`/`#2F4A6E`, INTERVIEW `#DDF0F2`/`#2F4A6E`, HIRED `#DCEFE3`/`#25603F`, REJECTED `#FBE6EA`/`#8A3346`) + „Eingegangen TT.MM.“ 12px. H1 Name `clamp(30px, 3.2vw, 40px)`. Position 15px `#5F6366`. Kontaktzeile 13px mit Icons mail / phone / map-pin in `#4D6D9A` (gap 8px 20px). Aktionen: Primary „Zum Interview einladen“ + calendar-Icon (min-width 190) → INTERVIEW; Secondary „Zusage“ → HIRED; Secondary „Absage“ (Text `#8E2C26`) → REJECTED.
   - **Rechts** (padding 24, `border-left: 2px`, bg `#FAFBFC`): „KI-MATCH-SCORE“ Kicker, Score-Zahl Archivo 800 **72px** lh 1 ls −0.03em in Ampel-Textfarbe + „/100“ 18px `#5F6366`; Urteil-Badge rechts daneben („Starke Passung“ ≥75, „Teilweise Passung“ 50–74, „Geringe Passung“ <50, „Ausstehend“). **Segmentbalken:** 20 Zellen à 5 Punkte, Höhe 12px, gap 2px, gefüllt = Ampel-Balkenfarbe, leer `#E3E6E8`. **Teil-Scores** (Grid 2 Spalten, gap 10px 20px): Skills, Erfahrung, Ausbildung, Sprachen – Label 12px + Wert 700, Balken 4px auf `#E3E6E8`, Füllfarbe nach Ampel des Teilwerts.
3. **Zwei Spalten:** Grid `repeat(auto-fit, minmax(min(100%, 460px), 1fr))`, gap 24 (unter ~970px Inhaltsbreite untereinander).
   - **Links „Lebenslauf“** (weiß, 2px oben): Kopf mit H4 + Segmented „Extrahierter Werdegang / PDF-Vorschau“.
     - *Werdegang:* Abschnitt „BERUFSERFAHRUNG“ (h6 13px uppercase ls .08em `#5F6366`). Einträge als Grid `130px 1fr`, 1px-Linie oben, padding 14px 0: Zeitraum 12px tabular-nums; Rolle 700; Firma · Ort 13px `#4D6D9A`; Stichpunkte 13px mit 6×2px-Strich `#86B3D1`. Darunter (2px-Linie) Ausbildung + Sprachen nebeneinander, dann „Technologien (extrahiert)“ als neutrale Tags.
     - *PDF-Vorschau:* grauer Rahmen `#E3E6E8` padding 20, weiße Seite A4-Format (`aspect-ratio: 1/1.414`, max-height 720, shadow-md). Im Produkt: echter PDF-Viewer (z. B. ngx-extended-pdf-viewer / pdf.js) mit der Originaldatei.
   - **Rechts „AI Insights“** (Panels untereinander, gap 24):
     - **KI-Zusammenfassung:** sparkles-Icon (Füllung `#99CED3`, Stroke `#4D6D9A`), H4, rechts Modell + Zeit 11px („gpt-4o · vor 2 Std.“). Text 15px lh 1.6 (`AnalysisResult.summary`, 3–4 Sätze).
     - **Pro / Contra:** Kopf H4 + „X von Y Anforderungen erfüllt“. Zwei Zellen (auto-fit min 200px): „GEFUNDENE SKILLS“ bg `#F3FAF6`, Label `#25603F`, check-Icon `#3F8F5F` · „FEHLENDE SKILLS“ bg `#FDF4F3`, Label `#8E2C26`, x-Icon `#C9473F`, 2px-Linie links. Darunter „RISIKOFAKTOREN“ bg `#FEF8EA`, Label `#75560E`, alert-triangle `#C48A12` (`riskFactors`).
     - **Interview-Helper:** Kopf H4 + „n / m besprochen“ + 80×4px Fortschrittsbalken `#4D6D9A`. Jede Frage eine klickbare Zeile (padding 14px 20px, 1px-Linie): 20×20 Checkbox-Quadrat (2px Rahmen `rgba(43,47,49,.45)`; checked: Füllung + Rahmen `#4D6D9A`, weißer Haken), Themen-Kicker 11px uppercase `#4D6D9A` (z. B. „Architektur“, „Fehlender Skill“, „Werdegang“), Frage 14px lh 1.45; abgehakt: Text `#5F6366` + line-through. Darunter Textarea „Notizen zum Gespräch“ und Ghost-Button „+ Weitere Fragen generieren“.
   - **Zustand „nicht analysiert“** (score null): Rechte Spalte zeigt statt der Insights ein Panel „KI-Analyse ausstehend“ mit Erklärtext und Primary „KI-Analyse starten“; Teil-Scores werden durch einen Hinweistext ersetzt, Score „—“.

### 4. Upload-Dialog (Modal über der Pipeline)
- Backdrop `rgba(45,43,43,.5)`, Klick schließt. Dialog weiß, `width: min(640px, 100%)`, max-height 90vh scroll, **4px-Linie oben `#4D6D9A`**, shadow-lg.
- Kopf: Kicker = Stellentitel, H3 „Lebensläufe hochladen“, rechts Icon-Button X (36×36, Secondary).
- **Dropzone:** 2px gestrichelt `#86B3D1`, bg `#F5F9FC`, padding 36×24, linksbündig: upload-Icon 28px, „PDF-Dateien hierher ziehen“ 17px/800, „oder klicken zum Auswählen · max. 10 MB pro Datei · mehrere Dateien möglich“ 13px. Dragover: Rahmen `#4D6D9A`, bg `#E2EDF6`. Klick öffnet versteckten `<input type="file" multiple accept=".pdf">`.
- **Dateiliste** (2px oben): pro Datei Grid `auto 1fr auto` – file-text-Icon, Name (ellipsis) + Größe „x.x MB“, 4px-Fortschrittsbalken (Transition width .5s), Stufen-Label, rechts Score-Badge („…“ bis fertig).
  - Stufen: 15 % „Hochgeladen“ → 45 % „Text extrahiert“ → 75 % „KI-Analyse läuft …“ → 100 % (Balken grün `#3F8F5F`) „Analysiert · in „KI-analysiert“ einsortiert“.
  - Im Produkt: Upload `POST /api/jobs/{jobId}/applications` (multipart), Fortschritt per SSE/WebSocket oder Polling; neue Bewerbung erscheint mit Status MATCHED in der Pipeline.
- Fuß: „Text-Extraktion: Apache Tika · Analyse: Spring AI“ 12px; „Demo-Dateien“ (nur Prototyp, entfällt) und Primary „Zur Pipeline“ (schließt).
- Fehlerzustände (im Mock nicht gezeigt, umsetzen): falscher Dateityp / > 10 MB → Zeile mit rotem Balken `#C9473F` und Meldung; Analyse fehlgeschlagen → „Analyse fehlgeschlagen – erneut versuchen“.

---

## Interactions & Behavior
- Navigation: Logo/„Stellen“ → `/jobs`; Kachel/Tabellenzeile → Pipeline; Karte → Detail; Breadcrumbs zurück. Beim Screenwechsel nach oben scrollen.
- Statuswechsel (Drag & Drop oder Buttons im Detail) aktualisiert Pipeline und Dashboard-Zähler sofort.
- „KI-Analyse starten“: Label „Analyse läuft …“, nach Abschluss Status MATCHED + Score; Button deaktivieren solange laufend.
- Interview-Checkboxen werden pro Bewerbung gespeichert (z. B. `InterviewNote`/`checkedQuestionIds`).
- Hover: Kacheln `#F7F9FB`, Tabellenzeilen 4 % Tint, Karten Shadow-Wechsel, Fragezeilen `#F5F8FB`. Buttons siehe Tokens.
- Fokus: `outline: 2px solid #4D6D9A; outline-offset: 2px` für alle interaktiven Elemente (`:focus-visible`).
- Ladezustände (umsetzen): Skeleton-Blöcke in Flächenfarbe `#E9ECEE` für Kacheln, Karten und Insights-Panels.

## State Management (Signals)
- `jobs = signal<JobPosition[]>`, `selectedJobId`, `applications = signal<ApplicationWithCandidate[]>` (pro Job), `analysis = signal<AnalysisResult | null>`.
- UI-State: `viewMode: 'tiles' | 'table'`, `dashQuery`, `deptFilter`, `pipelineQuery`, `minScore: 0 | 50 | 75`, `cvTab: 'cv' | 'pdf'`, `uploadOpen`, `uploads: {file, stage, progress, score?, error?}[]`, `checkedQuestions: Record<applicationId, Set<number>>`.
- Abgeleitet (`computed`): Spalten der Pipeline gefiltert/sortiert, Zähler pro Status & Ø Score pro Job, Ampel-Ton aus Score.
- API (Vorschlag): `GET /api/jobs` (inkl. Statuszählern und Ø Score), `GET /api/jobs/{id}/applications`, `GET /api/applications/{id}` (Candidate + parsedProfile + AnalysisResult), `PATCH /api/applications/{id}` `{status}`, `POST /api/applications/{id}/analyze`, `POST /api/jobs/{id}/applications` (multipart PDF).

## Design Tokens
**Farben – Kundenpalette (Hauptfarben)**
- `--tm-gray` `#5F6366` – Sekundärtext, Spalte „Eingegangen“
- `--tm-blue` `#4D6D9A` – Primärfarbe (Buttons, aktive Nav, Links, Kicker, Checkboxen, Fokus)
  - hover `#3F5A80`, pressed `#34496A`; Tint `#E6EEF6` (Tags), `#E2EDF6` (Dropzone aktiv/MATCHED-Tag), `#F5F9FC` (Dropzone)
- `--tm-lightblue` `#86B3D1` – Spalte „KI-analysiert“, Dropzone-Rahmen, Akzentstriche
- `--tm-teal` `#99CED3` – Spalte „Interview“, KI-Icon-Füllung; Tint `#DDF0F2`
- `--tm-pink` `#EDB5BF` – Spalte „Absage“; Tint `#FBE6EA`, Text `#8A3346`

**Neutrale**
- Ink (Text) `#2B2F31`, Text sekundär `#3D4144`, Hintergrund `#F3F4F5`, Fläche `#FFFFFF`, Fläche 2 `#E9ECEE`, Chip `#EEF1F3`, Track `#E3E6E8`, Fläche hell `#FAFBFC`
- Linie stark (2px) `rgba(43,47,49,.32)`, Linie fein (1px) `rgba(43,47,49,.15)`

**Ampel (Match-Score)** – Fläche / Text / Balken
- Grün ≥ 75: `#DCEFE3` / `#25603F` / `#3F8F5F` (Pro-Fläche `#F3FAF6`)
- Gelb 50–74: `#FBEFD0` / `#75560E` / `#E0A82E` (Risiko-Fläche `#FEF8EA`, Icon `#C48A12`)
- Rot < 50: `#F8DCD9` / `#8E2C26` / `#C9473F` (Contra-Fläche `#FDF4F3`)
- Ausstehend: `#E9ECEE` / `#5F6366` / `#9AA0A4`

**Typografie:** Archivo (Google Fonts, 400/600/800) für alles. Basis 15px / lh 1.55. Headings 800, lh 1.12, ls −0.015em: H1 42 (fluid s. o.), H2 32, H3 25, H4 20, H6 13 uppercase ls .08em. Kicker 10–11px uppercase ls .08–.1em 600. Body klein 13–14px, Meta 11–12px.

**Spacing:** 4 / 8 / 12 / 16 / 24 / 32px (Panels padding 20–24, Kacheln 22, Karten 12).
**Radius:** 0 überall (einzige Ausnahme: Radio-Punkte).
**Schatten:** sm `0 1px 2px rgba(45,43,43,.14)`, md `0 3px 10px rgba(45,43,43,.16)`, lg `0 12px 32px rgba(45,43,43,.22)`.
**Buttons:** 14px Archivo 800, padding 8×14 (große 10×14), Label linksbündig. Primary bg `#4D6D9A` / Text `#F3F4F5`; Secondary 1px Rahmen Linie stark, Hover 7 % Ink-Tint, Pressed 14 %; Ghost Text `#4D6D9A`, Hover 10 % Blau-Tint; disabled Opacity .45.
**Inputs:** min-height 36, padding 6×10, 14px, 1px Rahmen Linie stark, Fokus Rahmen `#4D6D9A`.
**Segmented Control:** 1px Rahmen, Optionen 13px padding 7×12, getrennt durch 1px; aktiv bg `#4D6D9A` Text weiß.

Die vollständige Basis-Stylesheet des Designsystems liegt in `_ds/…/styles.css` (im Prototyp werden dessen Akzent-Variablen per Inline-Override auf `#4D6D9A` umgestellt).

## Assets
- Keine Bilder. Icons: Lucide (search, upload, plus, layout-grid, list, arrow-left/right, mail, phone, map-pin, sparkles, check, x, alert-triangle, file-text, calendar).
- Logo ist ein reines CSS-Raster (4 Farbquadrate) – Platzhalter, bis ein echtes Logo existiert.
- Alle Personen, Firmen und Texte sind **Mockdaten** (im Script-Block der HTML-Datei); als Seed-Daten für die Demo-Szenarien Top-/Mid-/Mismatch-Kandidat nutzbar.

## Noch nicht gestaltet
Login (JWT), „Neue Stelle anlegen“-Formular, Bewerber-Rolle/Portal, Fehler-/Ladezustände (Regeln oben beschrieben).

## Files
- `TalentMatch.dc.html` – klickbarer Prototyp aller Views (Template + Logik + Mockdaten)
- `support.js` – Laufzeit nur für den Prototyp (nicht übernehmen)
- `_ds/modernist-…/styles.css`, `_ds_bundle.js` – Basis-Designsystem (Tokens, Komponentenklassen)
- `TalentMatch-Konzept.pdf` – fachliches Konzept, Datenmodell, KI-Workflows
