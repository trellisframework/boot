package net.trellisframework.util.aws;

import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.util.Date;

public class S3Object {
    private String bucket;
    private String key;
    private AwsS3ObjectType objectType;
    private Date createdAt;
    private long size;
    private String fileExtension;

    protected S3Object(S3ObjectSummary objectSummary) {
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

    // add getters here
    public String getBucket() {
        return bucket;
    }

    public String getKey() {
        return key;
    }

    public AwsS3ObjectType getObjectType() {
        return objectType;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public long getSize() {
        return size;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public enum AwsS3ObjectType {
        FOLDER,
        FILE
    }
}
