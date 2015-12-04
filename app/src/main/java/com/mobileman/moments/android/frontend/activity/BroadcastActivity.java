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

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.exception.KickflipException;
import io.kickflip.sdk.fragment.BroadcastFragment;

/**
 * BroadcastActivity manages a single live broadcast. It's a thin wrapper around {@link io.kickflip.sdk.fragment.BroadcastFragment}
 */
public class BroadcastActivity extends ImmersiveActivity implements BroadcastListener {
    private static final String TAG = "BroadcastActivity";

    public static final String ARGS_TITLE = "ARGS_TITLE";
    public static final String ARGS_POST_TO_FB_WALL = "ARGS_POST_TO_FB_WALL";

    private BroadcastFragment mFragment;
    private BroadcastListener mMainBroadcastListener;
    private Handler handler;
    private boolean fbStoryCreated;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        handler = new Handler();
        mMainBroadcastListener = Kickflip.getBroadcastListener();
        Kickflip.setBroadcastListener(this);

        if (savedInstanceState == null) {
            mFragment = BroadcastFragment.getInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, mFragment)
                    .commit();
        }

    }

    @Override
    public void onBackPressed() {
        if (mFragment != null) {
            mFragment.stopBroadcasting();
        }
        super.onBackPressed();
    }

    @Override
    public void onBroadcastStart() {
        mMainBroadcastListener.onBroadcastStart();
    }

    @Override
    public void onBroadcastStreaming(final Stream stream, int currentBitrate) {
        mMainBroadcastListener.onBroadcastStreaming(stream, currentBitrate);

        if ((getIntent().getExtras() != null) && (getIntent().getExtras().containsKey(ARGS_POST_TO_FB_WALL) && (!getIntent().getExtras().getBoolean(ARGS_POST_TO_FB_WALL)))) {
            // posting to FB wall disabled
            return;
        }
        if (!fbStoryCreated) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    notifyFBFriends(stream);
                }
            }).start();
        }

        if (currentBitrate >= Constants.MINIMUM_UPLOAD_BITRATE) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final View bandwithTooLowLayout = findViewById(R.id.bandwithTooLowLayout);
                    if ((bandwithTooLowLayout != null) && (bandwithTooLowLayout.getVisibility() == View.VISIBLE)) {
                        final View mCameraView = findViewById(R.id.cameraPreview);
                        if (mCameraView != null) {
                            mCameraView.setBackground(null);
                            mCameraView.setVisibility(View.VISIBLE);
                        }
                        bandwithTooLowLayout.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    @Override
    public void onBroadcastReady(Stream stream) {

    }

    @Override
    public void onBroadcastStop() {
        finish();
        mMainBroadcastListener.onBroadcastStop();
    }

    @Override
    public void onBroadcastError(final KickflipException error) {
        mMainBroadcastListener.onBroadcastError(error);
        if (error.getMessage() != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(BroadcastActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    @Override
    public void onTryingToReconnect(final boolean networkIssuesPresent) {
        final View reconnectView = findViewById(R.id.mediaPlayerTryingToReconnectTextView);
        if ((reconnectView != null) && (reconnectView instanceof TextView)) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView mediaPlayerTryingToReconnectTextView = (TextView) reconnectView;
                    mediaPlayerTryingToReconnectTextView.setText(networkIssuesPresent ? getResources().getString(R.string.tryingToReconnect) : "");
                    mediaPlayerTryingToReconnectTextView.setVisibility(networkIssuesPresent ? View.VISIBLE : View.GONE);
                }
            });
        }
    }

    @Override
    public void onLowUploadBitrateDetected() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final View mCameraView = findViewById(R.id.cameraPreview);
                        if (mCameraView != null) {
                            mCameraView.setBackgroundResource(R.color.transparentBlue84);
                            mCameraView.setVisibility(View.VISIBLE);
                            final View bandwithTooLowLayout = findViewById(R.id.bandwithTooLowLayout);
                            if ((bandwithTooLowLayout != null) && (bandwithTooLowLayout.getVisibility() != View.VISIBLE)) {

                                bandwithTooLowLayout.setVisibility(View.VISIBLE);
/*                                Button bandwithTooLowRetryButton = (Button) findViewById(R.id.bandwithTooLowRetryButton);
                                bandwithTooLowRetryButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mCameraView.setBackground(null);
                                        bandwithTooLowLayout.setVisibility(View.GONE);
                                    }
                                });
*/
                                Button bandwithTooLowCancelButton = (Button) findViewById(R.id.bandwithTooLowCancelButton);
                                final View mediaPlayerCloseViewButton = findViewById(R.id.mediaPlayerCloseViewButton);
                                bandwithTooLowCancelButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        mediaPlayerCloseViewButton.callOnClick();
                                    }
                                });
                            }
                        }
                    }
                });
    }

    @Override
    public void onBitrateChange(final int newVideoBitrate, final int bandwidth, final boolean bitrateDynamicallyUpdated, final int pendingItemsCount) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final TextView mediaPlayerDebugTextView =  (TextView) findViewById(R.id.mediaPlayerDebugTextView);
                if (mediaPlayerDebugTextView.getVisibility() != View.VISIBLE) {
                    mediaPlayerDebugTextView.setVisibility(View.VISIBLE);
                }
                if (mediaPlayerDebugTextView != null) {
                    mediaPlayerDebugTextView.setText((bitrateDynamicallyUpdated ? "New" : "Current") + " bitrate(video quality): " + Integer.toString(newVideoBitrate) + " kbps, \nbandwidth(network state): " + Integer.toString(bandwidth) + "kbps (Segment count: " + Integer.toString(pendingItemsCount) + ")");
                }
            }
        });
    }

    private void notifyFBFriends(Stream stream) {
        if (AccessToken.getCurrentAccessToken() != null) {
            if (AccessToken.getCurrentAccessToken().getPermissions().contains("publish_actions")
                    && (!AccessToken.getCurrentAccessToken().getDeclinedPermissions().contains("publish_actions"))) {
/*
                org.json.JSONObject jsonObject = new org.json.JSONObject();
                try {
                    jsonObject.put("link", stream.getShareUrl());
                    String username = "";
                    if (MainActivity.getLoggedUser() != null) {
                        username = MainActivity.getLoggedUser().getName() + " is ";
                    } else {
                        username = "I'm ";
                    }
                    String title = username + "live broadcasting on Moments.";
                    jsonObject.put("message", title);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                GraphRequest request = GraphRequest.newPostRequest(
                        AccessToken.getCurrentAccessToken(),
                        "me/feed",
                        jsonObject,
                        new GraphRequest.Callback() {
                            public void onCompleted(com.facebook.GraphResponse graphResponse) {
                                if (graphResponse.getError() != null) {
                                    if (graphResponse.getError().getErrorMessage() != null) {
                                        Log.d(Util.TAG, "FB post to feed error: " + graphResponse.getError().getErrorMessage());
                                        Toast.makeText(BroadcastActivity.this, graphResponse.getError().getErrorMessage(), Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Log.d(Util.TAG, "FB post to feed successful");
                                }
                            }
                        });
                request.executeAsync();
*/
/*                org.json.JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("fb:app_id", getResources().getString(R.string.facebook_app_id));
                    jsonObject.put("og:type", "video.other");
                    jsonObject.put("og:title", stream.getTitle());
                    jsonObject.put("og:image", stream.getFullThumbnailUrl());
                    jsonObject.put("og:url", stream.getShareUrl());

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                String jsonString = jsonObject.toString();
*/
                Bundle params = new Bundle();
                params.putBoolean("fb:explicitly_shared", true);
                params.putString("other", stream.getShareUrl());
                GraphRequest request = new GraphRequest(
                        AccessToken.getCurrentAccessToken(),
                        "me/{appname}:live_broadcast",
                        params,
                        HttpMethod.POST
                );
                request.setCallback(new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse graphResponse) {
                        if (graphResponse.getError() != null) {
                            if (graphResponse.getError().getErrorMessage() != null){
                                Log.d(Util.TAG, "FB story object creation error: " + graphResponse.getError().getErrorMessage());
                            }
                        } else {
                            fbStoryCreated = true;
                            Log.d(Util.TAG, "FB story object created successfully");
                        }
                    }
                });
                GraphResponse response = request.executeAndWait();


// handle the response
            } else {
                Log.e(Util.TAG, "FB posting not possible - denied by declined permission!");
            }
        } else {
            Log.e(Util.TAG, "FB posting not possible - missing access token!");
        }
    }
}
