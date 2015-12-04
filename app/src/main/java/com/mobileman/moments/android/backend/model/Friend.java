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

/**
 * Created by MobileMan on 07/05/15.
 */
public class Friend extends User implements Serializable {

    @Key("newFriend")
    private boolean newFriend;

    private boolean invitationSend;

    @Key("blocked")
    private boolean blocked;

    public Friend() {
        super();
    }

    public boolean isNewFriend() {
        return newFriend;
    }

    public boolean isInvitationSend() {
        return invitationSend;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setNewFriend(boolean newFriend) {
        this.newFriend = newFriend;
    }

    public void setInvitationSend(boolean invitationSend) {
        this.invitationSend = invitationSend;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
