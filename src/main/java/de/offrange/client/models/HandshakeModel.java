package de.offrange.client.models;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

/**
 * Model class that implements the {@link IModel} interface. This model is used for the handshake between the client and the server.
 */
public class HandshakeModel implements IModel {

    private RSAKey rsaKeyInformation;
    private byte[] aesKey;

    /**
     * @return {@link RSAKey} that contains information for the public RSA key.
     */
    public RSAKey getRsaKeyInformation() {
        return rsaKeyInformation;
    }

    /**
     * @return {@code byte[]} that represents the AES key.
     */
    public byte[] getAesKey() {
        return aesKey;
    }

    /**
     * Sets the encoded AES key so the server knows how to encrypt.
     * @param aesKey the encoded AES key.
     */
    public void setAesKey(byte[] aesKey) {
        this.aesKey = aesKey;
    }

    /**
     * Class that contains all necessary information for the public RSA key such as {@code exponent} and {@code modulus}.
     */
    public static class RSAKey{
        private byte[] exponent;
        private byte[] modulus;

        /**
         * @return a {@link PublicKey} composed of the exponent sent by the server and the modulus sent by the server.
         */
        public PublicKey toPublicKey(){
            try {
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent)));
            } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
