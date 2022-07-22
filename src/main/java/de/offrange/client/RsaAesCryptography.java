package de.offrange.client;

import java.security.*;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Class that is used to encrypt data with two supported algorithms for encryption - RSA and AES and
 * one algorithm for decryption AES.
 */
public class RsaAesCryptography {

    /**
     * Encrypts data using a {@link Key}. The key algorithm is used for encryption. Supported algorithms are RSA and AES.
     * @param data the data to encrypt.
     * @param key the key for the encryption.
     * @return the encrypted data.
     * @throws GeneralSecurityException if an error occurs while encrypting
     */
    public static byte[] encrypt(byte[] data, Key key) throws GeneralSecurityException {
        if(!(key.getAlgorithm().equals("RSA") || key.getAlgorithm().equals("AES")))
            throw new KeyException("Only RSA and AES are supported for encryption.");

        Cipher cipher = Cipher.getInstance(key.getAlgorithm().equals("RSA") ? "RSA/ECB/OAEPWithSHA-512AndMGF1Padding" : key.getAlgorithm());
        if(key.getAlgorithm().equals("RSA")){
            OAEPParameterSpec oaepParameterSpec = new OAEPParameterSpec("SHA-512", "MGF1",
                    MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.ENCRYPT_MODE, key, oaepParameterSpec);
        }else
            cipher.init(Cipher.ENCRYPT_MODE, key);

        return cipher.doFinal(data);
    }

    /**
     * Decrypts data using a {@link Key}. Only AES decryption is supported for decryption.
     * @param data the data to encrypt.
     * @param key the key for the encryption.
     * @return the decrypted data.
     * @throws GeneralSecurityException if an error occurs while decrypting
     */
    public static byte[] decryptAes(byte[] data, Key key) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }
}
