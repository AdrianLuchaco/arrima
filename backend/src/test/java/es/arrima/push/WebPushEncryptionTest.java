package es.arrima.push;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebPushEncryptionTest {

    /**
     * The worked example of RFC 8291, section 5. These keys and secrets are published in the RFC for
     * everyone to test with, so the secret scanner is told to let them through (gitleaks:allow).
     */
    private static final String PLAINTEXT = "When I grow up, I want to be a watermelon";
    private static final String AS_PUBLIC = "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8";
    private static final String AS_PRIVATE = "yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw"; // gitleaks:allow
    private static final String UA_PUBLIC = "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4";
    private static final String AUTH_SECRET = "BTBZMqHH6r4Tts7J_aSIgg"; // gitleaks:allow
    private static final String SALT = "DGv6ra1nlYgDCS1FRnbzlw";
    private static final String MESSAGE = "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN";

    @Test
    void reproducesTheExampleOfTheRfc() {
        KeyPair asKeys = new KeyPair(P256.publicKey(b64(AS_PUBLIC)), P256.privateKey(b64(AS_PRIVATE)));

        byte[] message = WebPushEncryption.encrypt(PLAINTEXT.getBytes(StandardCharsets.UTF_8),
                P256.publicKey(b64(UA_PUBLIC)), b64(AUTH_SECRET), asKeys, b64(SALT));

        assertThat(Base64.getUrlEncoder().withoutPadding().encodeToString(message)).isEqualTo(MESSAGE);
    }

    @Test
    void theBrowserCanReadWhatWeSend() throws Exception {
        KeyPair browser = P256.newKeyPair();
        byte[] authSecret = new byte[16];
        new java.security.SecureRandom().nextBytes(authSecret);

        byte[] message = WebPushEncryption.encrypt("¡Tiempo!".getBytes(StandardCharsets.UTF_8),
                (ECPublicKey) browser.getPublic(), authSecret);

        assertThat(new String(decryptAsTheBrowser(message, browser, authSecret), StandardCharsets.UTF_8)).isEqualTo("¡Tiempo!");
    }

    @Test
    void refusesKeysThatAreNotOnTheCurve() {
        byte[] offCurve = b64(UA_PUBLIC);
        offCurve[64] ^= 1;

        assertThatThrownBy(() -> P256.publicKey(offCurve)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> P256.publicKey(new byte[33])).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void keysSurviveTheRawEncoding() {
        KeyPair keys = P256.newKeyPair();

        assertThat(P256.publicKey(P256.encode((ECPublicKey) keys.getPublic())).getW())
                .isEqualTo(((ECPublicKey) keys.getPublic()).getW());
        assertThat(P256.privateKey(P256.encode((ECPrivateKey) keys.getPrivate())).getS())
                .isEqualTo(((ECPrivateKey) keys.getPrivate()).getS());
    }

    /** The receiving side of RFC 8291, written independently of the code under test. */
    private static byte[] decryptAsTheBrowser(byte[] message, KeyPair browser, byte[] authSecret) throws Exception {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        byte[] salt = new byte[16];
        buffer.get(salt);
        buffer.getInt();
        byte[] serverKey = new byte[buffer.get()];
        buffer.get(serverKey);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(browser.getPrivate());
        agreement.doPhase(P256.publicKey(serverKey), true);
        byte[] ecdh = agreement.generateSecret();
        byte[] uaPublic = P256.encode((ECPublicKey) browser.getPublic());
        byte[] ikm = hmac(hmac(authSecret, ecdh), join("WebPush: info\0".getBytes(), uaPublic, serverKey, new byte[] {1}));
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, join("Content-Encoding: aes128gcm\0".getBytes(), new byte[] {1})), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, join("Content-Encoding: nonce\0".getBytes(), new byte[] {1})), 12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(cek, "AES"), new GCMParameterSpec(128, nonce));
        byte[] padded = cipher.doFinal(ciphertext);
        assertThat(padded[padded.length - 1]).isEqualTo((byte) 2);
        return Arrays.copyOf(padded, padded.length - 1);
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] join(byte[]... parts) {
        int length = Arrays.stream(parts).mapToInt(part -> part.length).sum();
        ByteBuffer buffer = ByteBuffer.allocate(length);
        Arrays.stream(parts).forEach(buffer::put);
        return buffer.array();
    }

    private static byte[] b64(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
