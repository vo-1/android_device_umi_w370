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

package de.mm20.otaupdater.util;

import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Installation script command reference:
 * i [filename] -> Install [filename]
 * <p>
 * w [system/cache/data] -> Wipe [system/cache/data]
 * NOTE: wipe cache may not be called before the end of the script since it deletes the update files
 * and the openrecoveryscript. When everything is done, it must be called to clean up.
 * <p>
 * c [command] -> execute command
 * <p>
 * b [SDCBOM] -> Backup selected partitions:
 * S = /system
 * D = /data
 * C = /cache
 * B = /boot
 * O = enable compression
 * M = skip md5sum generation
 * <p>
 * $update -> will be replaced by update file name
 */
public class InstallationScriptProcessor {

    private static final String TAG = InstallationScriptProcessor.class.getSimpleName();

    /**
     * Prepares the execution of a command in recovery
     *
     * @param command
     * @return the command which should be written to /cache/recovery/openrecoveryscript
     * @throws IOException
     * @throws InterruptedException
     */
    public static String processCommand(String command) throws IOException, InterruptedException {
        char cmd = command.charAt(0);
        String arg = command.substring(2, command.length());
        Log.i(TAG, "Command: " + command);
        switch (cmd) {
            case 'i':
                File file = new File(arg);
                if (!file.exists()) return "";
                String filename = arg.substring(arg.lastIndexOf('/') + 1, arg.length());
                Process process = Runtime.getRuntime().exec("sh");
                DataOutputStream outputStream = new DataOutputStream(
                        process.getOutputStream());
                //Copy update zip to cache partition because we don't know the mount point of the
                //sdcard in recovery
                outputStream.writeBytes("cp -f " + arg +
                        " /cache/recovery/" + filename + "\n");
                //Copy md5 sum file to cache partition
                File md5File = new File(arg + ".md5sum");
                if (md5File.exists()) {
                    outputStream.writeBytes("cp -f " + arg +
                            ".md5sum /cache/recovery/" + filename + ".md5sum\n");
                    Log.i(TAG, "No md5 file found for file: " + arg);
                }
                outputStream.flush();
                outputStream.close();
                process.waitFor();
                return "install /cache/recovery/" + filename + "\n";
            case 'w':
                if (arg.equals("data") || arg.equals("cache")) return "wipe " + arg + "\n";
                Log.w(TAG, "Unknown partition to wipe: " + arg);
                break;
            case 'b':
                return "backup " + arg + "\n";
            case 'c':
                return "cmd " + arg + "\n";
        }
        return "";
    }
}