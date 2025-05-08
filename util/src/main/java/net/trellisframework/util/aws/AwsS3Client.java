package net.trellisframework.util.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.ConflictException;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.util.constant.Messages;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.time.Instant;
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
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 client = getClient(properties);
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(properties.getKey(), key, method);
        generatePresignedUrlRequest.setExpiration(Date.from(Instant.now().plus(expireDuration, expireDurationUnit)));
        return client.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    public static String getPublicUrl(String bucket, String key) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 client = getClient(properties);
        client.setObjectAcl(bucket, key, CannedAccessControlList.PublicRead);
        return client.getUrl(bucket, key).toString();
    }

    public static String upload(String bucket, String key, File file, boolean override) {
        return upload(bucket, key, file, override, CannedAccessControlList.Private);
    }

    public static String upload(String bucket, String key, File file, boolean override, CannedAccessControlList cannedAcl) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 client = getClient(properties);
        if (!override && client.doesObjectExist(properties.getKey(), key)) {
            throw new ConflictException(Messages.FILE_ALREADY_EXIST);
        } else {
            client.putObject(new PutObjectRequest(bucket, key, file).withCannedAcl(cannedAcl));
            return key;
        }
    }

    public static void download(String bucket, String key, File file) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 client = getClient(properties);
        if (!client.doesObjectExist(properties.getKey(), key))
            throw new NotFoundException(Messages.FILE_NOT_FOUND);
        client.getObject(new GetObjectRequest(bucket, key), file);
    }

    public static List<S3Object> browse(String bucket, String folder, Integer page, Integer size) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 client = getClient(properties);
        List<S3Object> s3Objects = new ArrayList<>();
        ObjectListing objectListing;
        String prefix = Optional.ofNullable(folder).orElse("").isEmpty() ? null : folder;
        int startItem = (page - 1) * size;
        int endItem = startItem + size;
        if (prefix == null) {
            objectListing = client.listObjects(bucket);
        } else {
            objectListing = client.listObjects(bucket, prefix);
        }
        for (int i = startItem; i < Math.min(endItem, objectListing.getMaxKeys()) && objectListing.isTruncated(); i++) {
            S3Object s3Object = new S3Object(objectListing.getObjectSummaries().get(i));
            s3Objects.add(s3Object);
        }
        return s3Objects;
    }

    private static AmazonS3 getClient(Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> property) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(property.getValue().getCredential().getAccessKey(), property.getValue().getCredential().getSecretKey()))).withPathStyleAccessEnabled(property.getValue().getPathStyle());
        Optional.ofNullable(property.getValue().getEndpoint()).ifPresentOrElse(
                x -> builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(Optional.ofNullable(property.getValue().getRegion()).map(Regions::getName).map(r -> r + ".").orElse(StringUtils.EMPTY) + property.getValue().getEndpoint(), Optional.ofNullable(property.getValue().getRegion()).map(Regions::getName).orElse(null))),
                () -> Optional.ofNullable(property.getValue().getRegion()).ifPresent(builder::withRegion)
        );
        return builder.build();
    }

}
