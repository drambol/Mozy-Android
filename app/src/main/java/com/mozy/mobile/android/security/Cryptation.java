/* Copyright 2009 Tactel AB, Sweden. All rights reserved.
 *                                   _           _
 *       _                 _        | |         | |
 *     _| |_ _____  ____ _| |_ _____| |    _____| |__
 *    (_   _|____ |/ ___|_   _) ___ | |   (____ |  _ \
 *      | |_/ ___ ( (___  | |_| ____| |   / ___ | |_) )
 *       \__)_____|\____)  \__)_____)\_)  \_____|____/
 *
 */

package com.mozy.mobile.android.security;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.mozy.mobile.android.utils.LogUtil;

/**
 * Encrypts/Decrypts strings based on seed.
 *
 * @author Daniel Olofsson (daniel.olofsson@tactel.se)
 *
 */
public class Cryptation {

    private static Object lock = new Object();

    public static final int DEFAULT_HASH_LENGTH = 32 * 1024;
    
    /**
     * Encrypts string.
     * 
     * @param seed Seed key for generating encryption. (Must match on decryption)
     * @param str String to encrypt.
     * @return encrypted string.
     * @throws Exception
     */
    public static String encrypt(String seed, String str) throws Exception {
        synchronized (lock) {
            SecretKeySpec skeySpec = new SecretKeySpec(getRawKey(seed.getBytes()), "AES");  
            Cipher cipher = Cipher.getInstance("AES");  
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);  
            byte[] encrypted = cipher.doFinal(str.getBytes());
            return byteArrayToHexString(encrypted);
        }
    }

    /**
     * Decrypts hash.
     * 
     * @param seed Seed key for generating decryption. (Must be identical to encryption)
     * @param hash String to decrypt.
     * @return decrypted string.
     * @throws Exception
     */
    public static String decrypt(String seed, String hash) throws Exception {
        synchronized (lock) {
            SecretKeySpec skeySpec = new SecretKeySpec(getRawKey(seed.getBytes()), "AES");  
            Cipher cipher = Cipher.getInstance("AES");  
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);  
            String decrypted = new String(cipher.doFinal(getByteFromHex(hash)));  
            return decrypted;
        }
    }

    public static String calculateHash(byte[] bytes, int length) {
        try {  
            // Create MD5 Hash  
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");  
            digest.update(bytes, 0, length);
            byte messageDigest[] = digest.digest();  

            StringBuffer hexString = new StringBuffer();  
            for (int i=0; i<messageDigest.length; i++)  
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));  
            return hexString.toString();  

        } catch (Exception e) {  
            LogUtil.exception("Cryptation", "calculateHash", e);
        }
        return null;
    }

    public static String calculateHash(File file, long length) {
        try {  
            // Create MD5 Hash  
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");  
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));

            int theLength = 0;
            int index = 0;
            byte[] bytes = new byte[1024];
            while ((theLength = in.read(bytes)) != -1 && index < length) {
                if (index + theLength > length) {
                    theLength = (int)length - index;
                }
                digest.update(bytes, 0, theLength);
                index += theLength;
            }
            in.close();
            byte messageDigest[] = digest.digest();  

            StringBuffer hexString = new StringBuffer();  
            for (int i=0; i<messageDigest.length; i++)  
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));  
            return hexString.toString();  

        } catch (Exception e) {  
            LogUtil.exception("Cryptation", "calculateHash", e);
        }
        return null;
    }

    public static byte[] getByteFromHex(String str) {
        int len = str.length()/2;  
        byte[] result = new byte[len];  
        for (int i = 0; i < len; i++)  
            result[i] = Integer.valueOf(str.substring(2*i, 2*i+2), 16).byteValue();  
        return result; 
    }


    private static byte[] getRawKey(byte[] seed) throws Exception {  
        KeyGenerator kgen = KeyGenerator.getInstance("AES");  
        SecureRandom sr = null;
        
        final int JELLY_BEAN_4_2 = 17;
        
        if (android.os.Build.VERSION.SDK_INT >= JELLY_BEAN_4_2) {
            sr = SecureRandom.getInstance("SHA1PRNG", "Crypto");
        } else {
            sr = SecureRandom.getInstance("SHA1PRNG");
        } 
        sr.setSeed(seed);  
        kgen.init(128, sr); 
        SecretKey skey = kgen.generateKey();  
        byte[] raw = skey.getEncoded();  
        return raw;  
    }

    public static String byteArrayToHexString(byte[] b){
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++){
            int v = b[i] & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase(Locale.getDefault());
    }
}
