/*******************************************************************************
 * Copyright 2015 MobileMan GmbH
 * www.mobileman.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.mobileman.moments.android.frontend.activity;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.FBUser;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.backend.service.NetworkStateListener;
import com.mobileman.moments.android.frontend.LocalPersistence;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;
import com.mobileman.moments.android.frontend.fragments.DebugSettingsFragment;
import com.mobileman.moments.android.frontend.fragments.LoginFragment;
import com.mobileman.moments.android.frontend.fragments.MyFriendsFragment;
import com.mobileman.moments.android.frontend.fragments.MyMomentsFragment;
import com.mobileman.moments.android.frontend.fragments.SetttingsFragment;
import com.mobileman.moments.android.frontend.fragments.StreamListFragment;
import com.parse.ParseInstallation;

import org.json.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.av.SessionConfig;
import io.kickflip.sdk.exception.KickflipException;


public class MainActivity extends BaseActivity implements MainFragmentInteractionListener, NetworkStateListener.NetworkStateListenerInterface{

    public static boolean userBusyNow;  // watching or broadcasting
    public static User getLoggedUser() {
        return user;
    }
//    public static ImageView loggedUserBitmap;

    private static final String TAG = "MainActivity";
    private static final String SERIALIZED_FILESTORE_NAME = "logged_user";
    private static final int CAMERA_DELAYED_INITIALIZATION = 1000;
    private static final int MAX_CAMERA_INIT_COUNT = 5;
    private boolean mKickflipReady = false;
    private int delayedInitCount;

    // FB related
    private static final String USER_SKIPPED_LOGIN_KEY = "user_skipped_login";
    private boolean isResumed = false;
    private boolean userSkippedLogin = false;
    private AccessTokenTracker accessTokenTracker;
    private CallbackManager callbackManager;

    private SurfaceView preview=null;
    private boolean userLoggedInWithinSession;
    private SurfaceHolder previewHolder=null;
    private Button noInternetConnectionView;
    private boolean inPreview=false;
    private Camera camera=null;
    private boolean cameraConfigured=false;
    private static User user;
    private KickflipApiClient kickflipApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            userSkippedLogin = savedInstanceState.getBoolean(USER_SKIPPED_LOGIN_KEY);
        }
        loadPersistedUser();
        userBusyNow = false;
        callbackManager = CallbackManager.Factory.create();
        accessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken,
                                                       AccessToken currentAccessToken) {
                if (isResumed) {
                    if (currentAccessToken != null) {
                        if (tokenHasNeededPermissions(currentAccessToken)) {
                            loadFbUserData(currentAccessToken);
                        } else if (oldAccessToken == null) {
                            gainNeededPermissions();    // try only once
                            showLoginView(true);
                        } else {
                            // the user refused again the access to friends list, give up
                            doLogout(false);
                        }
                    } else {
                        Log.d(TAG, "FB access token null, showing login");
                        MainActivity.user = null;
                        persistUser(true);
                        showLoginView(true);
                    }
                }
            }
        };

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        View mainFrameLayout = findViewById(R.id.container);
        noInternetConnectionView = (Button) findViewById(R.id.noInternetConnectionView);
        noInternetConnectionView.setText(R.string.noInternetConnection);
        noInternetConnectionView.setVisibility(NetworkStateListener.isNetworkAvailable() ? View.GONE : View.VISIBLE);

        preview = (SurfaceView) findViewById(R.id.cameraPreview);
        initializePreview(preview);

        chooseFirstScreen();

    }

    @Override
    public void onStart() {
        super.onStart();
        NetworkStateListener.setCallback(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        NetworkStateListener.setCallback(null);
    }

    @Override
    public void onResume() {

        if (MainActivity.user == null) {
            loadPersistedUser();
        }
        super.onResume();
        isResumed = true;

        boolean initializeCamera = gainCameraAccess();
        if (!initializeCamera) {
            delayedInitCount = 0;
            final Handler repeatHandler = new Handler();
            repeatHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(Util.TAG, "Trying to gain access to the camera");
                    if (!gainCameraAccess() && (delayedInitCount++ < MAX_CAMERA_INIT_COUNT)) {
                        repeatHandler.postDelayed(this, CAMERA_DELAYED_INITIALIZATION);
                    };
                }
            }, CAMERA_DELAYED_INITIALIZATION);
        }

    }

    @Override
    public void onPause() {
        persistUser(false);

        try {
            preview.setVisibility(View.GONE);
            previewHolder.removeCallback(surfaceCallback);
            if (camera != null) {
                camera.release();
            }
            camera = null;
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.onPause();
        isResumed = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        accessTokenTracker.stopTracking();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(USER_SKIPPED_LOGIN_KEY, userSkippedLogin);
    }

    private void chooseFirstScreen() {
        if(AccessToken.getCurrentAccessToken() == null) {
            showLoginView(true);
        } else if (!tokenHasNeededPermissions(AccessToken.getCurrentAccessToken())) {
            showLoginView(true);
//            gainNeededPermissions();
        } else if (NetworkStateListener.isNetworkAvailable()){
            showLoginView(false);
            loadFbUserData(AccessToken.getCurrentAccessToken());
        } else {
            showLoginView(false);
        }
    }

    public static boolean tokenHasNeededPermissions(AccessToken accessToken) {
        boolean result = ((!accessToken.getDeclinedPermissions().contains(Constants.FB_USER_FRIENDS_PERMISSION)
                && (accessToken.getPermissions().contains(Constants.FB_USER_FRIENDS_PERMISSION))));
        return result;
    }

    private void gainNeededPermissions() {
        LoginManager.getInstance().logInWithReadPermissions(MainActivity.this, Arrays.asList(Constants.FB_USER_FRIENDS_PERMISSION));
    }

    private boolean gainCameraAccess() {
        if (camera!=null){
            camera.stopPreview();
            camera.release();
            camera=null;
        }
        boolean initSuccessful = false;
        try {
            camera = Camera.open();
            if (camera == null) {
                Log.d(Util.TAG, "Camera not ready, will try later...");
            }
            previewHolder.addCallback(surfaceCallback);
            preview.setVisibility(View.VISIBLE);
            initSuccessful = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return initSuccessful;
    }

    protected void initializePreview(SurfaceView preview) {
        previewHolder=preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private void persistUser(boolean remove) {
        if (remove || (MainActivity.user != null)) {
            LocalPersistence.writeObjectToFile(getApplicationContext(), MainActivity.user, SERIALIZED_FILESTORE_NAME);
        }
    }

    private void loadPersistedUser() {
        Object savedUser = LocalPersistence.readObjectFromFile(getApplicationContext(), SERIALIZED_FILESTORE_NAME);
        if ((savedUser != null) && (savedUser instanceof User)) {
            MainActivity.user = (User) savedUser;
        }
    }

    private void stopRecordingPreview() {

        if ((camera != null) && inPreview) {
           camera.stopPreview();
        }

        if (camera != null) {
           camera.release();
        }
        camera=null;
        inPreview=false;
        //           cameraConfigured = false;
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
           try {
               camera.setPreviewDisplay(previewHolder);
           }
           catch (Throwable t) {
               Log.e("LiveFragment",
                       "Exception in setPreviewDisplay()", t);
               Toast
                       .makeText(this, t.getMessage(), Toast.LENGTH_LONG)
                       .show();
           }

           if (!cameraConfigured) {
               Camera.Parameters parameters=camera.getParameters();
               Camera.Size size=getBestPreviewSize(width, height,
                       parameters);

               if (size!=null) {
                   parameters.setPreviewSize(size.width, size.height);
                   camera.setParameters(parameters);
                   cameraConfigured=true;
               }
           }
        }
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                      Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
           if (size.width<=width && size.height<=height) {
               if (result==null) {
                   result=size;
               }
               else {
                   int resultArea=result.width*result.height;
                   int newArea=size.width*size.height;

                   if (newArea>resultArea) {
                       result=size;
                   }
               }
           }
        }

        return(result);
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
           camera.setDisplayOrientation(90);
           camera.startPreview();
           inPreview=true;
        }
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
    public void surfaceCreated(SurfaceHolder holder) {
       // no-op -- wait until surfaceChanged()
    }

    public void surfaceChanged(SurfaceHolder holder,
                              int format, int width,
                              int height) {

       initPreview(width, height);
       startPreview();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
       // no-op
    }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
       super.onActivityResult(requestCode, resultCode, data);
       callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private void loadFbUserData(final AccessToken access_token) {
        GraphRequest request = GraphRequest.newMeRequest(
                access_token,
                new GraphRequest.GraphJSONObjectCallback() {
                    @Override
                    public void onCompleted(
                            JSONObject object,
                            GraphResponse response) {
                        if (response.getError() == null) {
                            FBUser fbUser = new FBUser(object);
                            fbUser.setToken(access_token);
                            fbUser.setPushNotificationId(ParseInstallation.getCurrentInstallation().getInstallationId());
                            initializeApplication(fbUser);
                        } else {
                            Toast.makeText(MainActivity.this, response.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
        //        Bundle parameters = new Bundle();
        //        parameters.putString("fields", "id,name,link,email");
        //        request.setParameters(parameters);
        request.executeAsync();
    }

    private void initializeApplication(FBUser fbUser) {
       // This must happen before any other Kickflip interactions
        kickflipApiClient = Kickflip.setup(this, fbUser, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                mKickflipReady = true;
                if (response instanceof User) {
                    MainActivity.user = (User) response;
                    onLoginSuccess(userLoggedInWithinSession);
                } else if (user == null) {
                    Log.e(TAG, "Received response for createNewUser - NULL!");
//                    onFragmentEvent(EVENT.MISSING_PRIVILEGES, null);
                } else {
                    Log.e(TAG, "Received response for createNewUser, but with invalid type!");
                }
            }

            @Override
            public void onError(KickflipException error) {
                if (error != null) {
                    Log.e(TAG, error.getMessage());
                    if (error.isMissingPrivilegesError()) {
                        onFragmentEvent(EVENT.MISSING_PRIVILEGES, null);
                    } else {
                        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
       });
    }



    private BroadcastListener mBroadcastListener = new BroadcastListener() {
       @Override
       public void onBroadcastStart() {
           Log.i(TAG, "onBroadcastStart");
       }

       @Override
       public void onBroadcastStreaming(Stream stream, int currentBitrate) {

       }

        @Override
        public void onBroadcastReady(Stream stream) {

        }

        @Override
       public void onBroadcastStop() {
           Log.i(TAG, "onBroadcastStop");

           // If you're manually injecting the BroadcastFragment,
           // you'll want to remove/replace BroadcastFragment
           // when the Broadcast is over.

           //getFragmentManager().beginTransaction()
           //    .replace(R.id.container, MainFragment.getInstance())
           //    .commit();
       }

       @Override
       public void onBroadcastError(final KickflipException error) {
           Log.i(TAG, "onBroadcastError " + error.getMessage());
       }

        @Override
        public void onTryingToReconnect(final boolean networkIssuesPresent) {}

        @Override
        public void onLowUploadBitrateDetected() {}

        @Override
        public void onBitrateChange(final int newVideoBitrate, final int bandwidth, final boolean bitrateDynamicallyUpdated, final int pendingItemsCount) {}


    };
    // By default, Kickflip stores video in a "Kickflip" directory on external storage
    private String mRecordingOutputPath = new File(Environment.getExternalStorageDirectory(), "Moments/index.m3u8").getAbsolutePath();


   @Override
   public void onFragmentEvent(MainFragmentInteractionListener.EVENT event, Object parameters) {
       if (event.equals(EVENT.START_BROADCAST)) {
           Bundle parametersBundle = new Bundle();

           if ((parameters != null) && (parameters instanceof String)){
               String streamTitle = (String)parameters;
               parametersBundle.putString(BroadcastActivity.ARGS_TITLE, streamTitle);
           } else if (parameters instanceof Bundle) {
               parametersBundle = (Bundle) parameters;
           }
           startBroadcastingActivity(parametersBundle);
       } else if (event.equals(EVENT.LOGOUT)) {
           doLogout(false);
       } else if (event.equals(EVENT.MISSING_PRIVILEGES)) {
           doLogout(true);
       } else if (event.equals(EVENT.SHARE)) {
           shareApplicationInformation();
       } else if (event.equals(EVENT.FRIENDS_LIST)) {
           showFriendsList();
       } else if (event.equals(EVENT.MY_MOMENTS_LIST)) {
            showMyMomentsList();
        } else if (event.equals(EVENT.DEBUG_SETTINGS)) {
           showDebugSettings();
       } else if (event.equals(EVENT.BACK_TO_SETTINGS)) {
            toggleSettingsView(true, false);
       } else if (event.equals(EVENT.FB_TOKEN_ID_RECEIVED)) {
           if (AccessToken.getCurrentAccessToken() != null) {
               if (tokenHasNeededPermissions(AccessToken.getCurrentAccessToken())) {
                   loadFbUserData(AccessToken.getCurrentAccessToken());
               } else {
                   gainNeededPermissions();
               }
           } else {
               Log.e(Util.TAG, "No FB access token? Why it's gone???");
           }
       } else if (event.equals(EVENT.SHOW_STREAMS)) {
           showStreamList();
       }
   }

    @Override
    public void onStreamPlaybackRequested(User user) {
        Kickflip.startMediaPlayerActivity(this, user.getStream().getFullVideoUrl(), user.getStream().getFullThumbnailUrl(), user.getUuid(), false);

/*
// for testing purposes only
        Intent i = new Intent(MainActivity.this, IncomingCallActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle extras = new Bundle();
        extras.putString(IncomingCallActivity.ARG_STREAM_TITLE, "Dummy title");
        extras.putString(IncomingCallActivity.ARG_USER_ID, "553f78eacc7401de16916ee2");
        extras.putString(IncomingCallActivity.ARG_USERNAME, "John Doe");
        i.putExtras(extras);
        startActivity(i);
*/
    }

    @Override
    public void playHistoricalStream(final HlsStream stream) {
        Kickflip.startMediaPlayerActivityWithHistoricalStream(this, stream);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    /**
    * Unused method demonstrating how to use
    * Kickflip's BroadcastFragment.
    * <p/>
    * Note that in this scenario your Activity is responsible for
    * removing the BroadcastFragment in your onBroadcastStop callback.
    * When the user stops recording, the BroadcastFragment begins releasing
    * resources and freezes the camera preview.
    */
/*    public void startBroadcastFragment(String streamTitle) {
        // Before using the BroadcastFragment, be sure to
        // register your BroadcastListener with Kickflip
        configureNewBroadcast(streamTitle);
        Kickflip.setBroadcastListener(mBroadcastListener);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, BroadcastFragment.getInstance())
                .commit();
    }
*/



    private void startBroadcastingActivity(Bundle argumentsBundle) {
        String streamTitle = "";
        boolean fbStoryCreationEnabled =  false;
        if ((argumentsBundle != null) && (argumentsBundle.containsKey(BroadcastActivity.ARGS_TITLE))) {
            streamTitle = argumentsBundle.getString(BroadcastActivity.ARGS_TITLE);
        }
        if ((argumentsBundle != null) && (argumentsBundle.containsKey(BroadcastActivity.ARGS_POST_TO_FB_WALL))) {
            fbStoryCreationEnabled = argumentsBundle.getBoolean(BroadcastActivity.ARGS_POST_TO_FB_WALL);
        }

        configureNewBroadcast(streamTitle, fbStoryCreationEnabled);

        Kickflip.startBroadcastActivity(this, mBroadcastListener, argumentsBundle);
    }

    private void configureNewBroadcast(String streamTitle, boolean fbStoryCreationEnabled) {
        // Should reset mRecordingOutputPath between recordings
//        SessionConfig config = Util.create720pSessionConfig(mRecordingOutputPath);
        SessionConfig config = Util.create420pSessionConfig(mRecordingOutputPath);
        if (DebugSettingsFragment.DebugRecordingSettingsActive) {
            config = getCustomSessionConfig();
        }

        config.setFbStoryCreationEnabled(fbStoryCreationEnabled);
        config.setTitle(streamTitle);

        Kickflip.setSessionConfig(config);
    }

    private SessionConfig getCustomSessionConfig() {
        Log.d(Util.TAG, "Setting custom recording values -"
                        + " width: " + Integer.toString(DebugSettingsFragment.DebugRecordingSettingRecordingWidth)
                        + ", height: " + Integer.toString(DebugSettingsFragment.DebugRecordingSettingRecordingHeight)
                        + ", initial bitrate: " + Integer.toString(DebugSettingsFragment.DebugRecordingSettingInitialBitrate) + " kbps"
                        + ", segment duration: " + Integer.toString(DebugSettingsFragment.DebugRecordingSettingSegmentDuration)
        );
        SessionConfig config = new SessionConfig.Builder(mRecordingOutputPath, DebugSettingsFragment.DebugRecordingSettingSegmentDuration)
                .withVideoBitrate(1000* DebugSettingsFragment.DebugRecordingSettingInitialBitrate)  // the bitrate is in kbps
                .withAudioBitrate(64 * 1024)
                .withPrivateVisibility(false)
                .withAdaptiveStreaming(true)
                .withLocation(true)
                .withVideoResolution(DebugSettingsFragment.DebugRecordingSettingRecordingWidth, DebugSettingsFragment.DebugRecordingSettingRecordingHeight)
                .build();

        return config;
    }

    private void doLogout(boolean logoutBecauseOfMissingPrivileges) {
        Log.d(TAG, "Performing logout");
        if (kickflipApiClient != null) {
            kickflipApiClient.logoutUser(new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                    Log.d(TAG, "Logout successful");
                }

                @Override
                public void onError(KickflipException error) {
                    if ((error != null) && (error.getMessage() != null) && (!error.getMessage().contains("401 Unauthorized"))) {
                        Log.e(TAG, error.getMessage());
                        Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        LoginManager.getInstance().logOut();
        if (logoutBecauseOfMissingPrivileges) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, R.string.missingPrivilegesInformation, Toast.LENGTH_SHORT).show();
                }
            });
        }
        showLoginView(true);
    }

