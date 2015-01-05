/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.basicmultitouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.LinkedList;

/**
 * View that shows touch events and their history. This view demonstrates the
 * use of {@link #onTouchEvent(android.view.MotionEvent)} and {@link android.view.MotionEvent}s to keep
 * track of touch pointers across events.
 */
public class TouchDisplayView extends View {

    // Hold data for active touch pointer IDs
    private LinkedList<TouchPoint> mTouches;

    // Is there an active touch?
    private boolean mHasTouch = false;      //TODO if it is what I think it is then it should not be here
    private boolean mFirstTouch = false;    //initial state, before any interaction

    // Only one touch per side of board
    private boolean mUpTouch = false;
    private boolean mDownTouch = false;

    // the points of where the mallets are right now
    public TouchPoint malletUp;
    public TouchPoint malletDown;

    // puck global init
    public Puck puck;

    //for height/size of the actual display
    DisplayMetrics display = this.getContext().getResources().getDisplayMetrics();

    //scale for deciding how big the objects are going to be displayed
    public float scale;
    Integer malletRadius;   //radius of a mallet


    //bitmaps
    Bitmap mBitmapG = BitmapFactory.decodeResource(getResources(), R.drawable.mallet_green);
    Bitmap mBitmapP = BitmapFactory.decodeResource(getResources(), R.drawable.mallet_pink);


    /**
     * Holds data related to a touch pointer, including its current position,
     * pressure and historical positions. Objects are allocated through an
     * object pool using {@link #obtain()} and {@link #recycle()} to reuse
     * existing objects.
     */
    final class TouchPoint {

        public float x;
        public float y;
        public float radius;

        public int id;

        //three attributes for knowing where this point is, in the upper area or in the down area
        public boolean up = false;
        public boolean down = false;
        public boolean border = false;
        public boolean init = false;

        public TouchPoint(float x, float y, int id, float radius) {
            this.radius = radius;
            this.x = x - this.radius;
            this.y = y - this.radius;
            this.id = id;
            Log.i("New TouchPoint created at x = ",Float.toString(x-this.radius));


        }

        // arrray of pointer position history
        /*public PointF[] history = new PointF[HISTORY_COUNT];

        private static final int MAX_POOL_SIZE = 10;
        private static final SimplePool<TouchHistory> sPool =
                new SimplePool<TouchHistory>(MAX_POOL_SIZE);

        public static TouchHistory obtain(float x, float y) {
            TouchHistory data = sPool.acquire();
            if (data == null) {
                data = new TouchHistory();
            }

            data.setTouch(x, y);

            return data;
        }

        public TouchHistory() {

            // initialise history array
            for (int i = 0; i < HISTORY_COUNT; i++) {
                history[i] = new PointF();
            }
        }

        public void setTouch(float x, float y) {
            this.x = x;
            this.y = y;
        }


        *//**
         * Add a point to its history. Overwrites oldest point if the maximum
         * number of historical points is already stored.
         *
         * @param point
         *//*
        public void addHistory(float x, float y) {
            PointF p = history[historyIndex];
            p.x = x;
            p.y = y;

            historyIndex = (historyIndex + 1) % history.length;

            if (historyCount < HISTORY_COUNT) {
                historyCount++;
            }
        }*/

    }


    /*
    *   Suprisingly, a Puck class
    */

    final class Puck {
        //puck position
        public float x;
        public float y;
        //puck size
        public float radius;
        //puck dynamics
        public float verticalMov;
        public float horizontalMov;

        //petty constructor
        Puck(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    //TODO what is this function for?
    public TouchDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // SparseArray for touch events, indexed by touch id
        mTouches = new LinkedList<TouchPoint>();
        DisplayMetrics display = this.getContext().getResources().getDisplayMetrics();

        initialisePaint();
    }

    // BEGIN_INCLUDE(onTouchEvent)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getAction();

