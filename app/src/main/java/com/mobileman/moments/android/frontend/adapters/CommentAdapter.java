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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.mobileman.moments.android.Constants;
import com.mobileman.moments.android.R;
import com.mobileman.moments.android.Util;
import com.mobileman.moments.android.backend.model.Comment;
import com.mobileman.moments.android.frontend.RoundedTransformation;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

/**
 * Created by MobileMan on 05/05/15.
 */
public class CommentAdapter extends ArrayAdapter<Comment> {

    public static final int LAYOUT_ID = R.layout.comment_list_item;
//    public static final int LAYOUT_ID = R.layout.opaque_text_view;

    private static final int MOVE_DURATION = 1150;

    private HashMap<Long, Integer> mItemIdTopMap = new HashMap<Long, Integer>();
    private HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();
    private ListView listView;
    private boolean animating;
    private Context mContext;

    public CommentAdapter(final Context context, List<Comment> objects, ListView listView) {
        super(context, LAYOUT_ID, objects);
        mContext = context;
        this.listView = listView;
/*        for (int i = 0; i < objects.size(); ++i) {
            mIdMap.put(objects.get(i).getId(), i);
        }
   */
    }

  /*
    @Override
    public void add(Comment object) {
        super.add(object);
        int position = getPosition(object);
        mIdMap.put(object.getId(), position);

    }


    public void myRemove(final Comment object) {
        int position = getPosition(object);
        View view = listView.getChildAt(position);
        animateRemoval(listView, view, position);
            int rowHeight = getContext().getResources().getDimensionPixelSize(R.dimen.commentRowHeight);
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0, rowHeight);
            animator.setDuration(400);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animating = false;
                    remove(object);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    animating = false;
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }

            });

            animator.start();

    }
/*
    @Override
    public long getItemId(int position) {
        Comment comment = getItem(position);
        int result = mIdMap.get(comment.getId());
        if (result >= getCount()) {
            Log.d(Util.TAG, "ERORR");
        }
        return result;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
    */


//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
    public static View createCommentView(Context mContext, Comment comment) {
//        final Comment comment = getItem(position);
//        View view = super.getView(position, convertView, parent);
        View convertView = null;
        ViewGroup parent = null;
        if (convertView == null) {
            convertView = ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(LAYOUT_ID, parent, false);
        }

        ImageView mediaPlayerHeaderImageView = (ImageView) convertView.findViewById(R.id.mediaPlayerHeaderImageView);
        TextView commentListUsernameTextView = (TextView) convertView.findViewById(R.id.commentListUsernameTextView);
        if (comment.getAuthor() != null) {
            commentListUsernameTextView.setText(getCommentListUsername(mContext, comment));

            if (comment.getAuthor().getFacebookID() != null) {
                int imageFrameSize = mContext.getResources().getDimensionPixelSize(R.dimen.smallImageFrameSize);
                Picasso.with(mContext)
                        .load(Util.getFacebookPictureUrl(comment.getAuthor().getFacebookID()))
                        .transform(new RoundedTransformation(imageFrameSize / 2, 4))
                        .resize(imageFrameSize, imageFrameSize)
                        .placeholder(R.drawable.user)
                        .error(R.drawable.user)
                        .centerCrop().into(mediaPlayerHeaderImageView);
            } else {
                mediaPlayerHeaderImageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.user));
            }
        } else {
            commentListUsernameTextView.setText("");
        }
        TextView commentListCommentTextView = (TextView) convertView.findViewById(R.id.commentListCommentTextView);
        if (comment.isSystemComment()) {
            mediaPlayerHeaderImageView.setVisibility(View.GONE);
            commentListUsernameTextView.setVisibility(View.GONE);
            View streamHeaderInformationLayout = convertView.findViewById(R.id.streamHeaderInformationLayout);
            streamHeaderInformationLayout.setBackground(null);
            commentListCommentTextView.setBackgroundColor(mContext.getResources().getColor(R.color.buttonRedTransparent84));
            commentListCommentTextView.setPadding(mContext.getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin), 0, 0, 0);
        } else {
            mediaPlayerHeaderImageView.setVisibility(View.VISIBLE);
            commentListUsernameTextView.setVisibility(View.VISIBLE);
            commentListCommentTextView.setBackgroundColor(mContext.getResources().getColor(android.R.color.transparent));
        }
        commentListCommentTextView.setText(getCommentText(mContext, comment));

        if (((System.currentTimeMillis() - comment.getVisibleSinceDate()) > Constants.BROADCAST_COMMENTS_VISIBILITY_DURATION)) {
//            removeRow(convertView, position);
//            animateRemoval(listView, convertView, position);
/*            int rowHeight = getContext().getResources().getDimensionPixelSize(R.dimen.commentRowHeight);
            ObjectAnimator animator = ObjectAnimator.ofFloat(convertView, "translationY", 0, rowHeight);
            animator.setDuration(400);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animator) {

                }

                @Override
                public void onAnimationEnd(Animator animator) {
                    animating = false;
                    remove(comment);
                }

                @Override
                public void onAnimationCancel(Animator animator) {
                    animating = false;
                }

                @Override
                public void onAnimationRepeat(Animator animator) {

                }

            });

            animator.start();

*/

        }

        return convertView;
    }

    private static String getCommentListUsername(Context mContext, Comment comment) {
        String result = "";
        if ((comment.getAuthor() != null) && (comment.getAuthor().getName() != null)) {
            result = comment.getAuthor().getName();
        }
        if (comment.getCommentType() == Constants.kStreamEventTypeJoin) {
            result = result + " " + mContext.getResources().getString(R.string.watcherJoined);
        } else if (comment.getCommentType() == Constants.kStreamEventTypeLeave) {
            result = result + " " + mContext.getResources().getString(R.string.watcherLeft);
        }
        return result;
    }

    private static String getCommentText(Context mContext, Comment comment) {
        String result = comment.getText();
        if (comment.getCommentType() == Constants.kStreamEventTypeJoin) {
            result = "";
        } else if (comment.getCommentType() == Constants.kStreamEventTypeLeave) {
            result = "";
        }
        return result;
    }

    public void removeRow(final View row, final int position) {
        final int initialHeight = row.getHeight();
        Animation animation = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime,
                                               Transformation t) {
                super.applyTransformation(interpolatedTime, t);
                int newHeight = (int) (initialHeight * (1 - interpolatedTime));
                if (newHeight > 0) {
                    row.getLayoutParams().height = newHeight;
                    row.requestLayout();
                }
            }
        };
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationRepeat(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                row.getLayoutParams().height = initialHeight;
                row.requestLayout();
//                remove(position);
//                ((BaseAdapter) list.getAdapter()).notifyDataSetChanged();
            }
        });
        animation.setDuration(300);
        row.startAnimation(animation);
    }

    /**
     * This method animates all other views in the ListView container (not including ignoreView)
     * into their final positions. It is called after ignoreView has been removed from the
     * adapter, but before layout has been run. The approach here is to figure out where
     * everything is now, then allow layout to run, then figure out where everything is after
     * layout, and then to run animations between all of those start/end positions.
     */
    private void animateRemoval(final ListView listview, View viewToRemove, int position) {
        int firstVisiblePosition = listview.getFirstVisiblePosition();
        for (int i = 0; i < listview.getChildCount(); ++i) {
            View child = listview.getChildAt(i);
            if (child != viewToRemove) {
                int visiblePosition = firstVisiblePosition + i;
                long itemId = getItemId(visiblePosition);
                mItemIdTopMap.put(itemId, child.getTop());
            }
        }
        // Delete the item from the adapter
        if (listview == null) {
            Log.d(Util.TAG, "Empty list!");
        }
        if (viewToRemove == null) {
            Log.d(Util.TAG, "Empty view!");
        }
//        int position = listview.getPositionForView(viewToRemove);
        remove(getItem(position));
        final ViewTreeObserver observer = listview.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                observer.removeOnPreDrawListener(this);
                boolean firstAnimation = true;
                int firstVisiblePosition = listview.getFirstVisiblePosition();
                for (int i = 0; i < listview.getChildCount(); ++i) {
                    final View child = listview.getChildAt(i);
                    int position = firstVisiblePosition + i;
                    long itemId = getItemId(position);
                    Integer startTop = mItemIdTopMap.get(itemId);
                    int top = child.getTop();
                    if (startTop != null) {
                        if (startTop != top) {
                            int delta = startTop - top;
                            child.setTranslationY(delta);
                            child.animate().setDuration(MOVE_DURATION).translationY(0);
                            if (firstAnimation) {
                                child.animate().withEndAction(new Runnable() {
                                    public void run() {
//                                        mSwiping = false;
                                        listview.setEnabled(true);
                                    }
                                });
                                firstAnimation = false;
                            }
                        }
                    } else {
                        // Animate new views along with the others. The catch is that they did not
                        // exist in the start state, so we must calculate their starting position
                        // based on neighboring views.
                        int childHeight = child.getHeight() + listview.getDividerHeight();
                        startTop = top + (i > 0 ? childHeight : -childHeight);
                        int delta = startTop - top;
                        child.setTranslationY(delta);
                        child.animate().setDuration(MOVE_DURATION).translationY(0);
                        if (firstAnimation) {
                            child.animate().withEndAction(new Runnable() {
                                public void run() {
//                                    mSwiping = false;
                                    listview.setEnabled(true);
                                }
                            });
                            firstAnimation = false;
                        }
                    }
                }
                mItemIdTopMap.clear();
                return true;
            }
        });
    }
}
