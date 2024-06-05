package test.ticket.tickettools.domain.bo;

import java.io.Serializable;

public class LogInCSTMParam implements Serializable {
    private static final long serialVersionUID = -7864303773632678266L;
    private String phone;
    private String captchaImage;
    private String captchaImageBase64;
    private String verificationCode;
    private String uuid;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getCaptchaImage() {
        return captchaImage;
    }

    public void setCaptchaImage(String captchaImage) {
        this.captchaImage = captchaImage;
    }

    public String getCaptchaImageBase64() {
        return captchaImageBase64;
    }

    public void setCaptchaImageBase64(String captchaImageBase64) {
        this.captchaImageBase64 = captchaImageBase64;
    }

    public String getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
