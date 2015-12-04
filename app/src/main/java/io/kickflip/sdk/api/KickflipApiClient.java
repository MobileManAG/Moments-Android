package io.kickflip.sdk.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Handler;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.GenericData;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.MomentsApplication;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.backend.model.FBUser;
import com.mobileman.moments.android.backend.model.FriendsList;
import com.mobileman.moments.android.backend.model.HlsStreamList;
import com.mobileman.moments.android.backend.model.StreamMetadata;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.backend.model.UsersList;
import com.mobileman.moments.android.frontend.LocalPersistence;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.exception.KickflipException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Kickflip API Client
 * <p/>
 * After construction, requests can be immediately performed.
 * The client will handle acquiring and refreshing OAuth
 * Access tokens as needed.
 * <p/>
 * The client is intended to manage a unique Kickflip user per Android device installation.
 */
// TODO: Standardize Kickflip server error responses to have detail message
public class KickflipApiClient extends OAuthClient {

    private static final String SERIALIZED_FILESTORE_NAME = "CURRENTLY_LOGGED_USER";

    private static final String TAG                 = "KickflipApiClient";

    private static String BASE_URL;
    static {
        switch(MomentsApplication.ENDPOINT){
            case Production: BASE_URL = "https://apps.mobileman.ch/moments"; break;
            case Crm: BASE_URL = "https://crm.mobileman.ch:8443/moments-core"; break;
            case MpLocal: BASE_URL = "http://192.168.111.11/moments-core"; break;
        }
    }

    private static final boolean VERBOSE            = !MomentsApplication.isOnProduction();

    private static final String NEW_USER            = "/users/signin";
    private static final String LOGOUT_USER         = "/users/signout";
    private static final String START_STREAM        = "/streams/start";
    private static final String STOP_STREAM         = "/streams";
    private static final String STREAM_STATE        = "/streams";
    private static final String STREAM_LIST         = "/streams/live";
    private static final String USER_PROFILE        = "/users/profile";
    private static final String STREAM_METADATA     = "/streams";
    private static final String FRIENDS_LIST        = "/friends";
    private static final String MY_MOMENTS_LIST     = "/users/moments";
    private static final String ADD_COMMENT         = "/streams";
    private static final String BLOCK_STATE         = "/friends";
    private static final String DELETE_STREAM       = "/streams";

    private static final String GET_USER_PUBLIC     = "/user/info";
    private static final String GET_USER_PRIVATE    = "/user/uuid";
    private static final String EDIT_USER           = "/user/change";
    private static final String SET_META            = "/stream/change";
    private static final String GET_META            = "/stream/info";
    private static final String FLAG_STREAM         = "/stream/flag";
    private static final String SEARCH_KEYWORD      = "/search";
    private static final String SEARCH_USER         = "/search/user";
    private static final String SEARCH_GEO          = "/search/location";
    private static final String API_VERSION         = "/v1";

    private static List<String> noauthEndpointsArray =  Arrays.asList(NEW_USER);

    private static final int MAX_EOF_RETRIES        = 1;
    private static int UNKNOWN_ERROR_CODE           = R.integer.generic_error;    // Error code used when none provided from server

    private JsonObjectParser mJsonObjectParser;             // Re-used across requests
    private JsonFactory mJsonFactory;                       // Re-used across requests

    private Handler mCallbackHandler;                       // Ensure callbacks are posted to consistent thread
    private User momentsUser;

    /**
     * Construct a KickflipApiClient. All callbacks from this client will occur
     * on the current calling thread.
     *
     * @param appContext Your Application Context
     * @param key        Your Kickflip Account Key
     * @param secret     Your Kickflip Account Secret
     */
//    public KickflipApiClient(Context appContext, String key, String secret, FBUser fbUser) {
//        this(appContext, key, secret, null);
//    }

    /**
     * Construct a KickflipApiClient. All callbacks from this client will occur
     * on the current calling thread.
     *
     * @param appContext Your Application Context
     * @param cb         A callback to be notified when the provided Kickflip credentials are verified
     */
    public KickflipApiClient(Context appContext, FBUser fbUser, KickflipCallback cb) {
        super(appContext, new OAuthConfig()
                .setCredentialStoreName("KF")
                .setClientId("xxx")
                .setClientSecret("yyy")
                .setAccessTokenRequestUrl(BASE_URL + "/o/token/")
                .setAccessTokenAuthorizeUrl(BASE_URL + "/o/authorize/"), MomentsApplication.isOnDevServer());
        mCallbackHandler = new Handler();
        initialize(cb, fbUser);
    }

