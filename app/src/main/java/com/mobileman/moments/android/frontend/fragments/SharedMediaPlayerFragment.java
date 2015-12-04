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

import android.app.Fragment;
import android.location.Address;
import android.location.Geocoder;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.Comment;
import com.mobileman.moments.android.backend.model.StreamMetadata;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.frontend.RoundedTransformation;
import com.mobileman.moments.android.frontend.activity.MainActivity;
import com.mobileman.moments.android.frontend.adapters.CommentAdapter;
import com.mobileman.moments.android.frontend.adapters.WatchingUsersAdapter;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.api.json.Stream;
import io.kickflip.sdk.exception.KickflipException;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by MobileMan on 05/05/15.
 */
public class SharedMediaPlayerFragment extends Fragment {

    protected static final int DATA_REFRESH_INTERVAL = 1000;

    protected KickflipApiClient mKickflip;

    protected ImageView mediaPlayerHeaderImageView;
    protected TextView mediaPlayerUsernameTextView;
    protected TextView mediaPlayerLocationTextView;
    protected TextView mediaPlayerWatchersCountTextView;
    protected TextView mediaPlayerStreamTitle;
    protected EditText mediaPlayerAddCommentEditText;
    protected LinearLayout mediaPlayerHeaderWatchingUsersLayout;
    protected LinearLayout mediaPlayerCommentsView;
    protected long lastCommentTimestamp;
    protected User user;
    protected Handler repeatHandler;
//    protected CommentAdapter commentAdapter;
    protected WatchingUsersAdapter watchingUsersAdapter;
    protected HashMap<String, Object> commentsShownHashmap;
    protected HashMap<String, Object> watchingUsersShownHashmap;
    protected HashMap<String, Object> watchingUserShownViewsHashmap;
    protected boolean streamIsLive;
    protected boolean streamEnded;
    protected boolean streamIsHistorical;

    @Override
    public void onPause() {
        super.onPause();
        if (repeatHandler != null) {
            repeatHandler.removeCallbacksAndMessages(null);
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        repeatHandler = new Handler();
        repeatHandler.postDelayed(new Runnable() {
            public void run() {
                reloadStreamMetadata();
                repeatHandler.postDelayed(this, DATA_REFRESH_INTERVAL);
            }
        }, DATA_REFRESH_INTERVAL);
    }

    protected  void initializeWatchingUsersList(View rootView) {
        Object obj = rootView.findViewById(R.id.mediaPlayerHeaderWatchingUsersLayout);
        if (obj != null) {
            mediaPlayerHeaderWatchingUsersLayout = (LinearLayout) obj;
            ArrayList<User> usersWatchingArray = new ArrayList<>(0);
//            watchingUsersAdapter = new WatchingUsersAdapter(getActivity(), usersWatchingArray);
//            mediaPlayerWatchingUsersListView.setAdapter(watchingUsersAdapter);

//            int maxListHeight = Constants.BROADCAST_VIEWER_MAX_NUMBER_OF_PROFILE_IMAGES * getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.commentRowHeight);
//            mediaPlayerWatchingUsersListView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, maxListHeight));
//            mediaPlayerWatchingUsersListView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
            //mediaPlayerWatchingUsersListView.setStackFromBottom(true);
            watchingUsersShownHashmap = new HashMap<String, Object>();
            watchingUserShownViewsHashmap = new HashMap<String, Object>();
        }
    }

    protected void initializeCommentList(View rootView) {
        initializeWatchingUsersList(rootView);

        mediaPlayerCommentsView = (LinearLayout) rootView.findViewById(R.id.mediaPlayerCommentsView);

        int maxListHeight = Constants.BROADCAST_VIEWER_MAX_NUMBER_OF_COMMENTS_SHOWN * getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.buttonRowHeight);
        ScrollView mediaPlayerCommentsScrollView = (ScrollView) rootView.findViewById(R.id.mediaPlayerCommentsScrollView);
        mediaPlayerCommentsScrollView.fullScroll(View.FOCUS_DOWN);
        mediaPlayerCommentsScrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, maxListHeight));
//        mediaPlayerCommentsScrollView.setTranscriptMode(ListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
//        mediaPlayerCommentsScrollView.setStackFromBottom(true);

        ArrayList<Comment> commentsArray = new ArrayList<>(0);
