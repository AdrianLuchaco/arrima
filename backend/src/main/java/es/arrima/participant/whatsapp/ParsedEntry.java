package es.arrima.participant.whatsapp;

/**
 * One person read from the WhatsApp list.
 *
 * @param listNumber      the number they had on the list (null if it could not be read)
 * @param struckThrough   the name was ~struck through~ in WhatsApp: usually someone who signed out
 * @param beforeListStart the line came before the "1.": probably part of the header (e.g. a date)
 */
public record ParsedEntry(Integer listNumber, String name, boolean struckThrough, boolean beforeListStart) {
}
