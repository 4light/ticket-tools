package test.ticket.tickettools;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class AESUtil {

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

    public static void main(String[] args) {
        String s = doAES("[{\"x\":110,\"y\":22},{\"x\":144,\"y\":58},{\"x\":221,\"y\":101}]", "vGjgWVk2O13tdROv");
        System.out.println(s);
    }
}
