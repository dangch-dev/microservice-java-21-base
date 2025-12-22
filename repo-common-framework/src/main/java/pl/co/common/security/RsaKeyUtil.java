package pl.co.common.security;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyUtil {
    private RsaKeyUtil() {
    }

    public static RSAPrivateKey loadPrivateKey(String location) {
        byte[] bytes = readAllBytes(location);
        String pem = new String(bytes)
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load private key from " + location, e);
        }
    }

    public static RSAPublicKey loadPublicKey(String location) {
        byte[] bytes = readAllBytes(location);
        String pem = new String(bytes)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded = Base64.getDecoder().decode(pem);
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(decoded));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load public key from " + location, e);
        }
    }

    private static byte[] readAllBytes(String location) {
        if (!StringUtils.hasText(location)) {
            throw new IllegalStateException("Key location must not be empty");
        }
        try {
            if (location.startsWith("classpath:")) {
                String path = location.substring("classpath:".length());
                Resource resource = new ClassPathResource(path);
                return resource.getInputStream().readAllBytes();
            }
            if (location.startsWith("file:")) {
                return Files.readAllBytes(Path.of(location.substring("file:".length())));
            }
            return Files.readAllBytes(Path.of(location));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read key at " + location, e);
        }
    }
}
