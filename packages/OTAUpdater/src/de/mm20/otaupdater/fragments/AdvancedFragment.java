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

package de.mm20.otaupdater.fragments;

import android.annotation.Nullable;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import de.mm20.otaupdater.R;
import de.mm20.otaupdater.activities.FileChooserActivity;
import de.mm20.otaupdater.widget.InstallationScriptPreference;

public class AdvancedFragment extends PreferenceFragment implements InstallationScriptPreference.ActionListener, Preference.OnPreferenceClickListener {

    private ArrayList<String> mScript;
    private PreferenceCategory mScriptCategory;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.advanced_preferences);
        mScriptCategory = (PreferenceCategory) findPreference("installation_script");
        mScript = new ArrayList<>();
        try {
            JSONArray installationScript = new JSONArray(PreferenceManager
                    .getDefaultSharedPreferences(getContext())
                    .getString("installation_script", "[\"i $update\",\"w cache\"]"));
            for (int i = 0; i < installationScript.length(); i++) {
                mScript.add(installationScript.getString(i));
            }
        } catch (JSONException e) {
            Log.e(getClass().getSimpleName(), Log.getStackTraceString(e));
        }
        updateScriptPreferenceViews();
        Preference addPreference = findPreference("add_script_item");
        addPreference.setOnPreferenceClickListener(this);
    }

    private void updateScriptPreferenceViews() {
        mScriptCategory.removeAll();
        for (int i = 0; i < mScript.size(); i++) {
            mScriptCategory.addPreference(new InstallationScriptPreference(getContext(),
                    mScript.get(i), i, mScript.size()).setActionListener(this));
        }
    }

    private void save() {
        JSONArray json = new JSONArray();
        for (String cmd : mScript) {
            json.put(cmd);
        }
        PreferenceManager
                .getDefaultSharedPreferences(getContext())
                .edit()
                .putString("installation_script", json.toString())
                .apply();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) return;
        ((InstallationScriptPreference) mScriptCategory.getPreference(requestCode))
                .setFile(data.getData().getPath());
    }

    @Override
    public void onMoveUp(int index) {
        String cmd = mScript.remove(index);
        mScript.add(index - 1, cmd);
        updateScriptPreferenceViews();
        save();
    }

    @Override
    public void onMoveDown(int index) {
        String cmd = mScript.remove(index);
        mScript.add(index + 1, cmd);
        updateScriptPreferenceViews();
        save();
    }

    @Override
    public void onRemove(int index) {
        mScript.remove(index);
        mScriptCategory.removePreference(mScriptCategory.getPreference(index));
        save();
    }

    @Override
    public void onCommandChange(int index) {
        for (int i = 0; i < mScript.size(); i++) {
            mScript.set(i,
                    ((InstallationScriptPreference) mScriptCategory.getPreference(i)).getCommand());
        }
        save();
    }

    @Override
    public void onSelectFile(int index) {
        Intent filePickerIntent = new Intent(getContext(), FileChooserActivity.class);
        startActivityForResult(filePickerIntent, index);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setItems(R.array.add_script_item_entries, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                switch (which) {
                    //Install
                    case 0:
                        mScript.add(mScript.size() - 1, "i ");
                        break;
                    //Backup
                    case 1:
                        mScript.add(mScript.size() - 1, "b SDCB");
                        break;
                    //Command
                    case 2:
                        mScript.add(mScript.size() - 1, "c ls");
                        break;
                }
                updateScriptPreferenceViews();
            }
        });
        builder.create().show();
        return true;
    }
}
