package es.arrima.push;

import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

/**
 * The server's own key pair for Web Push (VAPID, RFC 8292). Browsers subscribe with the public key
 * and only accept notifications signed with the private one, so nobody else can notify our users.
 * Both travel base64url-encoded in raw form, as browsers and the usual generators write them.
 */
public record VapidKeys(ECPublicKey publicKey, ECPrivateKey privateKey) {

    public static VapidKeys parse(String publicKeyBase64Url, String privateKeyBase64Url) {
        ECPublicKey publicKey = P256.publicKey(decode(publicKeyBase64Url));
        ECPrivateKey privateKey = P256.privateKey(decode(privateKeyBase64Url));
        return new VapidKeys(publicKey, privateKey);
    }

    public static VapidKeys generate() {
        KeyPair pair = P256.newKeyPair();
        return new VapidKeys((ECPublicKey) pair.getPublic(), (ECPrivateKey) pair.getPrivate());
    }

    /** What the browser needs to subscribe ("applicationServerKey"). */
    public String publicKeyBase64Url() {
        return encode(P256.encode(publicKey));
    }

    public String privateKeyBase64Url() {
        return encode(P256.encode(privateKey));
    }

    private static byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value.strip());
    }

    private static String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
