package es.arrima.participant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A person signed up for one melee, with the number they had on the WhatsApp list. */
@Entity
@Table(name = "participant")
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private Integer listNumber;

    private String displayName;

    @Enumerated(EnumType.STRING)
    private ParticipantStatus status;

    private Instant createdAt;

    protected Participant() {
        // for JPA
    }

    Participant(long meleeId, Integer listNumber, String displayName, Instant now) {
        this.meleeId = meleeId;
        this.listNumber = listNumber;
        this.displayName = displayName;
        this.status = ParticipantStatus.ACTIVE;
        this.createdAt = now;
    }

    void edit(Integer listNumber, String displayName) {
        this.listNumber = listNumber;
        this.displayName = displayName;
    }

    void withdraw() {
        this.status = ParticipantStatus.WITHDRAWN;
    }

    void reinstate() {
        this.status = ParticipantStatus.ACTIVE;
    }

    public boolean isActive() {
        return status == ParticipantStatus.ACTIVE;
    }

    public Long getId() {
        return id;
    }

    public long getMeleeId() {
        return meleeId;
    }

    public Integer getListNumber() {
        return listNumber;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ParticipantStatus getStatus() {
        return status;
    }
}
