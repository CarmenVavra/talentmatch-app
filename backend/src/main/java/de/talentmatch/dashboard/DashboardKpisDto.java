package de.talentmatch.dashboard;

/**
 * Kennzahlen der KPI-Leiste im Dashboard.
 */
public record DashboardKpisDto(
        long openJobs,
        long newJobsThisMonth,
        long totalApplications,
        long applicationsLastWeek,
        long unprocessed,
        long interviews) {
}
