package net.trellisframework.util.aws;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.BadRequestException;
import net.trellisframework.http.exception.ConflictException;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.util.constant.Messages;
import net.trellisframework.util.crypto.CryptoUtil;
import net.trellisframework.util.string.StringUtil;
import net.trellisframework.util.url.URLUtil;
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

import java.io.File;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AwsS3Client {

    private static AwsS3ClientProperties properties;

    private static Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> getProperties(String bucket) {
        if (properties == null)
            properties = ApplicationContextProvider.context.getBean(AwsS3ClientProperties.class);
        return properties.getBuckets().entrySet().stream().filter(x -> x.getKey().equals(bucket)).findFirst().orElseThrow(() -> new NotFoundException(Messages.BUCKET_NOT_FOUND));
    }

    private static final DateTimeFormatter AMZ_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter DATE_STAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    public static String preSignedUrl(String bucket, String key, HttpMethod method, Instant expirationDate) {
        return preSignedUrl(bucket, key, method, expirationDate, Instant.now(), null);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, Instant expirationDate, String downloadFileName) {
        return preSignedUrl(bucket, key, method, expirationDate, Instant.now(), downloadFileName);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, Instant expirationDate, Instant signingDate) {
        return preSignedUrl(bucket, key, method, expirationDate, signingDate, null);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, Instant expirationDate, Instant signingDate, String downloadFileName) {
        long expiresInSeconds = Duration.between(signingDate, expirationDate).getSeconds();
        if (expiresInSeconds <= 0)
            throw new BadRequestException(Messages.EXPIRATION_DATE_MUST_BE_IN_THE_FUTURE.getMessage());
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        return generateSignedUrlV4(properties.getValue(), properties.getKey(), key, method, expiresInSeconds, signingDate, downloadFileName);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, long expireDuration, ChronoUnit expireDurationUnit) {
        return preSignedUrl(bucket, key, method, expireDuration, expireDurationUnit, Instant.now(), null);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, long expireDuration, ChronoUnit expireDurationUnit, String downloadFileName) {
        return preSignedUrl(bucket, key, method, expireDuration, expireDurationUnit, Instant.now(), downloadFileName);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, long expireDuration, ChronoUnit expireDurationUnit, Instant signingDate) {
        return preSignedUrl(bucket, key, method, expireDuration, expireDurationUnit, signingDate, null);
    }

    public static String preSignedUrl(String bucket, String key, HttpMethod method, long expireDuration, ChronoUnit expireDurationUnit, Instant signingDate, String downloadFileName) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        long expiresInSeconds = Duration.of(expireDuration, expireDurationUnit).getSeconds();
        return generateSignedUrlV4(properties.getValue(), properties.getKey(), key, method, expiresInSeconds, signingDate, downloadFileName);
    }

    private static String generateSignedUrlV4(AwsS3ClientProperties.S3PropertiesDefinition config, String bucket, String key, HttpMethod method, long expiresInSeconds, Instant signingDate, String downloadFileName) {
        try {
            boolean pathStyle = Optional.ofNullable(config.getPathStyle()).orElse(false);
            String accessKey = config.getCredential().getAccessKey();
            String secretKey = config.getCredential().getSecretKey();
            String region = Optional.ofNullable(config.getRegion()).orElse("us-east-1");
            String host;
            String path;
            if (config.getEndpoint() != null) {
                host = config.getEndpoint().replaceFirst("https?://", "");
                path = pathStyle ? "/" + bucket + "/" + key : "/" + key;
            } else {
                host = pathStyle ? "s3." + region + ".amazonaws.com" : bucket + ".s3." + region + ".amazonaws.com";
                path = pathStyle ? "/" + bucket + "/" + key : "/" + key;
            }
            String amzDate = AMZ_DATE_FORMAT.format(signingDate);
            String dateStamp = DATE_STAMP_FORMAT.format(signingDate);
            String credential = accessKey + "/" + dateStamp + "/" + region + "/s3/aws4_request";

            TreeMap<String, String> queryParams = new TreeMap<>();
            queryParams.put("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
            queryParams.put("X-Amz-Credential", credential);
            queryParams.put("X-Amz-Date", amzDate);
            queryParams.put("X-Amz-Expires", String.valueOf(expiresInSeconds));
            queryParams.put("X-Amz-SignedHeaders", "host");
            if (StringUtils.isNotBlank(downloadFileName))
                queryParams.put("response-content-disposition", "attachment; filename=\"" + downloadFileName + "\"");
            String canonicalQueryString = URLUtil.buildCanonicalQueryString(queryParams);
            String encodedPath = URLUtil.encodePath(path);
            String canonicalRequest = method.name() + "\n" + encodedPath + "\n" + canonicalQueryString + "\n" + "host:" + host + "\n\n" + "host\n" + "UNSIGNED-PAYLOAD";
            String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n" + dateStamp + "/" + region + "/s3/aws4_request\n" + CryptoUtil.sha256(canonicalRequest);
            byte[] signingKey = deriveSigningKey(secretKey, dateStamp, region);
            String signature = StringUtil.encodeHexString(CryptoUtil.hmacSha256(signingKey, stringToSign));
            return "https://" + host + encodedPath + "?" + canonicalQueryString + "&X-Amz-Signature=" + signature;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] deriveSigningKey(String secretKey, String dateStamp, String region) {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = CryptoUtil.hmacSha256(kSecret, dateStamp);
        byte[] kRegion = CryptoUtil.hmacSha256(kDate, region);
        byte[] kService = CryptoUtil.hmacSha256(kRegion, "s3");
        return CryptoUtil.hmacSha256(kService, "aws4_request");
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
