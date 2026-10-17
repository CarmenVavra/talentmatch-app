package de.talentmatch.seed;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import de.talentmatch.analysis.AnalysisResult;
import de.talentmatch.analysis.InterviewQuestion;
import de.talentmatch.application.Application;
import de.talentmatch.application.ApplicationStatus;
import de.talentmatch.auth.UserAccount;
import de.talentmatch.candidate.Candidate;
import de.talentmatch.candidate.CandidateProfile;
import de.talentmatch.candidate.CandidateProfile.WorkExperience;
import de.talentmatch.job.JobPosition;

/**
 * Demo-Datenbestand nach den Mockdaten des UI-Prototyps ({@code JOBS}, {@code CANDS0}, {@code SCEN}).
 * Alle Personen, Firmen und Texte sind fiktiv. Datumsangaben sind relativ zum Seed-Zeitpunkt.
 */
final class DemoData {

    private static final String MODEL = "gpt-4o";
    private static final int[] JITTER = {0, 9, -7, 4, -11, 13, -3, 6, -14, 2, 11, -5};
    private static final String[] FIRST_NAMES = {
        "Anna", "Lukas", "Marie", "Leon", "Sophia", "Finn", "Emilia", "Elias", "Mia", "Noah", "Lea", "Paul",
        "Laura", "Ben", "Julia", "Jan", "Lara", "Tim", "Sarah", "Moritz", "Lisa", "Niklas", "Katharina", "Max",
        "Johanna", "Simon", "Selin", "Can", "Fatma", "Luca", "Ida", "Jakob", "Greta", "Erik", "Nele", "Milan"};
    private static final String[] LAST_NAMES = {
        "Müller", "Schmidt", "Schneider", "Meyer", "Wolf", "Becker", "Schulz", "Koch", "Bauer", "Klein",
        "Schröder", "Zimmermann", "Braun", "Hartmann", "Krause", "Lehmann", "Köhler", "Huber", "Kaiser", "Peters",
        "Arslan", "Kaya", "Jung", "Möller", "König", "Walter", "Kowalski", "Nowak", "Fuchs", "Vogt", "Seidel"};
    private static final String[] CITIES = {
        "München", "Berlin", "Hamburg", "Köln", "Frankfurt", "Stuttgart", "Leipzig", "Nürnberg", "Düsseldorf",
        "Dresden", "Augsburg", "Hannover"};

    private final LocalDate today;
    private final List<JobPosition> jobs = new ArrayList<>();
    private final List<Application> applications = new ArrayList<>();
    private int personCounter;

    DemoData(LocalDate today) {
        this.today = today;
        buildSeniorJava();
        for (GeneratedJob spec : generatedJobs()) {
            buildGenerated(spec);
        }
    }

    List<JobPosition> jobs() {
        return List.copyOf(jobs);
    }

    List<Application> applications() {
        return List.copyOf(applications);
    }

    // ---------------------------------------------------------------- Stelle 1: exakt nach Prototyp

