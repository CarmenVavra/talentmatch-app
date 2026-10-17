import { ApplicantStatus } from '../models/api.models';

export interface ApplicantStatusMeta {
  label: string;
  message: string;
  tagBg: string;
  tagFg: string;
  /** Position im Fortschritt: 0 Eingegangen, 1 Interview, 2 Entscheidung. */
  step: number;
}

const META: Record<ApplicantStatus, ApplicantStatusMeta> = {
  RECEIVED: {
    label: 'Eingegangen',
    message: 'Ihre Bewerbung ist eingegangen und wird geprüft.',
    tagBg: '#E9ECEE',
    tagFg: '#2F4A6E',
    step: 0,
  },
  INTERVIEW: {
    label: 'Einladung zum Interview',
    message: 'Wir möchten Sie kennenlernen – das HR-Team meldet sich mit einem Terminvorschlag.',
    tagBg: '#DDF0F2',
    tagFg: '#2F4A6E',
    step: 1,
  },
  OFFER: {
    label: 'Zusage',
    message: 'Herzlichen Glückwunsch – wir möchten Ihnen ein Angebot machen.',
    tagBg: '#DCEFE3',
    tagFg: '#25603F',
    step: 2,
  },
  REJECTED: {
    label: 'Absage',
    message:
      'Leider haben wir uns für andere Bewerbungen entschieden. Vielen Dank für Ihr Interesse.',
    tagBg: '#FBE6EA',
    tagFg: '#8A3346',
    step: 2,
  },
};

export function applicantStatusMeta(status: ApplicantStatus): ApplicantStatusMeta {
  return META[status];
}
