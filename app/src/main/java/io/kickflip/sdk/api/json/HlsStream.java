package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;

import java.util.UUID;

/**
 * Created by davidbrodsky on 2/17/14.
 */
public class HlsStream extends Stream {

    @Key("bucketName")
    private String mBucket;

    @Key("pathPrefix")
    private String mPrefix;

    @Key("scak")
    private String mAwsKey;

    @Key("scsk")
    private String mAwsSecret;

    // old attributes
    @Key("aws_region")
    protected String mRegion;


    public String getAwsS3Bucket() {
        return mBucket;
    }

    public String getRegion() {
        return mRegion;
    }

    public String getAwsS3Prefix() {
        if ((mPrefix != null) && (mPrefix.startsWith("/"))) {
            return mPrefix.substring(1);
        } else {
            return mPrefix;
        }
    }

    public String getAwsKey() {
        return mAwsKey;
    }

    public String getAwsSecret() {
        return mAwsSecret;
    }

    public String toString(){
        return "Bucket: " + getAwsS3Bucket() + " streamUrl " + getFullVideoUrl();
    }


    public void setmBucket(String mBucket) {
        this.mBucket = mBucket;
    }

    public void setmRegion(String mRegion) {
        this.mRegion = mRegion;
    }

    public void setmPrefix(String mPrefix) {
        this.mPrefix = mPrefix;
    }


/*    public void setmAwsKey(String mAwsKey) {
        this.mAwsKey = mAwsKey;
    }

    public void setmAwsSecret(String mAwsSecret) {
        this.mAwsSecret = mAwsSecret;
    }
*/


    public HlsStream() {
        super(UUID.randomUUID().toString());
    }
}
