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
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.frontend.fragments.MediaPlayerFragment;

import io.kickflip.sdk.api.json.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @hide
 */
public class MediaPlayerActivity extends ImmersiveActivity {

    public static final String USER_ID = "USER_ID";
    public static final String MEDIA_URL = "MEDIA_URL";
    public static final String THUMBNAIL_URL = "THUMBNAIL_URL";
    public static final String HISTORICAL_STREAM = "HISTORICAL_STREAM";

    private static final String TAG = "MediaPlayerActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setUseImmersiveMode(false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_playback);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);


        // This must be setup before
        //Uri intentData = getIntent().getData();
        //String mediaUrl = isKickflipUrl(intentData) ? intentData.toString() : getIntent().getStringExtra("mediaUrl");
        String mediaUrl = getIntent().getStringExtra(MEDIA_URL);
        String userId = getIntent().getStringExtra(USER_ID);
        String thumbnailUrl = getIntent().getStringExtra(THUMBNAIL_URL);
        Stream historicalStream = null;
        if (getIntent().hasExtra(HISTORICAL_STREAM)) {
            historicalStream = (Stream) getIntent().getSerializableExtra(HISTORICAL_STREAM);
        }
        checkNotNull(mediaUrl, new IllegalStateException("MediaPlayerActivity started without a mediaUrl"));
        checkNotNull(userId, new IllegalStateException("MediaPlayerActivity started without a user ID"));
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, MediaPlayerFragment.newInstance(mediaUrl, thumbnailUrl, userId, historicalStream))
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.media_playback, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
