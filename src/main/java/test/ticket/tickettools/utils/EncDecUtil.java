package test.ticket.tickettools.utils;

import cn.hutool.crypto.digest.MD5;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
            byte[] w = Hex.decodeHex(encData.toCharArray());
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
    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    public static String decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return encoded;
        }

        StringBuilder decoded = new StringBuilder();
        int[] buffer = new int[4];
        int bufferIndex = 0;

        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '=') {
                break;
            }
            buffer[bufferIndex++] = BASE64_CHARS.indexOf(c);
            if (bufferIndex == 4) {
                int b = (buffer[0] << 18) + (buffer[1] << 12) + (buffer[2] << 6) + buffer[3];
                decoded.append((char) ((b >> 16) & 0xFF));
                decoded.append((char) ((b >> 8) & 0xFF));
                decoded.append((char) (b & 0xFF));
                bufferIndex = 0;
            }
        }

        // Handle padding if present
        if (bufferIndex > 0) {
            int b = (buffer[0] << 18) + (buffer[1] << 12);
            decoded.append((char) ((b >> 16) & 0xFF));
            if (bufferIndex == 3) {
                b += (buffer[2] << 6);
                decoded.append((char) ((b >> 8) & 0xFF));
            }
        }

        return new String(decoded.toString().getBytes(), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) {
       String s= decode("W3siZGlzdGluY3RfaWQiOiIxNzYxMDc3MzI3MyIsImlkZW50aXRpZXMiOnsiJGlkZW50aXR5X21wX2lkIjoiMTcxMjQ2ODcyOTIxMC00MzYwMzgyLTBjZDhmMzQ5YWNiNDE0LTE4Nzk5NDM5IiwiJGlkZW50aXR5X2xvZ2luX2lkIjoiMTc2MTA3NzMyNzMiLCIkaWRlbnRpdHlfYW5vbnltb3VzX2lkIjoib3NQZk40bjF0WmJib1M0RTE5LWFqTEZPZ1g0OCIsIiRpZGVudGl0eV9tcF93eDllMjkyN2RkNTk1YjA0NzNfb3BlbmlkIjoib3NQZk40bjF0WmJib1M0RTE5LWFqTEZPZ1g0OCJ9LCJsaWIiOnsiJGxpYiI6Ik1pbmlQcm9ncmFtIiwiJGxpYl9tZXRob2QiOiJjb2RlIiwiJGxpYl92ZXJzaW9uIjoiMS4xNy41In0sInByb3BlcnRpZXMiOnsiJGxpYiI6Ik1pbmlQcm9ncmFtIiwiJGxpYl92ZXJzaW9uIjoiMS4xNy41IiwiJG5ldHdvcmtfdHlwZSI6IldJRkkiLCIkYnJhbmQiOiJJUEhPTkUiLCIkbWFudWZhY3R1cmVyIjoiaVBob25lIiwiJG1vZGVsIjoiaVBob25lIDEyIG1pbmk8aVBob25lMTMsMT4iLCIkc2NyZWVuX3dpZHRoIjozNzUsIiRzY3JlZW5faGVpZ2h0Ijo4MTIsIiRvcyI6ImlPUyIsIiRvc192ZXJzaW9uIjoiMTUuMCIsIiRtcF9jbGllbnRfYXBwX3ZlcnNpb24iOiI4LjAuNDkiLCIkbXBfY2xpZW50X2Jhc2ljX2xpYnJhcnlfdmVyc2lvbiI6IjMuNC4zIiwiJHRpbWV6b25lX29mZnNldCI6LTQ4MCwiJGFwcF9pZCI6Ind4OWUyOTI3ZGQ1OTViMDQ3MyIsIiRhcHBfdmVyc2lvbiI6IjMuNy4yNDA0MjIiLCJhbGxfcGxhdGZvcm1fdHlwZSI6IuWwj+eoi+W6jyIsImFsbF9pc19sb2dpbiI6dHJ1ZSwiJGxhdGVzdF9zY2VuZSI6Ind4LTEwODkiLCIkdXJsX3F1ZXJ5IjoiIiwiJHVybF9wYXRoIjoic3ViUGFnZXMvdGlja2V0L3BlcnNvbmFsL2luZGV4L2luZGV4IiwiJHRpdGxlIjoi6aKE57qm5pyN5Yqh57O757ufIiwiZXZlbnRfZHVyYXRpb24iOjkuNDY3LCIkaXNfZmlyc3RfZGF5IjpmYWxzZSwiJHJlZmVycmVyIjoicGFnZXMvaW5kZXgvaW5kZXgiLCIkcmVmZXJyZXJfdGl0bGUiOiLkuK3lm73lm73lrrbljZrnianppoYiLCIkdXJsIjoic3ViUGFnZXMvdGlja2V0L3BlcnNvbmFsL2luZGV4L2luZGV4In0sImxvZ2luX2lkIjoiMTc2MTA3NzMyNzMiLCJhbm9ueW1vdXNfaWQiOiJvc1BmTjRuMXRaYmJvUzRFMTktYWpMRk9nWDQ4IiwidHlwZSI6InRyYWNrIiwiZXZlbnQiOiIkTVBQYWdlTGVhdmUiLCJfdHJhY2tfaWQiOjQ0MDAxODc2NiwidGltZSI6MTcxNjM2ODc1ODc2NiwiX2ZsdXNoX3RpbWUiOjE3MTYzNjg3NjA1MDN9LHsiZGlzdGluY3RfaWQiOiIxNzYxMDc3MzI3MyIsImlkZW50aXRpZXMiOnsiJGlkZW50aXR5X21wX2lkIjoiMTcxMjQ2ODcyOTIxMC00MzYwMzgyLTBjZDhmMzQ5YWNiNDE0LTE4Nzk5NDM5IiwiJGlkZW50aXR5X2xvZ2luX2lkIjoiMTc2MTA3NzMyNzMiLCIkaWRlbnRpdHlfYW5vbnltb3VzX2lkIjoib3NQZk40bjF0WmJib1M0RTE5LWFqTEZPZ1g0OCIsIiRpZGVudGl0eV9tcF93eDllMjkyN2RkNTk1YjA0NzNfb3BlbmlkIjoib3NQZk40bjF0WmJib1M0RTE5LWFqTEZPZ1g0OCJ9LCJsaWIiOnsiJGxpYiI6Ik1pbmlQcm9ncmFtIiwiJGxpYl9tZXRob2QiOiJjb2RlIiwiJGxpYl92ZXJzaW9uIjoiMS4xNy41In0sInByb3BlcnRpZXMiOnsiJGxpYiI6Ik1pbmlQcm9ncmFtIiwiJGxpYl92ZXJzaW9uIjoiMS4xNy41IiwiJG5ldHdvcmtfdHlwZSI6IldJRkkiLCIkYnJhbmQiOiJJUEhPTkUiLCIkbWFudWZhY3R1cmVyIjoiaVBob25lIiwiJG1vZGVsIjoiaVBob25lIDEyIG1pbmk8aVBob25lMTMsMT4iLCIkc2NyZWVuX3dpZHRoIjozNzUsIiRzY3JlZW5faGVpZ2h0Ijo4MTIsIiRvcyI6ImlPUyIsIiRvc192ZXJzaW9uIjoiMTUuMCIsIiRtcF9jbGllbnRfYXBwX3ZlcnNpb24iOiI4LjAuNDkiLCIkbXBfY2xpZW50X2Jhc2ljX2xpYnJhcnlfdmVyc2lvbiI6IjMuNC4zIiwiJHRpbWV6b25lX29mZnNldCI6LTQ4MCwiJGFwcF9pZCI6Ind4OWUyOTI3ZGQ1OTViMDQ3MyIsIiRhcHBfdmVyc2lvbiI6IjMuNy4yNDA0MjIiLCJhbGxfcGxhdGZvcm1fdHlwZSI6IuWwj+eoi+W6jyIsImFsbF9pc19sb2dpbiI6dHJ1ZSwiJGxhdGVzdF9zY2VuZSI6Ind4LTEwODkiLCIkdXJsX3BhdGgiOiJzdWJQYWdlcy90aWNrZXQvcGVyc29uYWwvZmlsbEluZm8vZmlsbEluZm8iLCIkdXJsX3F1ZXJ5IjoiIiwiJHJlZmVycmVyIjoic3ViUGFnZXMvdGlja2V0L3BlcnNvbmFsL2luZGV4L2luZGV4IiwiJHJlZmVycmVyX3RpdGxlIjoi6aKE57qm5pyN5Yqh57O757ufIiwiJHRpdGxlIjoi6aKE57qm5pyN5Yqh57O757ufIiwiJGlzX2ZpcnN0X2RheSI6ZmFsc2UsIiR1cmwiOiJzdWJQYWdlcy90aWNrZXQvcGVyc29uYWwvZmlsbEluZm8vZmlsbEluZm8ifSwibG9naW5faWQiOiIxNzYxMDc3MzI3MyIsImFub255bW91c19pZCI6Im9zUGZONG4xdFpiYm9TNEUxOS1hakxGT2dYNDgiLCJ0eXBlIjoidHJhY2siLCJldmVudCI6IiRNUFZpZXdTY3JlZW4iLCJfdHJhY2tfaWQiOjYyNTIwODc5NywidGltZSI6MTcxNjM2ODc1ODc5NywiX2ZsdXNoX3RpbWUiOjE3MTYzNjg3NjA1MDN9XQ==");
        System.out.println(s);
    }
}
