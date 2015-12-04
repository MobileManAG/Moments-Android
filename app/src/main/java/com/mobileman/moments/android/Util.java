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
import android.graphics.Typeface;
import android.os.Build;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.TextView;

import com.mobileman.moments.android.frontend.custom.OpenSans;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import io.kickflip.sdk.Kickflip;
import io.kickflip.sdk.av.SessionConfig;

/**
 * Created by David Brodsky on 3/20/14.
 */
public class Util {

    public static final String TAG = "Moments";

    //"04/03/2014 23:41:37",
    private static SimpleDateFormat mMachineSdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
    ;
    private static SimpleDateFormat mHumanSdf = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);

    public static final String CUSTOM_FONT_OPEN_SANS_LIGHT = "light";

    private static boolean fbPostingState;

    static {
        mMachineSdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static boolean isKitKat() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
    }

    public static String getHumanDateString() {
        return mHumanSdf.format(new Date());
    }

    public static String getHumanRelativeDateStringFromString(String machineDateStr) {
        String result = null;
        try {
            result = DateUtils.getRelativeTimeSpanString(mMachineSdf.parse(machineDateStr).getTime()).toString();
            result = result.replace("in 0 minutes", "just now");
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Create a {@link io.kickflip.sdk.av.SessionConfig}
     * corresponding to a 720p video stream
     *
     * @param outputPath the desired recording output path
     * @return the resulting SessionConfig
     */
/*
    public static SessionConfig create720pSessionConfig(String outputPath) {
        HashMap<String, String> extraData = new HashMap<>();
        extraData.put("key", "value");

        SessionConfig config = new SessionConfig.Builder(outputPath, Constants.DEFAULT_HLS_SEGMENT_DURATION)
                .withTitle(Util.getHumanDateString())
                .withDescription("A live stream!")
                .withAdaptiveStreaming(true)
                .withVideoResolution(720, 1280) // HD: (720, 1280)
                .withVideoBitrate(2 * 1000 * 1000)
                .withAudioBitrate(64 * 1000)
//                .withExtraInfo(extraData)
//                .withPrivateVisibility(false)
                .withLocation(true)
                .build();
        return config;
    }

    */

    /**
     * Create a {@link io.kickflip.sdk.av.SessionConfig}
     * corresponding to a 420p video stream
     *
     * @param outputPath the desired recording output path
     * @return the resulting SessionConfig
     */
    public static SessionConfig create420pSessionConfig(String outputPath) {
        int videoBitrate = 3 * 100 * 1024;
        if (!Kickflip.isKitKat()) {
            videoBitrate = Constants.API_18_FIXED_BITRATE;
        }
        SessionConfig config = new SessionConfig.Builder(outputPath, Constants.DEFAULT_HLS_SEGMENT_DURATION)
                .withTitle(Util.getHumanDateString())
                .withVideoBitrate(videoBitrate)
                .withAudioBitrate(64 * 1024)
                .withPrivateVisibility(false)
                .withAdaptiveStreaming(true)
                .withLocation(true)
                .withVideoResolution(480, 640)
                .build();
        return config;
    }

    public static String getFacebookPictureUrl(String facebookId) {
        String resultUrl = "https://graph.facebook.com/" + facebookId + "/picture?type=large";
        return resultUrl;
    }

    public static boolean setCustomFont(Context ctx, String fontName, TextView view) {
        Typeface tf = null;
        boolean isBold = false;
        boolean isItalic = false;
        if (view.getTypeface() != null) {
            isItalic = view.getTypeface().isItalic();
            isBold = view.getTypeface().isBold();
        }
        if((fontName != null) && (fontName.equals(CUSTOM_FONT_OPEN_SANS_LIGHT))) {
            isItalic = true;
        }
        try {
            if (isBold) {
                tf = OpenSans.getInstance(ctx).getSourceSansProBold();
            } else if (isItalic) {
                tf = OpenSans.getInstance(ctx).getSourceSansProLight();
            } else {
                tf = OpenSans.getInstance(ctx).getSourceSansProRegular();
            }
        } catch (Exception e) {
            Log.d("Iboga", "Custom font loading failed");
        }
        if (tf != null) {
            if (tf.isItalic()) {
                view.setTypeface(OpenSans.getInstance(ctx).getSystemDefault());
            } else view.setTypeface(tf);
        } else if (isItalic) {
            view.setTypeface(OpenSans.getInstance(ctx).getSystemDefault());
        }

        return true;
    }

    public static void saveFbPostingState(Context context, boolean value) {
        Util.fbPostingState = value;
//        SharedPreferences settings = context.getSharedPreferences(Constants.MOMENTS_PREFERENCES, 0);
//        SharedPreferences.Editor editor = settings.edit();
//        editor.putBoolean(Constants.MOMENTS_PREFERENCES_FB_STATE, value);
//        editor.commit();
    }

    public static boolean readFacebookPostingState(Context context) {
        boolean result = Util.fbPostingState;
//        SharedPreferences settings = context.getSharedPreferences(Constants.MOMENTS_PREFERENCES, 0);
//        result = settings.getBoolean(Constants.MOMENTS_PREFERENCES_FB_STATE, false);
        return result;
    }

}
