package net.trellisframework.util.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.util.constant.Messages;

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

    public static String preSignedUrl(String bucket, String key, long expireDuration, ChronoUnit expireDurationUnit) {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(properties.getValue().getCredential().getAccessKey(), properties.getValue().getCredential().getSecretKey())))
                .withRegion(properties.getValue().getRegion())
                .build();
        GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(properties.getKey(), key, HttpMethod.GET);
        generatePresignedUrlRequest.setExpiration(Date.from(Instant.now().plus(expireDuration, expireDurationUnit)));
        return s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    public static String uploadFile(String bucket, String serviceName, File file, boolean override) throws AssertionError {
        String fileKey = serviceName + "_" + file.getName();
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(properties.getValue().getCredential().getAccessKey(), properties.getValue().getCredential().getSecretKey())))
                .withRegion(properties.getValue().getRegion())
                .build();
        if (!override && s3Client.doesObjectExist(properties.getKey(), fileKey))
            throw new AssertionError(Messages.FILE_ALREADY_EXIST);
        s3Client.putObject(new PutObjectRequest(bucket, fileKey, file));
        return fileKey;
    }

    /**
     * @param bucket bucket name
     * @param key    target file key
     * @param file  target file
     */
    public static void downloadFile(String bucket, String key, File file) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(properties.getValue().getCredential().getAccessKey(), properties.getValue().getCredential().getSecretKey())))
                .withRegion(properties.getValue().getRegion())
                .build();
        if (!s3Client.doesObjectExist(properties.getKey(), key))
            throw new NotFoundException(Messages.FILE_NOT_FOUND);
        s3Client.getObject(new GetObjectRequest(bucket, key), file);
    }

    /**
     * @param bucket bucket name
     * @param folder  folder name
     */
    public static List<S3Object> getObjectList(String bucket, String folder, Integer pageNumber, Integer pageSize) throws NotFoundException {
        Map.Entry<String, AwsS3ClientProperties.S3PropertiesDefinition> properties = getProperties(bucket);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(properties.getValue().getCredential().getAccessKey(), properties.getValue().getCredential().getSecretKey())))
                .withRegion(properties.getValue().getRegion())
                .build();

        // Initialize a list to store the objects
        List<S3Object> s3Objects = new ArrayList<>();
        ObjectListing objectListing;
        String prefix = Optional.ofNullable(folder).orElse("").isEmpty() ? null : folder;

        // Calculate the starting index of the objects to fetch based on page number and page size
        int startItem = (pageNumber - 1) * pageSize;
        int endItem = startItem + pageSize;
        if (prefix == null) {
            // If folder is empty, list all objects in the main bucket
            objectListing = s3Client.listObjects(bucket);
        } else {
            // List objects in the specified folder
            objectListing = s3Client.listObjects(bucket, prefix);
        }

        for (int i = startItem; i < Math.min(endItem, objectListing.getMaxKeys()) && objectListing.isTruncated(); i++) {
            // Create and add the S3Object to the list
            S3Object s3Object = new S3Object(objectListing.getObjectSummaries().get(i));
            s3Objects.add(s3Object);
        }

        return s3Objects;
    }

}
