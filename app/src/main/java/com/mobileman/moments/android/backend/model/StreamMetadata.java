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
import java.util.List;

import io.kickflip.sdk.api.json.Response;

/**
 * Created by MobileMan on 30/04/15.
 */
public class StreamMetadata  extends Response implements Serializable {

    @Key("state")
    private int streamState;

    @Key("events")
    private List<Comment> comments;

    @Key("watchers")
    private List<User> watchers;

    @Key("watchersCount")
    private int watchersCount;


    public StreamMetadata() {
        super();
    }

    public int getStreamState() {
        return streamState;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public List<User> getWatchers() {
        return watchers;
    }

    public int getWatchersCount() {
        return watchersCount;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
    }
}
