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
 * Created by MobileMan on 07/05/15.
 */
public abstract class Slice extends Response implements Serializable {

    @Key("numberOfElements")
    private int mTotalItems;

    @Key("number")
    private int mPageNumber;

    @Key("size")
    private int mResultsPerPage;

    @Key("first")
    private boolean isFirst;

    @Key("last")
    private boolean isLast;

    public Slice() {
        super();
    }

    public abstract List getContent();

    public boolean isNextPageAvailable() {
        return (!isLast);
    }

    public int getTotalItems() {
        return mTotalItems;
    }

    public int getPageNumber() {
        return mPageNumber;
    }

    public int getResultsPerPage() {
        return mResultsPerPage;
    }

    public boolean isFirst() {
        return isFirst;
    }

    public boolean isLast() {
        return isLast;
    }

    public boolean hasNext() {
        return (!isLast);
    }

    public boolean isPreviousPageAvailable() {
        return (!isFirst);
    }
}
