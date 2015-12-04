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

import com.google.api.client.util.Key;

import java.io.Serializable;

import io.kickflip.sdk.api.json.Response;

/**
 * Created by MobileMan on 25/04/15.
 */
public class UserAccount extends Response implements Serializable {

    @Key("id")
    private String uuid;

    @Key("email")
    private String email;

    @Key("token")
    private String token;

    @Key("pushNotificationID")
    private String pushNotificationID;

    // not used on the server???
    private String password;

    // not used on the server???
    private String deviceID;

    public UserAccount() {
        super();
    }

    public UserAccount(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    public String getPassword() {
        return password;
    }

    public String getDeviceID() {
        return deviceID;
    }
}