//        commentAdapter = new CommentAdapter(getActivity(), commentsArray, mediaPlayerCommentsView);
//        mediaPlayerCommentsView.setAdapter(commentAdapter);

        commentsShownHashmap = new HashMap<String, Object>();

        if (mediaPlayerAddCommentEditText != null) {
            mediaPlayerAddCommentEditText.setOnKeyListener(new View.OnKeyListener() {
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    // If the event is a key-down event on the "enter" button
                    if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
                            (keyCode == KeyEvent.KEYCODE_ENTER)) {
                        String text = mediaPlayerAddCommentEditText.getText().toString();
                        mediaPlayerAddCommentEditText.setText("");
                        mKickflip.addComment(user.getStream().getStreamId(), text, new KickflipCallback() {
                            @Override
                            public void onSuccess(Response response) {
                                Log.d(Util.TAG, "Comment sent successfully");
                            }

                            @Override
                            public void onError(KickflipException error) {
                                if (error != null) {
                                    error.printStackTrace();
                                }
                            }
                        });

                        return true;
                    }
                    return false;
                }
            });
        }
    }



    protected void handleStreamMetadataChanges(final StreamMetadata streamMetadata) {

        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mediaPlayerWatchersCountTextView.setText(Integer.toString(streamMetadata.getWatchersCount()));
                    boolean watchersChanged = false;
                    if (streamMetadata.getComments() != null) {
                        for (Comment comment : streamMetadata.getComments()) {
                            checkArgument((comment.getId() != null), "Comment must have its ID!");
                            switch (comment.getCommentType()) {
                                case Constants.kStreamEventTypeJoin:
                                    addNewWatcher(comment.getAuthor(), comment.getId());
                                    watchersChanged = true;
                                    break;
                                case Constants.kStreamEventTypeLeave:
                                    removeWatcher(comment.getAuthor(), comment.getId());
                                    watchersChanged = true;
                                    commentsShownHashmap.put(comment.getId(), new Object());    // don't show comment about user leaving the broadcast
                                    break;
                            }
                            if (!commentsShownHashmap.containsKey(comment.getId())) {
                                final View commentView = CommentAdapter.createCommentView(getActivity(), comment);
                                mediaPlayerCommentsView.addView(commentView);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        mediaPlayerCommentsView.removeView(commentView);
                                    }
                                }, Constants.BROADCAST_COMMENTS_VISIBILITY_DURATION);

                                commentsShownHashmap.put(comment.getId(), new Object());
                            }
                        }
                    }