    public KickflipApiClient(Context appContext, User user, KickflipCallback cb) {
        super(appContext, new OAuthConfig()
                .setCredentialStoreName("KF")
                .setClientId("xxx")
                .setClientSecret("yyy")
                .setAccessTokenRequestUrl(BASE_URL + "/o/token/")
                .setAccessTokenAuthorizeUrl(BASE_URL + "/o/authorize/"), MomentsApplication.isOnDevServer());
        mCallbackHandler = new Handler();
        this.momentsUser = user;
    }

    private void initialize(KickflipCallback cb, FBUser fbUser) {
        if (getActiveUser() == null) {
            createNewUser(fbUser, cb);
        } else {
            postResponseToCallback(cb, getActiveUser());
            if (VERBOSE)
                Log.i(TAG, "Credentials stored " + getAWSCredentials());
        }
    }

    /**
     * Create a new Kickflip User.
     * The User created as a result of this request is cached and managed by this KickflipApiClient
     * throughout the life of the host Android application installation.
     * <p/>
     * The other methods of this client will be performed on behalf of the user created by this request,
     * unless noted otherwise.
     *
     * @param cb          This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                    or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void createNewUser(FBUser fbUser,  final KickflipCallback cb) {
//        GenericData data = new GenericData();
        Map<String, Object> json = new HashMap<String, Object>();
        json.put("facebookID", fbUser.getFacebookID());
        json.put("account_type", fbUser.getAcccount_type());
        json.put("firstName", fbUser.getFirstName());
        json.put("userName", fbUser.getUserName());
        json.put("gender", fbUser.getGender());
        json.put("email", fbUser.getEmail());
        json.put("token", fbUser.getToken().getToken());
        json.put("lastName", fbUser.getLastName());
        json.put("pushNotificationID", fbUser.getPushNotificationId());

        Map<String, Object> deviceHashmap = new HashMap<String, Object>();
        json.put("device", deviceHashmap);
        deviceHashmap.put("deviceType", Constants.kDeviceTypeAndroid);

        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), json);
        post(NEW_USER, jsonHttpContent, User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "createNewUser response: " + response);

                storeNewUserResponse((User) response, "");
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "createNewUser Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    public void logoutUser(final KickflipCallback cb) {
        Map<String, Object> json = new HashMap<String, Object>();
        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), json);
        post(LOGOUT_USER, jsonHttpContent, Response.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "logoutUser response: " + response);

                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "logoutUser Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    public void userProfile(String userId, final KickflipCallback cb) {
        get(USER_PROFILE + "/" + userId, User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "userProfile response: " + response);

                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "userProfile Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    public void streamMetadata(String streamId, long lastTimeStamp, final KickflipCallback cb) {
        String url = STREAM_METADATA + "/" + streamId + "/metadata";
        if (lastTimeStamp > 0) {
            url = url + "/" + Long.toString(lastTimeStamp);
        }

        get(url, StreamMetadata.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "streamMetadata response: " + response);

                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "userProfile Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }


    public void addComment(String streamId, String commentText, final KickflipCallback cb) {
        Map<String, Object> hashMapData = new HashMap<String, Object>();
        hashMapData.put("text", commentText);

        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), hashMapData);
        post(ADD_COMMENT + "/" + streamId + "/comment", jsonHttpContent, StreamMetadata.class, cb);
    }

    public void changeBlockState(String userId, boolean blockFriend, final KickflipCallback cb) {
        Map<String, Object> hashMapData = new HashMap<String, Object>();


        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), hashMapData);
        put(BLOCK_STATE + "/" + userId + "/block/" + (blockFriend ? "true" : "false"), jsonHttpContent, Response.class, cb);
    }

    /**
     * Login an exiting Kickflip User and make it active.
     *
     * @param username The Kickflip user's username
     * @param password The Kickflip user's password
     * @param cb       This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                 or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void loginUser(String username, final String password, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("username", username);
        data.put("password", password);

        post(GET_USER_PRIVATE, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "loginUser response: " + response);
                storeNewUserResponse((User) response, password);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "loginUser Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    /**
     * Set the current active user's meta info. Pass a null argument to leave it as-is.
     *
     * @param newPassword the user's new password
     * @param email       the user's new email address
     * @param displayName The desired display name
     * @param extraInfo   Arbitrary String data to associate with this user.
     * @param cb          This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                    or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void setUserInfo(final String newPassword, String email, String displayName, Map extraInfo, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        final String finalPassword;
        if (newPassword != null){
            data.put("new_password", newPassword);
            finalPassword = newPassword;
        } else {
            finalPassword = getPasswordForActiveUser();
        }
        if (email != null) data.put("email", email);
        if (displayName != null) data.put("display_name", displayName);
        if (extraInfo != null) data.put("extra_info", Jackson.toJsonString(extraInfo));

        post(EDIT_USER, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "setUserInfo response: " + response);
                storeNewUserResponse((User) response, finalPassword);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "setUserInfo Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }

    /**
     * Get public user info
     *
     * @param username The Kickflip user's username
     * @param cb       This callback will receive a User in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *                 or an Exception {@link io.kickflip.sdk.api.KickflipCallback#onError(io.kickflip.sdk.exception.KickflipException)}.
     */
    public void getUserInfo(String username, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("username", username);

        post(GET_USER_PUBLIC, new UrlEncodedContent(data), User.class, new KickflipCallback() {
            @Override
            public void onSuccess(final Response response) {
                if (VERBOSE)
                    Log.i(TAG, "getUserInfo response: " + response);
                postResponseToCallback(cb, response);
            }

            @Override
            public void onError(final KickflipException error) {
                Log.w(TAG, "getUserInfo Error: " + error);
                postExceptionToCallback(cb, error);
            }
        });
    }


