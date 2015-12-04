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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.login.LoginManager;
import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.MomentsApplication;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.User;
import com.mobileman.moments.android.backend.model.UsersList;
import com.mobileman.moments.android.frontend.EndlessScrollListener;
import com.mobileman.moments.android.frontend.LocalPersistence;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;
import com.mobileman.moments.android.frontend.activity.BroadcastActivity;
import com.mobileman.moments.android.frontend.activity.MainActivity;
import com.mobileman.moments.android.frontend.adapters.StreamAdapter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.exception.KickflipException;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 * <p/>
 */
public class StreamListFragment extends Fragment implements AbsListView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener, StreamAdapter.StreamListListener {

    public static final String TAG = "StreamListFragment";
    private static final String SERIALIZED_FILESTORE_NAME = "streams";
    private static final boolean VERBOSE = !MomentsApplication.isOnProduction();

    private MainFragmentInteractionListener mCallBack;
    private SwipeRefreshLayout mSwipeLayout;
    private KickflipApiClient mKickflip;
    private List<User> mStreams;
    private boolean mRefreshing;
    private View streamListMainLayout;
    private Button recordButton;
//    private Button recordButtonForKeyboardOpen;
    private EditText broadcastingTitleEditText;
    private TextView fragmentsStreamFacebookPostingStateTextView;
    private ImageView fragmentStreamFacebookLogoImageView;
    private int mCurrentPage = 1;
    private static final int ITEMS_PER_PAGE = 10;

    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public StreamListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateData();
    }

    @Override
    public void onResume() {
        super.onResume();
        broadcastingTitleEditText.clearFocus();
        if (streamListMainLayout != null) {
            streamListMainLayout.requestFocus();
        }
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_stream_list, container, false);

        initSettingsButton(view.findViewById(R.id.logoButton));

        initFBStateButtons(view);

        recordButton = (Button) view.findViewById(R.id.recordButton);
//        recordButtonForKeyboardOpen = (Button) view.findViewById(R.id.recordButtonForKeyboardOpen);
//        recordButtonForKeyboardOpen.setVisibility(View.INVISIBLE);
        streamListMainLayout = view.findViewById(R.id.streamListMainLayout);
        broadcastingTitleEditText = (EditText)view.findViewById(R.id.broadcastingTitleEditText);

        initializeRecordButton(recordButton);


/*        broadcastingTitleEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                recordButton.setVisibility(View.INVISIBLE);
                recordButtonForKeyboardOpen.setVisibility(View.VISIBLE);
                return false;
            }
        });
        broadcastingTitleEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if (((keyEvent.getAction() == KeyEvent.ACTION_DOWN) &&   // the keyboard will be hidden soon
                        (keyCode == KeyEvent.KEYCODE_ENTER)) || (keyCode == KeyEvent.KEYCODE_BACK)) {
                    recordButtonForKeyboardOpen.setVisibility(View.INVISIBLE);
                    recordButton.setVisibility(View.VISIBLE);
                    broadcastingTitleEditText.clearFocus();
                    if (streamListMainLayout != null) {
                        streamListMainLayout.requestFocus();
                    }
                }
                return false;
            }
        });
*/
        // Set the adapter
        mListView = (AbsListView) view.findViewById(android.R.id.list);