    private void buildSeniorJava() {
        JobPosition job = job("Senior Java Developer", "Engineering", "Senior", "München · Hybrid",
                "Du entwickelst und betreibst Microservices für unsere HR-Plattform mit Java 17+ und Spring Boot "
                        + "auf Kubernetes (AWS). Du übernimmst Verantwortung für Architektur, Code-Qualität und "
                        + "begleitest Junior-Entwickler:innen.",
                List.of("Java", "Spring Boot", "Kubernetes", "PostgreSQL", "AWS"), 15);
        List<SeedCandidate> candidates = List.of(
                seed("Lena", "Hoffmann", "Senior Backend Engineer", "FinCore", "München", ApplicationStatus.INTERVIEW,
                        91, 13, List.of("Java 17", "Spring Boot 3", "Kubernetes", "PostgreSQL", "REST / OpenAPI",
                                "JUnit 5"), List.of("AWS", "Terraform"),
                        List.of("Testcontainers", "Kafka", "Azure", "Docker")),
                seed("Jonas", "Weber", "Java Developer", "Siemens", "Erlangen", ApplicationStatus.MATCHED, 84, 11,
                        List.of("Java", "Spring Boot", "PostgreSQL", "AWS"), List.of("Kubernetes"),
                        List.of("Maven", "Jenkins")),
                seed("Aylin", "Demir", "Software Engineer", "Check24", "München", ApplicationStatus.INTERVIEW, 78, 12,
                        List.of("Java", "Spring Boot", "Kubernetes"), List.of("PostgreSQL", "AWS"),
                        List.of("MongoDB", "GitLab CI")),
                seed("Tobias", "Krüger", "Backend Developer", "Otto", "Hamburg", ApplicationStatus.MATCHED, 72, 9,
                        List.of("Java", "PostgreSQL", "AWS"), List.of("Kubernetes", "Spring Boot 3"),
                        List.of("Quarkus", "Docker")),
                seed("Sophie", "Brandt", "Java Entwicklerin", "DATEV", "Nürnberg", ApplicationStatus.MATCHED, 66, 8,
                        List.of("Java", "Spring MVC", "Oracle SQL"), List.of("Kubernetes", "AWS", "Senior-Erfahrung"),
                        List.of("Hibernate", "SVN")),
                seed("David", "Fischer", "Full-Stack Developer", "Freelance", "Augsburg", ApplicationStatus.MATCHED,
                        61, 7, List.of("Java", "Spring Boot", "Angular"), List.of("Kubernetes", "AWS", "PostgreSQL"),
                        List.of("MySQL", "TypeScript")),
                seed("Paul", "Richter", "Software Developer", "Bosch", "Stuttgart", ApplicationStatus.INTERVIEW, 69,
                        14, List.of("Java", "Spring Boot", "Docker"), List.of("Kubernetes", "AWS"),
                        List.of("C++", "Jenkins")),
                seed("Felix", "Schmitt", "Backend Engineer", "N26", "Berlin", ApplicationStatus.NEW, null, 1,
                        List.of(), List.of(), List.of("Kotlin", "Spring", "AWS")),
                seed("Hannah", "Lange", "Java Developer", "Zalando", "Berlin", ApplicationStatus.NEW, null, 0,
                        List.of(), List.of(), List.of("Java", "Kafka", "K8s")),
                seed("Clara", "Neumann", "Lead Java Engineer", "BMW", "München", ApplicationStatus.HIRED, 88, 18,
                        List.of("Java", "Spring Boot", "Kubernetes", "AWS"), List.of("PostgreSQL"),
                        List.of("OpenShift", "Kafka")),
                seed("Mehmet", "Yilmaz", "PHP Developer", "Agentur", "Köln", ApplicationStatus.REJECTED, 47, 16,
                        List.of("PostgreSQL", "Docker"), List.of("Java", "Spring Boot", "Kubernetes", "AWS"),
                        List.of("PHP", "Laravel", "Vue")),
                seed("Nina", "Wagner", "IT-Consultant", "Accenture", "Frankfurt", ApplicationStatus.REJECTED, 39, 17,
                        List.of("SQL"), List.of("Java", "Spring Boot", "Kubernetes", "AWS"), List.of("SAP", "ABAP")));

        for (SeedCandidate c : candidates) {
            Scenario scenario = Scenario.forScore(c.score());
            List<WorkExperience> timeline = scenario.timelineFor(c);
            CandidateProfile profile = new CandidateProfile(c.first(), c.last(), email(c.first(), c.last()),
                    phone(), c.city(), c.role(), c.company(), timeline,
                    concat(c.match(), c.extra()), scenario.education, scenario.languages, scenario.years);
            Application application = application(job, profile, c.receivedDaysAgo());
            if (c.score() != null) {
                List<InterviewQuestion> questions = new ArrayList<>();
                for (int i = 0; i < scenario.questions.size(); i++) {
                    String[] q = scenario.questions.get(i);
                    boolean discussed = "Hoffmann".equals(c.last()) && i == 0;
                    questions.add(new InterviewQuestion(q[0], q[1], discussed));
                }
                int[] sub = scenario.subOffsets;
                AnalysisResult.Scores scores = new AnalysisResult.Scores(c.score(), sub(c.score(), sub[0]),
                        sub(c.score(), sub[1]), sub(c.score(), sub[2]), sub(c.score(), sub[3]));
                application.applyAnalysis(new AnalysisResult(scores, scenario.summary.replace("{f}", c.first()),
                        c.match(), c.miss(), scenario.risks, questions, MODEL, analyzedAt(c.receivedDaysAgo())));
            }
            application.setStatus(c.status());
            applications.add(application);
        }
    }

