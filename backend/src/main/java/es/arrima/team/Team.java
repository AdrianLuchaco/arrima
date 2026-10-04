package es.arrima.team;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;

/** A team of a melee, numbered from 1. Its members are participant ids (table team_member). */
@Entity
@Table(name = "team")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private int number;

    @ElementCollection
    @CollectionTable(name = "team_member", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "participant_id")
    private Set<Long> memberIds = new HashSet<>();

    protected Team() {
        // for JPA
    }

    Team(long meleeId, int number, Set<Long> memberIds) {
        this.meleeId = meleeId;
        this.number = number;
        this.memberIds = new HashSet<>(memberIds);
    }

    boolean hasMember(long participantId) {
        return memberIds.contains(participantId);
    }

    void replaceMember(long leaving, long joining) {
        memberIds.remove(leaving);
        memberIds.add(joining);
    }

    public Long getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public Set<Long> getMemberIds() {
        return Set.copyOf(memberIds);
    }
}
