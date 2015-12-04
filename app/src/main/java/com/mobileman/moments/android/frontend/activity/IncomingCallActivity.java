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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.FBUser;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.frontend.RoundedTransformation;
import com.parse.ParseInstallation;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Created by MobileMan on 28/04/15.
 */
public class IncomingCallActivity extends BaseActivity {

    public static final String ARG_USER_ID = "USER_ID";

    public static final String ARG_STREAM_TITLE = "STREAM_TITLE";

    public static final String ARG_USERNAME = "USERNAME";

    public static final String ARG_MEDIA_URL = "MEDIA_URL";

    public static final String ARG_FACEBOOK_ID = "FACEBOOK_ID";

    public static final String ARG_MOMENTS_APPLICATION_IS_VISIBLE = "MOMENTS_APPLICATION_IS_VISIBLE";

    private static final int MAXIMUM_CALL_NOTIFICATION_DURATION = 30000;
    private static int NOTIFICATION_COUNTER;

    private String userName;
    private String streamTitle;
    private String userId;
    private String facebookId;
    private boolean callAccepted;
    private Window wind;
    protected Handler delayHandler;

    private static MediaPlayer mPlayer;
    private User streamingUser;
    private KickflipApiClient kickflipApiClient;
    private boolean momentsApplicationWasVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.incoming_call);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        userId = "";
        userName = "";
        streamTitle = "";
        facebookId = "";
        String streamThumbnailUrl = "";


        if (getIntent().getExtras() != null) {
            userName = getIntent().getExtras().getString(ARG_USERNAME);
            userId = getIntent().getExtras().getString(ARG_USER_ID);
            streamTitle = getIntent().getExtras().getString(ARG_STREAM_TITLE);
            facebookId = getIntent().getExtras().getString(ARG_FACEBOOK_ID);
            momentsApplicationWasVisible = getIntent().getExtras().getBoolean(ARG_MOMENTS_APPLICATION_IS_VISIBLE);
        }

/*        userId = "555243aba2358ab39833d8a5";
        userName=  "Keram Keram";
        streamTitle = "Testing123";
        facebookId = "1434162610212513";
*/

        if((facebookId != null) && (facebookId.length() > 0)) {
            loadUserPicture(IncomingCallActivity.this, facebookId);
        }
        fillTextWithStreamInformation(userName, streamTitle);
        initButtons();

        try {
            loadStreamData(userId);
        } catch (Exception e) {
            // not logged in
            Log.d(Util.TAG, "Incoming call, trying to log in");
            if (AccessToken.getCurrentAccessToken() == null) {
                Toast.makeText(IncomingCallActivity.this, "Incoming broadcast error - please log in to the application first.", Toast.LENGTH_LONG).show();
                Log.d(Util.TAG, "Missing FB access token, giving up");
            } else {
                Log.d(Util.TAG, "Incoming call, loading user information");
                loadFbUserData(AccessToken.getCurrentAccessToken());
            }
        }

        playRingtone();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            doCleanup();
        }
        super.onDestroy();
    }


    @Override
    protected void onResume() {
        super.onResume();

        MainActivity.userBusyNow = true;
        /******block is needed to raise the application if the lock is*********/
        wind = this.getWindow();
        wind.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        wind.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        wind.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    /* ^^^^^^^block is needed to raise the application if the lock is*/
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainActivity.userBusyNow = false;
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
                            Toast.makeText(IncomingCallActivity.this, response.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
        request.executeAsync();

    }

    private void initializeApplication(FBUser fbUser) {
        // This must happen before any other Kickflip interactions
        kickflipApiClient = Kickflip.setup(this, fbUser, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                loadStreamData(userId);
            }

            @Override
            public void onError(KickflipException error) {
                if (error != null) {
                    error.printStackTrace();
                }
            }
        });
    }

    private void doCleanup() {
        if (delayHandler != null) {
            delayHandler.removeCallbacksAndMessages(null);
        }
        if (mPlayer != null) {
            try {
                mPlayer.stop();
            } catch (IllegalStateException e) {
                // we don't care
            }
            mPlayer.release();
            mPlayer = null;
        }
        streamingUser = null;
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.cancel();
    }

    public static void sendGeneralNotificationOfMissedCall(Context context, String userName, String title) {
        Intent intent = new Intent(context, MainActivity.class);

        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder b = new NotificationCompat.Builder(context);
        String notificationTitle = userName + " " + context.getResources().getString(R.string.isLiveSuffix);
        String notificationText = title;
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        b.setAutoCancel(true)
                .setSmallIcon(R.drawable.ic_launcher)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setTicker("X")
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                .setContentIntent(contentIntent);


        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(++NOTIFICATION_COUNTER, b.build());
    }

    private void sendNotificationAndFinish() {
        if (streamingUser != null) {
            Intent intent = new Intent(getApplicationContext(), MediaPlayerActivity.class);
            Bundle extras = new Bundle();
            extras.putString(MediaPlayerActivity.MEDIA_URL, streamingUser.getStream().getFullVideoUrl());
            extras.putString(MediaPlayerActivity.THUMBNAIL_URL, streamingUser.getStream().getFullThumbnailUrl());
            extras.putString(MediaPlayerActivity.USER_ID, streamingUser.getUuid());
            intent.putExtras(extras);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_SINGLE_TOP);


            PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext());
            String notificationTitle = userName + " " + getResources().getString(R.string.isLiveSuffix);
            String notificationText = "";// getResources().getString(R.string.missedCallNotification) + " " + streamingUser.getName();
            String mediaUrl = streamingUser.getStream().getFullVideoUrl();

            if((streamingUser.getStream() != null) && (streamingUser.getStream().getTitle() != null)) {
                notificationText = streamingUser.getStream().getTitle(); //notificationText + "(" + streamingUser.getStream().getTitle() + ")";
            }
            ImageView incomingCallUserImageView = (ImageView) findViewById(R.id.incomingCallUserImageView);
            Bitmap bitmap = ((BitmapDrawable)incomingCallUserImageView.getDrawable()).getBitmap();
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
            b.setAutoCancel(true)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setWhen(System.currentTimeMillis())
                    .setTicker("X")
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationText)
                    .setDefaults(Notification.DEFAULT_LIGHTS| Notification.DEFAULT_SOUND)
                    .setContentIntent(contentIntent);