    // ---------------------------------------------------------------- Stellen 2–6: generierte Bewerbungen

    private List<GeneratedJob> generatedJobs() {
        return List.of(
                new GeneratedJob("Frontend Engineer (Angular)", "Engineering", "Mid", "Berlin · Remote",
                        "Du baust moderne, barrierearme Oberflächen mit Angular (Signals, Standalone Components) und "
                                + "RxJS und arbeitest eng mit UX und Backend zusammen.",
                        List.of("Angular", "TypeScript", "RxJS", "Signals"), 19, new int[] {6, 9, 4, 1, 5}, 71,
                        List.of("Frontend Developer", "Angular Developer", "Web Developer", "UI Engineer"),
                        List.of("Zalando", "HelloFresh", "Delivery Hero", "Trivago", "Personio", "N26"),
                        List.of("React", "SCSS", "Jest", "Cypress", "Storybook", "Vue", "HTML", "Figma"),
                        "B.Sc. Medieninformatik · HTW Berlin · 2019"),
                new GeneratedJob("Data Engineer", "Data", "Mid", "Hamburg · Hybrid",
                        "Du entwickelst skalierbare Datenpipelines mit Python, Spark und Airflow und sorgst für "
                                + "verlässliche Datenqualität im Data Warehouse.",
                        List.of("Python", "Spark", "SQL", "Airflow"), 26, new int[] {3, 7, 2, 0, 4}, 64,
                        List.of("Data Engineer", "BI Developer", "Data Analyst", "Analytics Engineer"),
                        List.of("Otto", "Hapag-Lloyd", "Hermes", "Xing", "Airbus", "Tchibo"),
                        List.of("dbt", "Kafka", "Snowflake", "Pandas", "Tableau", "Docker", "Scala", "BigQuery"),
                        "M.Sc. Data Science · Universität Hamburg · 2020"),
                new GeneratedJob("Product Owner HR-Tech", "Product", "Senior", "München · Vor Ort",
                        "Du verantwortest das Backlog unserer HR-Produkte, priorisierst mit Stakeholdern und "
                                + "führst Scrum-Teams zu messbaren Ergebnissen.",
                        List.of("Scrum", "Stakeholder-Mgmt", "Jira", "HR-Prozesse"), 32, new int[] {2, 5, 3, 1, 6},
                        58,
                        List.of("Product Owner", "Product Manager", "Business Analyst", "Projektmanager:in"),
                        List.of("SAP SuccessFactors", "Personio", "Allianz", "Siemens", "BMW", "Celonis"),
                        List.of("Confluence", "Kanban", "OKR", "SAFe", "Miro", "SQL", "UX Research", "Roadmapping"),
                        "M.A. Wirtschaftspsychologie · LMU München · 2016"),
                new GeneratedJob("DevOps Engineer", "Engineering", "Mid", "Remote (DE)",
                        "Du automatisierst unsere Infrastruktur mit Terraform, betreibst Kubernetes-Cluster und "
                                + "entwickelst CI/CD-Pipelines weiter.",
                        List.of("Docker", "Kubernetes", "Terraform", "CI/CD"), 38, new int[] {4, 3, 2, 0, 2}, 76,
                        List.of("DevOps Engineer", "Site Reliability Engineer", "Cloud Engineer", "System Engineer"),
                        List.of("IONOS", "Telekom", "SAP", "1&1", "STACKIT", "Scout24"),
                        List.of("AWS", "Ansible", "Prometheus", "Grafana", "Helm", "Linux", "GitLab CI", "Bash"),
                        "B.Sc. Informatik · KIT Karlsruhe · 2018"),
                new GeneratedJob("Werkstudent:in Recruiting", "People", "Junior", "München · Hybrid",
                        "Du unterstützt unser Recruiting-Team bei Stellenausschreibungen, Bewerbermanagement im ATS "
                                + "und der Organisation von Interviews.",
                        List.of("MS Office", "Kommunikation", "ATS"), 46, new int[] {8, 12, 3, 1, 9}, 47,
                        List.of("Werkstudent:in HR", "Praktikant:in Personal", "Studentische Hilfskraft",
                                "Aushilfe Empfang"),
                        List.of("Allianz", "Siemens", "Universität", "Agentur", "Start-up", "Stadtwerke"),
                        List.of("Excel", "Social Media", "Canva", "Englisch", "Organisation", "SAP HCM", "LinkedIn"),
                        "B.A. Psychologie (laufend) · LMU München"));
    }

