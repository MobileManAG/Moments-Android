package io.kickflip.sdk.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.frontend.activity.CustomFrameLayout;
import com.mobileman.moments.android.frontend.activity.MainActivity;
import com.mobileman.moments.android.frontend.fragments.SharedMediaPlayerFragment;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.Share;
import io.kickflip.sdk.av.Broadcaster;
import io.kickflip.sdk.av.FullFrameRect;
import io.kickflip.sdk.event.BroadcastIsBufferingEvent;
import io.kickflip.sdk.event.BroadcastIsLiveEvent;
import io.kickflip.sdk.event.BroadcastIsReadyEvent;
import io.kickflip.sdk.view.GLCameraEncoderView;

/**
 * This is a drop-in broadcasting fragment.
 * Currently, only one BroadcastFragment may be instantiated at a time by
 * design of {@link io.kickflip.sdk.av.Broadcaster}.
 */
public class BroadcastFragment extends SharedMediaPlayerFragment implements AdapterView.OnItemSelectedListener, CustomFrameLayout.DoubleTapCallback {

    private static final String TAG = "BroadcastFragment";
    private static final boolean VERBOSE = false;
    private static BroadcastFragment mFragment;
    private static Broadcaster mBroadcaster;        // Make static to survive Fragment re-creation
    private GLCameraEncoderView mCameraView;
//    private TextView mLiveBanner;
    private TextView preparingBroadcastTextView;
    private View bandwithTooLowLayout;
    private View mediaPlayerFooterLayout;
    private View mediaPlayerHeaderLayout;
    private View mediaPlayerLiveTextView;
    private AlertDialog.Builder closeBroadcastDialog;

