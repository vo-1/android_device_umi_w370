
package com.opium.dreams.video;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.view.MenuItem;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.mediatek.internal.telephony.ITelephonyEx;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import com.android.featureoption.FeatureOption;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.BroadcastReceiver;
import android.widget.VideoView;
import android.os.SystemClock;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import java.io.IOException;
import android.os.Environment;
import java.io.File;
import android.net.Uri;
import android.view.ViewGroup.LayoutParams;
import android.view.Surface;
import android.media.MediaMetadataRetriever;
import java.lang.Integer;
import android.content.res.Configuration;
import android.view.GestureDetector;
import android.view.MotionEvent;


public class VideoActivity extends Activity {

    static final String TAG = "VideoActivity_tsh";
    private WakeLock mWakeLock;
    private PowerManager pManager;
    private SurfaceView mVideoSurfaceView;
    private SurfaceHolder mVideoSurfaceHolder;
    private boolean isVideoPlay = false;
    private MediaPlayer mPlayerVideo = null;
    private TextView mTextErr;
    private GestureDetector mDetector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        //mContext = getApplicationContext();
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        Log.d(TAG, "onCreate() >>>");
        setContentView(R.layout.main_dream);

        mDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener(){
                        @Override
                        public boolean onSingleTapUp(MotionEvent e) {
                            Log.d(TAG, "onSingleTapUp >>>");
                            System.exit(0);
                            return false;
                        }
                });
        mTextErr = (TextView)findViewById(R.id.text_info);
        mTextErr.setVisibility(View.GONE);

        videoPath = getVideoPath();
        mVideoSurfaceView = (SurfaceView) findViewById(R.id.video_dream_play);
        //mVideoSurfaceView.setZOrderMediaOverlay(true);
        setVideoLayoutParams(mVideoSurfaceView);
        mVideoSurfaceHolder = mVideoSurfaceView.getHolder();
        mVideoSurfaceHolder.addCallback(mVideoSurfaceCallback);
        mVideoSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() >>>");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() >>>");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent >>>");
        return mDetector.onTouchEvent(event);
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop() >>>");
        stopVideo();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() >>>");
        stopVideo();
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
                Log.d(TAG, "onOptionsItemSelected() >>>");
        switch (item.getItemId()) {
            case android.R.id.home:
                Log.d(TAG, "onOptionsItemSelected() home>>>");
                //stopVideo();
                //android.os.Process.killProcess(android.os.Process.myPid());
                System.exit(0);
                break;
            default:
                break;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
       Log.d(TAG, "Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);

        int mOrientaiton =newConfig.orientation;
        Log.d(TAG, "mOrientaiton : " + mOrientaiton);
        setVideoLayoutParams(mVideoSurfaceView);
    }

//-----------------------视频播放---------------------------
    SurfaceHolder.Callback mVideoSurfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG,"surfaceCreated");
                playVideo();
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG,"surfaceChanged format : " + format);
            //Log.d(TAG,"surfaceChanged width  : " + width);
            //Log.d(TAG,"surfaceChanged height : " + height);
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            stopVideo();
        }
    };

    private void playVideo(){
        stopVideo();
        mPlayerVideo =  new MediaPlayer();
        mPlayerVideo.reset();
        try{
          if(videoPath == null){
                Log.d(TAG,"getVideoPath = null");
                //AssetFileDescriptor afd = getAssets().openFd("aging_test.mp4");
                //mPlayerVideo.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                mTextErr.setVisibility(View.VISIBLE);
                mVideoSurfaceView.setVisibility(View.GONE);
                return ;
          }else{
                mPlayerVideo.setDataSource(this, Uri.parse(videoPath));
          }
         mPlayerVideo.setDisplay(mVideoSurfaceHolder);
         mPlayerVideo.setLooping(true);
         mPlayerVideo.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG,"mPlayerVideo.start()");
         mPlayerVideo.start();
    }

    private void stopVideo(){
        if(mPlayerVideo != null){
            Log.d(TAG,"stopVideo()");
             mPlayerVideo.stop();
             mPlayerVideo.release();
             mPlayerVideo =null;
        }
    }

   private String getVideoPath(){
        String  filename ;
        String path = Environment.getExternalStorageDirectory() + File.separator;
        path += "Movies"+ File.separator;
        Log.d(TAG,"video path : "  + path);
        File files = new File(path);
        if (!files.exists()) {
            Log.d(TAG,"video does not exist ");
            return null;
        }
        if(files.listFiles() == null )return null;
        for(File file : files.listFiles()){
            //Log.d(TAG,"aa : " + file);
            if(file.isFile() && (file.getName().endsWith(".mp4") ||file.getName().endsWith(".3gp"))){
                Log.d(TAG,"bb : " + file.getName());
                path += file.getName();
                break;
            }
        }
        Log.d(TAG,"视频文件 ="  + path);
        return (path.endsWith(".mp4") || path.endsWith(".3gp")) ? path : null;
   } 

   /*private void getVideoInfo(String srcPath){
        MediaMetadataRetriever retrieverSrc = new MediaMetadataRetriever();
        retrieverSrc.setDataSource(srcPath);
        String degreesString = retrieverSrc
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
        Log.d(TAG, "degreesString ="  + degreesString);
        if (degreesString != null) {
            int degrees = Integer.parseInt(degreesString);
            Log.d(TAG, "degrees ="  + degrees);
        }
   }*/

   int mVideoWidth  = -1;
   int mVideoHeight = -1;
   int mVideoRotation = -1;
   String videoPath =null;
   private void getVideoSize(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            if(retriever ==null )return;
            mVideoWidth = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            mVideoHeight = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            mVideoRotation = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            Log.d(TAG,"videoWidth: " + mVideoWidth);
            Log.d(TAG,"videoHeight : " + mVideoHeight);
            Log.d(TAG,"mVideoRotation : " + mVideoRotation);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
        }
  }

    void setVideoLayoutParams(View v){
        if(v == null){
            return;
        }
        getVideoSize(videoPath);
        LayoutParams lp = (LayoutParams) v.getLayoutParams();
                
        Display dm = getWindowManager().getDefaultDisplay();
        int screenWidth  =  dm.getWidth();    
        int screenHeight = dm.getHeight();
            Log.d(TAG, ">> screenWidth : " + screenWidth);
            Log.d(TAG, ">> screenHeight : " + screenHeight);
            Log.d(TAG, ">> dm.getRotation : " + dm.getRotation());
        switch (dm.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                Log.d(TAG, "0-180");
                lp.width  = screenWidth;
                lp.height = screenHeight ; 
                if(mVideoRotation == 0 || mVideoRotation ==180){
                    lp.width = screenWidth;
                    lp.height =screenWidth * mVideoHeight / mVideoWidth; 
                }

                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                Log.d(TAG, "90-270");
                lp.width = screenHeight *  mVideoHeight / mVideoWidth;
                lp.height = screenHeight; 
                if(mVideoRotation == 0 || mVideoRotation == 180){
                    lp.width = screenHeight * mVideoWidth/mVideoHeight ;
                    lp.height =screenHeight ; 
                }
                break;
        }
            Log.d(TAG, "lp Width : " + lp.width);
            Log.d(TAG, "lp Height : " + lp.height);
        v.setLayoutParams(lp);
    
    }
}
