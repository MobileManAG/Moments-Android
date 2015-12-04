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
import android.graphics.Typeface;

/**
 * Created by MobileMan on 14/11/2014.
 */
public class OpenSans {

    private Context context;
    private static OpenSans instance;
    private static Typeface sourceSansProRegular;
    private static Typeface sourceSansProBold;
    private static Typeface sourceSansProLight;
    private static Typeface systemDefault;

    public OpenSans(Context context) {
        this.context = context;
    }

    public static OpenSans getInstance(Context context) {
        synchronized (OpenSans.class) {
            if (instance == null) {
                instance = new OpenSans(context);
                sourceSansProRegular = Typeface.createFromAsset(context.getResources().getAssets(), "fonts/OpenSans-Regular.ttf");
                sourceSansProBold = Typeface.createFromAsset(context.getResources().getAssets(), "fonts/OpenSans-Semibold.ttf");
                sourceSansProLight = Typeface.createFromAsset(context.getResources().getAssets(), "fonts/OpenSans-Light.ttf");
                systemDefault = Typeface.defaultFromStyle(Typeface.NORMAL);
            }
            return instance;
        }
    }

    public Typeface getSourceSansProRegular() {
        return sourceSansProRegular;
    }

    public Typeface getSourceSansProBold() {
        return sourceSansProBold;
    }

    public Typeface getSourceSansProLight() {
        return sourceSansProLight;
    }

    public Typeface getSystemDefault() {
        return systemDefault;
    }

}
