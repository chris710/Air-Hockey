package com.mosig.grenoble.airhockey;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.mosig.grenoble.airhockey.R;


public class DrawActivity extends Activity {

    // These variables are for testing purposes, do not modify
    private final static int RANDOM = 0;
    private final static int SINGLE = 1;
    private final static int STILL = 2;
    private static int speedMode = RANDOM;

    private static final int MENU_STILL = Menu.FIRST;
    private static final int MENU_SINGLE_SPEED = Menu.FIRST + 1;
    private static final int MENU_RANDOM_SPEED = Menu.FIRST + 2;

    private static final String TAG = "Lab-Graphics";

    // Main view
    static public RelativeLayout mFrame;

    // Bubble image
    static Bitmap mBitmap;

    // Display dimensions
    private int mDisplayWidth, mDisplayHeight;
    int scale;

    // Sound variables

    // AudioManager
    private AudioManager mAudioManager;
    // SoundPool
    private SoundPool mSoundPool;
    // ID for the bubble popping sound
    private int mSoundID;
    // Audio volume
    private float mStreamVolume;

    // Gesture Detector
    private GestureDetector mGestureDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Set up user interface
        mFrame = (RelativeLayout) findViewById(R.id.frame);

        // Load basic bubble Bitmap
        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.b64);

    }
    @Override
    protected void onResume() {
        super.onResume();

        // Manage bubble popping sound
        // Use AudioManager.STREAM_MUSIC as stream type

        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        mStreamVolume = (float) mAudioManager
                .getStreamVolume(AudioManager.STREAM_MUSIC)
                / mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        //  make a new SoundPool, allowing up to 10 streams
        mSoundPool = new SoundPool(10,AudioManager.STREAM_MUSIC,0);

        //  set a SoundPool OnLoadCompletedListener that calls setupGestureDetector()
        mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {

            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                //  Auto-generated method stub
                if(0==status){
                    setupGestureDetector();
                }
            }
        });

        //  load the sound from res/raw/bubble_pop.wav
        mSoundID = mSoundPool.load(this, R.raw.bubble_pop,1);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {

            // Get the size of the display so this view knows where borders are
            mDisplayWidth = mFrame.getWidth();
            mDisplayHeight = mFrame.getHeight();
            scale = mDisplayWidth/320;
            initObjects();

        }
    }

    /**************
     *  @param
     *
     */
    private void initObjects(){

        // Create Mallets and Puck
        MalletView Mallet1 = new MalletView(getApplicationContext(),mDisplayWidth/2,mDisplayHeight/4, mBitmap, mFrame);
        mFrame.addView(Mallet1);
        Mallet1.start();
        MalletView Mallet2 = new MalletView(getApplicationContext(),mDisplayWidth/2,3*mDisplayHeight/4, mBitmap, mFrame);
        mFrame.addView(Mallet2);
        Mallet2.start();
        MalletView Puck = new MalletView(getApplicationContext(),mDisplayWidth/2,mDisplayHeight/2, mBitmap, mFrame);
        mFrame.addView(Puck);
        Puck.start();
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

            case MotionEvent.ACTION_MOVE: {
                Log.i("gesture","ACTion_move");

                MalletView Mallet = new MalletView(getApplicationContext(),event.getX(),event.getY(), mBitmap, mFrame);
                mFrame.addView(Mallet);
                Mallet.start();
            }
        }

        return true;
    }


// Set up GestureDetector
    private void setupGestureDetector() {

        GestureDetector.SimpleOnGestureListener gestureListener = new GestureListener();

        mGestureDetector = new GestureDetector(this, gestureListener


                /*new GestureDetector.SimpleOnGestureListener() {

                    // If a fling gesture starts on a MalletView then change the
                    // MalletView's velocity

                    /*@Override
                    public boolean onFling(MotionEvent event1, MotionEvent event2,
                                           float velocityX, float velocityY) {

                        //  Implement onFling actions.
                        // You can get all Views in mFrame using the
                        // ViewGroup.getChildCount() method

                        //count of the number of Views in mFrame
                        //int childCount = mFrame.getChildCount();
                        //location of the tap event
                        float x = event1.getRawX();
                        float y = event1.getRawY();
                        //get the position of the child Views in mFrame
                        //MalletView mBubble = (MalletView) mFrame.getChildAt(childCount);
                        //get the position of the new Bubble which matches the tap position
                        //MalletView newBubble = new MalletView(mFrame.getContext(),x,y);

                        for (int i = 0; i < childCount; i++) {
                            // If the tap location overlaps an existing bubble and we should “pop” it
                            MalletView flingBubble = (MalletView) mFrame.getChildAt(i);
                            if (flingBubble.intersects(x, y)) {
                                flingBubble.deflect(velocityX, velocityY);
                                return true;
                            }
                        }

                        return false;

                    }

                    // If a single tap intersects a MalletView, then pop the MalletView
                    // Otherwise, create a new MalletView at the tap's location and add
                    // it to mFrame. You can get all views from mFrame with ViewGroup.getChildAt()

                    @Override
                    public boolean onSingleTapConfirmed(MotionEvent event) {

                        //  Implement onSingleTapConfirmed actions.
                        // You can get all Views in mFrame using the
                        // ViewGroup.getChildCount() method

                        //popping bubbles - deleted
                        for(int i=0;i<mFrame.getChildCount(); ++i) {
                            MalletView bubbleNew = (MalletView) mFrame.getChildAt(i);
                            if(bubbleNew.intersects(event.getX(), event.getY())) {
                                bubbleNew.stop(true);
                                return true;
                            }
                        }



                        return false
                    }*/
                );
        Log.i("test", "setup gesture detector");
    }

    /*@Override
    public boolean onTouchEvent(MotionEvent event) {

        //  delegate the touch to the gestureDetector
        return mGestureDetector.onTouchEvent(event);

    }*/

    @Override
    protected void onPause() {

        //  Release all SoundPool resources
        mSoundPool.unload(mSoundID);
        mSoundPool=null;

        super.onPause();
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_draw, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
