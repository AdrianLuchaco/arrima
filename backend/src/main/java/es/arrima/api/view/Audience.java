package es.arrima.api.view;

/**
 * Who the view is for. Going back a phase keeps later work (teams, schedule, Internacional...) so it
 * can be reconciled when moving forward again; the admin sees it to decide, spectators only see what
 * belongs to the phases already reached.
 */
public enum Audience {
    ADMIN,
    PUBLIC
}
