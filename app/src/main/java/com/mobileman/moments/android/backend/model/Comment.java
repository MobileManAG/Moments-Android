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
 * Created by MobileMan on 30/04/15.
 */
public class Comment extends Response implements Serializable, Comparable<Comment>{

    private boolean systemComment;

    private long visibleSinceDate;

    @Key("type")
    private int commentType;

    @Key("timestamp")
    private long timestamp;

    @Key("id")
    private String id;

    @Key("author")
    private User author;

    @Key("text")
    private String text;

    public Comment() {
        super();
        this.visibleSinceDate = System.currentTimeMillis();
    }

    public User getAuthor() {
        return author;
    }

    public String getText() {
        return text;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isSystemComment() {
        return systemComment;
    }

    public void setSystemComment(boolean isSystemComment) {
        this.systemComment = isSystemComment;
    }

    public long getVisibleSinceDate() {
        return visibleSinceDate;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getCommentType() {
        return commentType;
    }

    public void setCommentType(int commentType) {
        this.commentType = commentType;
    }

    @Override
    public int compareTo(Comment comment) {
        return ((comment.getTimestamp() == timestamp) ? 0 : (timestamp < comment.getTimestamp()) ? -1 : 1);
    }
}
