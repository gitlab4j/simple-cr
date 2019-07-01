package org.gitlab4j.simplecr.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class HashUtils {

    public static final int SHORT_HASH = 10;
    public static final int DEFAULT_HASH = -1;

    /*
     * IMPORTANT: The following properties are used to sign the URLs and cannot be changed.
     * If changed previous links will no longer function.
     */
    private static final String MD5_SECRET = "gitlab-rocks";
    private static final int ENCODE_BASE = 16;

    /**
     * Builds a signature hash for the specified parts
     *
     * @param parts a list of Objects used to build a hash signature
     * @return the signature hash for the specified parts
     */
    public static String makeHash(int hashLength, Object... parts) {

        StringBuffer hash = new StringBuffer();
        for (Object part : parts) {
            hash.append(part);
        }

        hash.append(MD5_SECRET);
        String md5Hash = getMD5Hash(hash.toString());
        if (hashLength < 1) {
            return (md5Hash);
        } else {
            return (hashLength > md5Hash.length() ? md5Hash : md5Hash.substring(0, hashLength));
        }
    }

    /**
     * Returns true if the hash matches the hash of the provided parts.
     *
     * @param hash the hash signature to compare
     * @param parts Object parts to compare to the provided hash
     * @return true if the hash matches the hash of the provided parts, otherwise returns false
     */
    public static boolean isValidHash(String hash, int hashLength, Object... parts) {

        if (parts == null) {
            return (false);
        }

        String partsHash = makeHash(hashLength, parts);
        return (partsHash.equalsIgnoreCase(hash));
    }

    /**
     * Gets a MD5 hash for the specified source string.
     *
     * @param source the source string to create a signature for
     * @return MD5 hash for the specified source string
     */
    public static final String getMD5Hash(String source) {

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5", "SUN");
        } catch (NoSuchAlgorithmException e) {
            return (null);
        } catch (NoSuchProviderException e) {
            return (null);
        }

        md.update(source.getBytes());
        byte[] bytes = md.digest();

        // convert the byte array to the ENCODE_BASE format with 0 padding
        StringBuilder hexBuffer = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {

            String hex = Integer.toString(0xff & bytes[i], ENCODE_BASE);
            if (hex.length() == 1) {
                hexBuffer.append('0');
            }

            hexBuffer.append(hex);
        }

        return (hexBuffer.toString());
    }
}
