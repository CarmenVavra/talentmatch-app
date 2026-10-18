# 🛠️ Praxisbeispiel: AI-Assisted Development am Projekt „TalentMatch“

Dieses Dokument beschreibt den konkreten Entwicklungs- und Validierungsprozess der **TalentMatch-App** unter intensivem Einsatz moderner KI-Agenten und Assistenzwerkzeuge.

---

## 🚀 1. Das Projekt & der Härtetest (Dogfooding)

**TalentMatch** ist eine Fullstack-Anwendung zur Optimierung des HR-Recruiting-Prozesses. HR-Verantwortliche legen Stellenprofile an, Bewerber:innen laden ihre Unterlagen hoch, und eine KI-Pipeline extrahiert die Texte, gleicht sie ab und liefert eine strukturierte Pro/Contra-Matrix sowie einen **Match-Score (maximal 100 Punkte)**.

### Der p.how-Validierungslauf

Um das System unter realen Bedingungen zu testen, wurde die aktuelle Stellenausschreibung der **p.how GmbH** für die Rolle *Fullstack-Entwickler:in Java / Angular (AI-assisted Development)* eingepflegt.

* **Input:** Original-Ausschreibung (karriere.at-ID: 10030468) + echter Lebenslauf & Skillprofil von Carmen Gregor.
* **Ergebnis der KI-Analyse:**
  * 🎯 **Match-Score: 95 / 100**
  * 📋 *Fazit der Pipeline:* Exzellente technologische Abdeckung (Angular-Lead-Erfahrung trifft auf Spring Boot und nachgewiesene KI-gestützte Arbeitsweisen).

Dieses Ergebnis diente als finaler Validierungsschritt (*Dogfooding*) für die Zuverlässigkeit der Prompts und der Parsing-Genauigkeit.

---

## 🧠 2. Der KI-Arbeitsablauf (Engineering-Prozess)

Der gesamte Entwicklungsprozess basierte auf einem synergetischen Zusammenspiel aus menschlicher Softwarearchitektur und agentenbasierter Umsetzung:

### 📋 Kontextpflege & Vorbereitung

Bevor Code generiert wurde, wurden im Repository strikte Systemanweisungen und eine `CLAUDE.md` gepflegt. Dies stellte sicher, dass **Claude-Code** und **GitHub Copilot** die technischen Restriktionen der Zielarchitektur von Beginn an berücksichtigten:
**Frontend:** Angular 21 (Standalone Components, Signals, Zoneless Change Detection)
**Backend:** Spring Boot 4, Java 21, Spring Security (JWT), Spring Data JPA

### ⚡ Iteratives Prompting & API-Anbindung

Beim Design der KI-Pipeline wurde gezielt **Spring AI 2** eingesetzt. Anstatt unstrukturierten Text zu verarbeiten, erzwingt die App über den `BeanOutputConverter` von Spring AI eine streng typisierte, strukturierte JSON-Ausgabe von OpenAI (GPT-4o). Die KI wurde hierbei als Sparringspartner genutzt, um die mathematische Gewichtung der Skill-Kategorien im Prompt-Kontext optimal zu balancieren.

---

## 🛡️ 3. Qualitätssicherung: Wann „trägt“ das Ergebnis?

Ein generiertes Feature gilt in meinem Workflow erst dann als fertig, wenn seine Stabilität mathematisch und funktional bewiesen ist.

| Validierungssäule | Umsetzung im Projekt | Ziel / Metrik |
| :--- | :--- | :--- |
| **Testautomatisierung** | KI-generierte Erkennung von Edge Cases & Randfällen | **155 Backend-Tests** (JUnit 5, MockMvc) / **182 Frontend-Tests** |
| **CI/CD Pipeline** | Automatisierte Validierung bei jedem Commit | GitHub Actions (Checkstyle, ESLint, Prettier) |
| **Nachvollziehbarer Score** | Regelbasierter Skill-Abgleich kombiniert mit LLM-Bewertung; feste Gewichtung (50 % Skills, 30 % Erfahrung, 10 % Ausbildung, 10 % Sprachen) | Teilscores einzeln sichtbar – die KI schlägt vor, Menschen entscheiden |
| **Compliance & DSGVO** | Automatische Löschung abgesagter Bewerbungen nach 6 Monaten; Evidenz nur mit Einwilligung (ohne Antwort Löschung nach 14 Tagen, höchstens 24 Monate) | Schutz sensibler personenbezogener Bewerberdaten |

---

## 📈 Fazit

Das Projekt zeigt: Der Einsatz von KI verkürzt die Time-to-Market drastisch – die Verantwortung für Architektur, Code-Reviews, Datenschutz und die finale Validierung verbleibt jedoch zu 100 % bei den Software-Engineers. Der Code wurde so strukturiert, dass er handgeschriebenen Qualitätsmaßstäben in nichts nachsteht.
