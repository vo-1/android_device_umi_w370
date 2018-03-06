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
package de.mm20.otaupdater.widget;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.Preference;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.mm20.otaupdater.R;

public class InstallationScriptPreference extends Preference implements View.OnClickListener {

    private static final String TAG = "OTAUpdater";
    private static final int STATE_DOWNLOAD = 0;
    private static final int STATE_ABORT_DOWNLOAD = 1;
    private static final int STATE_INSTALL = 2;

    private Context mContext;
    private ActionListener mActionListener;

    private TextView mTitle;
    private TextView mSummary;
    private ProgressBar mProgress;
    private ImageButton mUpIcon;
    private ImageButton mDownIcon;
    private ImageButton mEditIcon;
    private ImageButton mRemoveIcon;
    private int mIndex;

    private int mItemCount;

    String mCommand;
    private int mState;

    public InstallationScriptPreference(Context context, String cmd, int index, int itemCount) {
        super(context);
        mContext = context;
        mCommand = cmd;
        mIndex = index;
        mItemCount = itemCount;
        setLayoutResource(R.layout.script_preference);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        mTitle = (TextView) view.findViewById(R.id.title);
        mSummary = (TextView) view.findViewById(R.id.summary);
        mProgress = (ProgressBar) view.findViewById(R.id.progress);
        mUpIcon = (ImageButton) view.findViewById(R.id.icon_up);
        mDownIcon = (ImageButton) view.findViewById(R.id.icon_down);
        mRemoveIcon = (ImageButton) view.findViewById(R.id.icon_remove);
        mEditIcon = (ImageButton) view.findViewById(R.id.icon_edit);
        mUpIcon.setOnClickListener(this);
        mDownIcon.setOnClickListener(this);
        mRemoveIcon.setOnClickListener(this);
        mEditIcon.setOnClickListener(this);
        mTitle.setText(getTitleForCommand());
        mSummary.setText(getSummaryForCommand());
        if (mIndex == mItemCount - 1) {
            mUpIcon.setVisibility(View.GONE);
            mDownIcon.setVisibility(View.GONE);
            mRemoveIcon.setVisibility(View.GONE);
        } else if (mIndex == mItemCount - 2) {
            mDownIcon.setVisibility(View.GONE);
        }
        if (mIndex == 0) {
            mUpIcon.setVisibility(View.GONE);
        }
        if (mCommand.equals("i $update")) {
            mRemoveIcon.setVisibility(View.GONE);
            mEditIcon.setVisibility(View.GONE);
        }
    }

    private String getTitleForCommand() {
        switch (mCommand.charAt(0)) {
            case 'i':
                return getContext().getString(R.string.script_install);
            case 'w':
                return getContext().getString(R.string.script_wipe);
            case 'b':
                return getContext().getString(R.string.script_backup);
            case 'c':
                return getContext().getString(R.string.script_command);
        }
        return "";
    }

    private String getSummaryForCommand() {
        String arg = mCommand.substring(2, mCommand.length());
        arg = arg.replace("$update", mContext.getString(R.string.script_update_file));
        return arg;
    }

    public InstallationScriptPreference setActionListener(ActionListener listener) {
        mActionListener = listener;
        return this;
    }

    private void edit() {
        if (mCommand.charAt(0) == 'i') {
            if (mActionListener != null) {
                mActionListener.onSelectFile(mIndex);
            }
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(getTitleForCommand());
        switch (mCommand.charAt(0)) {
            case 'w':
                builder.setItems(R.array.dialog_wipe_items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        mCommand = "w " + (i == 0 ? "cache" : "data");
                        setSummary(getSummaryForCommand());
                        if (mActionListener != null) {
                            mActionListener.onCommandChange(mIndex);
                        }
                    }
                });
                break;
            case 'b':
                final boolean checkedItems[] = new boolean[6];
                String arg = mCommand.substring(2);
                if (arg.contains("S")) checkedItems[0] = true;
                if (arg.contains("D")) checkedItems[1] = true;
                if (arg.contains("C")) checkedItems[2] = true;
                if (arg.contains("B")) checkedItems[3] = true;
                if (arg.contains("M")) checkedItems[4] = true;
                if (arg.contains("O")) checkedItems[5] = true;
                builder.setMultiChoiceItems(R.array.dialog_backup_items, checkedItems,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i,
                                                boolean b) {
                                checkedItems[i] = b;
                            }
                        });
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String twrpArg = "";
                                if (checkedItems[0]) twrpArg += "S";
                                if (checkedItems[1]) twrpArg += "D";
                                if (checkedItems[2]) twrpArg += "C";
                                if (checkedItems[3]) twrpArg += "B";
                                if (checkedItems[4]) twrpArg += "M";
                                if (checkedItems[5]) twrpArg += "O";
                                mCommand = "b " + twrpArg;
                                setSummary(getSummaryForCommand());
                                if (mActionListener != null) {
                                    mActionListener.onCommandChange(mIndex);
                                }
                            }
                        });
                break;
            case 'c':
                final EditText cmdInput = new EditText(getContext());
                cmdInput.setText(mCommand.substring(2));
                builder.setView(cmdInput);
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                mCommand = "c " + cmdInput.getText().toString();
                                setSummary(getSummaryForCommand());
                                if (mActionListener != null) {
                                    mActionListener.onCommandChange(mIndex);
                                }
                            }
                        });
                break;
        }
        builder.create().show();
    }

    public void setFile(String fileName) {
        mCommand = "i " + fileName;
        mSummary.setText(getSummaryForCommand());
        if (mActionListener != null) mActionListener.onCommandChange(mIndex);
    }

    public String getCommand() {
        return mCommand;
    }

    @Override
    public void onClick(View view) {
        if (view == mEditIcon) {
            edit();
        }
        if (view == mUpIcon && mActionListener != null) {
            mActionListener.onMoveUp(mIndex);
        }
        if (view == mDownIcon && mActionListener != null) {
            mActionListener.onMoveDown(mIndex);
        }
        if (view == mRemoveIcon && mActionListener != null) {
            mActionListener.onRemove(mIndex);
        }
    }

    public interface ActionListener {
        void onMoveUp(int index);

        void onMoveDown(int index);

        void onRemove(int index);

        void onCommandChange(int index);

        void onSelectFile(int index);
    }
}