//            if (bitmap != null) {
//                b.setLargeIcon(bitmap);
//            }

            NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(++NOTIFICATION_COUNTER, b.build());

        }
        finish();
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        //vibrate for 300 milliseconds and then stop for 500 ms and repeat the same style. You can change the pattern and
        //long pattern[]={0,300,200,300,500};
        //start vibration with repeated count, use -1 if you don't want to repeat the vibration
        long pattern[]={0,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500,500,2500};
        vibrator.vibrate(pattern, 0);
    }

    private void playRingtone() {
        AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        if((audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) || (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0)) {
            vibrate();
            delayHandler = new Handler();
            delayHandler.postDelayed(new Runnable() {
                public void run() {
                    sendNotificationAndFinish();
                }
            }, MAXIMUM_CALL_NOTIFICATION_DURATION);
        } else if (audioManager.getRingerMode() != AudioManager.RINGER_MODE_SILENT) {
            if (mPlayer == null) {
                mPlayer = MediaPlayer.create(getApplicationContext(), R.raw.moments_30s);
                //            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        sendNotificationAndFinish();
                    }
                });
                mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
                        sendNotificationAndFinish();
                        return false;
                    }
                });
                mPlayer.start();
            }
        }
    }

    private void loadStreamData(String userId) {
        KickflipApiClient mKickflip = Kickflip.getApiClient(this, null, null);
        mKickflip.userProfile(userId, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (response instanceof  User) {
                    streamingUser = (User)response;
                    loadUserPicture(IncomingCallActivity.this, streamingUser.getFacebookID());
                    if (streamingUser.getStream() != null) {
                        loadStreamThumbnail(streamingUser.getStream().getFullThumbnailUrl());
                    }
                    if(callAccepted) {
                        acceptCallAndStartStreaming();
                    }

                } else {
                    Log.e(Util.TAG, "Incoming call - received invalid user profile response");
                }
            }

            @Override
            public void onError(KickflipException error) {
                if (error.getMessage() != null) {
                    Toast.makeText(IncomingCallActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void acceptCallAndStartStreaming() {
        if (streamingUser != null) {
            finish();
//            String dummyUrl = "https://moments-mobileman.s3.amazonaws.com/streams/20150428/100526/553f3f46cc749b1c75f10ce2/index.m3u8";
            Kickflip.startMediaPlayerActivity(this, streamingUser.getStream().getFullVideoUrl(), streamingUser.getStream().getFullThumbnailUrl(), streamingUser.getUuid(), !momentsApplicationWasVisible);
        }
    }

    private void initButtons() {
        View dismissButton = findViewById(R.id.dismissButton);
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                finish();
                sendNotificationAndFinish();
            }
        });

        View watchButton = findViewById(R.id.watchButton);
        watchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (streamingUser == null) {
                    callAccepted = true;
                } else {
                    acceptCallAndStartStreaming();
                }
            }
        });
    }

    private void fillTextWithStreamInformation(String callerName, String streamTitle) {
        TextView incomingCallUserNameTextView = (TextView)findViewById(R.id.incomingCallUserNameTextView);
        incomingCallUserNameTextView.setText(callerName);

        TextView incomingCallStreamTitleTextView = (TextView) findViewById(R.id.incomingCallStreamTitleTextView);
        incomingCallStreamTitleTextView.setText(streamTitle);
    }

    private void loadStreamThumbnail(String thumbnailUrl) {
        ImageView incomingCallThumbnailImageView = (ImageView)findViewById(R.id.incomingCallThumbnailImageView);
        Picasso.with(this)
                .load(thumbnailUrl)
                .into(incomingCallThumbnailImageView);
    }

    private void loadUserPicture(Context context, String fbUserId) {
        ImageView incomingCallUserImageView = (ImageView) findViewById(R.id.incomingCallUserImageView);
        String fbImageUrl = Util.getFacebookPictureUrl(fbUserId);
        int imageFrameSize = 2 * context.getResources().getDimensionPixelSize(R.dimen.imageFrameSize);

        Picasso.with(context)
                .load(fbImageUrl)
                .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                .resize(imageFrameSize, imageFrameSize)
                .placeholder(R.drawable.user)
                .error(R.drawable.user)
                .centerCrop().into(incomingCallUserImageView);
    }
}
