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
package com.mobileman.moments.android.frontend;

import com.mobileman.moments.android.backend.model.User;

import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Stream;

/**
 * All activities used with MainActivity implement this interface
 * for communication.
 */
public interface MainFragmentInteractionListener {

    public static enum EVENT { START_BROADCAST, LOGOUT, MISSING_PRIVILEGES, SHARE, FRIENDS_LIST, MY_MOMENTS_LIST, BACK_TO_SETTINGS, FB_TOKEN_ID_RECEIVED, SHOW_STREAMS, DEBUG_SETTINGS };

    public void onFragmentEvent(EVENT event, Object parameters);

    public void onStreamPlaybackRequested(User user);

    public void playHistoricalStream(HlsStream stream);

    public void toggleSettingsView(boolean show, boolean animateVertically);
}
