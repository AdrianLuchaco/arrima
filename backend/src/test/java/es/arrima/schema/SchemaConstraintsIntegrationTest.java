package es.arrima.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import es.arrima.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * The V1 migration encodes some melee rules as constraints, as a safety net under the domain code.
 * These tests prove the database rejects the forbidden cases. Each test rolls back its data.
 */
@IntegrationTest
@Transactional
class SchemaConstraintsIntegrationTest {

    @Autowired
    private JdbcClient jdbc;

    private long meleeId;
    private long team1;
    private long team2;
    private long team3;
    private long team4;

    @BeforeEach
    void createMeleeWithFourTeams() {
        long clubId = insertClub();
        meleeId = insertMelee(clubId);
        team1 = insertTeam(1);
        team2 = insertTeam(2);
        team3 = insertTeam(3);
        team4 = insertTeam(4);
    }

    @Test
    void twoTeamsCannotMeetTwice() {
        insertMatchup(1, team1, team2, 1);

        assertThatThrownBy(() -> insertMatchup(2, team1, team2, 2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void matchupTeamsMustBeStoredInIdOrder() {
        assertThatThrownBy(() -> insertMatchup(1, team2, team1, 1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void winnerMustBeOneOfTheTwoTeams() {
        long matchupId = insertMatchup(1, team1, team2, 1);

        assertThatThrownBy(() -> jdbc.sql("update matchup set winner_team_id = ? where id = ?")
                .params(team3, matchupId)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aMatchupThatWaitedCanUseACourtAlreadyUsedInTheSameRound() {
        insertMatchup(1, team1, team2, 1);

        assertThatCode(() -> insertMatchup(1, team3, team4, 1)).doesNotThrowAnyException();
    }

    @Test
    void aTeamRestsAtMostOnce() {
        insertBye(1, team1);

        assertThatThrownBy(() -> insertBye(2, team1))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyOneTeamRestsPerRound() {
        insertBye(1, team1);

        assertThatThrownBy(() -> insertBye(1, team2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void throwOutcomeMustMatchItsKind() {
        long roundId = insertInternationalRoundWith(team1);

        assertThatThrownBy(() -> insertThrow(roundId, team1, "POINTING", "CARREAU"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void onlyTeamsTakingPartInTheRoundCanThrow() {
        long roundId = insertInternationalRoundWith(team1);

        assertThatThrownBy(() -> insertThrow(roundId, team2, "POINTING", "ON_JACK"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void adminEmailMustBeStoredInLowerCase() {
        long clubId = insertClub();

        assertThatThrownBy(() -> jdbc.sql("""
                        insert into club_admin (club_id, email, password_hash) values (?, 'Paqui@Example.com', 'x')""")
                .param(clubId)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aPaymentIsUnmarkedPaidOrUnpaid() {
        assertThatThrownBy(() -> jdbc.sql("""
                        insert into participant (melee_id, display_name, status, payment_status)
                        values (?, 'Manuel', 'ACTIVE', 'MAYBE')""")
                .param(meleeId)
                .update())
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void everyApplicationTableHasRowLevelSecurityEnabled() {
        var tablesWithoutRls = jdbc.sql("""
                        select tablename from pg_tables
                        where schemaname = 'arrima' and not rowsecurity and tablename <> 'flyway_schema_history'""")
                .query(String.class)
                .list();

        assertThat(tablesWithoutRls).isEmpty();
    }

    private long insertClub() {
        return jdbc.sql("""
                        insert into club (name, court_count, default_rounds, default_prize_count,
                            points_pointing_out, points_pointing_big_circle, points_pointing_small_circle,
                            points_pointing_near_jack, points_pointing_on_jack, points_shooting_miss,
                            points_shooting_hit, points_shooting_hit_out, points_shooting_carreau)
                        values ('Club de prueba', 10, 3, 5, 0, 1, 2, 3, 5, 0, 1, 2, 5)
                        returning id""")
                .query(Long.class)
                .single();
    }

    private long insertMelee(long clubId) {
        return jdbc.sql("""
                        insert into melee (club_id, played_on, format, team_size, status, public_code,
                            rounds_count, prize_count, court_count, entry_fee_cents, match_minutes,
                            points_pointing_out, points_pointing_big_circle, points_pointing_small_circle,
                            points_pointing_near_jack, points_pointing_on_jack, points_shooting_miss,
                            points_shooting_hit, points_shooting_hit_out, points_shooting_carreau)
                        values (?, current_date, 'CLASSIC', 2, 'MATCHES', ?, 3, 5, 10, 0, 45, 0, 1, 2, 3, 5, 0, 1, 2, 5)
                        returning id""")
                .params(clubId, "TEST" + clubId)
                .query(Long.class)
                .single();
    }

    private long insertTeam(int number) {
        return jdbc.sql("insert into team (melee_id, number) values (?, ?) returning id")
                .params(meleeId, number)
                .query(Long.class)
                .single();
    }

    private long insertMatchup(int round, long teamA, long teamB, Integer court) {
        return jdbc.sql("""
                        insert into matchup (melee_id, round_number, team_a_id, team_b_id, court_number)
                        values (?, ?, ?, ?, ?)
                        returning id""")
                .params(meleeId, round, teamA, teamB, court)
                .query(Long.class)
                .single();
    }

    private void insertBye(int round, long teamId) {
        jdbc.sql("insert into bye (melee_id, round_number, team_id) values (?, ?, ?)")
                .params(meleeId, round, teamId)
                .update();
    }

    private long insertInternationalRoundWith(long teamId) {
        long groupId = jdbc.sql("""
                        insert into international_group
                            (melee_id, play_order, wins, best_prize_position, worst_prize_position, status)
                        values (?, 1, 3, 1, 3, 'IN_PROGRESS')
                        returning id""")
                .param(meleeId)
                .query(Long.class)
                .single();
        long roundId = jdbc.sql("insert into international_round (group_id, round_number) values (?, 1) returning id")
                .param(groupId)
                .query(Long.class)
                .single();
        jdbc.sql("insert into international_round_team (round_id, team_id, play_order) values (?, ?, 1)")
                .params(roundId, teamId)
                .update();
        return roundId;
    }

    private void insertThrow(long roundId, long teamId, String kind, String outcome) {
        jdbc.sql("""
                        insert into ball_throw (round_id, team_id, kind, ball_number, outcome, points)
                        values (?, ?, ?, 1, ?, 0)""")
                .params(roundId, teamId, kind, outcome)
                .update();
    }
}
