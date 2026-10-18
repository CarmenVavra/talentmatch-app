package de.talentmatch.talentpool.api;

/**
 * Vorschlag für die Evidenz-Anfrage: Empfänger:in, Betreff und bearbeitbarer Text. Die Platzhalterzeile
 * {@code [Antwort-Links]} ersetzt der Server beim Versand durch die persönlichen Antwort-Links und die Frist.
 * {@code deliveryEnabled} ist false, solange kein Mailserver konfiguriert ist (nur Postausgang).
 */
public record TalentPoolMailDto(String recipient, String subject, String body, boolean deliveryEnabled) {
}
