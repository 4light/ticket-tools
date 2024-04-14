package test.ticket.tickettools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.tesseract.TessBaseAPI;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import net.sourceforge.tess4j.OCRResult;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imread;


public class TestCOR {

    public static void main(String[] args) throws Exception {
        try{
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.setRequestFactory(new SimpleClientHttpRequestFactory() {
                {
                    setConnectTimeout(20000);
                    setReadTimeout(20000);
                }
            });
            Map param=new HashMap();
            param.put("captchaType","clickWord");
            param.put("clientUid","point-9e2197e2-86af-420f-8ff1-35543220327e");
            param.put("ts",System.currentTimeMillis());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("accept", "application/json");
            headers.set("cookie", "SL_G_WPT_TO=zh; SL_GWPT_Show_Hide_tmp=1; SL_wptGlobTipTmp=1");
            headers.set("user-agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36");
            HttpEntity entity = new HttpEntity<>(param,headers);

            /*ResponseEntity<String> response = restTemplate.exchange("https://jnt.mfu.com.cn/mmodule/mpwork.captcha/ajax/captcha/get", HttpMethod.POST, entity, String.class);
            String body = response.getBody();
            JSONObject jsonObject = JSON.parseObject(body);
            String string = jsonObject.getJSONObject("repData").getString("originalImageBase64");*/
            UUID uuid = UUID.randomUUID();
            String s = "./0cc8643c-f7d2-4ba9-9dec-e9196d627389.png";
            //imagCreate(string, s,155,330);
            toBlackImag(s,"0cc8643c-f7d2-4ba9-9dec-e9196d627389");

            //getCaptchaCode("80b80498-f004-4196-8c47-cf2992eb7583_black.png");
            ITesseract instance = new Tesseract();
            instance.setDatapath("/usr/local/Cellar/tesseract/5.3.4_1/share/tessdata");
            instance.setLanguage("chi_sim");

            File imageFile = new File("./0cc8643c-f7d2-4ba9-9dec-e9196d627389_black.png");
            OCRResult result;
            try {
                String s1 = instance.doOCR(imageFile);
                System.out.println(s1);
                /*Iterator<Rectangle> iterator = result.getIterator();

                while (iterator.hasNext()) {
                    Rectangle rect = iterator.next();
                    System.out.println("Text: " + result.getWordText(rect) + ", Coordinates: " + rect);
                }*/
            } catch (TesseractException e) {
                e.printStackTrace();
            }

        }catch (Exception e){
            e.printStackTrace();
        }
    }
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
        opencv_imgproc.threshold(imageMat, imageMat, 127, 255, opencv_imgproc.THRESH_BINARY);
        opencv_imgcodecs.imwrite("./" + name + "_black.png", imageMat);
    }
    public static String getCaptchaCode(String path){
        Mat imagePath = opencv_imgcodecs.imread(path);
        Mat grayImage = new Mat();
        opencv_imgproc.cvtColor(imagePath, grayImage, opencv_imgproc.COLOR_BGR2GRAY);
        Mat binaryImage = new Mat();
        opencv_imgproc.threshold(grayImage, binaryImage, 215, 235, opencv_imgproc.THRESH_BINARY_INV | opencv_imgproc.THRESH_OTSU);
        // Perform OCR
        TessBaseAPI tesseract = new TessBaseAPI();
        tesseract.Init("./", "eng"); // 指定tessdata路径和需要的语言数据
        tesseract.SetImage(binaryImage.data(), binaryImage.cols(), binaryImage.rows(), 1, binaryImage.cols());
        System.out.println(tesseract.AllWordConfidences());
        String result = tesseract.GetUTF8Text().getString();
        return result.trim().replaceAll(" ","").replaceAll("\n","");
    }
}
