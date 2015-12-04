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

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;


/**
 * Created by MobileMan on 23/04/15.
 */
public class SetttingsFragment extends Fragment {

    private static final int SETTINGS_OPTION_MY_FRIENDS     = 0;
    private static final int SETTINGS_OPTION_MY_MOMENTS     = 1;
    private static final int SETTINGS_OPTION_SHARE          = 2;
    private static final int SETTINGS_OPTION_LOGOUT         = 3;
    private static final int SETTINGS_OPTION_DEBUG          = 4;

    private MainFragmentInteractionListener mCallback;
    private ListView listView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setttings, container, false);

        initSettingsButton((ImageButton) view.findViewById(R.id.logoButton));
        initSettingsList((ListView) view.findViewById(R.id.settingsOptionsList));

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (MainFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }

    }

    private void initSettingsList(ListView listView) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(),
                R.layout.settings_list_row, R.id.settingsListRowText, getResources().getStringArray(R.array.settingsOptions));

        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                switch (position) {
                    case SETTINGS_OPTION_MY_FRIENDS:
                        showMyFriends();
                        break;
                    case SETTINGS_OPTION_MY_MOMENTS:
                        showMyMoments();
                        break;
                    case SETTINGS_OPTION_SHARE:
                        doShareApplication();
                        break;
                    case SETTINGS_OPTION_LOGOUT:
                        doLogout();
                        break;
                    case SETTINGS_OPTION_DEBUG:
                        showDebugSettings();
                        break;
                }
            }
        });
    }

    private void showMyFriends() {
        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.FRIENDS_LIST, null);
    }

    private void showMyMoments() {
        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.MY_MOMENTS_LIST, null);
    }
    private void doShareApplication() {
        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.SHARE, null);
    }

    private void doLogout() {
        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.LOGOUT, null);
    }

    private void showDebugSettings() {
        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.DEBUG_SETTINGS, null);
    }

    private void initSettingsButton(final ImageButton logoButton) {
        logoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.toggleSettingsView(false, true);
            }
        });
        logoButton.setOnTouchListener(new View.OnTouchListener() {
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    logoButton.setColorFilter(Color.argb(130, 0, 0, 0));
                    rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    logoButton.setColorFilter(Color.argb(0, 0, 0, 0));
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                        logoButton.setColorFilter(Color.argb(130, 0, 0, 0));
                    }
                }
                return false;
            }
        });
    };

}
