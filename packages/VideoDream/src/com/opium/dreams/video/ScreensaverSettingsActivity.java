/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.opium.dreams.video;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.TextView;
import android.app.Activity;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.provider.MediaStore;
import android.provider.MediaStore.Video;
import android.provider.MediaStore.Video.Media;
import android.util.Log;

/**
 * Settings for Clock Daydream
 */
public class ScreensaverSettingsActivity extends Activity {

    static final String KEY_CLOCK_STYLE = "screensaver_clock_style";
    static final String KEY_NIGHT_MODE = "screensaver_night_mode";
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screensaver_settings);
        mTextView = (TextView) findViewById(R.id.video_dreams_setting);
        //mTextView.setText("Video : ......");
        mTextView.setText(R.string.video_path);
    }

    @Override
    public void onResume() {
        super.onResume();
        //getVideos();
    }

    private void getVideos(){
        String videoPath="";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media.DATA}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do{
                        videoPath += "\n" + cursor.getString(0);
                } while (cursor.moveToNext());
            }
            //mTextView.setText("Video : ......\n videoPath :" + videoPath);
            mTextView.setText(R.string.video_path);
        } catch (final SQLiteException ex) {
            ex.printStackTrace();
        } catch (IllegalArgumentException e) {
            // if this exception happen, return false.
            Log.v("tsh", "ContentResolver query IllegalArgumentException");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
