package io.kickflip.sdk.api.json;

import com.google.api.client.util.Key;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.backend.model.Location;

import java.io.Serializable;
import java.util.Map;

import io.kickflip.sdk.api.Jackson;

/**
 * Kickflip base Stream response
 */
public class Stream extends Response implements Comparable<Stream>, Serializable {

    public  static final String HISTORICAL_VIDEO_FILENAME = "vod.m3u8";

    private static final String FILENAME_PREFIX_WIDE = "wide_";

    private static final String FILENAME_PREFIX_POSTER = "poster_";


    @Key("id")
    protected String mStreamId;

    @Key("createdBy")
    protected com.mobileman.moments.android.backend.model.User createdBy;

    @Key("baseUrl")
    protected String baseUrl;

    @Key("state")
    protected int streamState;

    @Key("videoFileName")
    protected String videoFileName;

    @Key("thumbnailPathPrefix")
    protected String thumbnailPathPrefix;

    @Key("thumbnailFileName")
    protected String thumbnailFileName;

    @Key("thumbnailBaseUrl")
    protected String thumbnailBaseUrl;

    @Key("text")
    protected String mTitle;

    @Key("location")
    protected Location location;

    @Key("sharingUrl")
    protected String shareUrl;

    // old attributes
    @Key("stream_type")
    protected String mStreamType;

    @Key("chat_url")
    protected String mChatUrl;

    @Key("kickflip_url")
    protected String mKickflipUrl;

    @Key("city")
    protected String mCity;

    @Key("country")
    protected String mCountry;

    @Key("private")
    protected boolean mPrivate;

    @Key("description")
    protected String mDescription;

    @Key("extra_info")
    protected String mExtraInfo;

    @Key("time_started")
    protected String mTimeStarted;

    @Key("length")
    protected int mLength;

    @Key("user_username")
    protected String mOwnerName;

    @Key("user_avatar")
    protected String mOwnerAvatar;

    @Key("deleted")
    protected boolean mDeleted;

    public boolean isDeleted() {
        return mDeleted;
    }

    public void setDeleted(boolean deleted) {
        mDeleted = deleted;
    }

    public String getOwnerName() {
        return mOwnerName;
    }

    public String getOwnerAvatar() {
        return mOwnerAvatar;
    }

    public String getThumbnailPathPrefix() {
        return thumbnailPathPrefix;
    }

    public String getStreamId() {
        return mStreamId;
    }

    public String getStreamType() {
        return mStreamType;
    }

    public int getStreamState() {
        return streamState;
    }

    public void setStreamState(int streamState) {
        this.streamState = streamState;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getChatUrl() {
        return mChatUrl;
    }

    public String getKickflipUrl() {
        return mKickflipUrl;
    }

    public String getTimeStarted() {
        return mTimeStarted;
    }

    public int getLengthInSeconds() {
        return mLength;
    }

    public Map getExtraInfo() {
        if (mExtraInfo != null && !mExtraInfo.equals("")) {
            return Jackson.fromJsonString(mExtraInfo, Map.class);
        }
        return null;
    }

    public void setExtraInfo(Map mExtraInfo) {
        this.mExtraInfo = Jackson.toJsonString(mExtraInfo);
    }

    public String getCity() {
        return mCity;
    }

    public void setCity(String mCity) {
        this.mCity = mCity;
    }

    public String getCountry() {
        return mCountry;
    }

    public void setCountry(String mCountry) {
        this.mCountry = mCountry;
    }

    public boolean isPrivate() {
        return mPrivate;
    }

    public void setIsPrivate(boolean mPrivate) {
        this.mPrivate = mPrivate;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String mDescription) {
        this.mDescription = mDescription;
    }

    public String getVideoFileName() {
        return videoFileName;
    }

    public String getFullVideoUrl() {
        return baseUrl + "/" + videoFileName;
    }

    public String getFullHistoricalVideoUrl() {
        return baseUrl + "/" + HISTORICAL_VIDEO_FILENAME;
    }

    public String getFullThumbnailUrl() {
        return thumbnailBaseUrl + "/" + getThumbnailFileName();
    }

    public String getThumbnailFileName() {
        return FILENAME_PREFIX_POSTER + this.thumbnailFileName;
    }

    public String getFBThumbnailFileName() {
        return FILENAME_PREFIX_WIDE + thumbnailFileName;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getShareUrl() {
        return shareUrl;
//        return DUMMY_SHARE_URL;
    }

    public String getThumbnailBaseUrl() {
        return thumbnailBaseUrl;
    }

    public void setThumbnailBaseUrl(String thumbnailBaseUrl) {
        this.thumbnailBaseUrl = thumbnailBaseUrl;
    }

    @Override
    public int compareTo(Stream another) {
        return another.getTimeStarted().compareTo(getTimeStarted());
    }

    @Override
    public String toString() {
        return Jackson.toJsonPrettyString(this);
    }

    public Stream(String mStreamId) {
        this.mStreamId = mStreamId;
    }

    public Stream() {
        super();
    }

    public boolean isStreaming() {
        return (this.streamState == Constants.kStreamStateStreaming);
    }

    public boolean isReady() {
        return (this.streamState == Constants.kStreamStateReady);
    }

    public com.mobileman.moments.android.backend.model.User getCreatedBy() {
        return createdBy;
    }
}
