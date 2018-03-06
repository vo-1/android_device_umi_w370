package com.opium.torch;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ToggleButton;
import android.provider.Settings;

public class MainActivity extends Activity {
	
	private Context context;
	private ToggleButton buttonOn;
	View mMyback;
	private boolean mTorchOn;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            IntentFilter statusfilter = new IntentFilter("com.opium.torch.status");
            statusfilter.addAction("com.opium.torch.status");
            registerReceiver(mFlashStatusReceiver, statusfilter);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.mainnew);
            context = this.getApplicationContext();
            buttonOn = (ToggleButton) findViewById(R.id.buttonOn);
            mMyback = findViewById(R.id.mainback);

            mTorchOn = (Settings.System.getInt(context.getContentResolver(), "torch_status", 0) == 1) ? true : false;
            mMyback.setBackgroundResource(mTorchOn ? R.drawable.bulb_on : R.drawable.bulb_off);
            buttonOn.setBackgroundResource(mTorchOn ? R.drawable.bulb_on_button : R.drawable.bulb_off_button);
	
            buttonOn.setOnClickListener(new OnClickListener() {
			
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent();
                        intent.setAction("com.opium.torch.toggle");
                        intent.putExtra("cmd", !mTorchOn);
                        sendBroadcast(intent);
		    }
	    });
	}
	
        private final BroadcastReceiver mFlashStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = "com.opium.torch.status";
                String receiverAction = intent.getAction();
                boolean status = intent.getBooleanExtra("torch_on", false);
                if (receiverAction.equals(action)) {
		    mMyback.setBackgroundResource(status ? R.drawable.bulb_on : R.drawable.bulb_off);
                    buttonOn.setBackgroundResource(status ? R.drawable.bulb_on_button : R.drawable.bulb_off_button);
		    mTorchOn = status;
                }
            }
        };
	
	@Override
	protected void onResume() {
            super.onResume();
	}
	
	@Override
	protected void onPause() {
            super.onPause();
	}
	
	@Override
	protected void onDestroy() {
            Intent intent = new Intent();
            intent.setAction("com.opium.torch.toggle");
            intent.putExtra("cmd",false);
            sendBroadcast(intent);
            super.onDestroy();
	}

}
