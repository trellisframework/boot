package net.trellisframework.util.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.trellisframework.util.json.JsonUtil;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Jwt {
    private String secret;
    private String id;
    private String subject;
    private String issuer;
    private Date issuedAt;
    private Date expiration;
    private Map<String, Object> claims;
    private Algorithm algorithm = Algorithm.HS256;

    public static Jwt builder() {
        return new Jwt();
    }

    public Jwt secret(String secret) {
        this.secret = secret;
        return this;
    }

    public Jwt id(String id) {
        this.id = id;
        return this;
    }

    public Jwt subject(String subject) {
        this.subject = subject;
        return this;
    }

    public Jwt issuer(String issuer) {
        this.issuer = issuer;
        return this;
    }

    public Jwt issuedAt(Date issuedAt) {
        this.issuedAt = issuedAt;
        return this;
    }

    public Jwt expiration(Date expiration) {
        this.expiration = expiration;
        return this;
    }

    private Map<String, Object> claims() {
        return claims = claims == null ? new HashMap<>() : claims;
    }

    public Jwt claim(String key, Object value) {
        claims().put(key, value);
        return this;
    }

    public Jwt claims(Map<String, Object> claims) {
        claims().putAll(claims);
        return this;
    }

    public Jwt algorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public String compact() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("alg", algorithm.name());
        headers.put("typ", "JWT");
        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(JsonUtil.toString(headers).getBytes(StandardCharsets.UTF_8));

        Map<String, Object> data = new HashMap<>();
        Optional.ofNullable(id).ifPresent(x -> data.put("jti", x));
        Optional.ofNullable(subject).ifPresent(x -> data.put("sub", x));
        Optional.ofNullable(issuer).ifPresent(x -> data.put("iss", x));
        Optional.ofNullable(issuedAt).ifPresent(x -> data.put("iat", x.getTime() / 1000));
        Optional.ofNullable(expiration).ifPresent(x -> data.put("exp", x.getTime() / 1000));
        Optional.ofNullable(claims).ifPresent(data::putAll);
        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(JsonUtil.toString(data).getBytes(StandardCharsets.UTF_8));
        String signature = signature(header, payload);
        return header + "." + payload + "." + signature;
    }

    private String signature(String header, String payload) {
        String data = header + "." + payload;
        byte[] hash = algorithm.hash(data.getBytes(StandardCharsets.UTF_8), secret.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    @Getter
    @AllArgsConstructor
    public enum Algorithm {
        HS256("HmacSHA256"),
        HS384("HmacSHA384"),
        HS512("HmacSHA512");

        private final String value;

        byte[] hash(byte[] data, byte[] key) {
            try {
                Mac mac = Mac.getInstance(this.getValue());
                mac.init(new SecretKeySpec(key, this.getValue()));
                return mac.doFinal(data);
            } catch (NoSuchAlgorithmException | InvalidKeyException e) {
                throw new RuntimeException("Failed to generate HMAC signature", e);
            }
        }
    }
}