    private void buildGenerated(GeneratedJob spec) {
        JobPosition job = job(spec.title(), spec.department(), spec.level(), spec.location(), spec.description(),
                spec.skills(), spec.createdDaysAgo());
        ApplicationStatus[] statuses = ApplicationStatus.values();
        for (int s = 0; s < statuses.length; s++) {
            for (int k = 0; k < spec.counts()[s]; k++) {
                applications.add(generatedApplication(job, spec, statuses[s], k));
            }
        }
    }

    private Application generatedApplication(JobPosition job, GeneratedJob spec, ApplicationStatus status, int k) {
        int person = personCounter++;
        String first = FIRST_NAMES[(person * 7) % FIRST_NAMES.length];
        String last = LAST_NAMES[(person * 11 + person / LAST_NAMES.length) % LAST_NAMES.length];
        String city = CITIES[(person * 5) % CITIES.length];
        String role = spec.roles().get(person % spec.roles().size());
        String company = spec.companies().get((person * 3) % spec.companies().size());
        Integer score = status == ApplicationStatus.NEW ? null : generatedScore(spec.averageScore(), status, k);

        List<String> required = spec.skills();
        int matchedCount = score == null ? Math.min(2, required.size())
                : Math.max(0, Math.min(required.size(), Math.round(required.size() * score / 100f)));
        List<String> matched = required.subList(0, matchedCount);
        List<String> missing = required.subList(matchedCount, required.size());
        List<String> extras = List.of(spec.extraPool().get(person % spec.extraPool().size()),
                spec.extraPool().get((person + 3) % spec.extraPool().size()));

        int startYear = today.getYear() - 1 - person % 4;
        List<WorkExperience> timeline = List.of(
                new WorkExperience(String.format("%02d/%d – heute", 1 + person % 12, startYear), role, company,
                        city, List.of("Schwerpunkte: " + String.join(", ", concat(matched, extras).stream()
                                .limit(3).toList()), "Zusammenarbeit in agilen, cross-funktionalen Teams")),
                new WorkExperience(String.format("%02d/%d – %02d/%d", 1 + (person + 4) % 12, startYear - 3,
                        1 + (person + 2) % 12, startYear), "Junior " + role, spec.companies().get(
                        (person * 3 + 1) % spec.companies().size()), city, List.of("Einstieg und Einarbeitung "
                        + "in " + (matched.isEmpty() ? extras.get(0) : matched.get(0)))));
        CandidateProfile profile = new CandidateProfile(first, last, email(first, last), phone(), city, role,
                company, timeline, concat(matched, extras), spec.education(),
                List.of("Deutsch (Muttersprache)", "Englisch (" + (person % 3 == 0 ? "B2" : "C1") + ")"),
                2 + person % 6);

        int receivedDaysAgo = status == ApplicationStatus.NEW ? 1 + k % 6
                : Math.max(2, spec.createdDaysAgo() - 2 - (k * 3) % Math.max(1, spec.createdDaysAgo() - 4));
        Application application = application(job, profile, receivedDaysAgo);
        if (score != null) {
            application.applyAnalysis(genericAnalysis(first, spec, score, matched, missing, receivedDaysAgo, k));
        }
        application.setStatus(status);
        return application;
    }

