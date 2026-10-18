/** API-Payloads des TalentMatch-Backends (Spring Boot, siehe backend/…/api). */

export type Role = 'HR_MANAGER' | 'APPLICANT';

export type ApplicationStatus = 'NEW' | 'MATCHED' | 'INTERVIEW' | 'HIRED' | 'REJECTED';

export interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  department: string | null;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
  user: User;
}

export interface RegisterRequest {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

/** Für Bewerber:innen sichtbarer Status (interne Schritte wie „KI-analysiert“ sind ausgeblendet). */
export type ApplicantStatus = 'RECEIVED' | 'INTERVIEW' | 'OFFER' | 'REJECTED';

/** Stelle im Bewerber-Portal – ohne Zähler und Scores. */
export interface OpenJob {
  id: number;
  title: string;
  department: string;
  experienceLevel: string;
  location: string;
  description: string | null;
  requiredSkills: string[];
  createdDate: string;
  applied: boolean;
}

/** Eigene Bewerbung im Portal – nur Status, kein Match-Score. */
export interface MyApplication {
  id: number;
  jobId: number;
  jobTitle: string;
  department: string;
  location: string;
  status: ApplicantStatus;
  submittedAt: string;
  resumeFileName: string | null;
  skillProfileFileName: string | null;
  coverLetterFileName: string | null;
}

/** Bewerbungsunterlagen (PDF): Lebenslauf ist Pflicht, Skill-Profil und Anschreiben sind optional. */
export interface ApplicationDocuments {
  resume: File;
  skillProfile: File | null;
  coverLetter: File | null;
}

/** Dokumentart im Backend. */
export type DocumentType = 'RESUME' | 'SKILL_PROFILE' | 'COVER_LETTER';

/** Merkmal, an dem Dateien bzw. Bewerbungen derselben Person erkannt wurden (stärkstes zuerst). */
export type GroupMatch = 'EMAIL' | 'PHONE' | 'NAME' | 'NONE';

/** Vorab ausgewertete Datei eines HR-Uploads. */
export interface InspectedFile {
  index: number;
  fileName: string;
  type: DocumentType | null;
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  error: string | null;
}

export interface FileGroup {
  fileIndexes: number[];
  match: GroupMatch;
  needsReview: boolean;
}

export interface UploadInspection {
  files: InspectedFile[];
  groups: FileGroup[];
}

export interface DuplicateDocument {
  type: DocumentType;
  fileName: string;
  suggestedAs: DocumentType;
}

/** Mögliches Duplikat einer Bewerbung (gleiche Stelle). */
export interface Duplicate {
  id: number;
  firstName: string;
  lastName: string;
  createdAt: string;
  status: ApplicationStatus;
  reason: GroupMatch;
  documents: DuplicateDocument[];
}

export interface MergeRequest {
  sourceId: number;
  documents: { from: DocumentType; to: DocumentType }[];
}

/** Pfadsegment der Unterlagen-Endpunkte {@code /api/applications/{id}/{kind}}. */
export type DocumentKind = 'resume' | 'skill-profile' | 'cover-letter';

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface Job {
  id: number;
  title: string;
  department: string;
  experienceLevel: string;
  location: string;
  description: string | null;
  requiredSkills: string[];
  createdDate: string;
  statusCounts: Record<ApplicationStatus, number>;
  totalApplications: number;
  averageScore: number | null;
}

export interface CreateJobRequest {
  title: string;
  department: string;
  experienceLevel: string;
  location: string;
  description: string;
  requiredSkills: string[];
}

export interface DashboardKpis {
  openJobs: number;
  newJobsThisMonth: number;
  totalApplications: number;
  applicationsLastWeek: number;
  unprocessed: number;
  interviews: number;
}

export interface CandidateSummary {
  id: number;
  firstName: string;
  lastName: string;
  currentRole: string | null;
  currentCompany: string | null;
  city: string | null;
}

export interface ApplicationSummary {
  id: number;
  jobPositionId: number;
  jobTitle: string;
  status: ApplicationStatus;
  createdAt: string;
  candidate: CandidateSummary;
  overallScore: number | null;
  skills: string[];
  /** Neue Portal-Bewerbung, die HR noch nicht geöffnet hat. */
  unread: boolean;
}

export interface WorkExperience {
  period: string | null;
  role: string | null;
  company: string | null;
  location: string | null;
  highlights: string[];
}

export interface CandidateProfile {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  phone: string | null;
  city: string | null;
  currentRole: string | null;
  currentCompany: string | null;
  positions: WorkExperience[];
  technologies: string[];
  education: string | null;
  languages: string[];
  yearsOfExperience: number | null;
}

export interface InterviewQuestion {
  index: number;
  topic: string;
  text: string;
  discussed: boolean;
}

export interface Analysis {
  overallScore: number;
  skillsScore: number;
  experienceScore: number;
  educationScore: number;
  languagesScore: number;
  summary: string;
  matchingSkills: string[];
  missingSkills: string[];
  /** Teilmenge von matchingSkills, die nur das Skill-Profil (Selbstauskunft) belegt. */
  skillsFromSkillProfile: string[];
  riskFactors: string[];
  questions: InterviewQuestion[];
  model: string;
  analyzedAt: string;
}

export interface JobRef {
  id: number;
  title: string;
  department: string;
  experienceLevel: string;
  location: string;
  requiredSkills: string[];
}

export interface CandidateDetail {
  id: number;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  city: string | null;
  currentRole: string | null;
  currentCompany: string | null;
  profile: CandidateProfile | null;
}

export interface ApplicationDetail {
  id: number;
  status: ApplicationStatus;
  createdAt: string;
  interviewNotes: string | null;
  resumeFileName: string | null;
  skillProfileFileName: string | null;
  coverLetterFileName: string | null;
  job: JobRef;
  candidate: CandidateDetail;
  analysis: Analysis | null;
  unread: boolean;
}

/** RFC-9457 Problem Details, wie vom GlobalExceptionHandler geliefert. */
export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}
