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
package com.mobileman.moments.android.backend.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.mobileman.moments.android.MomentsApplication;

/**
 * Created by MobileMan on 10.11.14.
 */
public class NetworkStateListener extends BroadcastReceiver {

    private static NetworkStateListenerInterface callback;

    public interface NetworkStateListenerInterface {
        public void internetConnectionAvailable();
        public void internetConnectionLost();

    }

    public static void setCallback(NetworkStateListenerInterface networkStateListenerInterface) {
        NetworkStateListener.callback = networkStateListenerInterface;
    }

    public static boolean isNetworkAvailable() {
        Context context = MomentsApplication.getApplication();
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        boolean available = info != null && info.isConnected();
        return available;
    }

    public static void initialize() {
        Context context = MomentsApplication.getApplication();
        updateStatus(context);
    }


    private static void updateStatus(Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo[] info = connectivityManager.getAllNetworkInfo();

        for (int i = 0; i<info.length; i++){
            if (info[i].getState() == NetworkInfo.State.CONNECTED){
                if (callback != null) {
                    callback.internetConnectionAvailable();
                }

                // at least one network interface is open
                break;
            }
        }

        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.internetConnectionLost();
            }
        }
    }

    public void onReceive(Context context, Intent intent) {
        Log.d("app", "Network connectivity change");
        updateStatus(context);
    }
}
