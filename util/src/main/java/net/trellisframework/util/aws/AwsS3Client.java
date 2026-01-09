package net.trellisframework.util.aws;

import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.ConflictException;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.util.constant.Messages;
import org.springframework.http.HttpMethod;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property = getProperties(bucket);
        try (S3Presigner presigner = getPresigner(property)) {
            Instant expiration = Instant.now().plus(expireDuration, expireDurationUnit);
            Duration duration = Duration.between(Instant.now(), expiration);
            if (method == HttpMethod.GET) {
                GetObjectRequest.Builder requestBuilder = GetObjectRequest.builder().bucket(bucket).key(key);
                if (downloadFileName != null)
                    requestBuilder.responseContentDisposition("attachment; filename=\"" + downloadFileName + "\"");
                PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(duration).getObjectRequest(requestBuilder.build()).build());
                return presignedRequest.url().toString();
            } else {
                PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(duration).putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key).build()).build());
                return presignedRequest.url().toString();
            }
        }
    }

    public static String getPublicUrl(String bucket, String key) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property = getProperties(bucket);
        try (S3Client client = getClient(property)) {
            client.putObjectAcl(PutObjectAclRequest.builder().bucket(bucket).key(key).acl(ObjectCannedACL.PUBLIC_READ)
                    .build());
            return client.utilities().getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build()).toString();
        }
    }

    public static String upload(String bucket, String key, File file, boolean override) {
        return upload(bucket, key, file, override, ObjectCannedACL.PRIVATE);
    }

    public static String upload(String bucket, String key, File file, boolean override, ObjectCannedACL cannedAcl) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property = getProperties(bucket);
        try (S3Client client = getClient(property)) {
            if (!override) {
                try {
                    client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                    throw new ConflictException(Messages.FILE_ALREADY_EXIST);
                } catch (NoSuchKeyException ignored) {
                }
            }
            client.putObject(PutObjectRequest.builder().bucket(bucket).key(key).acl(cannedAcl).build(), RequestBody.fromFile(file));
            return key;
        }
    }

    public static void download(String bucket, String key, File file) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property = getProperties(bucket);
        try (S3Client client = getClient(property)) {
            try {
                client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            } catch (NoSuchKeyException e) {
                throw new NotFoundException(Messages.FILE_NOT_FOUND);
            }
            try (FileOutputStream fos = new FileOutputStream(file)) {
                client.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build()).transferTo(fos);
            } catch (IOException e) {
                throw new NotFoundException(Messages.FILE_NOT_FOUND);
            }
        }
    }

    public static List<S3Object> browse(String bucket, String folder, Integer page, Integer size) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property = getProperties(bucket);
        try (S3Client client = getClient(property)) {
            List<S3Object> s3Objects = new ArrayList<>();
            String prefix = Optional.ofNullable(folder).orElse("").isEmpty() ? null : folder;
            ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder().bucket(bucket).maxKeys(size);
            if (prefix != null)
                requestBuilder.prefix(prefix);
            String continuationToken = null;
            for (int currentPage = 1; currentPage < page; currentPage++) {
                Optional.ofNullable(continuationToken).ifPresent(requestBuilder::continuationToken);
                ListObjectsV2Response response = client.listObjectsV2(requestBuilder.build());
                if (!response.isTruncated()) {
                    return s3Objects;
                }
                continuationToken = response.nextContinuationToken();
            }
            Optional.ofNullable(continuationToken).ifPresent(requestBuilder::continuationToken);
            ListObjectsV2Response response = client.listObjectsV2(requestBuilder.build());
            List<software.amazon.awssdk.services.s3.model.S3Object> contents = response.contents();
            if (contents != null) {
                for (software.amazon.awssdk.services.s3.model.S3Object s3ObjectSummary : contents) {
                    s3Objects.add(new S3Object(s3ObjectSummary));
                }
            }

            return s3Objects;
        }
    }

    private static S3Client getClient(Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property) {
        AwsS3ClientProperties.S3PropertiesDefinition props = property.getValue();
        String region = region(props.getRegion());
        S3ClientBuilder builder = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getCredential().getAccessKey(), props.getCredential().getSecretKey())))
                .region(Region.of(region));

        if (Optional.ofNullable(props.getPathStyle()).orElse(false))
            builder.forcePathStyle(true);

        if (props.getEndpoint() != null) {
            String endpoint = props.getEndpoint();
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                endpoint = region + "." + endpoint;
                endpoint = "https://" + endpoint;
            }
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private static S3Presigner getPresigner(Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property) {
        AwsS3ClientProperties.S3PropertiesDefinition props = property.getValue();
        String region = region(props.getRegion());
        var builder = S3Presigner.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(props.getCredential().getAccessKey(), props.getCredential().getSecretKey())))
                .region(Region.of(region));
        if (props.getEndpoint() != null) {
            String endpoint = props.getEndpoint();
            if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
                endpoint = region + "." + endpoint;
                endpoint = "https://" + endpoint;
            }
            builder.endpointOverride(URI.create(endpoint));
        }
        return builder.build();
    }

    private static String region(String region) {
        return Optional.ofNullable(region).map(x -> x.replace('_', '-')).orElse( "us-east-1");
    }
}
