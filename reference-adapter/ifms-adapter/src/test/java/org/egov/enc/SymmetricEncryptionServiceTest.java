package org.egov.enc;

import org.egov.xtra.enc.SymmetricEncryptionService;
import org.egov.xtra.key.EgovKeyGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SymmetricEncryptionServiceTest {

    private String baseURL;
    private String requestFilePath = "request.json";

    @BeforeAll
    public void init() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        baseURL = classLoader.getResource("").getFile();
    }

    @Test
    public void testDecryptSek() throws Exception {
        String appKey = "jq4ivcRJG26AceMqXax6u9mn54mGdUcR8e00HAV6wmM=";
        String ciphertext = "+jKNLNdDtC0DiVY8p8vfhQ9V+kPH5wSRnmNaeDjx7DnglZFeVbqmfM+ffaLXUe6D";
        byte[] secret = Base64.getDecoder().decode(appKey);
        String sek = SymmetricEncryptionService.decrypt(ciphertext, secret);
        System.out.println(sek);
    }

    @Test
    public void testDecryptResponse() throws Exception {
        String secretKey = "e1ro8WIs7BsGYwHtBbTWAWrmR/yzJSCoksOaMmOO0SI=";
        String ciphertext = "DIJJS8vuKbC15JdvMv7awq1BJ06RuVrDRfygeMxo9Wnqoyh1wtR8aZx53/D2GKZ7Bp/GyOrtH6E1MMYaKE0Zkw==";
        byte[] secret = Base64.getDecoder().decode(secretKey);
        String plaintext = SymmetricEncryptionService.decrypt(ciphertext, secret);
        plaintext = new String(Base64.getDecoder().decode(plaintext));
        System.out.println(plaintext);
    }

    @Test
    public void testEncrypt() throws Exception {
        String secretKey = "E1apGkAd4cZIDHCjSoqU7jXwv+KxDy1FGzNeKlxR6GY=";
        byte[] sekBytes = Base64.getDecoder().decode(secretKey);
        final byte[] fileBytes = Files.readAllBytes(Paths.get(baseURL + "request.json"));
        String requestBody = new String(fileBytes);
        System.out.println(requestBody);
        byte[] plainBytes = requestBody.getBytes();
        String ciphertext = SymmetricEncryptionService.encrypt(plainBytes, sekBytes);
        System.out.println(ciphertext);
        String rek = "e1ro8WIs7BsGYwHtBbTWAWrmR/yzJSCoksOaMmOO0SI=";
        byte[] rekBytes = Base64.getDecoder().decode(rek);
        String cipherKey = SymmetricEncryptionService.encrypt(rekBytes, sekBytes);
        System.out.println(cipherKey);
    }

    @Test
    public void testAESKeyGen() throws NoSuchAlgorithmException {
        String rek = EgovKeyGenerator.genAES256Key();
        System.out.println(rek);
    }

}