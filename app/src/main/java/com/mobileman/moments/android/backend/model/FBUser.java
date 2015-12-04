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
package com.mobileman.moments.android.backend.model;

import com.facebook.AccessToken;
import com.mobileman.moments.android.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

/**
 * Created by MobileMan on 24/04/15.
 */
public class FBUser implements Serializable {

    private static final String FB_MALE = "male";
    private static final String FB_FEMALE = "female";

    private String email;

    private String facebookID;

    private  String userName;

    private String firstName;

    private String lastName;

    private AccessToken token;

    private int gender;

    private int acccount_type;

    private String pushNotificationId;

    public FBUser() {
        super();
        this.acccount_type = Constants.kMomentsAuthTypeFacebook;
    }


    public FBUser(JSONObject jsonObject) {
        this();

        try {
            this.facebookID = jsonObject.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            this.email = jsonObject.getString("email");
        } catch (JSONException e) {
        }

        try {
            this.userName = jsonObject.getString("name");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try {
            this.firstName = jsonObject.getString("first_name");
        } catch (JSONException e) {
        }

        try {
            this.lastName = jsonObject.getString("last_name");
        } catch (JSONException e) {
        }
        try {
            String fbGenderString = jsonObject.getString("gender");
            if (fbGenderString.equals(FB_MALE)) {
                this.gender = Constants.kGenderMale;
            } else if (fbGenderString.equals(FB_FEMALE)) {
                this.gender = Constants.kGenderFemale;
            }  else this.gender = Constants.kGenderUnknown;
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public String getEmail() {
        return email;
    }

    public String getFacebookID() {
        return facebookID;
    }

    public String getUserName() {
        return userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public AccessToken getToken() {
        return token;
    }

    public int getAcccount_type() {
        return acccount_type;
    }

    public int getGender() {
        return gender;
    }

    public void setToken(AccessToken token) {
        this.token = token;
    }

    public String getPushNotificationId() {
        return pushNotificationId;
    }

    public void setPushNotificationId(String pushNotificationId) {
        this.pushNotificationId = pushNotificationId;
    }
}
