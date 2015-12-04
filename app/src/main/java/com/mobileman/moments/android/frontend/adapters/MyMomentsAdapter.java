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
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.mobileman.moments.android.R;
import com.squareup.picasso.Picasso;

import java.util.List;

import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Stream;

/**
 * Created by MobileMan on 29/05/15.
 */
public class MyMomentsAdapter extends ArrayAdapter<HlsStream> {

    public interface MyMomentsListListener {

        public void onStreamPlaybackRequested(HlsStream stream);

        public void onStreamDeletionRequested(HlsStream stream);
    }

    public static final int LAYOUT_ID = R.layout.my_moments_list_item;
    private MyMomentsListListener mListener;
    private String mUsername;

    public MyMomentsAdapter(final Context context, List<HlsStream> objects, MyMomentsListListener listener) {
        super(context, LAYOUT_ID, objects);
        mListener = listener;
    }

    /**
     * Refresh the entire data structure underlying this adapter,
     * resuming the precise scroll state.
     *
     * @param listView
     * @param streams
     */
    public void refresh(AbsListView listView, List<HlsStream> streams) {
        Parcelable state = listView.onSaveInstanceState();
        clear();
        addAll(streams);
        notifyDataSetChanged();
        listView.onRestoreInstanceState(state);
    }

    public View getView(final int position, View convertView, ViewGroup parent) {
        Stream stream = getItem(position);

        if (convertView == null) {
            convertView = ((LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, parent, false);
        }
        TextView myMomentsTitleTextView = (TextView) convertView.findViewById(R.id.myMomentsTitleTextView);
        if ((stream.getTitle() == null) || (stream.getTitle().length() == 0)) {
            myMomentsTitleTextView.setVisibility(View.INVISIBLE);
        } else {
            myMomentsTitleTextView.setVisibility(View.VISIBLE);
            myMomentsTitleTextView.setText(stream.getTitle());
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismissDeleteStreamPopupIfVisible(view);
            }
        });

        ImageButton myMomentsPlayImageButton = (ImageButton) convertView.findViewById(R.id.myMomentsPlayImageButton);
        myMomentsPlayImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    final HlsStream chosenStream = getItem(position);
                    mListener.onStreamPlaybackRequested(chosenStream);
                }
            }
        });

        final TextView myMomentsMenuTextView = (TextView) convertView.findViewById(R.id.myMomentsMenuTextView);
        final View myMomentsDeleteTextView = convertView.findViewById(R.id.myMomentsDeleteTextView);
        myMomentsDeleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    final HlsStream chosenStream = getItem(position);
                    mListener.onStreamDeletionRequested(chosenStream);
                }
                myMomentsDeleteTextView.setVisibility(View.GONE);
            }
        });

        myMomentsMenuTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myMomentsDeleteTextView.setVisibility(View.VISIBLE);
            }
        });

        ImageView myMomentsImageView = (ImageView) convertView.findViewById(R.id.myMomentsImageView);
        final View finalView = convertView;
        myMomentsImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!dismissDeleteStreamPopupIfVisible(finalView)) {
                    if (mListener != null) {
                        final HlsStream chosenStream = getItem(position);
                        mListener.onStreamPlaybackRequested(chosenStream);
                    }
                }
            }
        });

        if (stream.getThumbnailFileName() != null) {
            String thumbnailUrl = stream.getFullThumbnailUrl();
            Picasso.with(getContext())
                    .load(thumbnailUrl)
                    .into(myMomentsImageView);
        }

        return convertView;
    }

    private boolean dismissDeleteStreamPopupIfVisible(final View rootView) {
        boolean dismissed = false;
        final TextView myMomentsDeleteTextView = (TextView) rootView.findViewById(R.id.myMomentsDeleteTextView);
        if (myMomentsDeleteTextView.getVisibility() == View.VISIBLE) {
            myMomentsDeleteTextView.setVisibility(View.GONE);
            dismissed = true;
        }
        return dismissed;
    }

}
