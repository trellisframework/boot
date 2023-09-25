package net.trellisframework.util.aws;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import net.trellisframework.core.application.ApplicationContextProvider;
import net.trellisframework.http.exception.NotFoundException;
import net.trellisframework.util.constant.Messages;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

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

}
