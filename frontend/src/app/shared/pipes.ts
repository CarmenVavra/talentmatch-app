import { Pipe, PipeTransform } from '@angular/core';

import { ApplicationStatus } from '../core/models/api.models';
import { formatDate, formatDayMonth, relativeTime } from '../core/util/format';
import { ScoreTone, StatusMeta, scoreLabel, scoreTone, statusMeta } from '../core/util/score';

@Pipe({ name: 'scoreTone' })
export class ScoreTonePipe implements PipeTransform {
  transform(score: number | null | undefined): ScoreTone {
    return scoreTone(score);
  }
}

@Pipe({ name: 'scoreLabel' })
export class ScoreLabelPipe implements PipeTransform {
  transform(score: number | null | undefined): string {
    return scoreLabel(score);
  }
}

@Pipe({ name: 'statusMeta' })
export class StatusMetaPipe implements PipeTransform {
  transform(status: ApplicationStatus): StatusMeta {
    return statusMeta(status);
  }
}

/** TT.MM.JJJJ */
@Pipe({ name: 'deDate' })
export class DeDatePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return value ? formatDate(value) : '';
  }
}

/** TT.MM. */
@Pipe({ name: 'dayMonth' })
export class DayMonthPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return value ? formatDayMonth(value) : '';
  }
}

/** „vor 2 Std.“ */
@Pipe({ name: 'relativeTime' })
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return value ? relativeTime(value) : '';
  }
}
