package com.lvonasek.pilauncher.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;

import net.didion.loopy.iso9660.ISO9660FileEntry;
import net.didion.loopy.iso9660.ISO9660FileSystem;

public class PSPPlatform  extends AbstractPlatform {

    private static final String CONFIG_FILE = "/mnt/sdcard/PSP/SYSTEM/ppsspp.ini";
    private static final String EMULATOR_PACKAGE = "org.ppsspp.ppsspp";
    private static final String FILENAME_PREFIX = "FileName";
    private static final String RECENT_TAG = "[Recent]";
    public static final String PACKAGE_PREFIX = "psp/";

    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        for (String iso : locateISOs()) {
            ApplicationInfo app = new ApplicationInfo();
            app.name = iso.substring(iso.lastIndexOf('/') + 1);
            app.packageName = PACKAGE_PREFIX + iso;
            output.add(app);
        }
        return output;
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        PackageManager pm = activity.getPackageManager();
        for (ApplicationInfo info : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (info.packageName.compareTo(EMULATOR_PACKAGE) == 0) {
                icon.setImageDrawable(info.loadIcon(pm));
                break;
            }
        }

        final File file = pkg2path(activity, app.packageName);
        file.getParentFile().mkdirs();
        if (file.exists()) {
            if (AbstractPlatform.updateIcon(icon, file, app.packageName)) {
                return;
            }
        }

        new Thread(() -> {
            ISO9660FileSystem discFs;
            String isoToRead = app.packageName.substring(PACKAGE_PREFIX.length());
            try {
                discFs = new ISO9660FileSystem(new File(isoToRead), true);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

            Enumeration es = discFs.getEntries();
            while (es.hasMoreElements()) {
                ISO9660FileEntry fileEntry = (ISO9660FileEntry) es.nextElement();
                if (fileEntry.getName().contains("ICON0.PNG")) {
                    if (saveStream(discFs.getInputStream(fileEntry), file)) {
                        activity.runOnUiThread(() -> AbstractPlatform.updateIcon(icon, file, app.packageName));
                    }
                }
            }
        }).start();
    }

    @Override
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        String path = app.packageName.substring(PACKAGE_PREFIX.length());
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + path), "*/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setPackage(EMULATOR_PACKAGE);
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
