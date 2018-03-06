package com.opium.superscreenshot.longss;

import com.opium.superscreenshot.floatwindow.StandOutWindow;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.hardware.input.InputManager;
import android.view.MotionEvent;
import android.os.SystemClock;
import android.view.InputDevice;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.io.File;
import java.util.Date;

import android.graphics.Bitmap;
import android.view.Surface;
import android.view.SurfaceControl;
import android.view.WindowManager;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import android.content.Context;
import android.view.Display;
import android.os.SystemProperties;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import android.content.ServiceConnection;
import android.content.ComponentName;
import android.os.IBinder;
import com.opium.superscreenshot.longss.LongSCService;
import com.opium.superscreenshot.longss.LongSCService.MyBuild;
import android.net.Uri;
import android.widget.Toast;

import android.content.pm.PackageManager;
import android.app.ActivityManager;
import android.content.pm.ResolveInfo;
import com.opium.superscreenshot.R;
import android.util.Log;

public class LongScreenShot {
    private static final String TAG = "LongScreenShot";
    private Display mDisplay;
    private WindowManager mWindowManager;
    private DisplayMetrics mDisplayMetrics;
    private List<Bitmap> mSavedScreenBitmaps;
    private Bitmap mScreenBitmap;
    private Matrix mDisplayMatrix;
    private int mTopCommon;
    private int mBottomOverride;
    private Intent mServiceIntent;

    public static final int MAX_SCREEN = 7;
    public static final int SLEEP_BEFORE_SCREENSHOT = 500;
    public static final int SWIPE_DURATION = 2000;
    public static final int BOTTOM_FRAME_OVERRIDE_CHECKING_HEIGHT = 100;
    private LongSCService mService;
    private ConnectionService mConnectionService;
    private File mFile;
    private Context mContext;

    public LongScreenShot(Context context) {

	this.mContext = context;
        mServiceIntent = new Intent(mContext, LongSCService.class);
        mSavedScreenBitmaps = new ArrayList<Bitmap>();
        mTopCommon = -1;
        mBottomOverride = 0;
        mService = null;
        mConnectionService = new ConnectionService();
    }

