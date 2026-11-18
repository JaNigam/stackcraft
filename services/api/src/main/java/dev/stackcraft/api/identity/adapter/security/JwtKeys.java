package dev.stackcraft.api.identity.adapter.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * The RSA key that signs access tokens. Production passes a PKCS#8 PEM in {@code STACKCRAFT_AUTH_JWT_PRIVATE_KEY};
 * without one (dev, tests) a fresh 2048-bit key is generated at start-up, which signs everyone out on restart.
 */
record JwtKeys(RSAPublicKey publicKey, RSAPrivateCrtKey privateKey) {

    private static final Logger log = LoggerFactory.getLogger(JwtKeys.class);

    static JwtKeys from(String pem) {
        try {
            if (pem == null || pem.isBlank()) {
                log.warn("No STACKCRAFT_AUTH_JWT_PRIVATE_KEY: generating a throwaway signing key (dev only)");
                KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
                gen.initialize(2048);
                KeyPair pair = gen.generateKeyPair();
                return new JwtKeys((RSAPublicKey) pair.getPublic(), (RSAPrivateCrtKey) pair.getPrivate());
            }
            String base64 = pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey priv = (RSAPrivateCrtKey) kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
            RSAPublicKey pub = (RSAPublicKey) kf.generatePublic(new RSAPublicKeySpec(priv.getModulus(), priv.getPublicExponent()));
            return new JwtKeys(pub, priv);
        } catch (NoSuchAlgorithmException | java.security.spec.InvalidKeySpecException | ClassCastException e) {
            throw new IllegalStateException("Invalid JWT signing key: expected an RSA PKCS#8 PEM", e);
        }
    }
}
