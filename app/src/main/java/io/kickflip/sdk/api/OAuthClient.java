package io.kickflip.sdk.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayDeque;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Manage the OAuth Client Credentials authentication
 * to be negotiated prior to any API Requests being performed
 *
 * @hide
 */
public abstract class OAuthClient {
    private static final String TAG = "OAuthClient";
    private static final boolean VERBOSE = false;

    // For SharedPreferences storage
    private final String ACCESS_TOKEN_KEY = "AT";
    private final String CLIENT_ID = "CID";

    private HttpRequestFactory mRequestFactory;         // RequestFactory cached for life of mOAuthAccessToken
    private String mOAuthAccessToken;
    private OAuthConfig mConfig;                        // Immutable OAuth Configuration
    private SharedPreferences mStorage;
    private Context mContext;                           // Application Context
    private ArrayDeque<OAuthCallback> mCallbackQueue;   // Queued callbacks awaiting OAuth registration
    private boolean mOauthInProgress;                   // Is an OAuth authentication flow in progress

    public OAuthClient(Context context, OAuthConfig config, boolean devEnvironment) {
        mConfig = config;
        mContext = context;
        mStorage = context.getSharedPreferences(mConfig.getCredentialStoreName(), Context.MODE_PRIVATE);
        mOauthInProgress = false;
        mCallbackQueue = new ArrayDeque<>();

        if (devEnvironment) {
            disableCertificateValidation();
        }
    }

    public Context getContext() {
        return mContext;
    }

    public OAuthConfig getConfig() {
        return mConfig;
    }

    public SharedPreferences getStorage() {
        return mStorage;
    }

    /**
     * Force clear and re-acquire an OAuth Acess Token
     */
    protected void refreshAccessToken() {
        refreshAccessToken(null);
    }

    /**
     * Force clear and re-acquire an OAuth Acess Token
     * cb is always called on a background thread
     */
    protected void refreshAccessToken(final OAuthCallback cb) {
        clearAccessToken();
        acquireAccessToken(cb);
    }

    /**
     * Asynchronously attempt to jsonRequest an OAuth Access Token
     */
    protected void acquireAccessToken() {
        acquireAccessToken(null);
    }

    /**
     * Asynchronously attempt to acquire an OAuth Access Token
     *
     * @param cb called when AccessToken is acquired. Always called
     *           on a background thread suitable for networking.
     */
    protected void acquireAccessToken(final OAuthCallback cb) {
/*        if (isAccessTokenCached()) {
            // Execute the callback immediately with cached OAuth credentials
            if (VERBOSE) Log.d(TAG, "Access token cached");
            if (cb != null) {
                // Ensure networking occurs off the main thread
                // TODO: Use an ExecutorService and expose an application shutdown method to shutdown?
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        cb.onSuccess(getRequestFactoryFromCachedCredentials());
                    }
                }).start();
            }
        } else if (mOauthInProgress && cb != null) {
            // Add the callback to the queue for execution when outstanding OAuth negotiation complete
            mCallbackQueue.add(cb);
            if (VERBOSE) Log.i(TAG, "Adding cb to queue");
        } else {
            mOauthInProgress = true;
            // Perform an OAuth Client Credentials Request
            // TODO: Replace with new Thread()
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    TokenResponse response = null;
                    try {
                        if (VERBOSE)
                            Log.i(TAG, "Fetching OAuth " + mConfig.getAccessTokenRequestUrl());
                        response = new ClientCredentialsTokenRequest(new NetHttpTransport(), new JacksonFactory(), new GenericUrl(mConfig.getAccessTokenRequestUrl()))
                                .setGrantType("client_credentials")
                                .setClientAuthentication(new BasicAuthentication(mConfig.getClientId(), mConfig.getClientSecret()))
                                .execute();
                    } catch (IOException e) {
                        // TODO: Alert user Kickflip down
                        //       or client credentials invalid
                        if (cb != null) {
                            postExceptionToCallback(cb, e);
                        }
                        e.printStackTrace();
                    }
                    if (response != null) {
                        if (VERBOSE) Log.i(TAG, "Got Access Token " + response.getAccessToken().substring(0, 5) + "...");
                        storeAccessToken(response.getAccessToken());
                        mOauthInProgress = false;
                        if (cb != null)
                            cb.onSuccess(getRequestFactoryFromAccessToken(mStorage.getString(ACCESS_TOKEN_KEY, null)));
                        executeQueuedCallbacks();
                    } else {
                        mOauthInProgress = false;
                        Log.w(TAG, "Failed to get Access Token");
                    }
                    return null;
                }
            }.execute();
        }
        */
    }

