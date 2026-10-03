package es.arrima.club;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "club")
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String logoPath;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "roundsCount", column = @Column(name = "default_rounds")),
            @AttributeOverride(name = "prizeCount", column = @Column(name = "default_prize_count"))
    })
    private MeleeSettings meleeDefaults;

    @Embedded
    private ScoringTable scoring;

    private Instant createdAt;

    private Instant updatedAt;

    protected Club() {
        // for JPA
    }

    public Club(String name, Instant now) {
        this.name = name;
        this.meleeDefaults = MeleeSettings.DEFAULT;
        this.scoring = ScoringTable.DEFAULT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String name, MeleeSettings meleeDefaults, ScoringTable scoring, Instant now) {
        this.name = name;
        this.meleeDefaults = meleeDefaults;
        this.scoring = scoring;
        this.updatedAt = now;
    }

    public void changeLogo(String logoPath, Instant now) {
        this.logoPath = logoPath;
        this.updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public MeleeSettings getMeleeDefaults() {
        return meleeDefaults;
    }

    public ScoringTable getScoring() {
        return scoring;
    }
}