    View.OnClickListener mShareButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getTag() != null) {
                Intent shareIntent = Share.createShareChooserIntentWithTitleAndUrl(getActivity(), getString(R.string.share_broadcast), (String) v.getTag());
                startActivity(shareIntent);
            }
        }
    };

    View.OnClickListener mRecordButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mBroadcaster.isRecording()) {
                showEndBroadcastConfirmationDialog();
            } else {
                setBannerToBufferingState();
                mBroadcaster.startRecording();
                stopMonitoringOrientation();
//                v.setBackgroundResource(R.drawable.red_dot_stop);
            }
        }
    };

    private void showEndBroadcastConfirmationDialog() {
        if (closeBroadcastDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle("");
            builder.setMessage(R.string.endBroadcastConfirmation);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (mBroadcaster.isRecording()) {
                        mBroadcaster.stopRecording();
                        handleRecordingStopped();
                    }
                    closeBroadcastDialog = null;
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    closeBroadcastDialog = null;
                }
            });
            closeBroadcastDialog = builder;
            closeBroadcastDialog.show();
        }
    }

    private SensorEventListener mOrientationListener = new SensorEventListener() {
        final int SENSOR_CONFIRMATION_THRESHOLD = 5;
        int[] confirmations = new int[2];
        int orientation = -1;

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (getActivity() != null && getActivity().findViewById(R.id.rotateDeviceHint) != null) {
                //Log.i(TAG, "Sensor " + event.values[1]);
                if (event.values[1] > 10 || event.values[1] < -10) {
                    // Sensor noise. Ignore.
                } else if (event.values[1] < 5.5 && event.values[1] > -5.5) {
                    // Landscape
                    if (orientation != 1 && readingConfirmed(1)) {
                        if (mBroadcaster.getSessionConfig().isConvertingVerticalVideo()) {
                            if (event.values[0] > 0) {
                                mBroadcaster.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.LANDSCAPE);
                            } else {
                                mBroadcaster.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.UPSIDEDOWN_LANDSCAPE);
                            }
                        } else {
                            getActivity().findViewById(R.id.rotateDeviceHint).setVisibility(View.GONE);
                        }
                        orientation = 1;
                    }
                } else if (event.values[1] > 7.5 || event.values[1] < -7.5) {
                    // Portrait
                    if (orientation != 0 && readingConfirmed(0)) {
                        if (mBroadcaster.getSessionConfig().isConvertingVerticalVideo()) {
                            if (event.values[1] > 0) {
                                mBroadcaster.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.VERTICAL);
                            } else {
                                mBroadcaster.signalVerticalVideo(FullFrameRect.SCREEN_ROTATION.UPSIDEDOWN_VERTICAL);
                            }
                        } else {
                            getActivity().findViewById(R.id.rotateDeviceHint).setVisibility(View.VISIBLE);
                        }
                        orientation = 0;
                    }
                }
            }

        }

        /**
         * Determine if a sensor reading is trustworthy
         * based on a series of consistent readings
         */
        private boolean readingConfirmed(int orientation) {
            confirmations[orientation]++;
            confirmations[orientation == 0 ? 1 : 0] = 0;
            return confirmations[orientation] > SENSOR_CONFIRMATION_THRESHOLD;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    public BroadcastFragment() {
        // Required empty public constructor
        if (VERBOSE) Log.i(TAG, "construct");
    }

    public static BroadcastFragment getInstance() {
        if (mFragment == null) {
            // We haven't yet created a BroadcastFragment instance
            mFragment = recreateBroadcastFragment();
        } else if (mBroadcaster != null && !mBroadcaster.isRecording()) {
            // We have a leftover BroadcastFragment but it is not recording
            // Treat it as finished, and recreate
            mFragment = recreateBroadcastFragment();
        } else {
            Log.i(TAG, "Recycling BroadcastFragment");
        }
        return mFragment;
    }

    private static BroadcastFragment recreateBroadcastFragment() {
        Log.i(TAG, "Recreating BroadcastFragment");
        mBroadcaster = null;
        return new BroadcastFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        if (!Kickflip.readyToBroadcast()) {
            Log.e(TAG, "Kickflip not properly prepared by BroadcastFragment's onCreate. SessionConfig: " + Kickflip.getSessionConfig() + " key " + Kickflip.getApiKey() + " secret " + Kickflip.getApiSecret());
        } else {
            setupBroadcaster();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        if (VERBOSE) Log.i(TAG, "onAttach");
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
    public void onResume() {
        super.onResume();

        MainActivity.userBusyNow = true;

        if (mBroadcaster != null) {
            mBroadcaster.onHostActivityResumed();

            if (!mBroadcaster.isRecording()) {
                setBannerToBufferingState();
                mBroadcaster.startRecording();
            }

        }
//        startMonitoringOrientation();
    }

    @Override
    public void onPause() {
        super.onPause();

        MainActivity.userBusyNow = false;

        if (mBroadcaster != null)
            mBroadcaster.onHostActivityPaused();
        stopMonitoringOrientation();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBroadcaster != null && !mBroadcaster.isRecording())
            mBroadcaster.release();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (VERBOSE) Log.i(TAG, "onCreateView");

        View root;
        if (mBroadcaster != null && getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            root = inflater.inflate(R.layout.fragment_broadcast, container, false);
            initializeCommentList(root);

            CustomFrameLayout fragmentBroadcastMainLayout = (CustomFrameLayout) root.findViewById(R.id.fragmentBroadcastMainLayout);
            fragmentBroadcastMainLayout.setDoubleTapCallback(this);

            mCameraView = (GLCameraEncoderView) root.findViewById(R.id.cameraPreview);
            mCameraView.setKeepScreenOn(true);
            mCameraView.setBackgroundResource(R.color.transparentBlue84);

            preparingBroadcastTextView = (TextView) root.findViewById(R.id.preparingBroadcastTextView);
            mBroadcaster.setPreviewDisplay(mCameraView);

            this.mediaPlayerFooterLayout = root.findViewById(R.id.mediaPlayerFooterLayout);
            this.mediaPlayerFooterLayout.setVisibility(View.INVISIBLE);
            this.mediaPlayerHeaderLayout = root.findViewById(R.id.mediaPlayerHeaderLayout);
            this.mediaPlayerHeaderLayout.setVisibility(View.INVISIBLE);
            this.mediaPlayerLiveTextView = root.findViewById(R.id.mediaPlayerLiveTextView);

            this.mediaPlayerHeaderImageView = (ImageView) root.findViewById(R.id.mediaPlayerHeaderImageView);
            this.mediaPlayerUsernameTextView = (TextView) root.findViewById(R.id.mediaPlayerUsernameTextView);
            this.mediaPlayerLocationTextView = (TextView) root.findViewById(R.id.mediaPlayerLocationTextView);
            this.mediaPlayerWatchersCountTextView = (TextView) root.findViewById(R.id.mediaPlayerWatchersCountTextView);
            this.mediaPlayerStreamTitle = (TextView) root.findViewById(R.id.mediaPlayerStreamTitle);
            this.bandwithTooLowLayout = root.findViewById(R.id.bandwithTooLowLayout);
            View mediaPlayerAddCommentEditText = root.findViewById(R.id.mediaPlayerAddCommentEditText);
            mediaPlayerAddCommentEditText.setVisibility(View.GONE);

//            this.broadcastingTitleEditText = root.findViewById(R.id.broadcastingTitleEditText);
//            recordButton.setOnClickListener(mRecordButtonClickListener);
            View closeViewButton = root.findViewById(R.id.mediaPlayerCloseViewButton);
            closeViewButton.setOnClickListener(mRecordButtonClickListener);

//            mLiveBanner.setOnClickListener(mShareButtonClickListener);

/*            if (mBroadcaster.isLive()) {
                setGuiToReadyState();
                mLiveBanner.setVisibility(View.VISIBLE);
            }
*/
            if (mBroadcaster.isRecording()) {
//                recordButton.setBackgroundResource(R.drawable.red_dot_stop);
                if (!mBroadcaster.isLive()) {
                    setBannerToBufferingState();
//                    mLiveBanner.setVisibility(View.VISIBLE);
                } else {
                    setGuiToReadyState();
                }
            } else {
                handleRecordingStopped();
            }
            setupFilterSpinner(root);
            setupCameraFlipper(root);
        } else
            root = new View(container.getContext());
        return root;
    }

    protected void setupBroadcaster() {
        // By making the recorder static we can allow
        // recording to continue beyond this fragment's
        // lifecycle! That means the user can minimize the app
        // or even turn off the screen without interrupting the recording!
        // If you don't want this behavior, call stopRecording
        // on your Fragment/Activity's onStop()
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (mBroadcaster == null) {
                if (VERBOSE)
                    Log.i(TAG, "Setting up Broadcaster for output " + Kickflip.getSessionConfig().getOutputPath() + " client key: " + Kickflip.getApiKey() + " secret: " + Kickflip.getApiSecret());
                // TODO: Don't start recording until stream start response, so we can determine stream type...
                Context context = getActivity().getApplicationContext();
                mBroadcaster = new Broadcaster(context, Kickflip.getSessionConfig(), Kickflip.getApiKey(), Kickflip.getApiSecret());
                mBroadcaster.getEventBus().register(this);
                mBroadcaster.setBroadcastListener(Kickflip.getBroadcastListener());
                Kickflip.clearSessionConfig();
            }
        }
    }

    private void setupFilterSpinner(View root) {
/*        Spinner spinner = (Spinner) root.findViewById(R.id.filterSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.camera_filter_names, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);
*/
    }

    private void setupCameraFlipper(View root) {
        View flipper = root.findViewById(R.id.cameraFlipper);
// TODO MU handle camera switching
        if (Camera.getNumberOfCameras() == 1) {
//            flipper.setVisibility(View.GONE);
        } else {
/*            flipper.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mBroadcaster.requestOtherCamera();
                }
            });
*/
        }

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (((String) parent.getTag()).compareTo("filter") == 0) {
            mBroadcaster.applyFilter(position);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Subscribe
    public void onBroadcastIsBuffering(BroadcastIsBufferingEvent event) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setBannerToBufferingState();
                    animateLiveBanner();
                }
            });
        }
    }

    @Subscribe
    public void onBroadcastIsReady(final BroadcastIsReadyEvent readyEvent) {
        if (getActivity() != null) {
            mKickflip = Kickflip.getApiClient(getActivity(), null, null);
            final User user = mKickflip.getActiveUser();
            this.user = user;

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        setGuiToReadyState();
                        updateUIWithStreamDetails(user);
                    } catch (Exception e) {
                        Log.i(TAG, "onBroadcastIsLiveException");
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    @Subscribe
    public void onBroadcastIsLive(final BroadcastIsLiveEvent liveEvent) {
    }

    private void setBannerToBufferingState() {
/*        mLiveBanner.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        mLiveBanner.setBackgroundResource(R.drawable.live_orange_bg);
        mLiveBanner.setTag(null);
        mLiveBanner.setText(getString(R.string.buffering));
  */
        preparingBroadcastTextView.setVisibility(View.VISIBLE);
//        recordButton.setTextColor(getResources().getColor(R.color.whiteInactive));
//        recordButton.setBackgroundResource(R.color.buttonRedTransparent84);

    }


    private void setGuiToReadyState() {
        if (getActivity() != null) {
 /*           mLiveBanner.setBackgroundResource(R.drawable.live_red_bg);
            Drawable img = getActivity().getResources().getDrawable(R.drawable.ic_share_white);
            mLiveBanner.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
            if (watchUrl != null) {
                mLiveBanner.setTag(watchUrl);
            }
            mLiveBanner.setText(getString(R.string.live));
*/
            preparingBroadcastTextView.setVisibility(View.INVISIBLE);
            mediaPlayerHeaderLayout.setVisibility(View.VISIBLE);
            mediaPlayerFooterLayout.setVisibility(View.VISIBLE);
            mediaPlayerLiveTextView.setVisibility(View.VISIBLE);
            if (bandwithTooLowLayout.getVisibility() != View.VISIBLE) {
                mCameraView.setBackground(null);
            }
//            recordButton.setVisibility(View.GONE);
//            broadcastingTitleEditText.setVisibility(View.GONE);
        }

    }

    private void animateLiveBanner() {
/*        mLiveBanner.bringToFront();
        mLiveBanner.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_from_left));
        mLiveBanner.setVisibility(View.VISIBLE);
*/
    }

    private void handleRecordingStopped() {
//        mLiveBanner.startAnimation(AnimationUtils.loadAnimation(getActivity().getApplicationContext(), R.anim.slide_to_left));
//        mLiveBanner.setVisibility(View.INVISIBLE);
        preparingBroadcastTextView.setVisibility(View.INVISIBLE);

//        broadcastingTopHeaderView.setVisibility(View.GONE);
        mCameraView.setBackgroundResource(R.color.transparentBlue84);
//        recordButton.setVisibility(View.VISIBLE);
//        broadcastingTitleEditText.setVisibility(View.VISIBLE);

    }

    /**
     * Force this fragment to stop broadcasting.
     * Useful if your application wants to stop broadcasting
     * when a user leaves the Activity hosting this fragment
     */
    public void stopBroadcasting() {
        if (mBroadcaster.isRecording()) {
            mBroadcaster.stopRecording();
            mBroadcaster.release();
        }
    }


    protected void startMonitoringOrientation() {
        if (getActivity() != null) {
//            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
//            sensorManager.registerListener(mOrientationListener, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    protected void stopMonitoringOrientation() {
/*        if (getActivity() != null) {
            View deviceHint = getActivity().findViewById(R.id.rotateDeviceHint);
            if (deviceHint != null) deviceHint.setVisibility(View.GONE);
            SensorManager sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
            sensorManager.unregisterListener(mOrientationListener);
        }
*/
    }

    @Override
    public void doubleTapDetected() {
        if (mBroadcaster != null) {
            mBroadcaster.requestOtherCamera();
        }
    }
}