    private AnalysisResult genericAnalysis(String first, GeneratedJob spec, int score, List<String> matched,
                                           List<String> missing, int receivedDaysAgo, int k) {
        String matchedText = matched.isEmpty() ? "einzelnen Bereichen" : joinGerman(matched);
        String missingText = missing.isEmpty() ? "Alle geforderten Skills sind im Lebenslauf belegt."
                : "Es fehlen Nachweise zu " + joinGerman(missing) + ".";
        String summary;
        List<String> risks;
        if (score >= 75) {
            summary = first + " bringt einschlägige Erfahrung mit und erfüllt die Kernanforderungen der Stelle "
                    + "weitgehend (" + matchedText + "). " + missingText + " Werdegang und Ausbildung passen gut zum "
                    + "geforderten Level. Starke Passung – Einladung zum Interview empfohlen.";
            risks = List.of("Kündigungsfrist von 3 Monaten");
        } else if (score >= 50) {
            summary = first + " verfügt über solide Grundlagen in " + matchedText + ", erfüllt das Anforderungsprofil "
                    + "aber nur teilweise. " + missingText + " Ein Interview kann sinnvoll sein, um Lernbereitschaft "
                    + "und Praxiserfahrung zu prüfen.";
            risks = List.of("Lücke im Lebenslauf (6 Monate)",
                    "Wenig Praxiserfahrung mit " + (missing.isEmpty() ? spec.skills().get(0) : missing.get(0)));
        } else {
            summary = first + " kommt aus einem anderen fachlichen Umfeld; zentrale Anforderungen der Stelle werden "
                    + "nicht erfüllt. " + missingText + " Für diese Position ist die Passung gering.";
            risks = List.of("Keine einschlägige Berufserfahrung im Fachgebiet",
                    "Gehaltsvorstellung über Budget (laut Anschreiben)");
        }
        List<InterviewQuestion> questions = new ArrayList<>();
        missing.stream().limit(2).forEach(skill -> questions.add(new InterviewQuestion("Fehlender Skill",
                "Wie schätzen Sie Ihre Erfahrung mit " + skill + " ein – haben Sie bereits in Projekten damit "
                        + "gearbeitet?", false)));
        if (!matched.isEmpty()) {
            questions.add(new InterviewQuestion("Technik", "Beschreiben Sie ein Projekt, in dem Sie " + matched.get(0)
                    + " intensiv eingesetzt haben. Was war Ihre Rolle?", false));
        }
        questions.add(new InterviewQuestion("Motivation", "Was reizt Sie an der Position „" + spec.title()
                + "“ und wie passt sie in Ihren bisherigen Werdegang?", false));
        questions.add(new InterviewQuestion("Soft Skills",
                "Wie gehen Sie mit unterschiedlichen Erwartungen von Stakeholdern um?", false));

        int skillsScore = spec.skills().isEmpty() ? 100 : Math.round(100f * matched.size() / spec.skills().size());
        AnalysisResult.Scores scores = new AnalysisResult.Scores(score, sub(skillsScore, 0),
                sub(score, JITTER[(k + 2) % JITTER.length]), sub(score, JITTER[(k + 5) % JITTER.length] + 6),
                sub(score, JITTER[(k + 7) % JITTER.length] + 10));
        return new AnalysisResult(scores, summary, matched, missing, risks, questions, MODEL,
                analyzedAt(receivedDaysAgo));
    }

    private static int generatedScore(int average, ApplicationStatus status, int k) {
        int offset = switch (status) {
            case INTERVIEW -> 8;
            case HIRED -> 14;
            case REJECTED -> -16;
            default -> 0;
        };
        return Math.max(18, Math.min(96, average + offset + JITTER[k % JITTER.length]));
    }

    // ---------------------------------------------------------------- Bewerber-Portal (Demo-Konto)

