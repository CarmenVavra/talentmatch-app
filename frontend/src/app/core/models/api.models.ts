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
  job: JobRef;
  candidate: CandidateDetail;
  analysis: Analysis | null;
}

/** RFC-9457 Problem Details, wie vom GlobalExceptionHandler geliefert. */
export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}
