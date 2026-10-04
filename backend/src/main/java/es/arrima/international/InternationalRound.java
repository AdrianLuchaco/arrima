package es.arrima.international;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A round of a group: round 1 with every team of the group, and tie-break rounds (2, 3...) with the
 * tied teams only. Its teams never change once created.
 */
@Entity
@Table(name = "international_round")
public class InternationalRound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupId;

    private int roundNumber;

    @ElementCollection
    @CollectionTable(name = "international_round_team", joinColumns = @JoinColumn(name = "round_id"))
    @OrderBy("playOrder")
    private List<RoundTeam> teams = new ArrayList<>();

    protected InternationalRound() {
        // for JPA
    }

    /** @param teamIdsInOrder in team-number order, which is their order of play */
    InternationalRound(long groupId, int roundNumber, List<Long> teamIdsInOrder) {
        this.groupId = groupId;
        this.roundNumber = roundNumber;
        for (int i = 0; i < teamIdsInOrder.size(); i++) {
            teams.add(new RoundTeam(teamIdsInOrder.get(i), i + 1));
        }
    }

    boolean hasTeam(long teamId) {
        return teams.stream().anyMatch(team -> team.teamId() == teamId);
    }

    public Set<Long> teamIdSet() {
        return teams.stream().map(RoundTeam::teamId).collect(Collectors.toSet());
    }

    public Long getId() {
        return id;
    }

    public long getGroupId() {
        return groupId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public List<RoundTeam> getTeams() {
        return List.copyOf(teams);
    }
}
