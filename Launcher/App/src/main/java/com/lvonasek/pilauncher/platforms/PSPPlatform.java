package com.lvonasek.pilauncher.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import com.lvonasek.pilauncher.R;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class PSPPlatform  extends AbstractPlatform {

    private static final String CONFIG_FILE = "/mnt/sdcard/PSP/SYSTEM/ppsspp.ini";
    private static final String EMULATOR_PACKAGE = "org.ppsspp.ppsspp";
    private static final String FILENAME_PREFIX = "FileName";
    private static final String PACKAGE_PREFIX = "psp://";
    private static final String RECENT_TAG = "[Recent]";

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        for (String iso : locateISOs()) {
            ApplicationInfo app = new ApplicationInfo();
            app.name = iso; //TODO:get name
            app.packageName = PACKAGE_PREFIX + iso;
            output.add(app);
        }
        return output;
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        icon.setImageResource(R.drawable.ic_launcher);
        //TODO:proper implementation
    }

    @Override
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        String path = app.packageName.substring(PACKAGE_PREFIX.length());
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("file://" + path));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        //TODO:intent.setPackage(EMULATOR_PACKAGE);
        context.getApplicationContext().startActivity(intent);
    }

    private ArrayList<String> locateISOs() {
        ArrayList<String> output = new ArrayList<>();
        try {
            boolean enabled = false;
            FileInputStream fis = new FileInputStream(CONFIG_FILE);
            Scanner sc = new Scanner(fis);
            while (sc.hasNext()) {
                String line = sc.nextLine();
                if (enabled && line.startsWith(FILENAME_PREFIX)) {
                    output.add(line.substring(line.indexOf('/')));
                }

                if (line.startsWith(RECENT_TAG)) {
                    enabled = true;
                } else if (line.startsWith("[")) {
                    enabled = false;
                }
            }
            sc.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }
}
