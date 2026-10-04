package es.arrima.push;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Encrypts a notification for one browser, as RFC 8291 (Message Encryption for Web Push) asks, in
 * the "aes128gcm" format of RFC 8188. Only that browser can read it: the push service in between
 * (Google, Apple, Mozilla) just carries bytes. Each step is named as in the RFC, whose worked example
 * is the unit test.
 */
final class WebPushEncryption {

    /** RFC 8188 record size; a notification always fits in one record. */
    private static final int RECORD_SIZE = 4096;
    private static final int SALT_LENGTH = 16;
    private static final int TAG_LENGTH = 16;
    /** What fits in one record: record size minus the padding delimiter and the GCM tag. */
    static final int MAX_PLAINTEXT = RECORD_SIZE - 1 - TAG_LENGTH;
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushEncryption() {
    }

    static byte[] encrypt(byte[] plaintext, ECPublicKey uaPublic, byte[] authSecret) {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        // A new key pair for every message: nobody can link two notifications.
        return encrypt(plaintext, uaPublic, authSecret, P256.newKeyPair(), salt);
    }

    /** With the application server's keys and the salt given: deterministic, for the RFC's example. */
    static byte[] encrypt(byte[] plaintext, ECPublicKey uaPublic, byte[] authSecret, KeyPair asKeys, byte[] salt) {
        if (plaintext.length > MAX_PLAINTEXT) {
            throw new IllegalArgumentException("Notification too long: " + plaintext.length + " bytes");
        }
        byte[] uaPublicBytes = P256.encode(uaPublic);
        byte[] asPublicBytes = P256.encode((ECPublicKey) asKeys.getPublic());

        // RFC 8291 section 3.4: the shared secret, mixed with the browser's authentication secret.
        byte[] ecdhSecret = ecdh(asKeys.getPrivate(), uaPublic);
        byte[] prkKey = hmac(authSecret, ecdhSecret);
        byte[] keyInfo = concat(ascii("WebPush: info\0"), uaPublicBytes, asPublicBytes);
        byte[] ikm = hmac(prkKey, concat(keyInfo, new byte[] {1}));

        // RFC 8188 section 2.2/2.3: content encryption key and nonce.
        byte[] prk = hmac(salt, ikm);
        byte[] cek = Arrays.copyOf(hmac(prk, concat(ascii("Content-Encoding: aes128gcm\0"), new byte[] {1})), 16);
        byte[] nonce = Arrays.copyOf(hmac(prk, concat(ascii("Content-Encoding: nonce\0"), new byte[] {1})), 12);

        // A single, last record: the plaintext followed by the delimiter 0x02 and no padding.
        byte[] ciphertext = aesGcm(cek, nonce, concat(plaintext, new byte[] {2}));

        // Header: salt, record size, and the application server's public key as key id.
        ByteBuffer header = ByteBuffer.allocate(SALT_LENGTH + 4 + 1 + asPublicBytes.length)
                .put(salt).putInt(RECORD_SIZE).put((byte) asPublicBytes.length).put(asPublicBytes);
        return concat(header.array(), ciphertext);
    }

    private static byte[] ecdh(PrivateKey privateKey, ECPublicKey publicKey) {
        try {
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(privateKey);
            agreement.doPhase(publicKey, true);
            return agreement.generateSecret();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("ECDH failed", e);
        }
    }

    /** HMAC-SHA-256: both HKDF-Extract and, with a single 32-byte block, HKDF-Expand. */
    private static byte[] hmac(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 is not available", e);
        }
    }

    private static byte[] aesGcm(byte[] key, byte[] nonce, byte[] data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_LENGTH * 8, nonce));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM failed", e);
        }
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.US_ASCII);
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }
}
