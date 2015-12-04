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

import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;

/**
 * Created by MobileMan on 25/04/15.
 */
public class User extends Response implements Serializable {

    @Key("id")
    private String uuid;

    @Key("version")
    private int version;

    @Key("facebookId")
    private String facebookID;

    @Key("userName")
    private String name;

    @Key("firstName")
    private String firstName;

    @Key("lastName")
    private String lastName;

    @Key("account")
    private UserAccount account;

    @Key("gender")
    private int gender;

    @Key("stream")
    private HlsStream stream;

    private long createdOn;

    public User() {
        super();
        this.createdOn = System.currentTimeMillis();
    }

    public User(String uuid) {
        this();
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public int getVersion() {
        return version;
    }

    public String getFacebookID() {
        return facebookID;
    }

    public String getName() {
        return name;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UserAccount getAccount() {
        return account;
    }

    public int getGender() {
        return gender;
    }

    public HlsStream getStream() {
        return stream;
    }

    public void setStream(HlsStream stream) {
        this.stream = stream;
    }

    public void setFacebookID(String facebookID) {
        this.facebookID = facebookID;
    }

    public long getCreatedOn() {
        return createdOn;
    }
}
