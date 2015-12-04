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
package com.mobileman.moments.android.frontend.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;

public class TextViewCustomFont extends TextView {

    private static final String TAG = "TextView";

    public TextViewCustomFont(Context context) {
        super(context);
        setCustomFont(context, null);
    }

    public TextViewCustomFont(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, attrs);
    }

    public TextViewCustomFont(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setCustomFont(context, attrs);
    }

    private void setCustomFont(Context ctx, AttributeSet attrs) {
        String customFontString = null;
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.TextViewCustomFont);
        if (attrs != null) {
            customFontString = a.getString(R.styleable.TextViewCustomFont_customFont);
        }
        Util.setCustomFont(ctx, customFontString, this);
//        a.recycle();
    }
}