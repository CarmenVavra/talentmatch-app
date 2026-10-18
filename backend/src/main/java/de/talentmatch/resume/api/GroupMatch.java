package de.talentmatch.resume.api;

/**
 * Woran erkannt wurde, dass Dateien zur selben Person gehören – vom stärksten zum schwächsten Merkmal.
 */
public enum GroupMatch {
    EMAIL,
    PHONE,
    NAME,
    NONE
}