        /*
         * Switch on the action. The action is extracted from the event by
         * applying the MotionEvent.ACTION_MASK. Alternatively a call to
         * event.getActionMasked() would yield in the action as well.
         */
        switch (action & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_DOWN: {
                // first pressed gesture has started
                /*
                 * Only one touch event is stored in the MotionEvent. Extract
                 * the pointer identifier of this touch from the first index
                 * within the MotionEvent object.
                 */
                //TODO deleting touchpoints is a very bad idea, as they need to stay physical throughout the game
                int id = event.getPointerId(0);
                TouchPoint data = new TouchPoint(event.getX(0), event.getY(0), id,malletRadius);    //TODO change malletRadius or even better change this whole mechanism
                /*
                 * Store the data under its pointer identifier. The pointer
                 * number stays consistent for the duration of a gesture,
                 * accounting for other pointers going up or down.
                 */

                mTouches.add(id, data);

                mHasTouch = true;
                mFirstTouch = true;

                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN: {
                /*
                 * A non-primary pointer has gone down, after an event for the
                 * primary pointer (ACTION_DOWN) has already been received.
                 */

                /*
                 * The MotionEvent object contains multiple pointers. Need to
                 * extract the index at which the data for this particular event
                 * is stored.
                */
                int index = event.getActionIndex();
                int id = event.getPointerId(index);

                TouchPoint data = new TouchPoint(event.getX(index), event.getY(index), id,malletRadius);    //TODO change malletRadius or even better change this whole mechanism

                /*
                 * Store the data under its pointer identifier. The index of
                 * this pointer can change over multiple events, but this
                 * pointer is always identified by the same identifier for this
                 * active gesture.
                        */
                mTouches.add(id, data);


                break;
            }

            case MotionEvent.ACTION_UP: {
                /*
                 * Final pointer has gone up and has ended the last pressed
                 * gesture.
                 */

                /*
                 * Extract the pointer identifier for the only event stored in
                 * the MotionEvent object and remove it from the list of active
                 * touches.
                 */
                mTouches.clear();
                mHasTouch = false;
                malletDown.init = true;
                malletUp.init = true;

                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                /*
                 * A non-primary pointer has gone up and other pointers are
                 * still active.
                 */
                /*
                 * The MotionEvent object contains multiple pointers. Need to
                 * extract the index at which the data for this particular event
                 * is stored.
                 */
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                /*
                 * A change event happened during a pressed gesture. (Between
                 * ACTION_DOWN and ACTION_UP or ACTION_POINTER_DOWN and
                 * ACTION_POINTER_UP)
                 */
                /*
                 * Loop through all active pointers contained within this event.
                 * Data for each pointer is stored in a MotionEvent at an index
                 * (starting from 0 up to the number of active pointers). This
                 * loop goes through each of these active pointers, extracts its
                 * data (position and pressure) and updates its stored data. A
                 * pointer is identified by its pointer number which stays
                 * constant across touch events as long as it remains active.
                 * This identifier is used to keep track of a pointer across
                 * events.
                 */
                for (int index = 0; index < event.getPointerCount(); index++) {
                    // get pointer id for data stored at this index
                    int id = event.getPointerId(index);

                    TouchPoint data = new TouchPoint(event.getX(index), event.getY(index), id,malletRadius);    //TODO change malletRadius or even better change this whole mechanism
                    try {
                        mTouches.remove(id);
                    } catch (IndexOutOfBoundsException e) {
                        // do nothing
                    }
                    mTouches.add(id, data);

                }

                break;
            }
        }

        // trigger redraw on UI thread
        this.postInvalidate();

        return true;
    }

    // END_INCLUDE(onTouchEvent)

    @Override
    //I guess here is where we initialize things
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //setting scale
        scale = display.widthPixels / 320;

        // Canvas background color
        canvas.drawColor(BACKGROUND_ACTIVE);

        if(!mFirstTouch){
            // coordinates for drawing if there was no touch yet
            //initial mallets
            float x = display.widthPixels / 2;
            float y = display.heightPixels / 4;
            malletRadius = Math.round(mBitmapG.getHeight()*scale/2);
            mBitmapG = Bitmap.createScaledBitmap(mBitmapG, malletRadius *2, malletRadius *2,false);
            mBitmapP = Bitmap.createScaledBitmap(mBitmapP, malletRadius *2, malletRadius *2,false);


            //setting appropriate points
            TouchPoint data = new TouchPoint(x, 3*y, 3,malletRadius);
            data.down = true;
            malletDown = data;
            malletDown.init = true;

            //second point
            TouchPoint data2 = new TouchPoint(x, 1*y, 4, malletRadius);
            data2.up = true;
            malletUp = data2;
            malletUp.init = true;

            // initialize Puck
            Puck data3 = new Puck(x,2*y);
            data3.radius = scale*40;
            puck = data3;

        } else if (mHasTouch)  {
            //if somone is touching the display, the points could differ
            // setting boolean attributes of the Touchpoints in mTouches and setting malletUp and malletDown
            decideAttributes(canvas);
        }

