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
package com.mobileman.moments.android;

/**
 * Created by MobileMan on 24/04/15.
 */
public class Constants {

    public static final String MOMENTS_PREFERENCES = "MOMENTS_PREFERENCES";
    public static final String MOMENTS_PREFERENCES_FB_STATE = "MOMENTS_PREFERENCES_FB_STATE";

    public static final String MOMENTS_INVITE_IMAGE_LINK = "http://yourserver/FB_AppInvite_image_1200_628.png";
    public static final String MOMENTS_APP_LINK = "http://yourserver/myapp.html";

    public static final String FB_USER_FRIENDS_PERMISSION       = "user_friends";
    public static final String FB_PUBLISH_ACTIONS_PERMISSION    = "publish_actions";
    public static final String FB_PUBLISH_ACTION_VIDEO          = "user_actions.video";


    public static final int BROADCAST_VIEWER_MAX_NUMBER_OF_PROFILE_IMAGES = 4;
    public static final int BROADCAST_VIEWER_MAX_NUMBER_OF_COMMENTS_SHOWN = 4;

    public static final long BROADCAST_COMMENTS_VISIBILITY_DURATION = 8 * 1000l;

    public static final int kStreamEventTypeUnknown = -1;
    public static final int kStreamEventTypeComment = 0;
    public static final int kStreamEventTypeLeave = 1;
    public static final int kStreamEventTypeJoin = 2;

    public static final int kMomentsAuthTypeUnknown  = 0;
    public static final int kMomentsAuthTypeNative   = 1;
    public static final int kMomentsAuthTypeFacebook = 2;


    public static final int kGenderUnknown  = 0;
    public static final int kGenderMale     = 1;
    public static final int kGenderFemale   = 2;

    public static final int kStreamStateCreated     = 0;
    public static final int kStreamStateReady       = 1;
    public static final int kStreamStateStreaming   = 2;
    public static final int kStreamStateClosed      = 3;


    public static final int kDeviceTypeUnknown = 0;
    public static final int kDeviceTypeIOS = 1;
    public static final int kDeviceTypeAndroid = 2;

    public static final int kNotificationTypeBroadcastStartedCreated  = 0;

    public static final int MINIMUM_UPLOAD_BITRATE = 20000;

    public static final int API_18_FIXED_BITRATE = 100000;

    public static final int DEFAULT_HLS_SEGMENT_DURATION = 7;

}
