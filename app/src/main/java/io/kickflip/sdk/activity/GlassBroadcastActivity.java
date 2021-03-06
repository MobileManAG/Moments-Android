package io.kickflip.sdk.activity;

import android.os.Bundle;
import android.view.KeyEvent;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.frontend.activity.ImmersiveActivity;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.av.BroadcastListener;
import io.kickflip.sdk.exception.KickflipException;
import io.kickflip.sdk.fragment.GlassBroadcastFragment;

/**
 * BroadcastActivity manages a single live broadcast. It's a thin wrapper around {@link io.kickflip.sdk.fragment.BroadcastFragment}
 */
public class GlassBroadcastActivity extends ImmersiveActivity implements BroadcastListener{
    private static final String TAG = "GlassBroadcastActivity";

    private GlassBroadcastFragment mFragment;
    private BroadcastListener mMainBroadcastListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        mMainBroadcastListener = Kickflip.getBroadcastListener();
        Kickflip.setBroadcastListener(this);

        if (savedInstanceState == null) {
            mFragment = GlassBroadcastFragment.getInstance();
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
    public boolean onKeyDown(int keycode, KeyEvent event) {
        if (keycode == KeyEvent.KEYCODE_DPAD_CENTER) {
            mFragment.stopBroadcasting();
            return true;
        } else
            return super.onKeyDown(keycode, event);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onBroadcastStart() {
        mMainBroadcastListener.onBroadcastStart();
    }

    @Override
    public void onBroadcastStreaming(Stream stream, int currentBitrate) {
        mMainBroadcastListener.onBroadcastStreaming(stream, currentBitrate);
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
    public void onBroadcastError(KickflipException error) {
        mMainBroadcastListener.onBroadcastError(error);
    }

    @Override
    public void onTryingToReconnect(final boolean networkIssuesPresent) {}

    @Override
    public void onLowUploadBitrateDetected() {}

    @Override
    public void onBitrateChange(final int newVideoBitrate, final int bandwidth, final boolean bitrateDynamicallyUpdated, final int pendingItemsCount) {}

}
