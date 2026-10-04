package es.arrima.push;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECFieldFp;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.EllipticCurve;
import java.util.Arrays;

/**
 * The P-256 curve keys of Web Push in the raw form browsers use: a public key is the 65-byte
 * "uncompressed point" (0x04, then x and y), a private key is 32 bytes. Only the JDK's crypto.
 */
final class P256 {

    static final int PUBLIC_KEY_LENGTH = 65;
    private static final int COORDINATE_LENGTH = 32;
    private static final ECParameterSpec PARAMETERS = parameters();

    private P256() {
    }

    /**
     * Rejects anything that is not a point of the curve: a key sent by a browser comes from outside,
     * and a point off the curve is the basis of known attacks on ECDH.
     */
    static ECPublicKey publicKey(byte[] uncompressed) {
        if (uncompressed.length != PUBLIC_KEY_LENGTH || uncompressed[0] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed P-256 public key");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1, 1 + COORDINATE_LENGTH));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(uncompressed, 1 + COORDINATE_LENGTH, PUBLIC_KEY_LENGTH));
        requireOnCurve(x, y);
        try {
            return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(new ECPoint(x, y), PARAMETERS));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Not a valid P-256 public key", e);
        }
    }

    static ECPrivateKey privateKey(byte[] raw) {
        if (raw.length != COORDINATE_LENGTH) {
            throw new IllegalArgumentException("A P-256 private key has 32 bytes");
        }
        try {
            return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new ECPrivateKeySpec(new BigInteger(1, raw), PARAMETERS));
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Not a valid P-256 private key", e);
        }
    }

    static byte[] encode(ECPublicKey key) {
        byte[] encoded = new byte[PUBLIC_KEY_LENGTH];
        encoded[0] = 0x04;
        copyFixed(key.getW().getAffineX(), encoded, 1);
        copyFixed(key.getW().getAffineY(), encoded, 1 + COORDINATE_LENGTH);
        return encoded;
    }

    static byte[] encode(ECPrivateKey key) {
        byte[] encoded = new byte[COORDINATE_LENGTH];
        copyFixed(key.getS(), encoded, 0);
        return encoded;
    }

    static KeyPair newKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(PARAMETERS);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available", e);
        }
    }

    /** y² = x³ + ax + b (mod p), with both coordinates in the field. */
    private static void requireOnCurve(BigInteger x, BigInteger y) {
        EllipticCurve curve = PARAMETERS.getCurve();
        BigInteger p = ((ECFieldFp) curve.getField()).getP();
        if (x.compareTo(p) >= 0 || y.compareTo(p) >= 0) {
            throw new IllegalArgumentException("Point outside the P-256 field");
        }
        BigInteger left = y.multiply(y).mod(p);
        BigInteger right = x.pow(3).add(curve.getA().multiply(x)).add(curve.getB()).mod(p);
        if (!left.equals(right)) {
            throw new IllegalArgumentException("Point not on the P-256 curve");
        }
    }

    /** Big-endian, left-padded with zeros to 32 bytes (BigInteger may give 31 or 33). */
    private static void copyFixed(BigInteger value, byte[] target, int offset) {
        byte[] bytes = value.toByteArray();
        int length = Math.min(bytes.length, COORDINATE_LENGTH);
        System.arraycopy(bytes, bytes.length - length, target, offset + COORDINATE_LENGTH - length, length);
    }

    private static ECParameterSpec parameters() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available", e);
        }
    }
}