/*    private boolean handleLaunchingIntent() {
        Uri intentData = getIntent().getData();
        if (isKickflipUrl(intentData)) {
            Kickflip.startMediaPlayerActivity(this, intentData.toString(), true);
            finish();
            return true;
        }
        return false;
    }
*/
    private void showLoginView(boolean withLoginEnabled) {
        if (withLoginEnabled) {
            userLoggedInWithinSession = true;
        }
        LoginFragment loginFragment = new LoginFragment();
        Bundle data = new Bundle();
        data.putBoolean(LoginFragment.LOGIN_ENABLED, withLoginEnabled);
        loginFragment.setArguments(data);

        getFragmentManager().beginTransaction()
                .replace(R.id.container, loginFragment)
                .commit();
    }

    private void showStreamList() {
/*        if ((MainActivity.loggedUserBitmap == null) && (getLoggedUser() != null)) {
            int imageFrameSize = getApplicationContext().getResources().getDimensionPixelSize(R.dimen.imageFrameSize);
            MainActivity.loggedUserBitmap = new ImageView(MainActivity.this);
            Picasso.with(MainActivity.this)
                    .load(Util.getFacebookPictureUrl(user.getFacebookID()))
                    .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                    .resize(imageFrameSize, imageFrameSize)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .centerCrop().into(MainActivity.loggedUserBitmap, new Callback() {
                @Override
                public void onSuccess() {
                    Log.d(Util.TAG, "Loading of logged user FB avatar successful.");
                }

                @Override
                public void onError() {
                    Log.d(Util.TAG, "Loading of logged user FB avatar failed with error");
                }
            });
        } else {
            Log.d(Util.TAG, "Loading of logged user FB avatar failed!");
        }
*/
        userLoggedInWithinSession = false;
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new StreamListFragment())
                .commit();
    }

    private void showMyFriendsScreen() {
        MyFriendsFragment myFriendsFragment = new MyFriendsFragment();
        Bundle argumentsBundle = new Bundle();
        argumentsBundle.putBoolean(MyFriendsFragment.ARGS_BACK_NAVIGATION_RIGHT_SIDE, true);
        myFriendsFragment.setArguments(argumentsBundle);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, myFriendsFragment)
                .commit();
    }

    private void showFriendsList() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_anim_right_to_left, R.anim.exit_anim_right_to_left);
        transaction.replace(R.id.container, new MyFriendsFragment());
        transaction.commit();
    }

    private void showMyMomentsList() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_anim_right_to_left, R.anim.exit_anim_right_to_left);
        transaction.replace(R.id.container, new MyMomentsFragment());
        transaction.commit();
    }

    private void showDebugSettings() {
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.setCustomAnimations(R.anim.enter_anim_right_to_left, R.anim.exit_anim_right_to_left);
        transaction.replace(R.id.container, new DebugSettingsFragment());
        transaction.commit();
    }

    private void onLoginSuccess(boolean newLogin) {
        if (newLogin) {
            showMyFriendsScreen();
        } else {
            showStreamList();
        }
    }

    private void shareApplicationInformation() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, getResources().getString(R.string.app_name));
        shareIntent.putExtra(Intent.EXTRA_TEXT, getResources().getString(R.string.shareAppInformation));
        shareIntent.setType("text/plain");
        startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.shareAppDialogTitle)));
    }

    @Override
    public void toggleSettingsView(boolean show, boolean animateVertically) {
        if (show) {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            if (animateVertically) {
                transaction.setCustomAnimations(R.anim.enter_anim_top_to_down, R.anim.fade_out);
            } else {
                transaction.setCustomAnimations(R.anim.enter_anim_left_to_right, R.anim.exit_anim_left_to_right);
            }
            transaction.replace(R.id.container, new SetttingsFragment());
            transaction.commit();
        } else {
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.fade_in, R.anim.exit_anim_down_to_top);
            transaction.replace(R.id.container, new StreamListFragment());
            transaction.commit();
        }
    }

    @Override
    public void internetConnectionAvailable() {
        if (noInternetConnectionView != null) {
            noInternetConnectionView.setVisibility(View.GONE);
            if (!mKickflipReady) {
                chooseFirstScreen();
            }
        }
    }

    @Override
    public void internetConnectionLost() {
        if (noInternetConnectionView != null) {
            noInternetConnectionView.setVisibility(View.VISIBLE);
        }
    }
}
