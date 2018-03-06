
package com.opium.dreams.video;

import android.graphics.SurfaceTexture;
import android.service.dreams.DreamService;
import android.util.Log;
import android.view.TextureView;
import android.os.Handler;
import android.os.HandlerThread;
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
import android.widget.TextView;
import android.content.res.Configuration;
import android.view.WindowManager;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.widget.VideoView;
import android.os.SystemClock;
import android.app.KeyguardManager;

public class Video extends DreamService {
    static final String TAG = "tsh";
    static final boolean DEBUG = true;

    private WakeLock mWakeLock;
    private PowerManager pManager;
    private SurfaceView mVideoSurfaceView;
    private SurfaceHolder mVideoSurfaceHolder;
    private boolean isVideoPlay = false;
    private MediaPlayer mPlayerVideo = null;
    private TextView mTextErr;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate() >>>");
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.d(TAG, "onAttachedToWindow() >>>");
        pManager = ((PowerManager) getSystemService(POWER_SERVICE));  
        mWakeLock = pManager.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);  
        mWakeLock.acquire();
        //得到键盘锁管理器对象
        KeyguardManager km= (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);
        //解锁
        km.newKeyguardLock("unLock").disableKeyguard();

        setInteractive(false);
        //setLowProfile(true);
        setFullscreen(true);
        //setContentView(R.layout.main);
        /*mTextErr = (TextView)findViewById(R.id.tetx_err);
        mTextErr.setVisibility(View.GONE);

        mVideoSurfaceView = (SurfaceView) findViewById(R.id.video_play);
        //mVideoSurfaceView.setZOrderMediaOverlay(true);
        setVideoLayoutParams();
        mVideoSurfaceHolder = mVideoSurfaceView.getHolder();
        mVideoSurfaceHolder.addCallback(mVideoSurfaceCallback);
        mVideoSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);*/

                        Intent sIntent = new Intent("com.opium.video.dream");
                        sendBroadcast(sIntent);
            //Intent intent = new Intent(this, VideoActivity.class);
            //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //startActivity(intent);
            finish();
    }
   
    void setVideoLayoutParams(){
        if(mVideoSurfaceView == null){
            return;
        }
        LayoutParams lp = (LayoutParams) mVideoSurfaceView.getLayoutParams();
                
        Display dm = getWindowManager().getDefaultDisplay();
        int screenWidth  =  dm.getWidth();    
        int screenHeight = dm.getHeight();
            Log.d(TAG, ">> screenWidth : " + screenWidth);
            Log.d(TAG, ">> screenHeight : " + screenHeight);
        switch (dm.getRotation()) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                Log.d(TAG, "0-180");
                lp.width = screenWidth;
                lp.height =screenWidth * 9/16; 
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                Log.d(TAG, "90-270");
                lp.width = screenWidth;
                lp.height = screenHeight; 
                break;
        }
            Log.d(TAG, "lp Width : " + lp.width);
            Log.d(TAG, "lp Height : " + lp.height);
        mVideoSurfaceView.setLayoutParams(lp);
    
    }

    @Override
    public void onDetachedFromWindow() {
        Log.d(TAG, "Screensaver detached from window");
        super.onDetachedFromWindow();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() >>>");
        //stopVideo();
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        Log.d(TAG, "onDreamingStarted");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
       Log.d(TAG, "Screensaver configuration changed");
        super.onConfigurationChanged(newConfig);

        int mOrientaiton =newConfig.orientation;
        Log.d(TAG, "mOrientaiton : " + mOrientaiton);
        setVideoLayoutParams();
        /*if(mOrientaiton == Configuration.ORIENTATION_PORTRAIT){
        
        }else {
        
        }*/
        // Ignore the configuration change if no window exists.
        //if (getWindow() != null) {
        //}
    }
//-----------------------视频播放---------------------------
    SurfaceHolder.Callback mVideoSurfaceCallback = new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
                playVideo();
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            //Log.d("tsh","surfaceChanged format : " + format);
            //Log.d("tsh","surfaceChanged width  : " + width);
            //Log.d("tsh","surfaceChanged height : " + height);
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            stopVideo();
        }
    };

    private void playVideo(){
        stopVideo();
        mPlayerVideo =  new MediaPlayer();
        mPlayerVideo.reset();
        //getVideoPath();
        try{
            String videoPath =null;
            videoPath = getVideoPath();
          if(videoPath == null){
                Log.d("tsh","getVideoPath = null");
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
        Log.d("tsh","mPlayerVideo.start()");
         mPlayerVideo.start();
    }

    private void stopVideo(){
        if(mPlayerVideo != null){
            Log.d("tsh","stopVideo()");
             mPlayerVideo.stop();
             mPlayerVideo.release();
             mPlayerVideo =null;
        }
    }
// End

   private String getVideoPath(){
        String  filename ;
        String path = Environment.getExternalStorageDirectory() + File.separator;
        path += "Movies"+ File.separator;
        Log.d("tsh","video path : "  + path);
        File files = new File(path);
        if (!files.exists()) {
            Log.d("tsh","video does not exist ");
            return null;
        }
            //Log.d("tsh","files : " + files);
            //Log.d("tsh","files length : " + files.listFiles());
        if(files.listFiles() == null )return null;
        for(File file : files.listFiles()){
            //Log.d("tsh","aa : " + file);
            if(file.isFile() && (file.getName().endsWith(".mp4") ||file.getName().endsWith(".3gp"))){
                Log.d("tsh","bb : " + file.getName());
                path += file.getName();
                break;
            }
        }
        Log.d("tsh","video ="  + path);
        return (path.endsWith(".mp4") || path.endsWith(".3gp")) ? path : null;
   } 

   int mVideoWidth  = -1;
   int mVideoHeight = -1;
   private void getVideoSize(String path){
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(path);
            mVideoWidth = Integer
                    .parseInt(retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            mVideoHeight = Integer
                    .parseInt(retriever
                            .extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            Log.d("tsh","videoWidth: " + mVideoWidth);
            Log.d("tsh","videoHeight : " + mVideoHeight);
        } catch (IllegalArgumentException e) {
        } catch (Exception e) {
        }
  }

}