    /**
     * Zwei Portal-Bewerbungen des Demo-Bewerberkontos: Data Engineer (Interview) und DevOps Engineer (eingegangen).
     */
    static List<Application> applicantPortal(UserAccount account, JobPosition dataJob, JobPosition devopsJob,
                                             LocalDate today) {
        CandidateProfile profile = new CandidateProfile(account.getFirstName(), account.getLastName(),
                account.getEmail(), "+49 151 2384 7700", "Leipzig", "Data Analyst", "Hermes",
                List.of(new WorkExperience("05/2022 – heute", "Data Analyst", "Hermes", "Leipzig",
                                List.of("Reporting-Pipelines mit Python und SQL", "Airflow-DAGs für tägliche Loads")),
                        new WorkExperience("09/2019 – 04/2022", "Werkstudent BI", "Otto", "Hamburg",
                                List.of("Dashboards in Tableau"))),
                List.of("Python", "SQL", "Airflow", "Tableau", "Docker"), "M.Sc. Data Science · Universität Leipzig "
                        + "· 2022", List.of("Deutsch (Muttersprache)", "Englisch (C1)"), 4);

        Application interview = portalApplication(account, dataJob, profile, today.minusDays(9));
        interview.applyAnalysis(new AnalysisResult(new AnalysisResult.Scores(73, 75, 70, 80, 72),
                account.getFirstName() + " bringt solide Erfahrung mit Python, SQL und Airflow mit. Spark fehlt "
                        + "bisher im Lebenslauf. Teilweise Passung – ein Interview ist sinnvoll, um die "
                        + "Lernkurve für verteilte Verarbeitung einzuschätzen.",
                List.of("Python", "SQL", "Airflow"), List.of("Spark"), List.of("Kein Nachweis von Spark-Erfahrung"),
                List.of(new InterviewQuestion("Fehlender Skill",
                                "Haben Sie bereits mit Spark oder anderen verteilten Frameworks gearbeitet?", false),
                        new InterviewQuestion("Technik", "Wie strukturieren Sie Airflow-DAGs für Backfills?", false)),
                MODEL, today.minusDays(8).atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant()));
        interview.setStatus(ApplicationStatus.INTERVIEW);

        Application received = portalApplication(account, devopsJob, profile, today.minusDays(2));
        return List.of(interview, received);
    }

    private static Application portalApplication(UserAccount account, JobPosition job, CandidateProfile profile,
                                                  LocalDate submitted) {
        Candidate candidate = new Candidate(account.getFirstName(), account.getLastName());
        candidate.applyProfile(profile);
        candidate.setRawResumeText(resumeText(profile));
        Application application = new Application(job, candidate, submitted.atTime(10, 15));
        application.setApplicantAccount(account);
        return application;
    }

    // ---------------------------------------------------------------- Hilfsfunktionen

    private JobPosition job(String title, String department, String level, String location, String description,
                            List<String> skills, int createdDaysAgo) {
        JobPosition job = new JobPosition(title, department, level, location, description, skills,
                today.minusDays(createdDaysAgo));
        jobs.add(job);
        return job;
    }

    private Application application(JobPosition job, CandidateProfile profile, int receivedDaysAgo) {
        Candidate candidate = new Candidate(profile.firstName(), profile.lastName());
        candidate.applyProfile(profile);
        candidate.setRawResumeText(resumeText(profile));
        return new Application(job, candidate, today.minusDays(receivedDaysAgo).atTime(9 + receivedDaysAgo % 8,
                (receivedDaysAgo * 13) % 60));
    }

    private Instant analyzedAt(int receivedDaysAgo) {
        return today.minusDays(Math.max(0, receivedDaysAgo - 1)).atTime(8, 30)
                .atZone(ZoneId.systemDefault()).toInstant();
    }

    private static SeedCandidate seed(String first, String last, String role, String company, String city,
                                      ApplicationStatus status, Integer score, int receivedDaysAgo,
                                      List<String> match, List<String> miss, List<String> extra) {
        return new SeedCandidate(first, last, role, company, city, status, score, receivedDaysAgo, match, miss,
                extra);
    }

    private static int sub(int score, int offset) {
        return Math.max(5, Math.min(100, score + offset));
    }

    private static String email(String first, String last) {
        String local = (first + "." + last).toLowerCase(Locale.GERMAN)
                .replace("ä", "ae").replace("ö", "oe").replace("ü", "ue").replace("ß", "ss").replace(" ", "");
        return local + "@mail.de";
    }

    private String phone() {
        return "+49 151 2384 " + (1000 + (personCounter * 731 + applications.size() * 97) % 9000);
    }

    private static List<String> concat(List<String> a, List<String> b) {
        return Stream.concat(a.stream(), b.stream()).distinct().toList();
    }

    private static String joinGerman(List<String> items) {
        if (items.size() <= 1) {
            return String.join("", items);
        }
        return String.join(", ", items.subList(0, items.size() - 1)) + " und " + items.get(items.size() - 1);
    }