    /**
     * Start a new Stream. Must be called after
     * io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)
     * Delivers stream endpoint destination data via a {@link io.kickflip.sdk.api.KickflipCallback}.
     *
     * @param cb This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, RtmpStream, etc.
     */
    public void startStream(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        checkNotNull(stream);
        startStreamWithUser(getActiveUser(), stream, cb);
    }

    /**
     * Start a new Stream owned by the given User. Must be called after
     * io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)
     * Delivers stream endpoint destination data via a {@link io.kickflip.sdk.api.KickflipCallback}.
     *
     * @param user The Kickflip User on whose behalf this request is performed.
     * @param cb   This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *             depending on the Kickflip account type. Implementors should
     *             check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    private void startStreamWithUser(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(user);
        checkNotNull(stream);
        // TODO: Be HLS / RTMP Agnostic
        Map<String, Object> hashMapData = new HashMap<String, Object>();


//        hashMapData.put("uuid", user.getUuid());

        hashMapData.put("videoFileName", stream.getVideoFileName());
//        hashMapData.put("thumbnailFileName", stream.getThumbnailFileName());
        hashMapData.put("text", stream.getTitle());
        if ((stream.getLocation() != null) && (stream.getLocation().getLatitude() > 0)) {
            Map<String, Object> locationHashMapData = new HashMap<String, Object>();
            hashMapData.put("location", locationHashMapData);
            locationHashMapData.put("latitude", stream.getLocation().getLatitude());
            locationHashMapData.put("longitude", stream.getLocation().getLongitude());
        }

        if (stream.getDescription() != null) {
            hashMapData.put("description", stream.getDescription());
        }
        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), hashMapData);
        post(START_STREAM, jsonHttpContent, HlsStream.class, cb);
    }

    /**
     * Stop a Stream. Must be called after
     * io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback) and
     * {@link io.kickflip.sdk.api.KickflipApiClient#startStream(io.kickflip.sdk.api.json.Stream, KickflipCallback)}
     *
     * @param cb This callback will receive a Stream subclass in {@link io.kickflip.sdk.api.KickflipCallback#onSuccess(io.kickflip.sdk.api.json.Response)}
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    public void stopStream(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        stopStream(getActiveUser(), stream, cb);
    }

    /**
     * Stop a Stream owned by the given Kickflip User.
     *
     * @param cb This callback will receive a Stream subclass in #onSuccess(response)
     *           depending on the Kickflip account type. Implementors should
     *           check if the response is instanceof HlsStream, StartRtmpStreamResponse, etc.
     */
    private void stopStream(User user, Stream stream, final KickflipCallback cb) {
        checkNotNull(stream);
        // TODO: Be HLS / RTMP Agnostic
        // TODO: Add start / stop lat lon to Stream?
        Map<String, Object> hashMapData = new HashMap<String, Object>();
        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), hashMapData);
        post(STOP_STREAM + "/" + stream.getStreamId() + "/stop", jsonHttpContent, HlsStream.class, cb);
    }

    public void updateStreamState(Stream stream, int newState, final KickflipCallback cb) {
        Map<String, Object> hashMapData = new HashMap<String, Object>();

        String url = STREAM_STATE + "/" + stream.getStreamId();
        if (newState == Constants.kStreamStateReady) {
            url = url + "/" + "ready";
        } else if (newState == Constants.kStreamStateStreaming) {
            url = url + "/" + "streaming";
        }

        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), hashMapData);
        put(url, jsonHttpContent, HlsStream.class, cb);
    }

    public void getStreamList(final KickflipCallback cb) {
        get(STREAM_LIST, UsersList.class, cb);
    }

    public void getFriendsList(final KickflipCallback cb) {
        get(FRIENDS_LIST, FriendsList.class, cb);
    }

    public void getMyMomentsList(final KickflipCallback cb) {
        get(MY_MOMENTS_LIST, HlsStreamList.class, cb);
    }

    public void deleteStream(String streamId, final KickflipCallback cb) {
        delete(DELETE_STREAM + "/" + streamId, Response.class, cb);
    }

    /**
     * Send Stream Metadata for a {@link io.kickflip.sdk.api.json.Stream}.
     * The target Stream must be owned by the User created with io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback}
     * from this KickflipApiClient.
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    public void setStreamInfo(Stream stream, final KickflipCallback cb) {
        Map<String, Object> hashMapData = new HashMap<String, Object>();

        Map<String, Object> locationHashMapData = new HashMap<String, Object>();
        hashMapData.put("location", locationHashMapData);
        if (stream.getLocation() != null) {
            locationHashMapData.put("latitude", stream.getLocation().getLatitude());
            locationHashMapData.put("longitude", stream.getLocation().getLongitude());
        }

        JsonHttpContent jsonHttpContent = new JsonHttpContent(new JacksonFactory(), hashMapData);
        put(STREAM_STATE + "/" + stream.getStreamId(), jsonHttpContent, HlsStream.class, cb);
    }

    /**
     * Get Stream Metadata for a a public {@link io.kickflip.sdk.api.json.Stream}.
     * The target Stream must belong a User of your Kickflip app.
     *
     * @param stream the {@link io.kickflip.sdk.api.json.Stream} to get Meta data for
     * @param cb     A callback to receive the updated Stream upon request completion
     */
    public void getStreamInfo(Stream stream, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", stream.getStreamId());

        post(GET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get Stream Metadata for a a public {@link io.kickflip.sdk.api.json.Stream#mStreamId}.
     * The target Stream must belong a User within your Kickflip app.
     * <p/>
     * This method is useful when digesting a Kickflip.io/<stream_id> url, where only
     * the StreamId String is known.
     *
     * @param streamId the stream Id of the given stream. This is the value that appears
     *                 in urls of form kickflip.io/<stream_id>
     * @param cb       A callback to receive the current {@link io.kickflip.sdk.api.json.Stream} upon request completion
     */
    public void getStreamInfo(String streamId, final KickflipCallback cb) {
        GenericData data = new GenericData();
        data.put("stream_id", streamId);

        post(GET_META, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Flag a {@link io.kickflip.sdk.api.json.Stream}. Used when the active Kickflip User does not own the Stream.
     * <p/>
     * To delete a recording the active Kickflip User owns, use
     * {@link io.kickflip.sdk.api.KickflipApiClient#setStreamInfo(io.kickflip.sdk.api.json.Stream, KickflipCallback)}
     *
     * @param stream The Stream to flag.
     * @param cb     A callback to receive the result of the flagging operation.
     */
    public void flagStream(Stream stream, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("uuid", getActiveUser().getUuid());
        data.put("stream_id", stream.getStreamId());

        post(FLAG_STREAM, new UrlEncodedContent(data), Stream.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream} objects created by the given Kickflip User.
     *
     * @param username the target Kickflip username
     * @param cb       A callback to receive the resulting List of Streams
     */
    public void getStreamsByUsername(String username, int pageNumber, int itemsPerPage, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        addPaginationData(pageNumber, itemsPerPage, data);
        data.put("uuid", getActiveUser().getUuid());
        data.put("username", username);
        post(SEARCH_USER, new UrlEncodedContent(data), UsersList.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream}s containing a keyword.
     * <p/>
     * This method searches all public recordings made by Users of your Kickflip app.
     *
     * @param keyword The String keyword to query
     * @param cb      A callback to receive the resulting List of Streams
     */
    public void getStreamsByKeyword(String keyword, int pageNumber, int itemsPerPage, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        addPaginationData(pageNumber, itemsPerPage, data);
        data.put("uuid", getActiveUser().getUuid());
        if (keyword != null) {
            data.put("keyword", keyword);
        }
        post(SEARCH_KEYWORD, new UrlEncodedContent(data), UsersList.class, cb);
    }

    /**
     * Get a List of {@link io.kickflip.sdk.api.json.Stream}s near a geographic location.
     * <p/>
     * This method searches all public recordings made by Users of your Kickflip app.
     *
     * @param location The target Location
     * @param radius   The target Radius in meters
     * @param cb       A callback to receive the resulting List of Streams
     */
    public void getStreamsByLocation(Location location, int radius, int pageNumber, int itemsPerPage, final KickflipCallback cb) {
        if (!assertActiveUserAvailable(cb)) return;
        GenericData data = new GenericData();
        data.put("uuid", getActiveUser().getUuid());
        data.put("lat", location.getLatitude());
        data.put("lon", location.getLongitude());
        if (radius != 0) {
            data.put("radius", radius);
        }
        post(SEARCH_GEO, new UrlEncodedContent(data), UsersList.class, cb);
    }

    /**
     * Do a POST Request, creating a new user if necessary
     *
     * @param endpoint      Kickflip endpoint. e.g /user/new
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String endpoint, final Class responseClass, final KickflipCallback cb) {
        post(endpoint, null, responseClass, cb);
    }

    /**
     * Do a POST Request, creating a new user if necessary
     *
     * @param endpoint      Kickflip endpoint. e.g /user/new
     * @param body          POST body
     * @param responseClass Class of the expected response
     * @param cb            Callback that will receive an instance of responseClass
     */
    private void post(final String endpoint, final HttpContent body, final Class responseClass, final KickflipCallback cb) {
/*        acquireAccessToken(new OAuthCallback() {
            @Override
            public void onSuccess(HttpRequestFactory requestFactory) {
                HttpRequestFactory httpRequestFactory = getRequestFactoryNonAuthenticated();
                request(httpRequestFactory, METHOD.POST, makeApiUrl(endpoint, !noauthEndpointsArray.contains(endpoint)), body, responseClass, cb);
            }

            @Override
            public void onFailure(Exception e) {
                // communication temporally disabled
                postExceptionToCallback(cb, UNKNOWN_ERROR_CODE);
            }
        });
   */
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpRequestFactory httpRequestFactory = getRequestFactoryNonAuthenticated();
                boolean authenticated = !noauthEndpointsArray.contains(endpoint);
                request(httpRequestFactory, METHOD.POST, makeApiUrl(endpoint, authenticated), body, responseClass, authenticated, cb);
            }
        }).start();
    }

    private void get(final String endpoint,  final Class responseClass, final KickflipCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpRequestFactory httpRequestFactory = getRequestFactoryNonAuthenticated();
                boolean authenticated = !noauthEndpointsArray.contains(endpoint);
                request(httpRequestFactory, METHOD.GET, makeApiUrl(endpoint, authenticated), null, responseClass, authenticated, cb);
            }
        }).start();
    }

    private void delete(final String endpoint,  final Class responseClass, final KickflipCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpRequestFactory httpRequestFactory = getRequestFactoryNonAuthenticated();
                boolean authenticated = !noauthEndpointsArray.contains(endpoint);
                request(httpRequestFactory, METHOD.DELETE, makeApiUrl(endpoint, authenticated), null, responseClass, authenticated, cb);
            }
        }).start();
    }

    private void put(final String endpoint,  final HttpContent body, final Class responseClass, final KickflipCallback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HttpRequestFactory httpRequestFactory = getRequestFactoryNonAuthenticated();
                    boolean authenticated = !noauthEndpointsArray.contains(endpoint);
                    request(httpRequestFactory, METHOD.PUT, makeApiUrl(endpoint, authenticated), body, responseClass, authenticated, cb);
                } catch(Exception e) {
                    e.printStackTrace();
                    cb.onError(new KickflipException(e.getMessage(), 0));
                }
            }
        }).start();
    }

    private void request(HttpRequestFactory requestFactory, final METHOD method, final String url, final HttpContent content, final Class responseClass, final KickflipCallback cb) {
        this.request(requestFactory, method, url, content, responseClass, true, cb);
    }

    private void request(HttpRequestFactory requestFactory, final METHOD method, final String url, final HttpContent content, final Class responseClass, boolean authenticated, final KickflipCallback cb) {
        String body = "";
        if (VERBOSE) {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            if (content != null) {
                try {
                    content.writeTo(byteStream);
                    body = new String(byteStream.toByteArray(), "UTF-8");
                    Log.d(TAG, body);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, String.format("REQUEST: %S : %s body: %s", method, shortenUrlString(url), (content == null ? "" : body)));
        HttpRequest request = null;
        try {
            switch (method) {
                case GET:
                    request = requestFactory.buildGetRequest(
                            new GenericUrl(url)).setParser(getJsonObjectParser());
                    break;
                case POST:
                    request = requestFactory.buildPostRequest(
                            new GenericUrl(url), content).setParser(getJsonObjectParser());
                    break;
                case PUT:
                    request = requestFactory.buildPutRequest(
                            new GenericUrl(url), content).setParser(getJsonObjectParser());
                    break;
                case DELETE:
                    request = requestFactory.buildDeleteRequest(
                            new GenericUrl(url)).setParser(getJsonObjectParser());
                    break;


            }
            if (authenticated) {
                if (momentsUser == null) {
                    if (VERBOSE) {
                        Log.e(TAG, "Calling authenticated service with NO authenticated user!!!");
                    }
                } else {
                    String userUuid = momentsUser.getUuid();
                    if (VERBOSE) {
                        Log.e(TAG, "Calling authenticated service with user ID: " + userUuid);
                    }
                    request.getHeaders().setBasicAuthentication(userUuid, userUuid);
                }
            }
            executeAndRetryRequest(request, responseClass, cb);
        } catch (final IOException exception) {
            // First try to handle as HttpResponseException
            try {
                HttpResponseException httpException = (HttpResponseException) exception;
                // If this cast suceeds, the HTTP Status code must be >= 300
                Log.i(TAG, "HttpException: " + httpException.getStatusCode());
                boolean missingPrivilegesError = false;
                switch (httpException.getStatusCode()) {
                    case 403:
                        // OAuth Access Token invalid
                        Log.i(TAG, "Error 403: OAuth Token appears invalid. Clearing");
                        clearAccessToken();
                        acquireAccessToken(new OAuthCallback() {
                            @Override
                            public void onSuccess(HttpRequestFactory oauthRequestFactory) {
                                request(oauthRequestFactory, method, url, content, responseClass, cb);
                            }

                            @Override
                            public void onFailure(Exception e) {
                                postExceptionToCallback(cb, "Error 403: OAuth Token appears invalid", false);
                            }
                        });
                        break;
                    case 400:
                        // Bad Client Credentials
                        Log.e(TAG, "Error 400: Check your Client key and secret");
                        break;
                    case 422:
                        Log.e(TAG, "Error 422 - missing FB privileges");
                        missingPrivilegesError = true;
                        break;
                    default:
                        Log.w(TAG, String.format("Unhandled Http Error %d : %s",
                                httpException.getStatusCode(),
                                httpException.getMessage()));
                }
                if (VERBOSE)
                    Log.i(TAG, "RESPONSE: " + shortenUrlString(url) + " " + exception.getMessage());
                postExceptionToCallback(cb, exception.getMessage(), missingPrivilegesError);
            } catch (ClassCastException e) {
                // A non-HTTP releated error occured.
                Log.w(TAG, String.format("Unhandled Error: %s. Stack trace follows:", e.getMessage()));
                exception.printStackTrace();
                postExceptionToCallback(cb, exception.getMessage(), false);
            }
        }
    }

    /**
     * Execute a HTTPRequest and retry up to {@link io.kickflip.sdk.api.KickflipApiClient#MAX_EOF_RETRIES} times if an EOFException occurs.
     * This is an attempt to address what appears to be a bug in NetHttpTransport
     * <p/>
     * See <a href="https://code.google.com/p/google-api-java-client/issues/detail?id=869&can=4&colspec=Milestone%20Priority%20Component%20Type%20Summary%20ID%20Status%20Owner">This issue</a>
     *
     * @param request
     * @param responseClass
     * @param cb
     * @throws IOException
     */
    private void executeAndRetryRequest(HttpRequest request, Class responseClass, KickflipCallback cb) throws IOException {
        int numRetries = 0;
        while (numRetries < MAX_EOF_RETRIES + 1) {
            try {
                executeAndHandleHttpRequest(request, responseClass, cb);
                // If executeAndHandleHttpRequest completes without throwing EOFException
                // we're good
                return;
            } catch (EOFException eof) {
                if (VERBOSE) Log.i(TAG, "Got EOFException. Retrying..");
                // An EOFException may be due to a bug in the way Connections are recycled
                // within the NetHttpTransport package. Ignore and retry
            }
            numRetries++;
        }
        postExceptionToCallback(cb, getContext().getResources().getString(R.string.unknownError), false);
    }

    private void executeAndHandleHttpRequest(HttpRequest request, Class responseClass, KickflipCallback cb) throws IOException {
        handleHttpResponse(request.execute(), responseClass, cb);
    }

    /**
     * Verify HTTP response was successful
     * and pass to handleKickflipResponse.
     * <p/>
     * If we have an HttpResponse at all, it means
     * the status code was < 300, so as far as http inspection
     * goes, this method simply enforces status code of 200
     *
     * @param response
     * @param responseClass
     * @param cb            Must not be null
     * @throws IOException
     */
    private void handleHttpResponse(HttpResponse response, Class<? extends Response> responseClass, KickflipCallback cb) throws IOException {
        //Object parsedResponse = response.parseAs(responseClass);
        if (isSuccessResponse(response)) {
            // Http Success
            handleKickflipResponse(response, responseClass, cb);
            //cb.onSuccess(responseClass.cast(parsedResponse));
        } else {
            // Http Failure
            String errorMessage = getContext().getString(R.string.unknownError);
            if (response.getStatusCode() != 200) {
                errorMessage = errorMessage + " (Http " + Integer.toString(response.getStatusCode()) + ")";
            }
            if (VERBOSE)
                Log.i(TAG, String.format("RESPONSE (F): %s body: %s", shortenUrlString(response.getRequest().getUrl().toString()), response.getContent().toString()));
            postExceptionToCallback(cb, errorMessage, false);
        }
    }

    /**
     * Parse the HttpResponse as the appropriate Response subclass
     *
     * @param response
     * @param responseClass
     * @param cb
     * @throws IOException
     */
    private void handleKickflipResponse(HttpResponse response, Class<? extends Response> responseClass, KickflipCallback cb) throws IOException {
        if (cb == null) return;
        HashMap responseMap = null;
        Response kickFlipResponse = null;
        if (response.getContentType() != null) {
            try {
                kickFlipResponse = response.parseAs(responseClass);
            } catch (IOException e) {
                if (e.getMessage() != null) {
                    Log.e(TAG, e.getMessage());
                }
            }
        } else if (response.isSuccessStatusCode()) {
            kickFlipResponse = new Response();
        }
        if (VERBOSE)
            Log.i(TAG, String.format("RESPONSE: %s body: %s", shortenUrlString(response.getRequest().getUrl().toString()), Jackson.toJsonPrettyString(kickFlipResponse)));
//        if (Stream.class.isAssignableFrom(responseClass)) {
//            if( ((String) responseMap.get("stream_type")).compareTo("HLS") == 0){
//                kickFlipResponse = response.parseAs(HlsStream.class);
//            } else if( ((String) responseMap.get("stream_type")).compareTo("RTMP") == 0){
//                // TODO:
//            }
//        } else if(User.class.isAssignableFrom(responseClass)){
//            kickFlipResponse = response.parseAs(User.class);
//        }
        String errorMessage = getContext().getString(R.string.unknownError);
        if (kickFlipResponse == null) {
            postExceptionToCallback(cb, errorMessage, false);
        } else if (!kickFlipResponse.isSuccessful()) {
            postExceptionToCallback(cb, errorMessage, false);
        } else {
            postResponseToCallback(cb, kickFlipResponse);
        }
    }

    private void storeNewUserResponse(User response, String password) {
        this.momentsUser = response;
        LocalPersistence.writeObjectToFile(this.getContext(), this.momentsUser, SERIALIZED_FILESTORE_NAME);
/*        getStorage().edit()
                .putString("app_name", response.getApp())
                .putString("name", response.getName())
                .putString("password", password)
                .putString("uuid", response.getUUID())
                .putString("uuid", response.getUUID())
                .apply();
*/
    }

    private String getPasswordForActiveUser() {
        return getStorage().getString("password", null);
    }

    private boolean isUserCached() {
        //TODO: Ensure this use belongs to the current app
        return getStorage().contains("uuid");
    }

    private BasicAWSCredentials getAWSCredentials() {
        return new BasicAWSCredentials(
                getStorage().getString("aws_access_key", ""),
                getStorage().getString("aws_secret_key", ""));

    }


    public User getActiveUser() {
        return getActiveUser(null);
    }

    /**
     * Get the current active Kickflip User. If no User has been created, returns null.
     * <p/>
     * This will be the User created on the last call to
     * io.kickflip.sdk.api.KickflipApiClient#createNewUser(KickflipCallback)
     *
     * @return
     */
    public User getActiveUser(Context context) {
        SharedPreferences prefs = getStorage();
        if (momentsUser == null) {
            if (prefs.contains("uuid") && prefs.contains("name")) {
                if (context != null) {
                    Object userObject = LocalPersistence.readObjectFromFile(context, SERIALIZED_FILESTORE_NAME);
                    if (userObject != null) {
                        User user = (User) userObject;
                        return user;
                    }
                }
            }
        } else {
            return momentsUser;
        }
        return null;
    }

    private String getAWSBucket() {
        return getStorage().getString("app_name", "");
    }

    private JsonFactory getJsonFactory() {
        if (mJsonFactory == null)
            mJsonFactory = new JacksonFactory();
        return mJsonFactory;
    }

    private JsonObjectParser getJsonObjectParser() {
        if (mJsonObjectParser == null)
            mJsonObjectParser = new JsonObjectParser(getJsonFactory());
        return mJsonObjectParser;
    }

    private void postExceptionToCallback(final KickflipCallback cb, final String errorMessage, boolean isMissingPrivilegesError) {
        KickflipException error = new KickflipException(errorMessage, isMissingPrivilegesError);
        postExceptionToCallback(cb, error);
    }
/*
    private void postExceptionToCallback(final KickflipCallback cb, final int resourceCodeId) {
        final int errorCode = getContext().getResources().getInteger(resourceCodeId);
        final String message = getContext().getResources().getStringArray(R.array.error_messages)[errorCode];
        KickflipException error = new KickflipException(message, errorCode);
        postExceptionToCallback(cb, error);
    }
*/
    private void postExceptionToCallback(final KickflipCallback cb, final KickflipException exception) {
        if (cb != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onError(exception);
                }
            });
        }
    }

    private void postResponseToCallback(final KickflipCallback cb, final Response response) {
        if (cb != null) {
            mCallbackHandler.post(new Runnable() {
                @Override
                public void run() {
                    cb.onSuccess(response);
                }
            });
        }
    }

    /**
     * Given a string like https://api.kickflip.io/api/search
     * return /api/search
     *
     * @param url
     * @return
     */
    private String shortenUrlString(String url) {
        return url.substring(BASE_URL.length());
    }

    /**
     * Check if a Stream is owned by the active Kickflip User.
     *
     * @param stream the Stream to test.
     * @return true if the active Kickflip User owns the Stream. false otherwise.
     */
    public boolean activeUserOwnsStream(Stream stream) {
        return getActiveUser().getName().compareTo(stream.getOwnerName()) == 0;
    }

    private boolean assertActiveUserAvailable(KickflipCallback cb) {
        if (getActiveUser() == null) {
            Log.e(TAG, "getStreamsByKeyword called before user acquired. If this request needs to be performed on app start," +
                    "call it from the KickflipCallback provided to setup()");
            if (cb != null) {
                postExceptionToCallback(cb, "No active user found", false);
            }
            return false;
        }
        return true;
    }

    private static enum METHOD {GET, POST, PUT, DELETE}

    private String generateRandomPassword() {
        return new BigInteger(130, new SecureRandom()).toString(32);
    }

    private void addPaginationData(int pageNumber, int itemsPerPage, GenericData target) {
        target.put("results_per_page", itemsPerPage);
        target.put("page", pageNumber);
    }

    private String makeApiUrl(String endpoint, boolean authenticated) {
        return BASE_URL + (authenticated ? "/api/auth" : "/api/noauth") + API_VERSION + endpoint;
    }

}
