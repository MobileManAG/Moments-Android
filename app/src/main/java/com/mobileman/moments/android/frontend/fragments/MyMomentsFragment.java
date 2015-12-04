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
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.backend.model.HlsStreamList;
import com.mobileman.moments.android.frontend.LocalPersistence;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;
import com.mobileman.moments.android.frontend.activity.MainActivity;
import com.mobileman.moments.android.frontend.adapters.MyMomentsAdapter;

import java.util.ArrayList;
import java.util.List;

import io.kickflip.sdk.api.KickflipApiClient;
import io.kickflip.sdk.api.KickflipCallback;
import io.kickflip.sdk.api.json.HlsStream;
import io.kickflip.sdk.api.json.Response;
import io.kickflip.sdk.exception.KickflipException;

/**
 * Created by MobileMan on 29/05/15.
 */
public class MyMomentsFragment extends Fragment implements MyMomentsAdapter.MyMomentsListListener{

    private static final String SERIALIZED_FILESTORE_NAME = "my_moments";
    private ListView listView;
    private MainFragmentInteractionListener mCallback;
    private MyMomentsAdapter mAdapter;
    private List<HlsStream> myStreamsList;
    private KickflipApiClient mKickflip;
    private AlertDialog.Builder deleteConfirmationBuilder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_moments, container, false);

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
        listView = (ListView) rootView.findViewById(R.id.myMomentsListView);
        setupListViewAdapter();
    }

    private void initButtons(View rootView) {
        final ImageView myMomentsHeaderBackImageView = (ImageView) rootView.findViewById(R.id.myMomentsHeaderBackImageView);

        myMomentsHeaderBackImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mCallback != null) {
                    mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.BACK_TO_SETTINGS, null);
                }
            }
        });
        myMomentsHeaderBackImageView.setOnTouchListener(new View.OnTouchListener() {
            private Rect rect;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    myMomentsHeaderBackImageView.setColorFilter(Color.argb(130, 0, 0, 0));
                    rect = new Rect(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    myMomentsHeaderBackImageView.setColorFilter(Color.argb(0, 0, 0, 0));
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!rect.contains(v.getLeft() + (int) event.getX(), v.getTop() + (int) event.getY())) {
                        myMomentsHeaderBackImageView.setColorFilter(Color.argb(130, 0, 0, 0));
                    }
                }
                return false;
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
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
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
                displayStreams((List<HlsStream>) friends);
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
            LocalPersistence.writeObjectToFile(getActivity(), myStreamsList, SERIALIZED_FILESTORE_NAME);
        }
    }

    private void displayStreams(List<HlsStream> streams) {
        myStreamsList.clear();
        myStreamsList.addAll(streams);
//        Collections.sort(mStreams);
        mAdapter.notifyDataSetChanged();
    }

    private void getStreams(final boolean refresh) {
        if (mKickflip.getActiveUser() == null) return;

        mKickflip.getMyMomentsList(new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                if (getActivity() != null) {
                    displayStreams(((HlsStreamList) response).getStreams());
                }
            }

            @Override
            public void onError(KickflipException error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupListViewAdapter() {
        if (mAdapter == null) {
            myStreamsList = new ArrayList<>(0);
            mAdapter = new MyMomentsAdapter(getActivity(), myStreamsList, this);
            listView.setAdapter(mAdapter);
        }
    }

    @Override
    public void onStreamPlaybackRequested(final HlsStream stream) {
        mCallback.playHistoricalStream(stream);
    }

    @Override
    public void onStreamDeletionRequested(final HlsStream stream) {
        if (deleteConfirmationBuilder == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), AlertDialog.THEME_HOLO_LIGHT);
            builder.setTitle("");
            builder.setMessage(R.string.deleteStreamConfirmation);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    deleteStream(stream);
                    dialogInterface.dismiss();
                    deleteConfirmationBuilder = null;
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                    deleteConfirmationBuilder = null;
                }
            });
            deleteConfirmationBuilder = builder;
            deleteConfirmationBuilder.show();
        }
    }

    private void deleteStream(final HlsStream stream) {
        mKickflip.deleteStream(stream.getStreamId(), new KickflipCallback() {
            @Override
            public void onSuccess(Response response) {
                mAdapter.remove(stream);
            }

            @Override
            public void onError(KickflipException error) {
                Toast.makeText(getActivity(), error.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

}