    private static String resumeText(CandidateProfile p) {
        StringBuilder text = new StringBuilder()
                .append(p.firstName()).append(' ').append(p.lastName()).append('\n')
                .append(p.email()).append(" · ").append(p.phone()).append(" · ").append(p.city()).append("\n\n")
                .append("Berufserfahrung\n");
        for (WorkExperience w : p.positions()) {
            text.append(w.period()).append(": ").append(w.role()).append(", ").append(w.company()).append(", ")
                    .append(w.location()).append('\n');
            w.highlights().forEach(h -> text.append("- ").append(h).append('\n'));
        }
        return text.append("\nAusbildung: ").append(p.education())
                .append("\nSprachen: ").append(String.join(", ", p.languages()))
                .append("\nKenntnisse: ").append(String.join(", ", p.technologies()))
                .toString();
    }

    private record SeedCandidate(String first, String last, String role, String company, String city,
                                 ApplicationStatus status, Integer score, int receivedDaysAgo, List<String> match,
                                 List<String> miss, List<String> extra) {
    }

    private record GeneratedJob(String title, String department, String level, String location, String description,
                                List<String> skills, int createdDaysAgo, int[] counts, int averageScore,
                                List<String> roles, List<String> companies, List<String> extraPool,
                                String education) {
    }

