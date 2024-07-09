package test.ticket.tickettools.utils;

import cn.hutool.core.io.FileUtil;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;


import java.io.File;

public class ScreenshotUtil {

    public static void takeScreenshot(String orderId,String auth,String fileName) {
        String url="https://pcticket.cstm.org.cn/personal/order_detail?orderId="+orderId;
        // 设置ChromeDriver的路径
        //System.setProperty("webdriver.chrome.driver", "/Users/devin.zhang/Downloads/chromedriver-mac-x64/chromedriver");
        System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver-linux64/chromedriver");

        // 配置ChromeOptions
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        // 创建ChromeDriver实例
        ChromeDriver driver = new ChromeDriver(options);
        String script="window.localStorage.setItem('api_token','%s')";
        String format = String.format(script, auth);
        try {
            driver.manage().window().setSize(new org.openqa.selenium.Dimension(1920, 1080));
            driver.get(url);
            driver.executeScript(format);
            driver.navigate().refresh();
            // 等待页面加载或执行其他操作
            Thread.sleep(5000); // 可以使用更合适的等待方式，如WebDriverWait
            // 截图并保存
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String screenshotPath = "/root/"+fileName+".png";
            File destination = new File(screenshotPath);
            FileUtil.copyFile(screenshot, destination);
            //String screenshotBase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭浏览器
           // driver.quit();
        }
    }
}
