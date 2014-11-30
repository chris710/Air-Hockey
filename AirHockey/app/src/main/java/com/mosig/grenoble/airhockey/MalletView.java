package com.mosig.grenoble.airhockey;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by Maria on 26.11.2014.
 */
    // MalletView is a View that displays a bubble.
    // This class handles animating, drawing, popping amongst other actions.
    // A new MalletView is created for each bubble on the display

    public class MalletView extends View {

    // These variables are for testing purposes, do not modify
    private final static int RANDOM = 0;
    private final static int SINGLE = 1;
    private final static int STILL = 2;
    private static int speedMode = RANDOM;

    private static final int MENU_STILL = Menu.FIRST;
    private static final int MENU_SINGLE_SPEED = Menu.FIRST + 1;
    private static final int MENU_RANDOM_SPEED = Menu.FIRST + 2;

    private static final String TAG = "Lab-Graphics";

        private static final int BITMAP_SIZE = 64;
        private static final int REFRESH_RATE = 40;
        private final Paint mPainter = new Paint();
        private ScheduledFuture<?> mMoverFuture;
        private int mScaledBitmapWidth;
        private Bitmap mScaledBitmap;

    // Bubble image
    private Bitmap mBitmap;
    private RelativeLayout mFrame;

        // location, speed and direction of the bubble
        private float mXPos, mYPos, mDx, mDy;
        private long mRotate, mDRotate;

    //for adjusting the game on the device
    private float scale;

        public MalletView(Context context, float x, float y, Bitmap bitmap, RelativeLayout frame ) {
            super(context);
            log("Creating Bubble at: x:" + x + " y:" + y);
            mBitmap = bitmap;
            mFrame = frame;
            // Create a new random number generator to
            // randomize size, rotation, speed and direction
            Random r = new Random();

            // Adjusting the scale of the game, impact on width/height TODO - calculating and deciding how scale will be
            scale = 1;

            // Creates the bubble bitmap for this MalletView
            createScaledBitmap(r);

            // Adjust position to center the bubble under user's finger
            mXPos = x - mScaledBitmapWidth / 2;
            mYPos = y - mScaledBitmapWidth / 2;

            // Set the MalletView's speed and direction
            //setSpeedAndDirection(r);

            // Set the MalletView's rotation
            //setRotation(r);

            mPainter.setAntiAlias(true);

        }

/*        private void setRotation(Random r) {

            if (speedMode == RANDOM) {
                //  set rotation in range [1..3]
                mDRotate = r.nextInt(3)+1;
            } else {
                mDRotate = 0;
            }
        }*/

        private void setSpeedAndDirection(Random r) {

            // Used by test cases
            switch (speedMode) {

                case SINGLE:

                    // Fixed speed
                    mDx = 10;
                    mDy = 10;
                    break;

                case STILL:

                    // No speed
                    mDx = 0;
                    mDy = 0;
                    break;

                default:

                    //  Set movement direction and speed
                    // Limit movement speed in the x and y
                    // direction to [-3..3].

                    mDx=r.nextInt(7)-3;
                    mDy=r.nextInt(7)-3;

            }
        }

        private void createScaledBitmap(Random r) {

            if (speedMode != RANDOM) {

                mScaledBitmapWidth = BITMAP_SIZE * 3;

            } else {

                // set scaled bitmap size in range [1..3] * BITMAP_SIZE TODO check if scale is okay here
                mScaledBitmapWidth = 	(int) scale * BITMAP_SIZE; /*(r.nextInt(3)+1)*/

            }

            //  create the scaled bitmap using size set above
            mScaledBitmap = Bitmap.createScaledBitmap(mBitmap, mScaledBitmapWidth, mScaledBitmapWidth, true);

        }

        // Start moving the MalletView & updating the display
        public void start() {

            // Creates a WorkerThread
            ScheduledExecutorService executor = Executors
                    .newScheduledThreadPool(1);

            // Execute the run() in Worker Thread every REFRESH_RATE
            // milliseconds
            // Save reference to this job in mMoverFuture
            mMoverFuture = executor.scheduleWithFixedDelay(new Runnable() {
                @Override
                public void run() {
                    //  implement movement logic.
                    // Each time this method is run the MalletView should
                    // move one step. If the MalletView exits the display,
                    // stop the MalletView's Worker Thread.
                    // Otherwise, request that the MalletView be redrawn.
                    if(MalletView.this.moveWhileOnScreen()) {
                        MalletView.this.postInvalidate();
                        mMoverFuture.isDone();
                        //} else {
                        //MalletView.this.stop(false);
                    }

                }
            }, 0, REFRESH_RATE, TimeUnit.MILLISECONDS);
        }

        private synchronized boolean intersects(float x, float y) {

            //  Return true if the MalletView intersects position (x,y)
            //mXPos+=mDx;
            //mYPos+=mDy;
            //return isOutOfView();
            if(((mXPos<=x) && (x<=mXPos+mScaledBitmapWidth)) &&
                    ((mYPos<=y) && (y<=mYPos+mScaledBitmapWidth))) {
                return true;
            } else {
                return false;
            }
        }

        // Cancel the Bubble's movement
        // Remove Bubble from mFrame
        // Play pop sound if the MalletView was popped

        private void stop(final boolean popped) {

            if (null != mMoverFuture && mMoverFuture.cancel(true)) {

                // This work will be performed on the UI Thread
                //final MalletView mView = this;
                mFrame.post(new Runnable() {
                    @Override
                    public void run() {

                        //  Remove the MalletView from mFrame
                        mFrame.removeView(MalletView.this);


                        //no sound needed
                        /*if (popped) {
                            log("Pop!");

                            //  If the bubble was popped by user,
                            // play the popping sound
                            mSoundPool.play(mSoundID, mStreamVolume, mStreamVolume,0,0,1f);

                        }*/

                        log("Bubble removed from view!");

                    }
                });
            }
        }

        // Change the Bubble's speed and direction
        private synchronized void deflect(float velocityX, float velocityY) {
            log("velocity X:" + velocityX + " velocity Y:" + velocityY);

            // set mDx and mDy to be the new velocities divided by the REFRESH_RATE

            mDx = (float)velocityX/REFRESH_RATE;
            mDy = (float)velocityY/REFRESH_RATE;

        }

        // Draw the Bubble at its current location
        @Override
        protected synchronized void onDraw(Canvas canvas) {

            //  save the canvas
            canvas.save();

            //  increase the rotation of the original image by mDRotate
            mRotate+=mDRotate;


            //  Rotate the canvas by current rotation
            //canvas.rotate(mDRotate, mXPos+mScaledBitmapWidth/2, mYPos+mScaledBitmapWidth/2);
            canvas.rotate(mRotate, mXPos+mScaledBitmapWidth/2 , mYPos+mScaledBitmapWidth/2);



            //  draw the bitmap at it's new location
            canvas.drawBitmap(mScaledBitmap, mXPos, mYPos, mPainter);


            //  restore the canvas
            canvas.restore();


        }


        private synchronized boolean moveWhileOnScreen() {

            //  Move the MalletView
            // Returns true if the MalletView has exited the screen

            mXPos+=mDx;
            mYPos+=mDy;

            return isOutOfView();/* mXPos<0 -mScaledBitmapWidth || mXPos > mScaledBitmapWidth
					|| mYPos<0-mScaledBitmapWidth || mYPos>mScaledBitmapWidth)*/


        }

        private boolean isOutOfView() {

            //  Return true if the MalletView has exited the screen
            if(mXPos<0-mScaledBitmapWidth || mXPos>mScaledBitmapWidth || mYPos<0-mScaledBitmapWidth
                    || mYPos>mScaledBitmapWidth) {
                return true;
            } else {
                //popping - deleted
                //MalletView.this.stop(true);
                //MalletView.this.setVisibility(GONE);
                return false;
            }

        }

    private static void log (String message) {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Log.i(TAG, message);
    }
}
