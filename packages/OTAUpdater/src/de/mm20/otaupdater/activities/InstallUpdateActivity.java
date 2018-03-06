/*
 * Copyright 2015 MaxMustermann2.0
 *
 * Licensed under the Apache License,Version 2.0 (the "License");
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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.DataOutputStream;
import java.io.IOException;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.util.InstallationScriptProcessor;

public class InstallUpdateActivity extends Activity {
    private static final String TAG = "InstallUpdateActivity";
    Runnable failedToast = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(InstallUpdateActivity.this, R.string.install_failed, Toast.LENGTH_LONG)
                    .show();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        int installedDeprecated = getIntent().getIntExtra("installed_deprecated", 1);
        int msgId = R.string.confirm_install;
        if (installedDeprecated == 0) msgId = R.string.confirm_install_installed;
        else if (installedDeprecated == -1) msgId = R.string.confirm_install_deprecated;
        builder.setMessage(msgId)
                .setTitle(R.string.system_update)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = getIntent().getStringExtra("file_name");
                        installUpdate(fileName);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                }).create().show();

    }

    private void installUpdate(final String fileName) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.preparing_update));
        dialog.show();
        dialog.setCancelable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                String updateFile = Environment.getExternalStorageDirectory() + "/cmupdater/" +
                        fileName;
                String openRecoveryScript = "";
                try {
                    JSONArray installationScript = new JSONArray(PreferenceManager
                            .getDefaultSharedPreferences(InstallUpdateActivity.this)
                            .getString("installation_script", "[\"i $update\", \"w cache\"]"));
                    for (int i = 0; i < installationScript.length(); i++) {
                        String cmd = installationScript.optString(i, "");
                        cmd = cmd.replace("$update", updateFile);
                        openRecoveryScript += InstallationScriptProcessor.processCommand(cmd);
                        Process process = Runtime.getRuntime().exec("sh");
                        DataOutputStream outputStream = new DataOutputStream(
                                process.getOutputStream());
                        //Write recovery script to cache partition.
                        outputStream.writeBytes("printf \"" + openRecoveryScript
                                + "\" >/cache/recovery/openrecoveryscript\n");
                        outputStream.flush();
                        outputStream.close();
                        process.waitFor();
                    }

                    //Reboot to recovery
                    PowerManager powerManager = (PowerManager)
                            getSystemService(Context.POWER_SERVICE);
                    powerManager.reboot("recovery");
                } catch (IOException | InterruptedException | JSONException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                    finish();
                }
            }
        }).start();
    }
}
