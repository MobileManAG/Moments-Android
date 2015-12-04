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
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.frontend.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * StreamAdapter connects a List of Streams
 * to an Adapter backed view like ListView or GridView
 */
public class StreamAdapter extends ArrayAdapter<User> {

    public interface StreamListListener {
        public void onStreamPlaybackRequested(User user);
    }

    public static final int LAYOUT_ID = R.layout.stream_list_item;
    private StreamListListener mListener;
    private String mUsername;

    public StreamAdapter(final Context context, List<User> objects, StreamListListener listener) {
        super(context, LAYOUT_ID, objects);
        mListener = listener;
    }

    /**
     * Set a Kickflip username to enable this adapter
     * to stylize user-owned entries appropriately.
     *
     * Should be called before {@link #notifyDataSetChanged()}
     *
     * @param userName the Kickflip username this view should be stylized for
     */
    public void setUserName(String userName) {
        mUsername = userName;
    }

    /**
     * Refresh the entire data structure underlying this adapter,
     * resuming the precise scroll state.
     *
     * @param listView
     * @param streams
     */
    public void refresh(AbsListView listView, List<User> streams) {
        Parcelable state = listView.onSaveInstanceState();
        clear();
        addAll(streams);
        notifyDataSetChanged();
        listView.onRestoreInstanceState(state);
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        User user = getItem(position);
//        ViewHolder holder;
        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, parent, false);
/*            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.image);
            holder.titleView = (TextView) convertView.findViewById(R.id.title);
            holder.liveBannerView = (TextView) convertView.findViewById(R.id.liveLabel);
            holder.rightTitleView = (TextView) convertView.findViewById(R.id.rightTitle);
            holder.overflowBtn = (ImageButton) convertView.findViewById(R.id.overflowBtn);
            holder.actions = convertView.findViewById(R.id.actions);
            convertView.setTag(holder);
            convertView.findViewById(R.id.overflowBtn).setOnClickListener(mOverflowBtnClickListener);
*/
        } else {
//            holder = (ViewHolder) convertView.getTag();
        }
        TextView streamListUsernameTextView = (TextView) convertView.findViewById(R.id.streamListUsernameTextView);
        streamListUsernameTextView.setText(user.getName());

        Button liveButton = (Button) convertView.findViewById(R.id.liveButton);
        liveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    User chosenUser = getItem(position);
                    mListener.onStreamPlaybackRequested(chosenUser);
                }
            }
        });
        // Hide the user actions panel
/*        holder.actions.setVisibility(View.GONE);
        holder.overflowBtn.setTag(position);

        int streamLengthSec = 123; //user.getLengthInSeconds();
        if (streamLengthSec == 0) {
            // A Live Stream
            holder.liveBannerView.setVisibility(View.VISIBLE);
            holder.rightTitleView.setText("started ");// + Util.getHumanRelativeDateStringFromString(user.getTimeStarted()));
        } else {
            // A previously ended Stream
            holder.liveBannerView.setVisibility(View.GONE);
            holder.rightTitleView.setText(String.format("%dm %ds",
                    TimeUnit.SECONDS.toMinutes(streamLengthSec),
                    TimeUnit.SECONDS.toSeconds(streamLengthSec) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(streamLengthSec))
            ));
        }
*/
        ImageView streamListUserAvatarImageView = (ImageView) convertView.findViewById(R.id.streamListUserAvatarImageView);
        if (user.getFacebookID() != null) {
            int imageFrameSize = getContext().getResources().getDimensionPixelSize(R.dimen.imageFrameSize);
            Picasso.with(getContext())
                    .load(Util.getFacebookPictureUrl(user.getFacebookID()))
                    .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                    .resize(imageFrameSize, imageFrameSize)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .centerCrop().into(streamListUserAvatarImageView);
        } else {
            streamListUserAvatarImageView.setImageDrawable(getContext().getResources().getDrawable(R.drawable.user));
        }

        return convertView;
    }

    public static class ViewHolder {
        ImageView imageView;
        TextView titleView;
        TextView liveBannerView;
        TextView rightTitleView;
        ImageButton overflowBtn;
        View actions;
    }
/*
    private View.OnClickListener mOverflowBtnClickListener = new View.OnClickListener(){

        @Override
        public void onClick(View overflowBtn) {
            // Toggle the Action container's visibility and set Tags on its subviews

            View listItemParent = ((View)overflowBtn.getParent());
            if (isActionContainerVisible(listItemParent)) {
                hideActionContainer(listItemParent);
            } else {
                View actionContainer = listItemParent.findViewById(R.id.actions);
                if (mUsername != null) {
                    // TODO: Server returns AppUser, not individual user.
                    if (mUsername.compareTo(getItem(((Integer) overflowBtn.getTag())).getOwnerName()) == 0) {
                        ((ImageButton) actionContainer.findViewById(R.id.flagBtn)).setImageResource(R.drawable.ic_trash);
                    } else {
                        ((ImageButton) actionContainer.findViewById(R.id.flagBtn)).setImageResource(R.drawable.ic_red_flag);
                    }

                }
                showActionContainer(listItemParent);
                // Transfer the overflowBtn tag to the two newly revealed buttons
                actionContainer.findViewById(R.id.flagBtn).setTag(overflowBtn.getTag());
                actionContainer.findViewById(R.id.flagBtn).setOnClickListener(mFlagBtnClick);
                actionContainer.findViewById(R.id.shareBtn).setTag(overflowBtn.getTag());
                actionContainer.findViewById(R.id.shareBtn).setOnClickListener(mShareBtnClick);
            }
        }

    };
*/

    /*
    private View.OnClickListener mFlagBtnClick = new View.OnClickListener(){

        @Override
        public void onClick(View flagBtn) {
            mActionListener.onFlagButtonClick(getItem((Integer) flagBtn.getTag()));
            hideActionContainer((View) flagBtn.getParent().getParent());
        }
    };

    private View.OnClickListener mShareBtnClick = new View.OnClickListener(){

       @Override
        public void onClick(View shareBtn) {
            mActionListener.onShareButtonClick(getItem((Integer) shareBtn.getTag()));
            hideActionContainer((View) shareBtn.getParent().getParent());
        }
    };
*/
    private boolean isActionContainerVisible(View listItemParent) {
        return false; //listItemParent.findViewById(R.id.actions).getVisibility() == View.VISIBLE;
    }

    private void showActionContainer(View listItemParent) {
/*        View actionContainer = listItemParent.findViewById(R.id.actions);

        ObjectAnimator imageWashOut = ObjectAnimator.ofFloat(listItemParent.findViewById(R.id.image), "alpha", 1f, 0.4f);
        imageWashOut.setDuration(250);
        imageWashOut.start();

        actionContainer.setVisibility(View.VISIBLE);
*/
    }

    private void hideActionContainer(View listItemParent) {
/*        View actionContainer = listItemParent.findViewById(R.id.actions);

        ObjectAnimator imageWashOut = ObjectAnimator.ofFloat(listItemParent.findViewById(R.id.image), "alpha", 0.4f, 1.0f);
        imageWashOut.setDuration(250);
        imageWashOut.start();

        actionContainer.setVisibility(View.GONE);
*/
    }

/*    public static interface StreamAdapterActionListener {
        public void onFlagButtonClick(Stream stream);
        public void onShareButtonClick(Stream stream);

    }
    */

}
