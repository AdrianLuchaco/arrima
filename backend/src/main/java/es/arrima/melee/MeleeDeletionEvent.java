package es.arrima.melee;

/**
 * Published inside the transaction, just before a melee is deleted. The database cascades take care
 * of the rows; listeners clean up what lives elsewhere (photos in Supabase Storage).
 */
public record MeleeDeletionEvent(long meleeId) {
}