    public void onLongScreenShotClick() {

        if (isLauncher(mContext)) {
            Toast.makeText(mContext, mContext.getString(R.string.label_not_support),Toast.LENGTH_SHORT).show();
            return;
        }

        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = mWindowManager.getDefaultDisplay();
        mDisplayMetrics = new DisplayMetrics();
        mDisplay.getRealMetrics(mDisplayMetrics);
        mDisplayMatrix = new Matrix();

        //first screen shot
        try {
            Thread.sleep(SLEEP_BEFORE_SCREENSHOT);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        takeScreenshot();
        mContext.bindService(mServiceIntent, mConnectionService, 1);
        mSavedScreenBitmaps.add(mScreenBitmap);

        for (int i=0; i < MAX_SCREEN ;i++) {
            sendSwipe(InputDevice.SOURCE_TOUCHSCREEN,
                mDisplayMetrics.widthPixels / 2,
                mDisplayMetrics.heightPixels * 3 / 4,
                mDisplayMetrics.widthPixels / 2,
                mDisplayMetrics.heightPixels * 1 / 4,
                SWIPE_DURATION);

            try {
                Thread.sleep(SLEEP_BEFORE_SCREENSHOT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            takeScreenshot();
            if (mService != null) {
                if (mService.requestStop()) {
                    Log.i(TAG, "request stop!!");
                    break;
                }
            }
            if (isEqual(mSavedScreenBitmaps.get(mSavedScreenBitmaps.size() - 1), mScreenBitmap)) {
                computeBottomOverride();
                Log.i(TAG, "hit bottom!!");
                break;
            }
            mSavedScreenBitmaps.add(mScreenBitmap);
	    int j  = 1 + (i + 1) / 2;
            Log.i("yuanzhenlan", "j =  " + j + "" + "i = " + i + "");
	    if(mService != null){
                mService.updateFrame(j);
	    }
	
            /*
            try {
                saveBitmap(mScreenBitmap);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            */
        }

        mTopCommon = mDisplayMetrics.heightPixels / 4;
        Log.i("yuanzhenlan", "mSavedScreenBitmaps.size() =  " + mSavedScreenBitmaps.size() + "" );
        //do merge bitmap
        if (mSavedScreenBitmaps.size() >= 1) {
            Bitmap mergedScreenBitmap =  doMergeBitmap();
            try {
                saveBitmap(mergedScreenBitmap);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            mergedScreenBitmap.recycle();
        } else {
            Toast.makeText(mContext, mContext.getString(R.string.label_not_support),Toast.LENGTH_SHORT).show();
        }


        //recycle bitmap
       for(Iterator it =mSavedScreenBitmaps.iterator(); it.hasNext(); )
       {
            ((Bitmap)it.next()).recycle();
       }
       mService = null;
       mContext.unbindService(mConnectionService);

       //notify media scanner
       Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
       if(mFile != null){
           Uri uri = Uri.fromFile(mFile);
           intent.setData(uri);
           mContext.sendBroadcast(intent);
       }

       //show the image
       intent = new Intent();
       intent.setAction(android.content.Intent.ACTION_VIEW);
       intent.setDataAndType(Uri.fromFile(mFile), "image/png");
       intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
       mContext.startActivity(intent);
    }

    class ConnectionService  implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService=((MyBuild)service).getMyService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    }
    public int diffArray(int[] a1, int[] a2) {
        int diff = 0;

        for (int i=0; i<a1.length;  i++){
            if (a1[i] != a2[i]) {
                diff++;
            }
        }
        return diff;
    }

    public void computeBottomOverride() {
        if (mSavedScreenBitmaps.size() <= 1) {
            return;
        }
        int[] pixels1 = new int[mDisplayMetrics.widthPixels*BOTTOM_FRAME_OVERRIDE_CHECKING_HEIGHT];
        mSavedScreenBitmaps.get(mSavedScreenBitmaps.size() - 2).getPixels(pixels1,0,mDisplayMetrics.widthPixels,
            0, mDisplayMetrics.heightPixels * 3 / 4,
            mDisplayMetrics.widthPixels, BOTTOM_FRAME_OVERRIDE_CHECKING_HEIGHT);

        int[] pixels2 = new int[mDisplayMetrics.widthPixels*BOTTOM_FRAME_OVERRIDE_CHECKING_HEIGHT];
        int maxDiff = 99999;
        for (int i = 0; i < mDisplayMetrics.heightPixels / 2; i++) {
            mSavedScreenBitmaps.get(mSavedScreenBitmaps.size() - 1).getPixels(pixels2,0,mDisplayMetrics.widthPixels,
                0, mDisplayMetrics.heightPixels / 4 + i,
                mDisplayMetrics.widthPixels, BOTTOM_FRAME_OVERRIDE_CHECKING_HEIGHT);
            int currDiff = diffArray(pixels1, pixels2);
            if (currDiff < maxDiff) {
                maxDiff  = currDiff;
                mBottomOverride = i;
            }
        }
    }
    public boolean isEqual(Bitmap oneBitmap, Bitmap anotherBitmap) {
        int nonMatchingPixels = 0;
        int allowedMaxNonMatchPixels = mDisplayMetrics.widthPixels * mDisplayMetrics.heightPixels / 100;
        int firstNonmatchPixel = -1;
        int lastNonmatchPixel = -1;

        if(oneBitmap == null || anotherBitmap == null) {
            return false;
        }

        int[] expectedBmpPixels = new int[oneBitmap.getWidth() * oneBitmap.getHeight()];
        oneBitmap.getPixels(expectedBmpPixels, 0, oneBitmap.getWidth(), 0, 0, oneBitmap.getWidth(), oneBitmap.getHeight());

        int[] actualBmpPixels = new int[anotherBitmap.getWidth() * anotherBitmap.getHeight()];
        anotherBitmap.getPixels(actualBmpPixels, 0, anotherBitmap.getWidth(), 0, 0, anotherBitmap.getWidth(), anotherBitmap.getHeight());

        if (expectedBmpPixels.length != actualBmpPixels.length) {
            return false;
        }

        for (int i = 0; i < expectedBmpPixels.length; i++) {
            if (expectedBmpPixels[i] != actualBmpPixels[i]) {
                if (nonMatchingPixels == 0) {
                    firstNonmatchPixel = i;
                }
                lastNonmatchPixel = i;
                nonMatchingPixels++;
            }
        }
        if (nonMatchingPixels > allowedMaxNonMatchPixels) {
            return false;
        }
        return true;
    }

    private void sendSwipe(int inputSource, float x1, float y1, float x2, float y2, int duration) {
        if (duration < 0) {
            duration = 300;
        }
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x1, y1, 1.0f);
        long startTime = now;
        long endTime = startTime + duration;
        while (now < endTime) {
            long elapsedTime = now - startTime;
            float alpha = (float) elapsedTime / duration;
            injectMotionEvent(inputSource, MotionEvent.ACTION_MOVE, now, lerp(x1, x2, alpha),
                    lerp(y1, y2, alpha), 1.0f);
            now = SystemClock.uptimeMillis();
        }
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x2, y2, 0.0f);
    }

    private void sendTap(int inputSource, float x, float y) {
        long now = SystemClock.uptimeMillis();
        injectMotionEvent(inputSource, MotionEvent.ACTION_DOWN, now, x, y, 1.0f);
        injectMotionEvent(inputSource, MotionEvent.ACTION_UP, now, x, y, 0.0f);
    }

    private void injectMotionEvent(int inputSource, int action, long when, float x, float y, float pressure) {
        final float DEFAULT_SIZE = 1.0f;
        final int DEFAULT_META_STATE = 0;
        final float DEFAULT_PRECISION_X = 1.0f;
        final float DEFAULT_PRECISION_Y = 1.0f;
        final int DEFAULT_DEVICE_ID = 0;
        final int DEFAULT_EDGE_FLAGS = 0;
        MotionEvent event = MotionEvent.obtain(when, when, action, x, y, pressure, DEFAULT_SIZE,
                DEFAULT_META_STATE, DEFAULT_PRECISION_X, DEFAULT_PRECISION_Y, DEFAULT_DEVICE_ID,
                DEFAULT_EDGE_FLAGS);
        event.setSource(inputSource);
        InputManager.getInstance().injectInputEvent(event,
                InputManager.INJECT_INPUT_EVENT_MODE_WAIT_FOR_FINISH);
    }

    private static final float lerp(float a, float b, float alpha) {
        return (b - a) * alpha + a;
    }

    private float getDegreesForRotation(int value) {
       switch (value) {
       case Surface.ROTATION_90:
           return 360f - 90f;
       case Surface.ROTATION_180:
           return 360f - 180f;
       case Surface.ROTATION_270:
           return 360f - 270f;
       }
       return 0f;
   }

   private void takeScreenshot() {
       Log.i("yuanzhenlan", "will takeScreenshot ");
       float[] dims = { mDisplayMetrics.widthPixels,
               mDisplayMetrics.heightPixels };

       int value = mDisplay.getRotation();
       String hwRotation = SystemProperties.get("ro.sf.hwrotation", "0");
       if (hwRotation.equals("270") || hwRotation.equals("90")) {
           value = (value + 3) % 4;
       }
       float degrees = getDegreesForRotation(value);

       boolean requiresRotation = (degrees > 0);
       if (requiresRotation) {
           // Get the dimensions of the device in its native orientation
           mDisplayMatrix.reset();
           mDisplayMatrix.preRotate(-degrees);
           mDisplayMatrix.mapPoints(dims);

           dims[0] = Math.abs(dims[0]);
           dims[1] = Math.abs(dims[1]);
       }

       mScreenBitmap = SurfaceControl.screenshot((int) dims[0], (int) dims[1]);

       if (requiresRotation) {
           // Rotate the screenshot to the current orientation
           Bitmap ss = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                   mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
           Canvas c = new Canvas(ss);
           c.translate(ss.getWidth() / 2, ss.getHeight() / 2);
           c.rotate(degrees);
           c.translate(-dims[0] / 2, -dims[1] / 2);
           c.drawBitmap(mScreenBitmap, 0, 0, null);
           c.setBitmap(null);
            mScreenBitmap = ss;
        }

        // If we couldn't take the screenshot, notify the user
        if (mScreenBitmap == null) {
            return;
        }

        // Optimizations
        mScreenBitmap.setHasAlpha(false);
        mScreenBitmap.prepareToDraw();

    }

    public Bitmap doMergeBitmap() {
        Bitmap resultBitmap = Bitmap.createBitmap(mDisplayMetrics.widthPixels,
                mDisplayMetrics.heightPixels * (mSavedScreenBitmaps.size() + 1) / 2 - mBottomOverride, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(resultBitmap);

        for(int i = 0; i < mSavedScreenBitmaps.size(); i++) {
            Bitmap bmp1 = (Bitmap) mSavedScreenBitmaps.get(i);
            Rect srcRect;
            Rect dstRect;
            int from = 0;
            int to = mDisplayMetrics.heightPixels;
            if (i != 0) {
                from = mTopCommon;
            }
            srcRect = new Rect(0, from, mDisplayMetrics.widthPixels, to);
            if (i == mSavedScreenBitmaps.size() -1) {
                dstRect = new Rect(0,
                    i * mDisplayMetrics.heightPixels / 2 - mBottomOverride + from,
                    mDisplayMetrics.widthPixels,
                    i * mDisplayMetrics.heightPixels / 2 - mBottomOverride + to
                );
            } else {
                dstRect = new Rect(0,
                    i * mDisplayMetrics.heightPixels / 2 + from,
                    mDisplayMetrics.widthPixels,
                    i * mDisplayMetrics.heightPixels / 2 + to
                );
            }
            //Log.i(TAG, "srcRect: " + srcRect);
            //Log.i(TAG, "dstRect: " + dstRect);
            canvas.drawBitmap(bmp1, srcRect, dstRect, null);
        }

        return resultBitmap;
    }

    public void saveBitmap(Bitmap bitmap) throws IOException {
        String imageDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
                .format(new Date(System.currentTimeMillis()));
        File dir = new File("/sdcard/Pictures/Screenshots");
        if(!dir.exists()) {
            dir.mkdirs();
        }
        mFile = new File(dir + "/Screenshot_"+imageDate+".png");
        if(!mFile.exists()){
            mFile.createNewFile();
        }
        FileOutputStream out;
        try {
            out = new FileOutputStream(mFile);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 70, out)) {
                out.flush();
                out.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public List getLauncherPackageName(Context context) {
        List packageNames = new ArrayList(); 
        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> resolveInfo = context.getPackageManager().queryIntentActivities(intent,
            PackageManager.MATCH_DEFAULT_ONLY);
        for(ResolveInfo ri : resolveInfo){  
            packageNames.add(ri.activityInfo.packageName);  
        }  
        if(packageNames == null || packageNames.size() == 0){
            return null;
        }else{
            return packageNames;
        }
    }


    public boolean isLauncher(Context context) {
        ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> rti = mActivityManager.getRunningTasks(1);
        String topPackageName = rti.get(0).topActivity.getPackageName();
        List launcherName = getLauncherPackageName(context);
        if (launcherName != null && launcherName.size() != 0) {
            for (int i = 0; i < launcherName.size(); i ++) {
                if (launcherName.get(i) != null && launcherName.get(i).equals(topPackageName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
