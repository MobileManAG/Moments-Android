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
package com.mobileman.moments.android.frontend.fragments;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileman.moments.android.MomentsApplication;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.frontend.activity.MainActivity;
import com.squareup.picasso.Picasso;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.exception.KickflipException;

/**
 * MediaPlayerFragment demonstrates playing an HLS Stream, and fetching
 * stream metadata via the .m3u8 manifest to decorate the display for Live streams.
 */
public class MediaPlayerFragment extends SharedMediaPlayerFragment implements TextureView.SurfaceTextureListener, MediaController.MediaPlayerControl {

    private static final String TAG = "MediaPlayerFragment";
    private static final boolean VERBOSE = !MomentsApplication.isOnProduction();

    private static final String STREAM_PLAYLIST_FULL_URL = "STREAM_PLAYLIST_FULL_URL";
    private static final String STREAM_THUMBNAIL_IMAGE_FULL_URL = "STREAM_THUMBNAIL_IMAGE_FULL_URL";
    private static final String ARG_USER_ID = "userId";
    private static final String HISTORICAL_STREAM = "HISTORICAL_STREAM";

    private static final int MAX_PLAYER_RECOVERY_COUNT = 15;

    private ProgressBar mProgress;
    private TextureView mTextureView;
    private TextView mediaPlayerBufferingTextView;
    private ImageView mediaPlayerBackgroundImageView;
    private LayoutInflater layoutInflater;
    private TextView mediaPlayerLiveTextView;
    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController;

    private String mMediaUrl;
    private String thumbnailUrl;
    private String mUserId;
    // M3u8 Media properties inferred from .m3u8
    private int mDurationMs;
    private boolean mIsLive;
    private boolean reconnecting;
    private Surface mSurface;
    private int recoveryCount;
    private Handler repeatHandler;
/*
    private M3u8Parser.M3u8ParserCallback m3u8ParserCallback = new M3u8Parser.M3u8ParserCallback() {
        @Override
        public void onSuccess(Playlist playlist) {
            updateUIWithM3u8Playlist(playlist);

        }

        @Override
        public void onError(Exception e) {
            if (VERBOSE) Log.i(TAG, "m3u8 parse failed " + e.getMessage());
        }
    };
    */

    private View.OnTouchListener mTextureViewTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mMediaController != null && isResumed()) {
                mMediaController.show();
            }
            return false;
        }
    };

    public static MediaPlayerFragment newInstance(String mediaUrl, String thumbnailImageFullUrl, String userId, Stream historicalStream) {
        MediaPlayerFragment fragment = new MediaPlayerFragment();
        Bundle args = new Bundle();
        args.putString(STREAM_PLAYLIST_FULL_URL, mediaUrl);
        args.putString(ARG_USER_ID, userId);
        if (thumbnailImageFullUrl != null) {
            args.putString(STREAM_THUMBNAIL_IMAGE_FULL_URL, thumbnailImageFullUrl);
        }
        if (historicalStream != null) {
            args.putSerializable(HISTORICAL_STREAM, historicalStream);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public MediaPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mMediaUrl = getArguments().getString(STREAM_PLAYLIST_FULL_URL);
            mUserId = getArguments().getString(ARG_USER_ID);
            thumbnailUrl = getArguments().getString(STREAM_THUMBNAIL_IMAGE_FULL_URL);
            HlsStream historicalStream = null;
            if (getArguments().containsKey(HISTORICAL_STREAM)) {
                historicalStream = (HlsStream) getArguments().getSerializable(HISTORICAL_STREAM);
            }
            // NOTE: the kickflip client may not be fully initialized immediately.
            mKickflip = Kickflip.getApiClient(getActivity(), null, null);
//            parseM3u8FromMediaUrl();
            if ((mUserId != null) && (historicalStream == null)) {
//                Log.i(TAG, "MediaPlayerFragment got stream url");
                String streamId = Kickflip.getStreamIdFromKickflipUrl(Uri.parse(mMediaUrl));
                mKickflip.userProfile(mUserId, new KickflipCallback() {
                    @Override
                    public void onSuccess(Response response) {
                        user = (User) response;
                        Log.i(TAG, "got kickflip user profile: " + user.getStream().getFullVideoUrl());
                        mMediaUrl = user.getStream().getFullVideoUrl();
                        updateUIWithStreamDetails(user);
                        if (user.getStream() != null) {
                            if (user.getStream().isStreaming()) {
                                streamIsLive = true;
                                initializeMediaPlayer();
                            } else if (user.getStream().isReady()) {
                                // let's wait
                            }
                        }
                    }

                    @Override
                    public void onError(KickflipException error) {
                        Log.i(TAG, "get user profile failed");
                    }
                });
            } else if (historicalStream != null) {
                this.streamIsHistorical = true;
                user = historicalStream.getCreatedBy();
                user.setStream(historicalStream);
                mMediaUrl = historicalStream.getFullHistoricalVideoUrl();
            }
        }
    }

    private void loadThumbnailImage(String thumbnailImageUrl) {
        Picasso.with(getActivity().getApplicationContext())
                .load(thumbnailImageUrl)
                .into(mediaPlayerBackgroundImageView);
    }

    private void initializeMediaPlayer() {
        Log.i(TAG, "Stream video is available, initializing MediaPlayer");
        if (mSurface != null) {
            setupMediaPlayer(mSurface);
        } else {
            Log.e(TAG, "Can't play stream - surface is not initialized yet!");
        }
    }

