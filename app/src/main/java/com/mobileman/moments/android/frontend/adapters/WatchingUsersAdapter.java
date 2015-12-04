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
package com.mobileman.moments.android.frontend.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.frontend.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by MobileMan on 12/05/15.
 */
public class WatchingUsersAdapter extends ArrayAdapter<User> {

    public static final int LAYOUT_ID = R.layout.watching_user_list_item;

    public WatchingUsersAdapter(final Context context, List<User> objects) {
        super(context, LAYOUT_ID, objects);
    }

    public static View createWatchingUserView(Context mContext, User user) {
        ViewGroup parent = null;
        View convertView = null;
        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, parent, false);
        }
        ImageView mediaPlayerHeaderWatchingUserImageView = (ImageView) convertView.findViewById(R.id.mediaPlayerHeaderWatchingUserImageView);
        if (user.getFacebookID() != null) {
            int imageFrameSize = mContext.getResources().getDimensionPixelSize(R.dimen.smallImageFrameSize);
            Picasso.with(mContext)
                    .load(Util.getFacebookPictureUrl(user.getFacebookID()))
                    .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                    .resize(imageFrameSize, imageFrameSize)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .centerCrop().into(mediaPlayerHeaderWatchingUserImageView);
        }

//        if ((System.currentTimeMillis() - user.getCreatedOn()) > Constants.BROADCAST_WATCHERS_VISIBILITY_DURATION) {
//            remove(user);
//        }
        return convertView;
    }
}
