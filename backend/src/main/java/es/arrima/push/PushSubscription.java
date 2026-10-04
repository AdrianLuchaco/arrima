package es.arrima.push;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** A phone that asked to be told when a round's time is up in this melee. */
@Entity
@Table(name = "push_subscription")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long meleeId;

    private String endpoint;

    private String p256dh;

    private String auth;

    @Enumerated(EnumType.STRING)
    private PushAudience audience;

    private Instant createdAt;

    protected PushSubscription() {
        // for JPA
    }

    PushSubscription(long meleeId, NewPushSubscription subscription, PushAudience audience, Instant now) {
        this.meleeId = meleeId;
        this.endpoint = subscription.endpoint();
        this.createdAt = now;
        renew(subscription, audience);
    }

    /** The same phone subscribing again (new keys); an admin's phone stays an admin's phone. */
    void renew(NewPushSubscription subscription, PushAudience audience) {
        this.p256dh = subscription.p256dh();
        this.auth = subscription.auth();
        if (this.audience != PushAudience.ADMIN) {
            this.audience = audience;
        }
    }

    public Long getId() {
        return id;
    }

    public long getMeleeId() {
        return meleeId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dh() {
        return p256dh;
    }

    public String getAuth() {
        return auth;
    }

    public PushAudience getAudience() {
        return audience;
    }
}
