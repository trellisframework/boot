package net.trellisframework.util.aws;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import lombok.Data;
import net.trellisframework.core.payload.Payload;
import net.trellisframework.util.constant.AwsS3ObjectType;

import java.util.Date;

@Data
public class S3Object implements Payload {
    private String bucket;
    private String key;
    private AwsS3ObjectType objectType;
    private Date createdAt;
    private long size;
    private String fileExtension;

    S3Object(S3ObjectSummary objectSummary) {
        this.bucket = objectSummary.getBucketName();
        this.key = objectSummary.getKey();
        this.objectType = objectSummary.getKey().endsWith("/") ? AwsS3ObjectType.FOLDER : AwsS3ObjectType.FILE;
        this.createdAt = objectSummary.getLastModified();
        this.size = objectSummary.getSize();
        this.fileExtension = null;
        if (objectType.equals(AwsS3ObjectType.FILE)) {
            int lastDotIndex = objectSummary.getKey().lastIndexOf('.');
            if (lastDotIndex != -1) {
                this.fileExtension = objectSummary.getKey().substring(lastDotIndex + 1);
            }
        }
    }


}
