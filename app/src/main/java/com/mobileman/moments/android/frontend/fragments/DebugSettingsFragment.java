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
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.mobileman.moments.android.R;
import com.mobileman.moments.android.frontend.MainFragmentInteractionListener;

/**
 * Created by MobileMan on 27/05/15.
 */
public class DebugSettingsFragment extends Fragment {

    private final static int MIN_RECORDING_WIDTH = 480;
    private final static int MIN_RECORDING_HEIGHT = 720;
    private final static int INITIAL_BITRATE_STEP = 50;

    public static boolean DebugRecordingSettingsActive;
    public static int DebugRecordingSettingRecordingWidth = MIN_RECORDING_WIDTH;
    public static int DebugRecordingSettingRecordingHeight = MIN_RECORDING_HEIGHT;
    public static int DebugRecordingSettingSegmentDuration = 3;
    public static int DebugRecordingSettingInitialBitrate = 2000;


    private MainFragmentInteractionListener mCallback;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_debug, container, false);

        initButtons(view);

        return view;
    }


    private void initButtons(View view) {
        final ToggleButton settingsDebugRecordingToggleButton = (ToggleButton) view.findViewById(R.id.settingsDebugRecordingToggleButton);
        final View settingsDebugRecordingChildLayout = view.findViewById(R.id.settingsDebugRecordingChildLayout);
        settingsDebugRecordingToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean active) {
                DebugRecordingSettingsActive = active;
                if (active) {
                    if (settingsDebugRecordingChildLayout.getVisibility() != View.VISIBLE) {
                        settingsDebugRecordingChildLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (settingsDebugRecordingChildLayout.getVisibility() == View.VISIBLE) {
                        settingsDebugRecordingChildLayout.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });
        if (DebugRecordingSettingsActive) {
            settingsDebugRecordingToggleButton.setChecked(true);
        }
        initBackButton(view);
        initWidthButton(view);
        initHeightButton(view);
        initDurationButton(view);
        initInitialBitrateButton(view);
    }

    private void initBackButton(View view) {
        View settingsDebugRecordingBackImageView = view.findViewById(R.id.settingsDebugRecordingBackImageView);
        settingsDebugRecordingBackImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mCallback.onFragmentEvent(MainFragmentInteractionListener.EVENT.BACK_TO_SETTINGS, null);
            }
        });
    }

    private void initInitialBitrateButton(View view) {
        SeekBar settingsDebugRecordingSegmentInitialBitrateSeekbar = (SeekBar) view.findViewById(R.id.settingsDebugRecordingSegmentInitialBitrateSeekbar);
        final TextView settingsDebugRecordingSegmentInitialBitrateTextView = (TextView) view.findViewById(R.id.settingsDebugRecordingSegmentInitialBitrateTextView);
        settingsDebugRecordingSegmentInitialBitrateSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i == 0) {
                    i = 1;
                }
                int bitrate = INITIAL_BITRATE_STEP * i;
                DebugRecordingSettingInitialBitrate = bitrate;
                settingsDebugRecordingSegmentInitialBitrateTextView.setText(Integer.toString(bitrate) + " kbps");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if (DebugRecordingSettingInitialBitrate != 0) {
            int progress = (DebugRecordingSettingInitialBitrate / INITIAL_BITRATE_STEP);
            settingsDebugRecordingSegmentInitialBitrateSeekbar.setProgress(progress);
        }
    }

    private void initDurationButton(View view) {
        SeekBar settingsDebugRecordingSegmentDurationSeekbar = (SeekBar) view.findViewById(R.id.settingsDebugRecordingSegmentDurationSeekbar);
        final TextView settingsDebugRecordingSegmentDurationTextView = (TextView) view.findViewById(R.id.settingsDebugRecordingSegmentDurationTextView);
        settingsDebugRecordingSegmentDurationSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (i < 2) {
                    i = 2;
                }
                DebugRecordingSettingSegmentDuration = i;
                settingsDebugRecordingSegmentDurationTextView.setText(Integer.toString(i));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if (DebugRecordingSettingSegmentDuration != 0) {
            settingsDebugRecordingSegmentDurationSeekbar.setProgress(DebugRecordingSettingSegmentDuration);
        }
    }

    private void initHeightButton(View view) {
        SeekBar settingsDebugRecordingHeightSeekbar = (SeekBar) view.findViewById(R.id.settingsDebugRecordingHeightSeekbar);
        final TextView settingsDebugRecordingHeightTextView = (TextView) view.findViewById(R.id.settingsDebugRecordingHeightTextView);
        settingsDebugRecordingHeightSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int newHeight = MIN_RECORDING_HEIGHT + 10 * i;
                DebugRecordingSettingRecordingHeight = newHeight;
                settingsDebugRecordingHeightTextView.setText(Integer.toString(newHeight));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if (DebugRecordingSettingRecordingHeight != 0) {
            int progress = (DebugRecordingSettingRecordingHeight - MIN_RECORDING_HEIGHT) / 10;
            settingsDebugRecordingHeightSeekbar.setProgress(progress);
        }
    }

    private void initWidthButton(View view) {
        final SeekBar settingsDebugRecordingWidthSeekbar = (SeekBar) view.findViewById(R.id.settingsDebugRecordingWidthSeekbar);
        final TextView settingsDebugRecordingWidthTextView = (TextView) view.findViewById(R.id.settingsDebugRecordingWidthTextView);

        settingsDebugRecordingWidthSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                int newWidth = MIN_RECORDING_WIDTH + 10 * i;
                DebugRecordingSettingRecordingWidth = newWidth;
                settingsDebugRecordingWidthTextView.setText(Integer.toString(newWidth));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if (DebugRecordingSettingRecordingWidth != 0) {
            int progress = (DebugRecordingSettingRecordingWidth - MIN_RECORDING_WIDTH) / 10;
            settingsDebugRecordingWidthSeekbar.setProgress(progress);
        }
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
}
