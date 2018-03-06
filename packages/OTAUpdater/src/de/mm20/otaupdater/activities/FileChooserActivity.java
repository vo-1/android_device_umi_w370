/*
 * Copyright 2015 MaxMustermann2.0
 *
 * Licensed under the Apache License,Version2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.mm20.otaupdater.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Comparator;

import de.mm20.otaupdater.R;

//Ugly implementation. TODO: Write a better one.

public class FileChooserActivity extends Activity implements DialogInterface.OnClickListener {

    private String mSelectedPath;
    private String[] mFiles;
    private Dialog mDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSelectedPath = PreferenceManager
                .getDefaultSharedPreferences(this)
                .getString("file_chooser_preselected_path",
                        Environment.getExternalStorageDirectory().getAbsolutePath());
        File file = new File(mSelectedPath);
        if (!file.exists()) {
            mSelectedPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        showFiles();
    }

    private String[] listZipFiles(File file) {
        if (!file.exists()) return new String[]{};
        String[] files = file.list(new FilenameFilter() {
            @Override
            public boolean accept(File file, String name) {
                return new File(file, name).isDirectory() || name.toLowerCase().endsWith(".zip");
            }
        });
        Arrays.sort(files, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.toLowerCase().compareTo(s2.toLowerCase());
            }
        });
        return files;
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        mSelectedPath = mSelectedPath + '/' + mFiles[i];
        showFiles();
    }

    private void showFiles() {
        if (mDialog != null) mDialog.dismiss();
        if (new File(mSelectedPath).isDirectory()) {
            File file = new File(mSelectedPath);
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mFiles = listZipFiles(file);
            builder.setTitle(mSelectedPath);
            builder.setItems(mFiles, this);
            builder.setCancelable(false);
            builder.setNegativeButton(android.R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        }
                    });
            builder.setNeutralButton(R.string.up, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    up();
                }
            });
            mDialog = builder.create();
            mDialog.show();
        } else {
            Intent intent = new Intent();
            intent.setData(Uri.parse(mSelectedPath));
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void up() {
        if (!mSelectedPath.startsWith("/storage/emulated/0")) mSelectedPath = mSelectedPath
                .substring(0, mSelectedPath.lastIndexOf('/'));
        showFiles();
    }

    @Override
    public void onBackPressed() {
        up();
    }

    @Override
    protected void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putString("file_chooser_preselected_path", mSelectedPath)
                .apply();
    }
}