//        mListView.setOnScrollListener(mEndlessScrollListener);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        // Why does this selection remain if I long press, release
        // without activating onListItemClick?
        //mListView.setSelector(R.drawable.stream_list_selector_overlay);
        //mListView.setDrawSelectorOnTop(true);

        mSwipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.refreshLayout);
        mSwipeLayout.setOnRefreshListener(this);
        mSwipeLayout.setColorScheme(R.color.kickflip_green,
                R.color.kickflip_green_shade_2,
                R.color.kickflip_green_shade_3,
                R.color.kickflip_green_shade_4);

        // Set OnItemClickListener so we can be notified on item clicks
        mListView.setOnItemClickListener(this);

        setupListViewAdapter();
        return view;
    }



    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallBack = (MainFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement MainFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    private void handleFBStateButton() {
        if (AccessToken.getCurrentAccessToken() == null) {
            return;
        }
        boolean newPostingState = Util.readFacebookPostingState(getActivity());
        if (!AccessToken.getCurrentAccessToken().getPermissions().contains(Constants.FB_PUBLISH_ACTIONS_PERMISSION)
                || (AccessToken.getCurrentAccessToken().getDeclinedPermissions().contains(Constants.FB_PUBLISH_ACTIONS_PERMISSION))) {
            newPostingState = false;
        }
        if (newPostingState) { // on
            fragmentsStreamFacebookPostingStateTextView.setText(getResources().getString(R.string.postOn));
            fragmentsStreamFacebookPostingStateTextView.setAlpha(1.0f);
            fragmentStreamFacebookLogoImageView.setAlpha(1f);
        } else {
            fragmentsStreamFacebookPostingStateTextView.setText(getResources().getString(R.string.postOff));
            fragmentsStreamFacebookPostingStateTextView.setAlpha(0.6f);
            fragmentStreamFacebookLogoImageView.setAlpha(0.6f);
        }
        Util.saveFbPostingState(getActivity(), newPostingState);

    }

    private void initFBStateButtons(View rootView) {
        fragmentsStreamFacebookPostingStateTextView = (TextView) rootView.findViewById(R.id.fragmentsStreamFacebookPostingStateTextView);
        fragmentStreamFacebookLogoImageView = (ImageView) rootView.findViewById(R.id.fragmentStreamFacebookLogoImageView);
        fragmentStreamFacebookLogoImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFacebookPostingState = Util.readFacebookPostingState(getActivity());
                newFacebookPostingState = !newFacebookPostingState;
                Util.saveFbPostingState(getActivity(), newFacebookPostingState);
                if (newFacebookPostingState) {
                    gainFBPublishRights();
                }
                handleFBStateButton();
            }
        });
        fragmentsStreamFacebookPostingStateTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean newFacebookPostingState = Util.readFacebookPostingState(getActivity());
                newFacebookPostingState = !newFacebookPostingState;
                Util.saveFbPostingState(getActivity(), newFacebookPostingState);
                if (newFacebookPostingState) {
                    gainFBPublishRights();
                }
                handleFBStateButton();
            }
        });
        handleFBStateButton();

    }

    private void gainFBPublishRights() {
        if (!AccessToken.getCurrentAccessToken().getPermissions().contains(Constants.FB_PUBLISH_ACTIONS_PERMISSION)
                || (AccessToken.getCurrentAccessToken().getDeclinedPermissions().contains(Constants.FB_PUBLISH_ACTIONS_PERMISSION))) {
            Set<String> permissions = AccessToken.getCurrentAccessToken().getPermissions();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    LoginManager.getInstance().logInWithPublishPermissions(getActivity(), Arrays.asList(Constants.FB_PUBLISH_ACTIONS_PERMISSION));
                }
            });
        }
    }

    private void initSettingsButton(View logoButton) {
        logoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallBack.toggleSettingsView(true, true);
            }
        });
    };

    private void initializeRecordButton(View recordButton) {
        final Activity finalActivity = getActivity();
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });
/*        recordButtonForKeyboardOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startRecording();
            }
        });
*/
    }

    private void startRecording() {
        String title = broadcastingTitleEditText.getText().toString();
        Bundle argumentsBundle = new Bundle();
        boolean fbPostingState = Util.readFacebookPostingState(getActivity());
        argumentsBundle.putString(BroadcastActivity.ARGS_TITLE, title);
        argumentsBundle.putBoolean(BroadcastActivity.ARGS_POST_TO_FB_WALL, fbPostingState);
        broadcastingTitleEditText.setText("");
        mCallBack.onFragmentEvent(MainFragmentInteractionListener.EVENT.START_BROADCAST, argumentsBundle);


/*                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        doIt();
                    }
                }).start();
*/
    }

    private void doIt() {

//        View v = ((LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.overlay_thumbnail, null, false);
        /*    Bitmap overlayBitmap = Bitmap.createBitmap(getResources().getDimensionPixelOffset(R.dimen.thumbnailWidth), getResources().getDimensionPixelOffset(R.dimen.thumbnailHeight),Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(overlayBitmap);
        Drawable bgDrawable = overlayView.getBackground();
        if (bgDrawable!=null)
            bgDrawable.draw(canvas);
        else
            canvas.drawColor(Color.WHITE);
        overlayView.draw(canvas);

*/
        LayoutInflater  mInflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        //Inflate the layout into a view and configure it the way you like
        RelativeLayout view = new RelativeLayout(getActivity());
        mInflater.inflate(R.layout.overlay_thumbnail, view, true);
//        TextView tv = (TextView) findViewById(R.id.my_text);
//        tv.setText("Beat It!!");

        //Provide it with a layout params. It should necessarily be wrapping the
        //content as we not really going to have a parent for it.
        view.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT));

        //Pre-measure the view so that height and width don't remain null.
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        //Assign a size and position to the view and all of its descendants
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());

        //Create the bitmap
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(),
                view.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888);
        //Create a canvas with the specified bitmap to draw into
        Canvas c = new Canvas(bitmap);

        //Render this view (and all of its children) to the given Canvas
        view.draw(c);


        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut = null;
        File file = new File(path, "testing.jpg"); // the File to save to
        try {
            fOut = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut); // saving the Bitmap to a file compressed as a JPEG with 85% compression rate
        try {
            fOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fOut.close(); // do not forget to close the stream
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }



    /**
     * The Adapter which will be used to populate the ListView/GridView with
     * Views.
     */
    private StreamAdapter mAdapter;

