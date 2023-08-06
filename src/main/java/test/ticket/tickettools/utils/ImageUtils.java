package test.ticket.tickettools.utils;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.tesseract.TessBaseAPI;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Base64;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

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

    public static void toBlackImag(String path,String name){
        Mat imageMat = imread(path, opencv_imgcodecs.IMREAD_GRAYSCALE);
        opencv_imgproc.threshold(imageMat, imageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        opencv_imgcodecs.imwrite("./" + name + "_black.png", imageMat);
    }

    public static String getCaptchaCode(String path){
        Mat imagePath = opencv_imgcodecs.imread(path);
        Mat grayImage = new Mat();
        opencv_imgproc.cvtColor(imagePath, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
        Mat binaryImage = new Mat();
        opencv_imgproc.threshold(grayImage, binaryImage, 215, 255, opencv_imgproc.THRESH_BINARY_INV | opencv_imgproc.THRESH_OTSU);
        // Perform OCR
        TessBaseAPI tesseract = new TessBaseAPI();
        tesseract.Init("./", "eng"); // 指定tessdata路径和需要的语言数据
        tesseract.SetImage(binaryImage.data(), binaryImage.cols(), binaryImage.rows(), 1, binaryImage.cols());
        String result = tesseract.GetUTF8Text().getString();
        return result.trim().replaceAll(" ","").replaceAll("\n","");
    }

    public static void main(String[] args) {
        String captchaCode = ImageUtils.getCaptchaCode("./6e8e6d56-0a3e-46ae-bf66-170081c9b512_black.png");
        System.out.println(captchaCode);
    }
}