        // draw the data to the canvas
        //mallets
        drawCircle(canvas, malletDown);
        drawCircle(canvas, malletUp);
        //puck
        if(puck != null) {
            mCirclePaint.setColor(COLORS[3]);
            canvas.drawCircle(puck.x, puck.y, puck.radius, mCirclePaint); //drawing puck
        }

        //reset booleans, otherwise they would never be drawn again
        mDownTouch = false;
        mUpTouch = false;

    }

    /*
     * Below are only helper methods and variables required for drawing.
     */

    // radius of active touch circle in dp
    private static final float CIRCLE_RADIUS_DP = 75f;

    private Paint mCirclePaint = new Paint();
    private Paint mTextPaint = new Paint();

    private static final int BACKGROUND_ACTIVE = Color.DKGRAY;

    // inactive border
    private static final float INACTIVE_BORDER_DP = 15f;
    private static final int INACTIVE_BORDER_COLOR = Color.WHITE;
    private Paint mBorderPaint = new Paint();
    private float mBorderWidth;

    public final int[] COLORS = {   //not used anymore
            0xFF33B5E5, 0xFFAA66CC, 0xFF99CC00, 0xFFFFBB33, 0xFFFF4444,
            0xFF0099CC, 0xFF9933CC, 0xFF669900, 0xFFFF8800, 0xFFCC0000
    };

    /**
     * Sets up the required {@link android.graphics.Paint} objects for the screen density of this
     * device.
     */
    private void initialisePaint() {

        // Calculate radiuses in px from dp based on screen density
        float density = getResources().getDisplayMetrics().density;

        // Setup text paint for circle label
        mTextPaint.setTextSize(27f);
        mTextPaint.setColor(Color.BLACK);

        // Setup paint for inactive border
        mBorderWidth = INACTIVE_BORDER_DP * density;
        mBorderPaint.setStrokeWidth(mBorderWidth);
        mBorderPaint.setColor(INACTIVE_BORDER_COLOR);
        mBorderPaint.setStyle(Paint.Style.STROKE);


    }

    /**
     * Draws the data encapsulated by a {@link com.example.android.basicmultitouch.TouchDisplayView.TouchPoint} object to a canvas.
     * A large circle indicates the current position held by the
     * {@link TouchDisplayView.TouchPoint} object.
     *
     * @param canvas
     * @param data
     */
    protected void drawCircle(Canvas canvas, TouchPoint data) {
        //set color of circle/ set picture depending on who they are = upper boeard or down half
        int color = COLORS[0];

        if(data == malletUp & !mUpTouch) {
            color = COLORS[1];
            mUpTouch = true;    //only one circle in the upper region of the board
            //canvas.drawBitmap(mBitmapP, data.x - data.radius, data.y - data.radius, mCirclePaint);
            canvas.drawBitmap(mBitmapP, data.x, data.y, mCirclePaint);

        } else if(data == malletDown & !mDownTouch) {
            mDownTouch = true;  //only one circle in the down region of the board
            //canvas.drawBitmap(mBitmapG, data.x - data.radius, data.y - data.radius, mCirclePaint);
            canvas.drawBitmap(mBitmapG, data.x, data.y, mCirclePaint);

        } else return;

        Log.i("mTouches.size", Integer.toString(mTouches.size()));
        mCirclePaint.setColor(color);

        /*
         * Draw the circle, size scaled to its pressure. Pressure is clamped to
         * 1.0 max to ensure proper drawing. (Reported pressure values can
         * exceed 1.0, depending on the calibration of the touch screen).
         */
        //float pressure = Math.min(data.pressure, 1f);


        //canvas.drawCircle(data.x, data.y, radius,  mCirclePaint);
        // Load basic bubble Bitmap


        // draw all historical points with a lower alpha value
        /*mCirclePaint.setAlpha(125);
        for (int j = 0; j < data.history.length && j < data.historyCount; j++) {
            PointF p = data.history[j];
            canvas.drawCircle(p.x, p.y, mCircleHistoricalRadius, mCirclePaint);
        }*/

        // draw its label next to the main circle
        /*canvas.drawText(data.label, data.x + radius, data.y
                - radius, mTextPaint);*/
    }