    protected HttpRequestFactory getRequestFactoryFromCachedCredentials() {
        return getRequestFactoryFromAccessToken(mStorage.getString(ACCESS_TOKEN_KEY, null));
    }

    public HttpRequestFactory getRequestFactoryNonAuthenticated() {
        NetHttpTransport mHttpTransport = new NetHttpTransport.Builder().build();
        mRequestFactory = mHttpTransport.createRequestFactory();
        return mRequestFactory;
    }

    private HttpRequestFactory getRequestFactoryFromAccessToken(String accessToken) {
        if (accessToken == null) {
            throw new NullPointerException("getRequestFactoryFromAccessToken got null Access Token");
        }
        if (mRequestFactory == null || !accessToken.equals(mOAuthAccessToken)) {
            Credential credential = new Credential(BearerToken.authorizationHeaderAccessMethod()).setAccessToken(accessToken);
            NetHttpTransport mHttpTransport = new NetHttpTransport.Builder().build();
            mRequestFactory = mHttpTransport.createRequestFactory(credential);
            mOAuthAccessToken = accessToken;
        }
        return mRequestFactory;
    }

    protected boolean isAccessTokenCached() {
        // An Access Token is stored along with a Client ID that matches what's currently provided
        boolean validCredentialsStored = (mStorage.contains(ACCESS_TOKEN_KEY) && mStorage.getString(CLIENT_ID, "").equals(mConfig.getClientId()));
        if (!validCredentialsStored)
            clearAccessToken();
        return validCredentialsStored;
    }

    protected void storeAccessToken(String accessToken) {
        getContext().getSharedPreferences(mConfig.getCredentialStoreName(), mContext.MODE_PRIVATE).edit()
                .putString(ACCESS_TOKEN_KEY, accessToken)
                .putString(CLIENT_ID, mConfig.getClientId())
                .apply();
    }

    protected void clearAccessToken() {
        getContext().getSharedPreferences(mConfig.getCredentialStoreName(), mContext.MODE_PRIVATE).edit()
                .clear()
                .apply();
    }

    protected boolean isSuccessResponse(HttpResponse response) {
        if (VERBOSE) Log.i(TAG, "Response status code: " + response.getStatusCode());
        return response.getStatusCode() == 200;
    }

    /**
     * Execute queued callbacks once valid OAuth
     * credentials are acquired.
     */
    protected void executeQueuedCallbacks() {
        if (VERBOSE) Log.i(TAG, String.format("Executing %d queued callbacks", mCallbackQueue.size()));
        for (OAuthCallback cb : mCallbackQueue) {
            cb.onSuccess(getRequestFactoryFromCachedCredentials());
        }
    }

    private void postExceptionToCallback(final OAuthCallback cb, final Exception e) {
        if (cb != null) {
            cb.onFailure(new OAuthException(e.getMessage()));
        }
    }

    public class OAuthException extends Exception {
        public OAuthException(String detail) {
            super(detail);
        }
    }


    protected void disableCertificateValidation() {
        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = createSSLContext();
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {}
    }

    private static SSLContext createSSLContext() throws KeyManagementException, NoSuchAlgorithmException {
        // TODO On production setup standard SSLContext
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }};

        // Ignore differences between given hostname and certificate hostname
        HostnameVerifier hv = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) { return true; }
        };

        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new SecureRandom());
        return sc;
    }
}
