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
package com.mobileman.moments.android.frontend.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.backend.model.Friend;
import com.mobileman.moments.android.backend.model.FriendsList;
import com.mobileman.moments.android.frontend.LocalPersistence;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;
import com.mobileman.moments.android.frontend.adapters.MyFriendsAdapter;
import com.mobileman.moments.android.frontend.activity.MainActivity;

import java.util.ArrayList;
import java.util.List;

import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Created by MobileMan on 07/05/15.
 */
public class MyFriendsFragment extends Fragment implements MyFriendsAdapter.MyFriendsAdapterListener {

    public static final String ARGS_BACK_NAVIGATION_RIGHT_SIDE = "ARGS_BACK_NAVIGATION_RIGHT_SIDE";
    public static final String ARGS_NAVIGATION_EVENT_CALLBACK = "ARGS_NAVIGATION_EVENT_CALLBACK";

    private static final String SERIALIZED_FILESTORE_NAME = "my_friends";
    private ListView listView;
    private MainFragmentInteractionListener mCallback;
    private MyFriendsAdapter mAdapter;
    private List<Friend> myFriendsList;
    private KickflipApiClient mKickflip;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.my_friends, container, false);

        initListView(view);
        initButtons(view);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mCallback = (MainFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateData();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadPersistedStreams();
        getStreams(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        persistStreams();
    }

    private void initListView(View rootView) {
        listView = (ListView) rootView.findViewById(R.id.myFriendsListview);
        setupListViewAdapter();
    }

    private void initButtons(View rootView) {
        final ImageView myFriendsHeaderBackImageView = (ImageView) rootView.findViewById(R.id.myFriendsHeaderBackImageView);
        boolean arrowOnRightSide = false;
        if (getArguments() != null) {
            if (getArguments().containsKey(ARGS_BACK_NAVIGATION_RIGHT_SIDE) && getArguments().getBoolean(ARGS_BACK_NAVIGATION_RIGHT_SIDE)) {
                arrowOnRightSide = true;
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.RIGHT | Gravity.CENTER_VERTICAL;
                myFriendsHeaderBackImageView.setLayoutParams(params);
                Bitmap bmpOriginal = BitmapFactory.decodeResource(this.getResources(), R.drawable.arrow);
                Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas tempCanvas = new Canvas(bmResult);
                tempCanvas.rotate(180, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
                tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
                myFriendsHeaderBackImageView.setImageBitmap(bmResult);
                myFriendsHeaderBackImageView.setPadding(0, 0, getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin), 0);
            }
        }
        final boolean finalArrowOnRightSide = arrowOnRightSide;
        myFriendsHeaderBackImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallback != null) {
                    if (finalArrowOnRightSide) {
                        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.SHOW_STREAMS, null);
                    } else {
                        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.BACK_TO_SETTINGS, null);
                    }
                }
            }
        });
        myFriendsHeaderBackImageView.setOnTouchListener(new View.OnTouchListener() {
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    myFriendsHeaderBackImageView.setColorFilter(Color.argb(130, 0, 0, 0));
                    rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    myFriendsHeaderBackImageView.setColorFilter(Color.argb(0, 0, 0, 0));
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                        myFriendsHeaderBackImageView.setColorFilter(Color.argb(130, 0, 0, 0));
                    }
                }
                return false;
            }
        });

        View inviteFBFriends = rootView.findViewById(R.id.myFriendsInviteButton);
        inviteFBFriends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AppInviteDialog.canShow()) {
                    AppInviteContent content = new AppInviteContent.Builder()
                            .setApplinkUrl(Constants.MOMENTS_APP_LINK)
                            .setPreviewImageUrl(Constants.MOMENTS_INVITE_IMAGE_LINK)
                            .build();
                    AppInviteDialog.show(getActivity(), content);
                }
            }
        });
    }

    private void updateData() {
        mKickflip = new KickflipApiClient(getActivity(), MainActivity.getLoggedUser(), new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                getStreams(true);
            }

            @Override
            public void onError(KickflipException error) {
                if (error != null) {
                    if (error.isMissingPrivilegesError()) {
                        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.MISSING_PRIVILEGES, null);
                    } else if (error.getMessage() != null) {
                        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }

    /**
     * Load persisted Streams from disk if available.
     */
    private void loadPersistedStreams() {
        if (getActivity() != null) {
            Object friends = LocalPersistence.readObjectFromFile(getActivity(), SERIALIZED_FILESTORE_NAME);
            if (friends != null) {
                displayStreams((List<Friend>) friends);
            }
        }
    }

    /**
     * Serialize a few Streams to disk so the UI is quickly populated
     * on application re-launch
     *
     * If we had reason to keep a robust local copy of the data, we'd use sqlite
     */
    private void persistStreams() {
        if (getActivity() != null) {
            LocalPersistence.writeObjectToFile(getActivity(), myFriendsList, SERIALIZED_FILESTORE_NAME);
        }
    }

    private void displayStreams(List<Friend> streams) {
       myFriendsList.clear();
       myFriendsList.addAll(streams);
//        Collections.sort(mStreams);
        mAdapter.notifyDataSetChanged();
    }

    private void getStreams(final boolean refresh) {
        if (mKickflip.getActiveUser() == null) return;

        mKickflip.getFriendsList(new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (getActivity() != null) {
                    displayStreams(((FriendsList) response).getFriendsList());
                }
            }

            @Override
            public void onError(KickflipException error) {
                if ((error != null) && (error.getMessage() != null)) {
                    if (error.isMissingPrivilegesError()) {
                        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.MISSING_PRIVILEGES, null);
                    } else if (error.getMessage() != null) {
                        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    private void setupListViewAdapter() {
        if (mAdapter == null) {
            myFriendsList = new ArrayList<>(0);
            mAdapter = new MyFriendsAdapter(getActivity(), myFriendsList, this);
            listView.setAdapter(mAdapter);
        }
    }

    public void blockUnblockUser(final Friend friend, final boolean block) {
        if (mKickflip.getActiveUser() == null) return;

        mKickflip.changeBlockState(friend.getUuid(), block, new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (getActivity() != null) {
                    Toast.makeText(getActivity(), friend.getName() + " " + getResources().getString(block ? R.string.userBlockedSuccessfully : R.string.userUnblockedSuccessfully), Toast.LENGTH_LONG).show();
                    friend.setBlocked(block);
                    mAdapter.notifyDataSetChanged();    // reload data
                }
            }

            @Override
            public void onError(KickflipException error) {
                if (error != null) {
                    if (error.isMissingPrivilegesError()) {
                        mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.MISSING_PRIVILEGES, null);
                    } else if (error.getMessage() != null) {
                        Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

}
