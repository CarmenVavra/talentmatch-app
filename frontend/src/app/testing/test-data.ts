import {
  Analysis,
  ApplicationDetail,
  ApplicationSummary,
  DashboardKpis,
  Duplicate,
  Job,
  User,
} from '../core/models/api.models';

/** Testdaten-Fabriken für Unit-Tests. */

export function makeUser(overrides: Partial<User> = {}): User {
  return {
    id: 1,
    email: 'katrin.vogel@talentmatch.de',
    firstName: 'Katrin',
    lastName: 'Vogel',
    department: 'People & Culture',
    role: 'HR_MANAGER',
    ...overrides,
  };
}

export function makeJob(overrides: Partial<Job> = {}): Job {
  return {
    id: 1,
    title: 'Senior Java Developer',
    department: 'Engineering',
    experienceLevel: 'Senior',
    location: 'München · Hybrid',
    description: 'Microservices',
    requiredSkills: ['Java', 'Spring Boot', 'Kubernetes'],
    createdDate: '2026-10-02',
    statusCounts: { NEW: 2, MATCHED: 4, INTERVIEW: 3, HIRED: 1, REJECTED: 0 },
    totalApplications: 10,
    averageScore: 72,
    ...overrides,
  };
}

export function makeKpis(overrides: Partial<DashboardKpis> = {}): DashboardKpis {
  return {
    openJobs: 6,
    newJobsThisMonth: 2,
    totalApplications: 114,
    applicationsLastWeek: 18,
    unprocessed: 25,
    interviews: 9,
    ...overrides,
  };
}

export function makeSummary(overrides: Partial<ApplicationSummary> = {}): ApplicationSummary {
  return {
    id: 10,
    jobPositionId: 1,
    jobTitle: 'Senior Java Developer',
    status: 'MATCHED',
    createdAt: '2026-10-06T09:00:00',
    candidate: {
      id: 20,
      firstName: 'Jonas',
      lastName: 'Weber',
      currentRole: 'Java Developer',
      currentCompany: 'Siemens',
      city: 'Erlangen',
    },
    overallScore: 84,
    skills: ['Java', 'Spring Boot', 'PostgreSQL', 'AWS'],
    unread: false,
    ...overrides,
  };
}

export function makeAnalysis(overrides: Partial<Analysis> = {}): Analysis {
  return {
    overallScore: 91,
    skillsScore: 93,
    experienceScore: 95,
    educationScore: 92,
    languagesScore: 86,
    summary: 'Sehr starke Passung.',
    matchingSkills: ['Java', 'Spring Boot'],
    missingSkills: ['AWS'],
    skillsFromSkillProfile: [],
    riskFactors: ['Kündigungsfrist von 3 Monaten'],
    questions: [
      { index: 0, topic: 'Architektur', text: 'Welche Muster?', discussed: true },
      { index: 1, topic: 'Fehlender Skill', text: 'Wie gut kennen Sie AWS?', discussed: false },
    ],
    model: 'gpt-4o',
    analyzedAt: '2026-10-17T08:00:00Z',
    ...overrides,
  };
}

export function makeDetail(overrides: Partial<ApplicationDetail> = {}): ApplicationDetail {
  return {
    id: 10,
    status: 'INTERVIEW',
    createdAt: '2026-10-04T09:00:00',
    interviewNotes: null,
    resumeFileName: null,
    skillProfileFileName: null,
    coverLetterFileName: null,
    job: {
      id: 1,
      title: 'Senior Java Developer',
      department: 'Engineering',
      experienceLevel: 'Senior',
      location: 'München · Hybrid',
      requiredSkills: ['Java', 'Spring Boot', 'AWS'],
    },
    candidate: {
      id: 20,
      firstName: 'Lena',
      lastName: 'Hoffmann',
      email: 'lena.hoffmann@mail.de',
      phone: '+49 151 2384 1000',
      city: 'München',
      currentRole: 'Senior Backend Engineer',
      currentCompany: 'FinCore',
      profile: {
        firstName: 'Lena',
        lastName: 'Hoffmann',
        email: 'lena.hoffmann@mail.de',
        phone: null,
        city: 'München',
        currentRole: 'Senior Backend Engineer',
        currentCompany: 'FinCore',
        positions: [
          {
            period: '03/2025 – heute',
            role: 'Senior Backend Engineer',
            company: 'FinCore',
            location: 'München',
            highlights: ['Microservices mit Spring Boot 3'],
          },
        ],
        technologies: ['Java 17', 'Spring Boot 3', 'Kafka'],
        education: 'M.Sc. Informatik · TU München · 2018',
        languages: ['Deutsch (Muttersprache)', 'Englisch (C1)'],
        yearsOfExperience: 7,
      },
    },
    analysis: makeAnalysis(),
    unread: false,
    ...overrides,
  };
}

export function makeDuplicate(overrides: Partial<Duplicate> = {}): Duplicate {
  return {
    id: 20,
    firstName: 'Carmen',
    lastName: 'Gregor',
    createdAt: '2026-10-18T09:00:00',
    status: 'MATCHED',
    reason: 'NAME',
    documents: [
      { type: 'RESUME', fileName: 'Skillprofil_Carmen_Gregor.pdf', suggestedAs: 'SKILL_PROFILE' },
    ],
    ...overrides,
  };
}