    /**
     * Die drei Demo-Szenarien des Prototyps (Top-, Mid- und Mismatch-Kandidat).
     */
    private enum Scenario {
        TOP("{f} bringt 7 Jahre Backend-Erfahrung mit Java und Spring Boot mit, davon 3 Jahre in einer "
                + "Microservice-Architektur auf Kubernetes. Die Kernanforderungen der Stelle werden nahezu vollständig "
                + "erfüllt. Cloud-Erfahrung beschränkt sich auf Azure, AWS-Kenntnisse fehlen. Sehr starke Passung – "
                + "Einladung zum Interview empfohlen.",
                List.of("Kurze Verweildauer in letzter Position (14 Monate)", "Kündigungsfrist von 3 Monaten"),
                List.of(new String[] {"Architektur", "Welche Microservice-Muster (z. B. Saga, Outbox) haben Sie "
                                + "produktiv eingesetzt – und mit welchen Erfahrungen?"},
                        new String[] {"Fehlender Skill", "Ihre Cloud-Erfahrung liegt bei Azure. Wie schätzen Sie den "
                                + "Umstieg auf AWS (EKS, RDS) ein?"},
                        new String[] {"Werdegang", "Was ist der Grund für den Wechsel nach 14 Monaten bei der "
                                + "aktuellen Position?"},
                        new String[] {"Technik", "Wie sind Sie bei der Migration von Java 11 auf 17 vorgegangen?"},
                        new String[] {"Soft Skills", "Welche Rolle hatten Sie bei Code-Reviews und im Mentoring von "
                                + "Junior-Entwickler:innen?"}),
                List.of(new WorkExperience("03/2025 – heute", "Senior Backend Engineer", "FinCore GmbH", "München",
                                List.of("Microservices mit Spring Boot 3 auf Kubernetes (AKS)",
                                        "Einführung von Testcontainers und Contract-Tests")),
                        new WorkExperience("01/2021 – 02/2025", "Java Developer", "Allianz Technology", "Unterföhring",
                                List.of("REST-APIs für die Schadensabwicklung, PostgreSQL", "Migration Java 11 → 17")),
                        new WorkExperience("09/2018 – 12/2020", "Junior Software Engineer", "msg systems", "Ismaning",
                                List.of("Spring MVC, Oracle, CI mit Jenkins"))),
                "M.Sc. Informatik · TU München · 2018", List.of("Deutsch (Muttersprache)", "Englisch (C1)"), 7,
                new int[] {2, 4, 1, -5}),
        MID("{f} verfügt über solide Java-Grundlagen und rund 4 Jahre Berufserfahrung, überwiegend mit Spring MVC in "
                + "monolithischen Systemen. Container- und Cloud-Plattformen tauchen im Lebenslauf nicht auf. Die "
                + "Senior-Anforderung wird nur teilweise erfüllt. Ein Interview kann sinnvoll sein, um "
                + "Lernbereitschaft und Architekturverständnis zu prüfen.",
                List.of("Kein Nachweis von Container- oder Cloud-Erfahrung", "Lücke im Lebenslauf 2022 (8 Monate)"),
                List.of(new String[] {"Fehlender Skill", "Haben Sie privat oder in Projekten bereits mit Docker oder "
                                + "Kubernetes gearbeitet?"},
                        new String[] {"Architektur", "Wie würden Sie einen bestehenden Monolithen schrittweise in "
                                + "Services zerlegen?"},
                        new String[] {"Werdegang", "Was haben Sie zwischen 03/2022 und 11/2022 gemacht?"},
                        new String[] {"Technik", "Erfahrung mit Spring Boot 3 und Java 17+ vorhanden?"}),
                List.of(new WorkExperience("12/2022 – heute", "Java Entwickler:in", "DATEV eG", "Nürnberg",
                                List.of("Weiterentwicklung eines Spring-MVC-Monolithen",
                                        "Oracle-Datenbank, Hibernate")),
                        new WorkExperience("08/2019 – 02/2022", "Software Developer", "Sparkassen IT", "Nürnberg",
                                List.of("Wartung von Java-EE-Anwendungen"))),
                "B.Sc. Wirtschaftsinformatik · TH Nürnberg · 2019", List.of("Deutsch (Muttersprache)", "Englisch (B2)"),
                4, new int[] {-4, -8, 6, 3}),
        LOW("{f} kommt aus der Web-Entwicklung mit PHP bzw. Beratung; Java wird nur aus dem Studium erwähnt. Zentrale "
                + "Anforderungen wie Spring Boot, Kubernetes und AWS fehlen vollständig. Positiv sind Erfahrungen mit "
                + "Docker und relationalen Datenbanken. Für diese Senior-Position ist die Passung gering.",
                List.of("Keine professionelle Java-Erfahrung", "Gehaltsvorstellung über Budget (laut Anschreiben)"),
                List.of(new String[] {"Fehlender Skill", "Wie viel praktische Erfahrung haben Sie mit Java seit dem "
                                + "Studium gesammelt?"},
                        new String[] {"Motivation", "Was reizt Sie am Wechsel in die Java-Backend-Entwicklung?"},
                        new String[] {"Technik", "Wie gehen Sie bei der Einarbeitung in ein neues Framework vor?"}),
                List.of(new WorkExperience("04/2021 – heute", "Web Developer", "Pixelwerk Agentur", "Köln",
                                List.of("Laravel-Backends, Vue-Frontends", "Docker-basierte Entwicklungsumgebung")),
                        new WorkExperience("10/2019 – 03/2021", "Werkstudent Entwicklung", "Startup GmbH", "Köln",
                                List.of("WordPress-Plugins, MySQL"))),
                "B.Sc. Medieninformatik · TH Köln · 2021",
                List.of("Deutsch (C2)", "Türkisch (Muttersprache)", "Englisch (B2)"), 3, new int[] {-10, -4, 8, 12});

        private final String summary;
        private final List<String> risks;
        private final List<String[]> questions;
        private final List<WorkExperience> timeline;
        private final String education;
        private final List<String> languages;
        private final int years;
        private final int[] subOffsets;

        Scenario(String summary, List<String> risks, List<String[]> questions, List<WorkExperience> timeline,
                 String education, List<String> languages, int years, int[] subOffsets) {
            this.summary = summary;
            this.risks = risks;
            this.questions = questions;
            this.timeline = timeline;
            this.education = education;
            this.languages = languages;
            this.years = years;
            this.subOffsets = subOffsets;
        }

        static Scenario forScore(Integer score) {
            if (score == null) {
                return MID;
            }
            if (score >= 75) {
                return TOP;
            }
            return score >= 50 ? MID : LOW;
        }

        /**
         * Werdegang des Szenarios; die aktuelle Station wird an Rolle und Firma der Kandidat:in angepasst.
         */
        List<WorkExperience> timelineFor(SeedCandidate c) {
            List<WorkExperience> result = new ArrayList<>(timeline);
            WorkExperience current = result.get(0);
            result.set(0, new WorkExperience(current.period(), c.role(), c.company(), c.city(),
                    current.highlights()));
            return result;
        }
    }
}