/*    private StreamAdapter.StreamAdapterActionListener mStreamActionListener = new StreamAdapter.StreamAdapterActionListener() {
        @Override
        public void onFlagButtonClick(final Stream stream) {
            KickflipCallback cb = new KickflipCallback() {
                @Override
                public void onSuccess(Response response) {
                   if (getActivity() != null) {
                        if (mKickflip.activeUserOwnsStream(stream)) {
                            mAdapter.remove(stream);
                            mAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(getActivity(), getActivity().getString(R.string.stream_flagged), Toast.LENGTH_LONG).show();
                        }
                    }

                }

                @Override
                public void onError(KickflipException error) {}
            };

            if (mKickflip.activeUserOwnsStream(stream)) {
                stream.setDeleted(true);
                mKickflip.setStreamInfo(stream, cb);
            } else {
                mKickflip.flagStream(stream, cb);
            }
        }

        @Override
        public void onShareButtonClick(Stream stream) {
            Intent shareIntent = Share.createShareChooserIntentWithTitleAndUrl(getActivity(), getString(R.string.share_broadcast), stream.getKickflipUrl());
            startActivity(shareIntent);
        }
    };
*/
    private EndlessScrollListener mEndlessScrollListener = new EndlessScrollListener() {
        @Override
        public void onLoadMore(int page, int totalItemsCount) {
            Log.i(TAG, "Loading more streams");
            getStreams(false);
        }
    };


    private void updateData() {
        mKickflip = new KickflipApiClient(getActivity(), MainActivity.getLoggedUser(), new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (mAdapter != null) {
                    mAdapter.setUserName("Username???");
                }
                getStreams(true);
                // Update profile display when we add that
            }

            @Override
            public void onError(KickflipException error) {
                showNetworkError();
                if ((error != null) && (error.getMessage() != null)) {
                    if (error.isMissingPrivilegesError()) {
                        mCallBack.onFragmentEvent(MainFragmentInteractionListener.EVENT.MISSING_PRIVILEGES, null);
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
            Object streams = LocalPersistence.readObjectFromFile(getActivity(), SERIALIZED_FILESTORE_NAME);
            if (streams != null) {
                displayStreams((List<User>) streams, false);
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
            while (mStreams.size() > 7) {
                mStreams.remove(mStreams.size()-1);
            }
            LocalPersistence.writeObjectToFile(getActivity(), mStreams, SERIALIZED_FILESTORE_NAME);
        }
    }


    /**
     * Fetch Streams and display in ListView
     *
     * @param refresh whether this fetch is for a subsequent page
     *                      or to refresh the first page
     */
    private void getStreams(final boolean refresh) {
        if (mKickflip.getActiveUser() == null || mRefreshing) return;
        mRefreshing = true;
        if (refresh) mCurrentPage = 1;
        mKickflip.getStreamList(new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (VERBOSE) Log.i("API", "request succeeded " + response);
                if (getActivity() != null) {
                    displayStreams(((UsersList) response).getStreams(), !refresh);
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
                mCurrentPage++;
            }

            @Override
            public void onError(KickflipException error) {
                if (VERBOSE) Log.i("API", "request failed " + error.getMessage());
                if (error.isMissingPrivilegesError()) {
                    mCallBack.onFragmentEvent(MainFragmentInteractionListener.EVENT.MISSING_PRIVILEGES, null);
                } else if (error.getMessage() != null) {
                    Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
                }
                if (getActivity() != null) {
                    showNetworkError();
                }
                mSwipeLayout.setRefreshing(false);
                mRefreshing = false;
            }
        });
    }

    private void setupListViewAdapter() {
        if (mAdapter == null) {
            mStreams = new ArrayList<>(0);
            mAdapter = new StreamAdapter(getActivity(), mStreams, this);
            mAdapter.setNotifyOnChange(false);
            mListView.setAdapter(mAdapter);
//            if (mKickflip.getActiveUser() != null) {
//                mAdapter.setUserName(mKickflip.getActiveUser().getUserName());
//            }
        }
    }

    /**
     * Display the given List of {@link io.kickflip.sdk.api.json.Stream} Objects
     *
     * @param streams a List of {@link io.kickflip.sdk.api.json.Stream} Objects
     * @param append whether to append the given streams to the current list
     *               or use the given streams as the absolute dataset.
     */
    private void displayStreams(List<User> streams, boolean append) {
        if (append) {
            mStreams.addAll(streams);
        } else {
            mStreams = streams;
        }
//        Collections.sort(mStreams);
        mAdapter.refresh(mListView, mStreams);
        if (mStreams.size() == 0) {
            showNoBroadcasts();
        }
    }

    /**
     * Inform the user that a network error has occured
     */
    public void showNetworkError() {
        setEmptyListViewText(getString(R.string.no_network));
    }

    /**
     * Inform the user that no broadcasts were found
     */
    public void showNoBroadcasts() {
//        setEmptyListViewText(getString(R.string.no_broadcasts));
    }

    /**
     * If the ListView is hidden, show the
     *
     * @param text
     */
    private void setEmptyListViewText(String text) {
        View emptyView = mListView.getEmptyView();

        if (emptyView instanceof TextView) {
            ((TextView) emptyView).setText(text);
        }
    }

    @Override
    public void onRefresh() {
        if (!mRefreshing) {
            getStreams(true);
        }

    }

    @Override
    public void onStreamPlaybackRequested(User user) {
        mCallBack.onStreamPlaybackRequested(user);
    }

}
