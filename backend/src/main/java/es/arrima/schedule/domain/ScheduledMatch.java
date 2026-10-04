package es.arrima.schedule.domain;

/** @param court court number from 1, or null if the match waits for a free court ("en espera") */
public record ScheduledMatch(Pairing pairing, Integer court) {
}
