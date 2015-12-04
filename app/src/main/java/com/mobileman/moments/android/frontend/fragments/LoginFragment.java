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
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.login.widget.LoginButton;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.backend.service.NetworkStateListener;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;
import com.mobileman.moments.android.frontend.activity.MainActivity;

/**
 * Created by MobileMan on 22/04/15.
 */

public class LoginFragment extends Fragment {

    public final static String LOGIN_ENABLED = "LoginEnabled";

    private MainFragmentInteractionListener mCallBack;
    private boolean loginEnabled;
    private LoginButton loginButton;
//    private CallbackManager callbackManager;

    private View login_button;
    private View termsOfServicesTextView;

    public LoginFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_login, container, false);
        TextView mLink = (TextView) view.findViewById(R.id.termsOfServicesTextView);
        if (mLink != null) {
            mLink.setMovementMethod(LinkMovementMethod.getInstance());
        }

        if ((getArguments() != null) && (getArguments().containsKey(LOGIN_ENABLED))) {
            this.loginEnabled = getArguments().getBoolean(LOGIN_ENABLED);
        }

        login_button = view.findViewById(R.id.loginButton);
        termsOfServicesTextView = view.findViewById(R.id.termsOfServicesTextView);

        if (loginEnabled) {
            initializeFBLogin(view);
        } else {
            hideLoginButtons();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AccessToken.getCurrentAccessToken() != null) {
            if (MainActivity.tokenHasNeededPermissions(AccessToken.getCurrentAccessToken())) {
                hideLoginButtons();
                if (NetworkStateListener.isNetworkAvailable()) {
                    mCallBack.onFragmentEvent(MainFragmentInteractionListener.EVENT.FB_TOKEN_ID_RECEIVED, null);
                }
            }
//            else {
//                mCallBack.onFragmentEvent(MainFragmentInteractionListener.EVENT.LOGOUT, null);
//            }
        }

    }

    private void hideLoginButtons() {
        if (login_button != null) {
            login_button.setVisibility(View.GONE);
        }
        if (termsOfServicesTextView != null) {
            termsOfServicesTextView.setVisibility(View.GONE);
        }
    }

    private void initializeFBLogin(View rootView) {
//        callbackManager = CallbackManager.Factory.create();
        loginButton = (LoginButton) rootView.findViewById(R.id.loginButton);
        loginButton.setReadPermissions(Constants.FB_USER_FRIENDS_PERMISSION);
//        loginButton.setFragment(this);
    }
/*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }
*/
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallBack = (MainFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }
    }
}

