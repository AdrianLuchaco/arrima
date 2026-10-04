package es.arrima.prize;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A photo of a team with its prize. The image itself is in the private storage bucket. */
@Entity
@Table(name = "prize_photo")
public class PrizePhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long prizeId;

    private String storagePath;

    private String contentType;

    private int sizeBytes;

    private Instant createdAt;

    protected PrizePhoto() {
        // for JPA
    }

    PrizePhoto(long prizeId, String storagePath, String contentType, int sizeBytes, Instant now) {
        this.prizeId = prizeId;
        this.storagePath = storagePath;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public long getPrizeId() {
        return prizeId;
    }

    public String getStoragePath() {
        return storagePath;
    }
}