    /*
      * TODO change that name as I guess there will be all the physics there
      * TODO also divide content into smaller functions
      * function for setting the attributes up and down of the TouchPoints inside mTouches
      * helper function for onDraw, the first time Canvas is used
      * @param canvas
     */
    private void decideAttributes(Canvas canvas) {
        float border = display.heightPixels /2 - malletRadius;

        //display height and width
        int width, height;

        width = display.widthPixels - 2* malletRadius;
        height = display.heightPixels - 2* malletRadius;


        //TODO what does this for loop does? write in comment
        for (int i = 0; i < mTouches.size(); i++) { //TODO always getLast()?
            TouchPoint data = mTouches.get(i);
            //is the mallet inside the view?
            if (data.x < 0 | data.y < 0 ) {
                if(data.x < 0) {
                    data.x = 0;
                }
                if(data.y < 0) {
                    data.y = 0;
                };
            }
            if (data.x > width | data.y > height) {
                if(data.x > width) {
                    data.x = width;
                }
                if(data.y > height) {
                    data.y = height;
                }
            }

            //is one of the attributes concerning which area they are in already set?
            if (!data.down & !data.up & !data.border) {
                if (data.y < border - malletRadius) {  //upper area
                    data.up = true;
                } else if (data.y > border + malletRadius) {   //down area
                    data.down = true;
                } else {                    //point is in border area
                    data.border = true;
                }
                //removing and setting the point with the newly set attributes
                mTouches.remove(i);
                mTouches.add(i, data);
            }

            //reset mallets when no one touched the display
            if (data.up && malletUp.init) {
                if(malletDown.init) {
                    malletUp = data;
                    malletDown.id = 1;
                } else if(malletDown.id != data.id) {
                    malletUp = data;
                }

                return;
            } else if(data.down && malletDown.init) {
                if (malletUp.init) {
                    malletDown = data;
                    malletUp.id = 1;
                } else if(malletUp.id != data.id) {
                    malletDown = data;
                }
                return;
            }

           //are there new positions for the pointers
            if (data.id == malletUp.id) {
                if(data.border | data.down) {
                    malletUp.x = data.x;
                } else {
                    //mTouches.remove(i);
                    malletUp = data;
                }
            } else if (data.id == malletDown.id) {
                if(data.border | data.up) {
                    malletDown.x = data.x;
                } else {
                    //mTouches.remove(i);
                    malletDown = data;
                }
            }
        }

        puckPhys();

        /*if (mTouches.getLast().border) {
            return;
        } else if (mTouches.getLast().up) {
            malletUp = mTouches.getLast();
        } else {
            malletDown = mTouches.getLast();
        }*/
    }

    /*
     *  changing movement of puck
     */
    private void puckPhys() {
        //loop for checking a collision of mallet with puck
        for (int i = 0; i < mTouches.size(); i++) {
            TouchPoint data = mTouches.get(i);
            float  power = checkCollision(data.x, data.y, data.radius);
            Log.i("Power: ", Float.toString(power));
            if (power != -1) {  //there is a collision
                puck.horizontalMov = power/1000*(puck.x - data.x );   //determining puck speed changes  //TODO magic antigravity
                puck.verticalMov = power/1000*(puck.y - data.y);
                puck.x += puck.horizontalMov;
                puck.y += puck.verticalMov;
            }
            else {  //slow down cowboy
                if(puck.horizontalMov>0) puck.horizontalMov--;
                if(puck.horizontalMov<0) puck.horizontalMov++;
                if(puck.verticalMov>0) puck.verticalMov--;
                if(puck.verticalMov<0) puck.verticalMov++;
            }
        }
        //TODO boundaries
    }

    /*
     * small function for determining whether there is a collision between a mallet an a puck
     * returns -1 if there is no collision and power value if there is
     */
    private float checkCollision(float mx, float my, float mr){
        if(Math.sqrt(Math.pow((mx-puck.x),2)+Math.pow((my-puck.y),2))<=mr+puck.radius)
            return (float)Math.sqrt(Math.pow((mx-puck.x),2)+Math.pow((my-puck.y),2));
        return -1;
    }

}

