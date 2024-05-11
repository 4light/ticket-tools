package test.ticket.tickettools.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class EncDecUtil {
    public static String doAES(String data,String key){
        try {
            // 创建AES加密器
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            // 进行加密
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());

            // 将加密后的数据进行Base64编码
            String encryptedData = Base64.getEncoder().encodeToString(encryptedBytes);
            return encryptedData;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
    public static String decData(String pKey,String encData){
        try {
            Security.addProvider(new BouncyCastleProvider());
            // 设置私钥
            String privateKeyString = "MIIEpAIBAAKCAQEAobg5iEask2geDtxuvqFnan3O4PIG1mFRP5U/rNFa1B9DQ8aHXLgG4LDllTRxRYNIUDXfKCUxaHcfZDSBIfIHzEvp7oE+5O+ynbczH2SjXJfN8VGA5NZPGbvKQjwdkRRgoQdcEVZUYe7FWxeFSIRNU2N20dfVjpv7rtlMTKTBBaaFpnOb6oZEiTwK2mAuHTD1GfQ2RAbGZs59gQBG2MDycSf6i27QqbW0I4AOwm7mIhWe42Y+//aQRzF3qc6AMNATVyByWEIK8jULyRuR0M5B+f2lU1vWBf21KnN4mlMdDSHRliPfWRrXrvKrAc2FymE4/DiZ01VB3uy3M4TV/rgdywIDAQABAoIBABi6Bs3v5G4rcsEZ8jLikeHl945MY0A/JAGhS9mcLxOU7h98SPEj0CVl1syf9pvGzXU6L3M/cJUE9b9ICeCLVabmio+loly1y60yuDXaGOJM8beumxMiM3j/ThcfgvPOVlH4wpqCBSfuLq3VZFMoq3wPDrlaE3SZI/vhjLmBTWQUCeuUgPDHsyztsVp9hiJfdlXlLAt7a5Aw63A5mYvyqIi0DcdVhVti6kZAdhkvbA2RSQyTajkgxoXcpe8PXDMbgT9WV2gyTJz832/nDw3HuYkwMS89CUoUa2P5HuBPErxca/0Ydv7V7xRfyNF+naUEIpTYVsOcbhyQFJSsf1ylwpkCgYEA41Ziuelp9rFnHz//ZSHeEm8SL8KTmRbnFTNVWr45ZsSfHzIVOUdrnKnlz+yu5vcMwytSBFwuBlgSBHWAFH+42Su0J4eI06/77pwriGgESjZzkvtsT3LI7YWvctNMo4XqERa3ouTFIjkVFfsXHEnW7Zx9+amQvemt7DuZnpRfLtkCgYEAthvwHhKEhtluizUIpICacGJDenZgO3ZxZk6WHpwtrqo7qkZfif/VrgkixpqlIJjx0vj4jafbtVyNPwSZ9VfcRFq44LcOPkOg6ngf2ao42WcW5LebGlOmr0lk82+gzoojkrqN0PE9LCjbD47r4TcFD6GeTaVFzmquzYEBYMyC00MCgYAbyfR5e0G7qQXM+Rqz9wbZRAB6HBPEs9r9aW/2jqgfmstEme+kN8m8tbvkxa6/htVligcVh1sM5XkWWHKWjuI+kawM5PFhxvJJwYdEvko/9BX+koMz1vkep6fBpniIyJbLDfbWj5ZVT5r3O+EgURpXozh26zZJMKZU6RgnHUXhSQKBgQCaHFh+yoL2r2i6S74toFuSAcZDC4xypdBfmN+3tcl/B7cIaReO7D9DUZ3pXpOhW21Cccm97zCicVli3B0CIEFaY0ATgzZ9gLPb2J5zkHcdm/0mvy52ABaOPlk9HdmDECn8kP1UteJjzYtcxkFdzTbuPIKACP5jKasWZDbrWQbZiwKBgQCIgGepO63kS+wSV/iswXTnWx8ZIYNQ+JrsDyQyJTLhfTGYhvwe0+DCbnt96KL0TZ6HVgT5RYhlzcD4hZJQNqfdjXHP9Bredt7AehiiMkU3B+O81JBM0mroR0vhtyqId37PhFHiRtErxgbfOEKKPAVbWXaAPrmc4DkDqNFdedZIzw==";
            byte[] privateKeyBytes =Base64.getDecoder().decode(privateKeyString);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
            // 解密数据
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] encryptedDataBytes = Base64.getDecoder().decode(pKey);
            byte[] decryptedDataBytes = cipher.doFinal(encryptedDataBytes);
            String decryptedData = new String(decryptedDataBytes);
            byte[] w = DatatypeConverter.parseHexBinary(encData);
            String k = Base64.getEncoder().encodeToString(w);

            // Decrypt
            byte[] encryptedBytes = Base64.getDecoder().decode(k);
            byte[] keyBytes = decryptedData.getBytes("UTF-8");
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
            IvParameterSpec ivParameterSpec = new IvParameterSpec("2584532147856215".getBytes("UTF-8"));

            Cipher decCipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            decCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
            byte[] decryptedBytes = decCipher.doFinal(encryptedBytes);
            String decryptedString = new String(decryptedBytes, "UTF-8");
            return decryptedString;
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }
}
