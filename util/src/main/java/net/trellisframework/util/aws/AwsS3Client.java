package net.trellisframework.util.aws;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.ConflictException;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.util.constant.Messages;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AwsS3Client {

    private static AwsS3ClientProperties properties;

    private static Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> getProperties(String bucket) {
        if (properties == null)
            properties = ApplicationContextProvider.context.getBean(AwsS3ClientProperties.class);
        return properties.getBuckets().entrySet().stream().filter(x -> x.getKey().equals(bucket)).findFirst().orElseThrow(() -> new NotFoundException(Messages.BUCKET_NOT_FOUND));
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, long expireDuration, ChronoUnit expireDurationUnit) {
        return preSignedUrl(bucket, key, method, expireDuration, expireDurationUnit, null);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, long expireDuration, ChronoUnit expireDurationUnit, String downloadFileName) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        return generateSignedUrl(properties.getValue(), properties.getKey(), key, method, Instant.now().plus(expireDuration, expireDurationUnit).getEpochSecond(), downloadFileName);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, LocalDateTime expirationDateTime) {
        return preSignedUrl(bucket, key, method, expirationDateTime, null);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, LocalDateTime expirationDateTime, String downloadFileName) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AwsS3ClientProperties.S3PropertiesDefinition config = properties.getValue();
        long expires = expirationDateTime.atZone(ZoneOffset.UTC).toEpochSecond();
        return generateSignedUrl(config, properties.getKey(), key, method, expires, downloadFileName);
    }

    private static String generateSignedUrl(AwsS3ClientProperties.S3PropertiesDefinition config, String bucket, String key, HttpMethod method, long expires, String downloadFileName) {
        try {
            boolean pathStyle = Optional.ofNullable(config.getPathStyle()).orElse(false);
            String resource = "/" + bucket + "/" + key;
            String stringToSign = method.name() + "\n\n\n" + expires + "\n" + resource;
            Mac hmac = Mac.getInstance("HmacSHA1");
            hmac.init(new SecretKeySpec(config.getCredential().getSecretKey().getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            String signature = Base64.getEncoder().encodeToString(hmac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8)));
            String host;
            String path;
            if (config.getEndpoint() != null) {
                host = config.getEndpoint().replaceFirst("https?://", "");
                path = pathStyle ? "/" + bucket + "/" + key : "/" + key;
            } else {
                host = pathStyle
                        ? "s3." + config.getRegion() + ".amazonaws.com"
                        : bucket + ".s3." + config.getRegion() + ".amazonaws.com";
                path = pathStyle ? "/" + bucket + "/" + key : "/" + key;
            }
            StringBuilder url = new StringBuilder("https://").append(host).append(path)
                    .append("?AWSAccessKeyId=").append(URLEncoder.encode(config.getCredential().getAccessKey(), StandardCharsets.UTF_8))
                    .append("&Expires=").append(expires)
                    .append("&Signature=").append(URLEncoder.encode(signature, StandardCharsets.UTF_8));
            if (StringUtils.isNotBlank(downloadFileName))
                url.append("&response-content-disposition=").append(URLEncoder.encode("attachment; filename=\"" + downloadFileName + "\"", StandardCharsets.UTF_8));
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPublicUrl(String bucket, String key) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        S3Client client = getClient(properties);
        client.putObjectAcl(PutObjectAclRequest.builder().bucket(bucket).key(key).acl(ObjectCannedACL.PUBLIC_READ).build());
        return getUrl(properties, bucket, key);
    }

    public static String upload(String bucket, String key, File file, boolean override) {
        return upload(bucket, key, file, override, ObjectCannedACL.PRIVATE);
    }

    public static String upload(String bucket, String key, File file, boolean override, ObjectCannedACL cannedAcl) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        S3Client client = getClient(properties);
        if (!override && doesObjectExist(client, properties.getKey(), key)) {
            throw new ConflictException(Messages.FILE_ALREADY_EXIST);
        } else {
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).acl(cannedAcl).build(), RequestBody.fromFile(file));
            return key;
        }
    }

    public static void download(String bucket, String key, File file) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        S3Client client = getClient(properties);
        if (!doesObjectExist(client, properties.getKey(), key))
            throw new NotFoundException(Messages.FILE_NOT_FOUND);
        client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build(), file.toPath());
    }

    public static List<S3Object> browse(String bucket, String folder, Integer page, Integer size) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        S3Client client = getClient(properties);
        List<S3Object> s3Objects = new ArrayList<>();
        ListObjectsV2Response objectListing;
        String prefix = Optional.ofNullable(folder).orElse("").isEmpty() ? null : folder;
        int startItem = (page - 1) * size;
        int endItem = startItem + size;
        if (prefix == null) {
            objectListing = client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).build());
        } else {
            objectListing = client.listObjectsV2(ListObjectsV2Request.builder().bucket(bucket).prefix(prefix).build());
        }
        for (int i = startItem; i < Math.min(endItem, objectListing.maxKeys()) && objectListing.isTruncated(); i++) {
            S3Object s3Object = new S3Object(objectListing.contents().get(i));
            s3Objects.add(s3Object);
        }
        return s3Objects;
    }

    private static boolean doesObjectExist(S3Client client, String bucket, String key) {
        try {
            client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    private static String getUrl(Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property, String bucket, String key) {
        if (property.getValue().getEndpoint() != null)
            return getEndpointUrl(property.getValue().getEndpoint(), property.getValue().getRegion()) + "/" + bucket + "/" + key;
        return "https://" + bucket + ".s3." + property.getValue().getRegion() + ".amazonaws.com/" + key;
    }

    private static String getEndpointUrl(String endpoint, String region) {
        String regionPrefix = Optional.ofNullable(region).map(r -> r + ".").orElse(StringUtils.EMPTY);
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://"))
            return endpoint.contains(regionPrefix) ? endpoint : endpoint.replaceFirst("://", "://" + regionPrefix);
        return "https://" + regionPrefix + endpoint;
    }

    private static S3Client getClient(Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property) {
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(property.getValue().getCredential().getAccessKey(), property.getValue().getCredential().getSecretKey()))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(Optional.ofNullable(property.getValue().getPathStyle()).orElse(false)).build());
        Optional.ofNullable(property.getValue().getEndpoint()).ifPresentOrElse(
                x -> builder.endpointOverride(URI.create(getEndpointUrl(x, property.getValue().getRegion()))).region(Region.of(Optional.ofNullable(property.getValue().getRegion()).orElse("us-east-1"))),
                () -> Optional.ofNullable(property.getValue().getRegion()).ifPresent(r -> builder.region(Region.of(r)))
        );
        return builder.build();
    }

}
