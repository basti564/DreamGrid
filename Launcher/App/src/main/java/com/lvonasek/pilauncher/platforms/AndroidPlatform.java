package com.lvonasek.pilauncher.platforms;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.widget.ImageView;

import java.util.ArrayList;

public class AndroidPlatform extends AbstractPlatform {
    @Override
    public ArrayList<ApplicationInfo> getInstalledApps(Context context) {
        PackageManager pm = context.getPackageManager();
        ArrayList<ApplicationInfo> output = new ArrayList<>();
        for (ApplicationInfo app : pm.getInstalledApplications(PackageManager.GET_META_DATA)) {
            if (!isVirtualRealityApp(app)) {
                output.add(app);
            }
        }
        return output;
    }

    @Override
    public void loadIcon(Activity activity, ImageView icon, ApplicationInfo app, String name) {
        icon.setImageDrawable(app.loadIcon(activity.getPackageManager()));
    }

    @Override
    public void runApp(Context context, ApplicationInfo app, boolean multiwindow) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (multiwindow) {
            context.getApplicationContext().startActivity(launchIntent);
        } else {
            context.startActivity(launchIntent);
        }
    }
}
