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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.mobileman.moments.android.frontend.activity.IncomingCallActivity;
import com.mobileman.moments.android.frontend.activity.MainActivity;
import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by MobileMan on 28/04/15.
 */
public class MomentsPushBroadcastReceiver extends ParsePushBroadcastReceiver {


    protected void onPushReceive(android.content.Context context, android.content.Intent intent) {
        Log.d("Push", "onPushReceive");
        if((intent.getExtras() != null) && (intent.getExtras().getString(Constants.PUSH_CLASS_NAME) != null)) {
            String streamTitle = "";
            String userId = "";
            String userName = "";
            String fbId = "";
            int type = 0;
            boolean momentsApplicationVisible = MomentsApplication.isActivityVisible();

            JSONObject json = null;

            try {
                json = new JSONObject(intent.getExtras().getString(Constants.PUSH_CLASS_NAME));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (json != null) {
                try {
                    type = json.getInt("type");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (type == Constants.kNotificationTypeBroadcastStartedCreated) {
                    try {
                        streamTitle =  json.getString("alert");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        streamTitle = "";
                    }
                    try {
                        userId =  json.getString("userId");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        userId = "";
                    }
                    try {
                        userName =  json.getString("userName");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        userName = "";
                    }
                    try {
                        fbId =  json.getString("fbid");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        fbId = "";
                    }
                }

                if ((userId != null) && (userId.length() > 0)) {
                    if (!MainActivity.userBusyNow) {
                        showIncomingCallScreen(context, streamTitle, userId, userName, fbId, momentsApplicationVisible);
                    } else {
                        IncomingCallActivity.sendGeneralNotificationOfMissedCall(context, userName, streamTitle);
                    }
                }
            }
        }

//        super.onPushReceive(context, intent);
    }

    @Override
    public void onPushOpen(Context context, Intent intent) {
        // should not happen
        Log.d("Push", "onPushOpen");
    }

    private void showIncomingCallScreen(Context context, String streamTitle, String userId, String userName, String fbId, boolean momentsApplicationVisible) {
        Intent i = new Intent(context, IncomingCallActivity.class);
        int flags = Intent.FLAG_ACTIVITY_NO_HISTORY  | Intent.FLAG_ACTIVITY_NEW_TASK ;
        if (!MomentsApplication.isActivityVisible()) {
            flags = flags| Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP;
        }
        i.setFlags(flags);
        Bundle extras = new Bundle();
        extras.putString(IncomingCallActivity.ARG_STREAM_TITLE, streamTitle);
        extras.putString(IncomingCallActivity.ARG_USER_ID, userId);
        extras.putString(IncomingCallActivity.ARG_USERNAME, userName);
        extras.putString(IncomingCallActivity.ARG_FACEBOOK_ID, fbId);
        extras.putBoolean(IncomingCallActivity.ARG_MOMENTS_APPLICATION_IS_VISIBLE, momentsApplicationVisible);
        i.putExtras(extras);

        context.startActivity(i);
    }
}
