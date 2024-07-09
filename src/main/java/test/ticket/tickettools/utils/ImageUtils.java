package test.ticket.tickettools.utils;

import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.tesseract.TessBaseAPI;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.ObjectUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;

public class ImageUtils {


    /**
     * base64转图片
     *
     * @param base64String
     * @param imagePath
     * @param width
     */
    public static void imagCreate(String base64String, String imagePath, Integer height, Integer width) {
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

    public static void toBlackImag(String path, String name) {
        Mat imageMat = imread(path, opencv_imgcodecs.IMREAD_GRAYSCALE);
        opencv_imgproc.threshold(imageMat, imageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        opencv_imgcodecs.imwrite("./" + name + "_black.png", imageMat);
    }

    public static String getCaptchaCode(String path) {
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
        return result.trim().replaceAll(" ", "").replaceAll("\n", "");
    }

    public static Double getPoint(String backImagePath, String sliderImagePath, String uid) {
        Mat backImageMat = opencv_imgcodecs.imread(backImagePath, opencv_imgcodecs.IMREAD_GRAYSCALE);
        Mat sliderImageMat = opencv_imgcodecs.imread(sliderImagePath, opencv_imgcodecs.IMREAD_GRAYSCALE);
        opencv_imgproc.threshold(backImageMat, backImageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        opencv_imgproc.threshold(sliderImageMat, sliderImageMat, 215, 255, opencv_imgproc.THRESH_BINARY);
        //保存为黑白图片
        opencv_imgcodecs.imwrite("." + File.separator + uid + "_backBlack.png", backImageMat);
        opencv_imgcodecs.imwrite("." + File.separator + uid + "_sliderBlack.png", sliderImageMat);
        Mat result = new Mat();
        opencv_imgproc.matchTemplate(sliderImageMat, backImageMat, result, opencv_imgproc.TM_CCORR_NORMED);
        opencv_core.normalize(result, result, 1, 0, opencv_core.NORM_MINMAX, -1, new Mat());
        DoublePointer doublePointer = new DoublePointer(new double[2]);
        org.bytedeco.opencv.opencv_core.Point maxLoc = new org.bytedeco.opencv.opencv_core.Point();
        opencv_core.minMaxLoc(result, null, doublePointer, null, maxLoc, null);
        opencv_imgproc.rectangle(sliderImageMat, maxLoc, new Point(maxLoc.x() + backImageMat.cols(), maxLoc.y() + backImageMat.rows()), new Scalar(0, 255, 0, 1));
        try {
            Files.delete(Paths.get("./" + uid + "_backBlack.png"));
            Files.delete(Paths.get("./" + uid + "_sliderBlack.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        int real = maxLoc.x() * 330 / 310;
        //log.info("real:{}", real);
        return real * 310 / 330.0;
    }

    public static String mergeBase64Images(String base64Img1, String base64Img2) {
        try {
            // 解码Base64字符串
            byte[] imgBytes1 = Base64.getDecoder().decode(base64Img1);
            byte[] imgBytes2 = Base64.getDecoder().decode(base64Img2);

            // 将字节数组转换为BufferedImage
            BufferedImage img1 = ImageIO.read(new ByteArrayInputStream(imgBytes1));
            BufferedImage img2 = ImageIO.read(new ByteArrayInputStream(imgBytes2));
            // 合并图片
            BufferedImage mergedImage = mergeImages(img1, img2);
            // 将合并后的图片编码为Base64
            String mergedBase64 = encodeImageToBase64(mergedImage);
            return mergedBase64;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static BufferedImage mergeImages(BufferedImage img1, BufferedImage img2) {
        int width = Math.max(img1.getWidth(), img2.getWidth());
        int height = img1.getHeight() + img2.getHeight();
        BufferedImage mergedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = mergedImage.createGraphics();
        g2d.drawImage(img1, 0, 0, null);
        g2d.drawImage(img2, 0, img1.getHeight(), null);
        g2d.dispose();
        return mergedImage;
    }

    private static String encodeImageToBase64(BufferedImage image) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(imageBytes);
    }

    public static String getVerCode(String imageBase64) {
        JSONObject body = new JSONObject();
        body.put("image", imageBase64);
        body.put("token", "2J3UHYaDJTbELG55unhlt9JkNLKoLpcY9gsEOvbZ2Uc");
        body.put("type", "10103");
        HttpResponse execute = HttpUtil.createPost("http://api.jfbym.com/api/YmServer/customApi")
                .header("Content-Type", "application/json")
                .body(JSON.toJSONString(body))
                .timeout(60000)
                .execute();
        String res = execute.body();
        if (!ObjectUtils.isEmpty(res)) {
            JSONObject resJson = JSON.parseObject(res);
            if (resJson.getIntValue("code") == 10000) {
                return resJson.getJSONObject("data").getString("data");
            }
        }
        return null;
    }
}
