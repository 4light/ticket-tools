function getPoint(dataText,secretKey) {
    var key = CryptoJS.enc.Utf8.parse(secretKey);
    var iv = CryptoJS.enc.Utf8.parse(dataText);
    var ciphertext = CryptoJS.AES.encrypt(iv, key, {
        iv: iv,
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
    });
   return ciphertext.toString();
}