//                    if (watchersChanged) {
//                        watchingUsersAdapter.notifyDataSetChanged();
//                    }
                }
            });
        }
    }

    private void addNewWatcher(User watcher, String commentId) {
        if (!watchingUsersShownHashmap.containsKey(commentId)) {
            checkArgument((watcher.getUuid() != null), "Watcher must have its UUID!");
            checkArgument((watcher.getFacebookID() != null), "Watcher must have its Fb ID!");
            final View watchingUserView = WatchingUsersAdapter.createWatchingUserView(getActivity(), watcher);
            watchingUsersShownHashmap.put(commentId, new Object());
            watchingUserShownViewsHashmap.put(watcher.getUuid(), watchingUserView);
            mediaPlayerHeaderWatchingUsersLayout.addView(watchingUserView);
//            watchingUsersAdapter.add(watcher);
        }
    }

    private void removeWatcher(User watcher, String commentId) {
        if (!watchingUsersShownHashmap.containsKey(commentId)) {
            checkArgument((watcher.getUuid() != null), "Watcher must have its UUID!");
            checkArgument((watcher.getFacebookID() != null), "Watcher must have its Fb ID!");
            watchingUsersShownHashmap.put(commentId, new Object());

            if (watchingUserShownViewsHashmap.containsKey(watcher.getUuid())) {
                View view = (View) watchingUserShownViewsHashmap.get(watcher.getUuid());
                watchingUserShownViewsHashmap.remove(watcher.getUuid());
                mediaPlayerHeaderWatchingUsersLayout.removeView(view);
            }
        }
    }

    protected void handleStreamStateChanges(StreamMetadata streamMetadata) {
        if (streamMetadata.getStreamState() == Constants.kStreamStateClosed) {
            if (!streamEnded) {
                streamEnded = true;
                onBroadcastingEnded();
            }
        } else if (streamMetadata.getStreamState() == Constants.kStreamStateStreaming) {
            if (!streamIsLive) {
                streamIsLive = true;
                onBroadcastingIsLive();
            }
        }
    }

    protected void onBroadcastingEnded() {
    }

    protected void onBroadcastingIsLive() {
    }

    protected void reloadStreamMetadata() {
        if ((user != null) && (mKickflip != null) && (!streamIsHistorical)) {
            mKickflip.streamMetadata(user.getStream().getStreamId(), lastCommentTimestamp, new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                    StreamMetadata streamMetadata = (StreamMetadata) response;
                    Log.i(Util.TAG, "got kickflip stream metadata ");
                    if ((streamMetadata.getComments() != null) && (streamMetadata.getComments().size() > 0)) {
                        Comment latestComment = streamMetadata.getComments().get(0);
                        lastCommentTimestamp = latestComment.getTimestamp();
                        Collections.sort(streamMetadata.getComments());
                    }
/*                    if (streamMetadata.getComments() == null) {
                        streamMetadata.setComments(new ArrayList<Comment>());
                    }
                    Comment newComment = new Comment();
                    newComment.setAuthor(MainActivity.getLoggedUser());
                    newComment.setId(UUID.randomUUID().toString());
                    newComment.setCommentType(Constants.kStreamEventTypeComment);
                    newComment.setText(UUID.randomUUID().toString());
                    streamMetadata.getComments().add(newComment);
*/
                    handleStreamMetadataChanges(streamMetadata);
                    handleStreamStateChanges(streamMetadata);
                }

                @Override
                public void onError(KickflipException error) {
                    Log.i(Util.TAG, "get kickflip stream meta failed");
                }
            });
        }
    }

    protected void updateUIWithStreamDetails(final User user) {
        String facebookId = user.getFacebookID();
        if ((facebookId == null) && (MainActivity.getLoggedUser().getUuid().equals(user.getUuid()))) {
            facebookId = MainActivity.getLoggedUser().getFacebookID();
        }
        if (facebookId != null) {
            int imageFrameSize = getActivity().getApplicationContext().getResources().getDimensionPixelSize(R.dimen.imageFrameSize);
            Picasso.with(getActivity().getApplicationContext())
                    .load(Util.getFacebookPictureUrl(facebookId))
                    .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                    .resize(imageFrameSize, imageFrameSize)
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .centerCrop().into(mediaPlayerHeaderImageView);
        } else {
            mediaPlayerHeaderImageView.setImageDrawable(getActivity().getApplicationContext().getResources().getDrawable(R.drawable.user));
        }
        mediaPlayerUsernameTextView.setText(user.getName());
        mediaPlayerLocationTextView.setText("");
        mediaPlayerStreamTitle.setText(user.getStream().getTitle());

        new Thread(new Runnable() {
            @Override
            public void run() {
                guessLocation(user.getStream());
            }
        }).start();
    }

    protected void guessLocation(Stream stream) {
        if((stream.getLocation() == null) || (stream.getLocation().getLatitude() == 0)) {
            return;
        }
        Geocoder gcd = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
        List<Address> addresses = null;
//        stream.setLatitude(54.7613889);
//        stream.setLongitude(17.5506497);

        try {
            addresses = gcd.getFromLocation(stream.getLocation().getLatitude(), stream.getLocation().getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if ((addresses != null) && (addresses.size() > 0)) {
            final Address address = addresses.get(0);
            String locationString = (address.getLocality() == null) ? (getResources().getString(R.string.somewhereIn) + " ") : (address.getLocality() + ", ");
            locationString =  locationString + address.getCountryName();
            final String finalLocationString = locationString;
            System.out.println(addresses.get(0).toString());
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //uddate UI
                        mediaPlayerLocationTextView.setText(finalLocationString);
                        ViewGroup parentLayout = (ViewGroup) mediaPlayerLocationTextView.getParent();
                        parentLayout.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }
}
