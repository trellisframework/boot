package net.trellisframework.util.aws;

import lombok.Data;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.util.constant.AwsS3ObjectType;

import java.util.Date;

@Data
public class S3Object implements Payload {
    private String key;
    private AwsS3ObjectType objectType;
    private Date createdAt;
    private long size;
    private String fileExtension;

    S3Object(software.amazon.awssdk.services.s3.model.S3Object objectSummary) {
        this.key = objectSummary.key();
        this.objectType = objectSummary.key().endsWith("/") ? AwsS3ObjectType.FOLDER : AwsS3ObjectType.FILE;
        this.createdAt = objectSummary.lastModified() != null 
                ? Date.from(objectSummary.lastModified()) 
                : new Date();
        this.size = objectSummary.size() != null ? objectSummary.size() : 0;
        this.fileExtension = null;
        if (objectType.equals(AwsS3ObjectType.FILE)) {
            int lastDotIndex = objectSummary.key().lastIndexOf('.');
            if (lastDotIndex != -1) {
                this.fileExtension = objectSummary.key().substring(lastDotIndex + 1);
            }
        }
    }


}
