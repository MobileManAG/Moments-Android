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
package com.mobileman.moments.android.frontend.activity;

import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * Created by MobileMan on 12/05/15.
 */
public class CustomFrameLayout extends FrameLayout {

    public interface DoubleTapCallback {
        public void doubleTapDetected();
    }

    private DoubleTapCallback doubleTapCallback;

    private GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            Log.d("TEST", "onDoubleTap");
            if (doubleTapCallback != null) {
                doubleTapCallback.doubleTapDetected();
            }
            return super.onDoubleTap(e);
        }
    });

    public CustomFrameLayout(android.content.Context context) {
        super(context);
    }

    public CustomFrameLayout(android.content.Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomFrameLayout(android.content.Context context, android.util.AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.onInterceptTouchEvent(ev);
    }

    public DoubleTapCallback getDoubleTapCallback() {
        return doubleTapCallback;
    }

    public void setDoubleTapCallback(DoubleTapCallback doubleTapCallback) {
        this.doubleTapCallback = doubleTapCallback;
    }
}
