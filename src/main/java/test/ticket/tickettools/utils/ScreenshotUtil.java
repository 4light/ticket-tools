package test.ticket.tickettools.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;


import java.io.File;
import java.util.concurrent.TimeUnit;

public class ScreenshotUtil {

    public static void takeScreenshot(String orderId,String auth,String fileName) {
        String url="https://pcticket.cstm.org.cn/personal/order_detail?orderId="+orderId;
        // 设置ChromeDriver的路径
        System.setProperty("webdriver.chrome.driver", "/Users/devin.zhang/Downloads/chromedriver-mac-x64/chromedriver");
        //System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver-linux64/chromedriver");

        // 配置ChromeOptions
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--headless");
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
            driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
            driver.findElement(new By.ByClassName("ticket"));
            Thread.sleep(1000); // 可以使用更合适的等待方式，如WebDriverWait
            // 截图并保存
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String screenshotPath = "/root/screenShort/"+fileName+".png";
            File destination = new File(screenshotPath);
            FileUtil.copyFile(screenshot, destination);
            //String screenshotBase64 = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭浏览器
           //driver.quit();
        }
    }
}