/*    private void updateUIWithM3u8Playlist(Playlist playlist) {
        int durationSec = 0;
        for (Element element : playlist.getElements()) {
            durationSec += element.getDuration();
        }
        mIsLive = !playlist.isEndSet();
        mDurationMs = durationSec * 1000;
    }
*/
    private void setupMediaPlayer(final Surface displaySurface) {
        if (mMediaUrl.startsWith("https")) {
            mMediaUrl = "http" + mMediaUrl.substring(5);    // switch to http in case of https
        }
        if( VERBOSE) {
            Log.i(TAG, "Loading video: " + mMediaUrl);
        }

        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = new MediaPlayer();
                mMediaPlayer.setSurface(displaySurface);
                setupMediaPlayerListeners(mMediaPlayer);
                if (streamIsHistorical) {
                    mMediaController = new MediaController(getActivity());
                    mMediaController.setAnchorView(mTextureView);
                    mMediaController.setMediaPlayer(this);
                }
            }
            mMediaPlayer.setDataSource(mMediaUrl);
            mMediaPlayer.prepareAsync();
        } catch (Exception ioe) {
            ioe.printStackTrace();
            if (ioe.getMessage() != null) {
                Log.e(TAG, ioe.getMessage());
            }
        }
    }

    private void setupMediaPlayerListeners(final MediaPlayer mediaPlayer) {
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (VERBOSE) Log.i(TAG, "media player prepared");
                mProgress.setVisibility(View.GONE);
                mediaPlayerLiveTextView.setVisibility(View.VISIBLE);
                if (mMediaController != null) {
                    mMediaController.setEnabled(true);
                }

                mTextureView.setOnTouchListener(mTextureViewTouchListener);
                mp.start();
            }
        });
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                if ((getActivity() != null) && (!reconnecting) && (streamEnded)) {
                    if (VERBOSE) Log.i(TAG, "media player complete. finishing");
                    closeMediaPlayerScreen();
                }
            }
        });

        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mediaPlayer, int what, int extra) {

                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    Log.e(TAG, "Media player Info: MEDIA_INFO_BUFFERING_START, extra: " + Integer.toString(extra));
                    if (mediaPlayerBufferingTextView != null) {
                        mediaPlayerBufferingTextView.setVisibility(View.VISIBLE);
                    }
                } else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    Log.e(TAG, "Media player Info: MEDIA_INFO_BUFFERING_END, extra: " + Integer.toString(extra));
                    if (mediaPlayerBufferingTextView != null) {
                        mediaPlayerBufferingTextView.setVisibility(View.GONE);
                    }
                } else if (what == MediaPlayer.MEDIA_INFO_UNKNOWN) {
                    Log.e(TAG, "Media player Info: MEDIA_INFO_UNKNOWN, extra: " + Integer.toString(extra));
/*                    if ((getActivity() != null) && (!reconnecting)) {
                        if (VERBOSE)
                            Log.i(TAG, "media info unknown - well, let's finish then...");
                        getActivity().finish();
                    }
*/
                } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                    Log.e(TAG, "Media player Info: MEDIA_INFO_VIDEO_RENDERING_START, extra: " + Integer.toString(extra));
                    recoveryCount = 0;
                } else if (what == MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING) {
                    Log.e(TAG, "Media player Info: MEDIA_INFO_VIDEO_TRACK_LAGGING, extra: " + Integer.toString(extra));
                } else {
                    Log.e(TAG, "Media player Info: what: " + Integer.toString(what) + " , extra: " + Integer.toString(extra));
                }

                return false;
            }
        });
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
                Log.i(TAG, "media player buffering: " + Integer.toString(i));
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
                    Log.e(TAG, "Media player error: MEDIA_ERROR_UNKNOWN, extra: " + Integer.toString(extra));
                } else if (what == MediaPlayer.MEDIA_ERROR_IO) {
                    Log.e(TAG, "Media player error: MEDIA_ERROR_IO, extra: " + Integer.toString(extra));
                } else {
                    Log.e(TAG, "Media player error: what: " + Integer.toString(what) + " , extra: " + Integer.toString(extra));
                }
                if ((!streamEnded) && (!streamIsHistorical) && (recoveryCount++ < MAX_PLAYER_RECOVERY_COUNT)) {
                    Log.i(TAG, "Media player IO error, let's try to recover...");
                    final MediaPlayer currentMediaPlayer = mp;
                    fireReconnectRepeatHandler(currentMediaPlayer);
                } else {
                    onBroadcastingEnded();
                }
                return false;
            }
        });
    }

    private void fireReconnectRepeatHandler(final MediaPlayer currentMediaPlayer) {
        if (repeatHandler != null) {
            repeatHandler.removeCallbacksAndMessages(null);
        }
        repeatHandler = new Handler();
        repeatHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                tryToReconnect(currentMediaPlayer, mSurface);
            }
        }, 1500);
    }

    private void tryToReconnect(final MediaPlayer mediaPlayer, final Surface displaySurface) {
        reconnecting = true;
        try {
            mediaPlayer.stop();
        } catch (Exception e) {}

        try {
            mediaPlayer.reset();
        } catch (Exception e) {}

        setupMediaPlayer(displaySurface);
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity.userBusyNow = true;

        PowerManager powerManager = (PowerManager)getActivity().getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");
        wakeLock.acquire();
        mProgress.setVisibility(View.VISIBLE);

        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        MainActivity.userBusyNow = false;

        mTextureView.setSurfaceTextureListener(null);
        cleanUpMediaPlayer();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_media_player, container, false);
        layoutInflater = inflater;
        if (root != null) {
            mTextureView = (TextureView) root.findViewById(R.id.textureView);
            mTextureView.setSurfaceTextureListener(this);
            mProgress = (ProgressBar) root.findViewById(R.id.progress);
            mediaPlayerLocationTextView = (TextView) root.findViewById(R.id.mediaPlayerLocationTextView);
            mediaPlayerUsernameTextView = (TextView) root.findViewById(R.id.mediaPlayerUsernameTextView);
            mediaPlayerHeaderImageView = (ImageView) root.findViewById(R.id.mediaPlayerHeaderImageView);
            mediaPlayerStreamTitle = (TextView) root.findViewById(R.id.mediaPlayerStreamTitle);
            mediaPlayerWatchersCountTextView = (TextView) root.findViewById(R.id.mediaPlayerWatchersCountTextView);
            mediaPlayerAddCommentEditText = (EditText) root.findViewById(R.id.mediaPlayerAddCommentEditText);
            mediaPlayerBufferingTextView = (TextView) root.findViewById(R.id.mediaPlayerTryingToReconnectTextView);
            mediaPlayerLiveTextView = (TextView) root.findViewById(R.id.mediaPlayerLiveTextView);
            mediaPlayerBackgroundImageView = (ImageView) root.findViewById(R.id.mediaPlayerBackgroundImageView);
            initializeCommentList(root);

            View mediaPlayerCloseViewButton = root.findViewById(R.id.mediaPlayerCloseViewButton);
            mediaPlayerCloseViewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    closeMediaPlayerScreen();
                }
            });

            if (thumbnailUrl != null) {
                loadThumbnailImage(thumbnailUrl);
            }
        }
        if ((streamIsHistorical) && (user != null) && (user.getStream() != null)) {
            View mediaPlayerHeaderWatchersCountLayout = root.findViewById(R.id.mediaPlayerHeaderWatchersCountLayout);
            if((user != null) && (user.getStream().getTitle() != null) && (user.getStream().getTitle().length() > 0)) {
                mediaPlayerHeaderWatchersCountLayout.setVisibility(View.VISIBLE);
                View mediaPlayerWatchersCountTextView = root.findViewById(R.id.mediaPlayerWatchersCountTextView);
                mediaPlayerWatchersCountTextView.setVisibility(View.GONE);

                View mediaPlayerWatchersTextView = root.findViewById(R.id.mediaPlayerWatchersTextView);
                mediaPlayerWatchersTextView.setVisibility(View.GONE);

                mediaPlayerStreamTitle.setText(user.getStream().getTitle());
            } else {
                mediaPlayerHeaderWatchersCountLayout.setVisibility(View.GONE);
            }


            View mediaPlayerAddCommentEditText = root.findViewById(R.id.mediaPlayerAddCommentEditText);
            mediaPlayerAddCommentEditText.setVisibility(View.GONE);

        }
        return root;
    }

    private void cleanUpMediaPlayer() {
        if (mMediaPlayer != null) {
            try {
                mMediaPlayer.reset();
            } catch (IllegalStateException e) {}

            try {
                mMediaPlayer.release();
            } catch (IllegalStateException e) {}
            mMediaPlayer = null;
        }
    }

    private void closeMediaPlayerScreen() {
        cleanUpMediaPlayer();
        getActivity().finish();
    }

 /*   private void parseM3u8FromMediaUrl() {
        M3u8Parser.parseM3u8(mMediaUrl, m3u8ParserCallback);
    }
*/
    @Override
    protected void onBroadcastingEnded() {
        super.onBroadcastingEnded();
        Toast.makeText(getActivity(), getResources().getString(R.string.broadcastingEnded), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onBroadcastingIsLive() {
        super.onBroadcastingIsLive();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if((mMediaPlayer ==  null) && (mSurface != null)) {
                    initializeMediaPlayer();
                }
            }
        });
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        mSurface = new Surface(surfaceTexture);
        if((streamIsHistorical) || ((mMediaPlayer == null) && streamIsLive)) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUIWithStreamDetails(user);
                    initializeMediaPlayer();
                }
            });
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        int duration = 0;
        try {
            duration = mMediaPlayer.getDuration();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return duration;
    }

    @Override
    public int getCurrentPosition() {
        int result = 0;
        try {
            result = mMediaPlayer.getCurrentPosition();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;

    }

    @Override
    public void seekTo(int pos) {
/*        try {
            mMediaPlayer.seekTo(pos);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
*/
    }

    @Override
    public boolean isPlaying() {
        boolean result = false;
        try {
            result = mMediaPlayer.isPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
