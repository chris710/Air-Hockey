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
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
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
    private boolean mFirstTouch = false;    //initial state, before any interaction
    private boolean mInitialDraw = true;   //since onDraw sometimes gets used two times, this is for security measures

    // Only one touch per side of board
    private boolean mUpTouch = false;
    private boolean mDownTouch = false;

    // the points of where the mallets are right now
    public TouchPoint malletUp;
    public TouchPoint malletDown;

    // puck global
    public Puck puck;

    //for height/size of the actual display
    DisplayMetrics display = this.getContext().getResources().getDisplayMetrics();

    //scale for deciding how big the objects are going to be displayed
    public float scale;
    Integer malletRadius;   //radius of a mallet
    Integer powerReduction = 90;    //slowing puck down coefficient


    //bitmaps
    Bitmap mBitmapG = BitmapFactory.decodeResource(getResources(), R.drawable.mallet_green);
    Bitmap mBitmapP = BitmapFactory.decodeResource(getResources(), R.drawable.mallet_pink);

    //for detecting scaling movement
    private ScaleGestureDetector mScaleDetector;
    private float mScaleFactor = 1.f;

    /**
     * Holds data related to a touch pointer, including its current position
     * and historical positions.
     */
    final class TouchPoint {

        public float x;
        public float y;
        public float radius;
        public Integer score;       //the score of the player

        //goal fields
        public float gs;    //goal start
        public float ge;    //goal end

        public int id;

        //three attributes for knowing where this point is, in the upper area or in the down area
        public boolean up = false;
        public boolean down = false;
        public boolean border = false;
        public boolean init = false;

        public TouchPoint(float x, float y, int id, float radius, float gs, float ge) {
            this.radius = radius;
            this.score = 0;
            this.x = x;
            this.y = y;
            this.id = id;
            this.ge = ge;
            this.gs = gs;
            //Log.i("New TouchPoint created at x = ",Float.toString(x-this.radius));
        }
        public TouchPoint(float x, float y, int id) {
            this.x = x;
            this.y = y;
            this.id = id;
            //Log.i("New TouchPoint created at x = ",Float.toString(x-this.radius));
        }
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
            this.verticalMov = 0;
            this.horizontalMov = 0;
        }

        void reset() {
            this.x = display.widthPixels / 2;
            this.y = display.heightPixels / 2;
            this.verticalMov = 0;
            this.horizontalMov = 0;
        }

    }


    //constructor
    public TouchDisplayView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // SparseArray for touch events, indexed by touch id
        mTouches = new LinkedList<TouchPoint>();
        DisplayMetrics display = this.getContext().getResources().getDisplayMetrics();
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        initialisePaint();
    }

    // BEGIN_INCLUDE(onTouchEvent)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(event);

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

                int id = event.getPointerId(0);
                TouchPoint data = new TouchPoint(event.getX(0), event.getY(0), id);
                /*
                 * Store the data under its pointer identifier. The pointer
                 * number stays consistent for the duration of a gesture,
                 * accounting for other pointers going up or down.
                 */

                mTouches.add(id, data);

                //activating the redrawing function, for continued drawing on canvas
                if(mFirstTouch == false) {
                    mHandler.removeCallbacks(mTick);
                    mHandler.post(mTick);
                }
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

                TouchPoint data = new TouchPoint(event.getX(index), event.getY(index), id);

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

                int index = event.getActionIndex();
                int id = event.getPointerId(index);

                if(id == malletDown.id) {
                    malletDown.init = true;
                }
                if(id == malletUp.id) {
                    malletUp.init = true;
                }

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

                    TouchPoint data = new TouchPoint(event.getX(index), event.getY(index), id);
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

        return true;
    }

    // END_INCLUDE(onTouchEvent)

    @Override
    //I guess here is where we draw things
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Canvas background color
        canvas.drawColor(BACKGROUND_ACTIVE);

        if(!mFirstTouch && mInitialDraw){
            //setting scale
            scale = display.scaledDensity;


            // coordinates for drawing if there was no touch yet
            //initial mallets
            float x = display.widthPixels / 2;
            float y = display.heightPixels / 4;
            malletRadius = Math.round(mBitmapG.getHeight()*scale/3);
            mBitmapG = Bitmap.createScaledBitmap(mBitmapG, malletRadius *2, malletRadius *2,false);
            mBitmapP = Bitmap.createScaledBitmap(mBitmapP, malletRadius *2, malletRadius *2,false);


            //setting appropriate points
            TouchPoint data = new TouchPoint(x, 3*y, 3,malletRadius,x/2,3*x/2);
            data.down = true;
            malletDown = data;
            malletDown.init = true;

            //second point
            TouchPoint data2 = new TouchPoint(x, 1*y, 4, malletRadius,x/2,3*x/2);
            data2.up = true;
            malletUp = data2;
            malletUp.init = true;

            // initialize Puck
            Puck data3 = new Puck(x,2*y);
            data3.radius = scale*30;
            puck = data3;

            mInitialDraw = false;

        }
        //if someone is touching the display, the points could differ
        // setting boolean attributes of the Touchpoints in mTouches and setting malletUp and malletDown
        decideAttributes(canvas);


        /********
         *  draw the data to the canvas
         ********/
        //draw line in the center
        mPaint.setARGB(128,255,255,255);
        canvas.drawLine(0,display.heightPixels/2,display.widthPixels,display.heightPixels/2,mPaint);

        //draw the score text int the middle
        //canvas.scale(1f, -1f, cx, cy);
        mPaint.setTextSize(50f);
        canvas.drawText(Integer.toString(malletUp.score)+":"+Integer.toString(malletDown.score), display.widthPixels/4, display.heightPixels/2, mPaint);

        //draw goals
        mPaint.setColor(COLORS[0]);
        canvas.drawRect(malletDown.gs,display.heightPixels-scale*5,malletDown.ge,display.heightPixels,mPaint);
        canvas.drawRect(malletUp.gs,0,malletUp.ge,scale*5,mPaint);

        //draw mallets
        drawCircle(canvas, malletDown);
        drawCircle(canvas, malletUp);
        
        //draw puck
        if(puck != null) {
            mPaint.setColor(COLORS[3]); //make orange
            canvas.drawCircle(puck.x, puck.y, puck.radius, mPaint); //drawing puck
        }
        
    }

    /*
     * Below are only helper methods and variables required for drawing.
     */


    private Paint mPaint = new Paint();
    private Paint mTextPaint = new Paint();

    private static final int BACKGROUND_ACTIVE = Color.DKGRAY;

    // inactive border
    private static final float INACTIVE_BORDER_DP = 15f;
    private static final int INACTIVE_BORDER_COLOR = Color.WHITE;
    private Paint mBorderPaint = new Paint();
    private float mBorderWidth;

    public final int[] COLORS = {
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
        Bitmap curBitmap;
        if(data == malletUp & !mUpTouch) {
            //only one circle in the upper region of the board
            //canvas.drawBitmap(mBitmapP, data.x - data.radius, data.y - data.radius, mPaint);
            //canvas.drawBitmap(mBitmapP, data.x+data.radius, data.y+data.radius, mPaint);
            curBitmap = mBitmapP;
        } else if(data == malletDown & !mDownTouch) {
            //only one circle in the down region of the board
            //canvas.drawBitmap(mBitmapG, data.x - data.radius, data.y - data.radius, mPaint);
            //canvas.drawBitmap(mBitmapG, data.x, data.y, mPaint);
            curBitmap = mBitmapG;
        } else return;
        canvas.drawBitmap(curBitmap, data.x - data.radius, data.y - data.radius, mPaint);
    }

    /*
      * function for detecting where the touchPoints belong
      * function for setting the attributes up and down of the TouchPoints inside mTouches
      * helper function for onDraw, the first time Canvas is used
      * @param canvas
     */
    private void decideAttributes(Canvas canvas) {
        float border = display.heightPixels /2;

        for (int i = 0; i < mTouches.size(); i++) {
            TouchPoint data = mTouches.get(i);

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

            //resets mallets when no one touches the display
            if (data.up && malletUp.init) {
                if(malletDown.init) {
                    data.radius = malletUp.radius;
                    data.gs = malletUp.gs;
                    data.ge = malletUp.ge;
                    data.score = malletUp.score;
                    malletUp = data;
                    malletDown.id = 1;
                } else if(malletDown.id != data.id) {
                    data.radius = malletUp.radius;
                    data.ge = malletUp.ge;
                    data.gs = malletUp.gs;
                    data.score = malletUp.score;
                    malletUp = data;
                }

                return;
            } else if(data.down && malletDown.init) {
                if (malletUp.init) {
                    data.radius = malletDown.radius;
                    data.gs = malletDown.gs;
                    data.ge = malletDown.ge;
                    data.score = malletDown.score;
                    malletDown = data;
                    malletUp.id = 1;
                } else if(malletUp.id != data.id) {
                    data.radius = malletDown.radius;
                    data.gs = malletDown.gs;
                    data.ge = malletDown.ge;
                    data.score = malletDown.score;
                    malletDown = data;
                }
                return;
            }

           //are there new positions for the pointers
            if (data.id == malletUp.id) {
                if(data.border || data.down) {
                    malletUp.x = data.x;
                    malletUp.y = border - malletUp.radius;
                } else {
                    data.radius = malletUp.radius;
                    data.gs = malletUp.gs;
                    data.ge = malletUp.ge;
                    data.score = malletUp.score;
                    malletUp = data;
                }
            } else if (data.id == malletDown.id) {
                if(data.border || data.up) {
                    malletDown.x = data.x;
                    malletDown.y = border + malletDown.radius;
                } else {
                    data.radius = malletDown.radius;
                    data.gs = malletDown.gs;
                    data.ge = malletDown.ge;
                    data.score = malletDown.score;
                    malletDown = data;
                }
            }
        }
        insideView(malletDown);
        insideView(malletUp);

        puckPhys(malletDown);
        puckPhys(malletUp);

    }

    /*
     *  changing movement of puck
     */
    private void puckPhys(TouchPoint data) {
        float  power = checkCollision(data);
        double friction = 0.98;

        if (power != -1) {  //there is a collision
            puck.horizontalMov = power/powerReduction*(puck.x - data.x );   //determining puck speed changes
            puck.verticalMov = power/powerReduction*(puck.y - data.y);

        }
        else {  //slow down cowboy
            if(puck.horizontalMov>0  || puck.horizontalMov<0) puck.horizontalMov *= friction;
            //if(puck.horizontalMov<0) puck.horizontalMov*=0.9;
            if(puck.verticalMov>0 || puck.verticalMov<0) puck.verticalMov *= friction;
            //if(puck.verticalMov<0) puck.verticalMov*=0.9;
        }
       /* Log.i("hor: ", Float.toString(puck.horizontalMov));
        Log.i("ver: ", Float.toString(puck.verticalMov));*/

        //change puck position
        puck.x += puck.horizontalMov;
        puck.y += puck.verticalMov;

        //boundaries & goals
        if (puck.x-puck.radius < 0 ) {  //left edge
            puck.horizontalMov *= -1;
            puck.x = puck.radius;
        }
        if (puck.x+puck.radius > display.widthPixels) {     //right edge
            puck.horizontalMov *= -1;
            puck.x = display.widthPixels - puck.radius;
        }
        if (puck.y-puck.radius < 0 ) {  //upper edge
            //if(puck.x - puck.radius > malletUp.gs && puck.x + puck.radius >malletUp.ge) // fits clearly
            if( ((puck.x - puck.radius)<malletUp.gs && puck.x>malletUp.gs) ) {//|| (puck.x + puck.radius>malletUp.ge && puck.x<malletUp.ge) ) {   //angled bounce from left corner
                float nx = puck.x - malletUp.gs;
                float ny = puck.y - 0;
                float length = (float)Math.sqrt(nx * nx + ny * ny);
                //nx /= length;
                //ny /= length;
                //float projection = 2*(puck.horizontalMov * nx + puck.verticalMov * ny);

                float projection = 2*((puck.horizontalMov* nx + puck.verticalMov* ny) / length);
                puck.horizontalMov -= projection * nx/powerReduction;
                puck.verticalMov -= projection * ny/powerReduction;
            } else if ((puck.x + puck.radius>malletUp.ge && puck.x<malletUp.ge) ) { //angled bounce from right corner
                float nx = puck.x - malletUp.ge;
                float ny = puck.y - 0;
                float length = (float)Math.sqrt(nx * nx + ny * ny);
                float projection = 2*((puck.horizontalMov* nx + puck.verticalMov* ny) / length);
                puck.horizontalMov -= projection * nx/powerReduction;
                puck.verticalMov -= projection * ny/powerReduction;
            } else if( !( puck.x - puck.radius > malletUp.gs && puck.x + puck.radius < malletUp.ge) ) {        //not in a goal
                puck.verticalMov *= -1;
                puck.y = puck.radius;
            }   //in other case go smoothly through the goal

            if(puck.y<0) { //in case of score reset the puck and add score
                puck.reset();
                malletDown.score++;      //masterpiece of OO programming
            }
        }
        if (puck.y+puck.radius > display.heightPixels) {    //lower edge
            if( ((puck.x - puck.radius)<malletDown.gs && puck.x>malletDown.gs) )  {   //angled bounce from left corner
                float nx = puck.x - malletDown.gs;
                float ny = puck.y - display.heightPixels;
                float length = (float)Math.sqrt(nx * nx + ny * ny);
                float projection = 2*((puck.horizontalMov* nx + puck.verticalMov* ny) / length);
                puck.horizontalMov -= projection * nx/powerReduction;
                puck.verticalMov -= projection * ny/powerReduction;
            } else if ((puck.x + puck.radius>malletDown.ge && puck.x<malletDown.ge) ) { //angled bounce from right corner
                float nx = puck.x - malletDown.ge;
                float ny = puck.y - display.heightPixels;
                float length = (float)Math.sqrt(nx * nx + ny * ny);
                float projection = 2*((puck.horizontalMov* nx + puck.verticalMov* ny) / length);
                puck.horizontalMov -= projection * nx/powerReduction;
                puck.verticalMov -= projection * ny/powerReduction;
            } else if( !( puck.x - puck.radius > malletDown.gs && puck.x + puck.radius < malletDown.ge) ) {        //not in a goal
                puck.verticalMov *= -1;
                puck.y = display.heightPixels - puck.radius;
            }   //in other case go smoothly through the goal

            if(puck.y>display.heightPixels) { //in case of score reset the puck and add score
                puck.reset();
                malletUp.score++;      //masterpiece of OO programming
            }

        }

    }

    /*
     * small function for determining whether there is a collision between a mallet an a puck
     * returns -1 if there is no collision and power value if there is
     */
    private float checkCollision(TouchPoint data){
        if(Math.sqrt(Math.pow((data.x-puck.x),2)+Math.pow((data.y-puck.y),2))<=data.radius+puck.radius)
            return (float)(data.radius + puck.radius - Math.sqrt(Math.pow((data.x-puck.x),2)+Math.pow((data.y-puck.y),2)));
        return -1;
    }

    /*
    *small function to determine if the given TouchPoint is inside the display, and if not
    * change the x or y attributes so it is inside the display
    * @param TouchPoint data
     */
    private void insideView(TouchPoint data) {
        if (data.x < data.radius | data.y < data.radius ) {
            if(data.x < data.radius) {
                data.x = data.radius;
            }
            if(data.y < data.radius) {
                data.y = data.radius;
            }
        }
        if (data.x > display.widthPixels - data.radius | data.y > display.heightPixels - data.radius) {
            if(data.x > display.widthPixels - data.radius) {
                data.x = display.widthPixels - data.radius;
            }
            if(data.y > display.heightPixels - data.radius) {
                data.y = display.heightPixels - data.radius;
            }
        }
    }

    /*
    function and attributes for constant redraw of the canvas
     */
    Handler mHandler = new Handler();
    Runnable mTick = new Runnable() {
        public void run() {
            reDraw();
            mHandler.postDelayed(this, 20); // 20ms == 60fps
        }
    };

    private void reDraw() {
        this.postInvalidate();
    }

    /*
    listener for cathcing scaling movements of the players
     */
    private class ScaleListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            mScaleFactor *= detector.getScaleFactor();

            // Don't let the object get too small or too large.
            mScaleFactor = Math.max(0.1f, Math.min(mScaleFactor, 5.0f));

            Log.i("scaleFactor", Float.toString(mScaleFactor));

            if (mTouches.size() > 3) {
                scalingMallet();
            }

            invalidate();
            return true;
        }
    }

    /*
    function for displaying the scale action of the player on the mallet
     */
    LinkedList<Float> scaleList = new LinkedList<Float>();
    private void scalingMallet() {
        scaleList.add(mScaleFactor);

        if(scaleList.size() > 3) {
            boolean up,down = false;
            up = false;
            for (int i = 0; i < mTouches.size(); i++) {
                if (mTouches.get(i).down){
                    if(down){
                        //malletDown.
                    }
                    down = true;
                } else if(mTouches.get(i).up) {
                    if(up) {

                    }
                    up = true;
                }
            }
        }
    }

}

