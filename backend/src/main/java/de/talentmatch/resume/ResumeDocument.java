package de.talentmatch.resume;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Original-PDF eines Lebenslaufs (für die PDF-Vorschau).
 */
@Entity
@Table(name = "resume_document")
public class ResumeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    private long sizeBytes;

    /** {@code bytea} wird von PostgreSQL und von H2 (PostgreSQL-Modus) gleichermaßen unterstützt. */
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    protected ResumeDocument() {
    }

    public ResumeDocument(String fileName, String contentType, byte[] data) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.data = data.clone();
        this.sizeBytes = data.length;
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data.clone();
    }
}
