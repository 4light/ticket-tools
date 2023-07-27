package test.ticket.tickettools.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

public class ImageUtils {

    /**
     * base64转图片
     * @param base64String
     * @param imagePath
     * @param width
     */
    public static void imagCreate(String base64String, String imagePath, Integer height,Integer width) {
        try {
            // 将 Base64 字符串解码为字节数组
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            // 创建 ByteArrayInputStream 以读取字节数组
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            // 使用 ImageIO 读取 ByteArrayInputStream 中的图像数据
            BufferedImage originalImage = ImageIO.read(bis);
            // 指定所需的宽度和高度
            int desiredWidth = width;
            int desiredHeight = height;
            // 创建调整后尺寸的图像
            BufferedImage adjustedImage = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = adjustedImage.createGraphics();
            g.drawImage(originalImage, 0, 0, desiredWidth, desiredHeight, null);
            g.dispose();
            // 将图像保存到文件中
            File outputImage = new File(imagePath);
            ImageIO.write(adjustedImage, "png", outputImage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
