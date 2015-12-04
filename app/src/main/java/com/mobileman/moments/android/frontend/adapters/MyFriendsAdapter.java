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
import android.widget.TextView;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.Friend;
import com.mobileman.moments.android.frontend.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * Created by MobileMan on 07/05/15.
 */
public class MyFriendsAdapter extends ArrayAdapter<Friend> {

    public interface MyFriendsAdapterListener {

        public void blockUnblockUser(Friend friend, boolean block);

    }

    public static final int LAYOUT_ID = R.layout.my_friends_list_item;
    private MyFriendsAdapterListener listenerCallback;


    public MyFriendsAdapter(final Context context, List<Friend> objects, MyFriendsAdapterListener callback) {
        super(context, LAYOUT_ID, objects);
        listenerCallback = callback;
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        final Friend friend = getItem(position);
        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, parent, false);
        }
        final View finalConvertView = convertView;
        ImageView mediaPlayerHeaderImageView = (ImageView) convertView.findViewById(R.id.myFriendsListItemAvatarImageView);
        View myFriendsListItemBlockUserTextView = convertView.findViewById(R.id.myFriendsListItemBlockUserTextView);
        TextView myFriendsListItemUsernameInfoTextView = (TextView) convertView.findViewById(R.id.myFriendsListItemUsernameInfoTextView);
        myFriendsListItemUsernameInfoTextView.setText("");
        if (friend.isNewFriend()) {
            myFriendsListItemUsernameInfoTextView.setTextColor(getContext().getResources().getColor(android.R.color.holo_red_light));
            myFriendsListItemUsernameInfoTextView.setText(getContext().getResources().getString(R.string.newFriend));
        } else if (friend.isBlocked()) {
            myFriendsListItemUsernameInfoTextView.setTextColor(getContext().getResources().getColor(android.R.color.black));
            myFriendsListItemUsernameInfoTextView.setText(getContext().getResources().getString(R.string.blocked));
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissBlockUserPopupIfVisible(view);
            }
        });

        myFriendsListItemBlockUserTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Friend selectedFriend = getItem(position);
//                handleBlockUnblockButtonClicker(finalConvertView, friend);
            }
        });

        if (friend.getFacebookID() != null) {
            TextView usernameTextView = (TextView) convertView.findViewById(R.id.myFriendsListItemUsernameTextView);
            usernameTextView.setText(friend.getName());

            if (friend.getFacebookID() != null) {
                int imageFrameSize = getContext().getResources().getDimensionPixelSize(R.dimen.imageFrameSize);
                Picasso.with(getContext())
                        .load(Util.getFacebookPictureUrl(friend.getFacebookID()))
                        .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                        .resize(imageFrameSize, imageFrameSize)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .centerCrop().into(mediaPlayerHeaderImageView);
            } else {
                mediaPlayerHeaderImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.user));
            }
        }

        return convertView;
    }

    private void dismissBlockUserPopupIfVisible(View rootView) {
        final TextView myFriendsListItemBlockUnblockButtonTextView = (TextView) rootView.findViewById(R.id.myFriendsListItemBlockUnblockButtonTextView);
        if (myFriendsListItemBlockUnblockButtonTextView.getVisibility() == View.VISIBLE) {
            myFriendsListItemBlockUnblockButtonTextView.setVisibility(View.GONE);
        }
    }

    private void handleBlockUnblockButtonClicker(View rootView, final Friend friend) {
        final TextView myFriendsListItemBlockUnblockButtonTextView = (TextView) rootView.findViewById(R.id.myFriendsListItemBlockUnblockButtonTextView);
        String buttonLabel = "";
        boolean futureStateBlocked = false;
        if (friend.isBlocked()) {
            buttonLabel = getContext().getResources().getString(R.string.unblockUser);
        } else {
            buttonLabel = getContext().getResources().getString(R.string.blockUser);
            futureStateBlocked = true;
        }

        myFriendsListItemBlockUnblockButtonTextView.setText(buttonLabel);
        myFriendsListItemBlockUnblockButtonTextView.setVisibility(View.VISIBLE);
        final boolean finalFutureState = futureStateBlocked;
        myFriendsListItemBlockUnblockButtonTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (listenerCallback != null) {
                    listenerCallback.blockUnblockUser(friend, finalFutureState);
                }
                myFriendsListItemBlockUnblockButtonTextView.setVisibility(View.GONE);
            }
        });
    }
}
