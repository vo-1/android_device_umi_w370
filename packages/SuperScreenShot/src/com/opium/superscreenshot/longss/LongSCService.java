package com.opium.superscreenshot.longss;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageButton;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.widget.TextView;
import com.opium.superscreenshot.R;

import android.os.Message;
import android.os.Binder;

public class LongSCService extends Service {

    RelativeLayout mFloatLayout;
    WindowManager.LayoutParams wmParams;
    WindowManager mWindowManager;

    TextView mTextView;
    ImageButton mImageButton;
    int mCount;
    private Handler mHandler;
    private boolean requestStop;
    private static final String TAG = "LongSCService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "oncreat");
        createFloatView();
        mHandler = new FrameUpdateHandler();
        requestStop = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MyBuild();
    }

    public void updateFrame(int frameNo) {
        mHandler.sendMessage(mHandler.obtainMessage(FrameUpdateHandler.UPDATE_FRAME, frameNo, 0));
    }

    public boolean requestStop() {
        if (requestStop) {
            requestStop = false;
            return true;
        }
        return false;
    }

    private void createFloatView() {
        wmParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager)getApplication().getSystemService(getApplication().WINDOW_SERVICE);
        wmParams.type = LayoutParams.TYPE_PHONE;
        wmParams.format = PixelFormat.RGBA_8888;
        wmParams.flags = LayoutParams.FLAG_NOT_FOCUSABLE;

        wmParams.gravity = Gravity.LEFT | Gravity.TOP;

        wmParams.x = 0;
        wmParams.y = 0;

        wmParams.width = WindowManager.LayoutParams.FILL_PARENT;
        wmParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        LayoutInflater inflater = LayoutInflater.from(getApplication());
        mFloatLayout = (RelativeLayout) inflater.inflate(R.layout.view_scrollscreenshot_controller, null);
        mWindowManager.addView(mFloatLayout, wmParams);

        Log.i(TAG, "mFloatLayout-->left" + mFloatLayout.getLeft());
        Log.i(TAG, "mFloatLayout-->right" + mFloatLayout.getRight());
        Log.i(TAG, "mFloatLayout-->top" + mFloatLayout.getTop());
        Log.i(TAG, "mFloatLayout-->bottom" + mFloatLayout.getBottom());


        mFloatLayout.measure(View.MeasureSpec.makeMeasureSpec(0,
                    View.MeasureSpec.UNSPECIFIED), View.MeasureSpec
                .makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

        mCount = 1;
        mTextView= (TextView) mFloatLayout.findViewById(R.id.page_count_tv);
        mImageButton = (ImageButton)mFloatLayout.findViewById(R.id.stop);
        mTextView.setText(String.format(getResources().getString(R.string.label_notice_sss_page), mCount));

        mImageButton.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                requestStop = true;
                return true;
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatLayout != null) {
            mWindowManager.removeView(mFloatLayout);
        }
    }

    public class MyBuild extends Binder {
        public LongSCService getMyService() {
            return LongSCService.this;
        }
    }

    private final class FrameUpdateHandler extends Handler {
        static final int UPDATE_FRAME = 0;

        public FrameUpdateHandler() {
            super();
        }

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;

            if (what == UPDATE_FRAME) {
                mCount  = msg.arg1;
                mTextView.setText(String.format(getResources().getString(R.string.label_notice_sss_page), mCount));
            }
        }
    }
}
