package com.lvonasek.pilauncher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.widget.ImageView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;

public class IconDownloader {

    private static final HashMap<String, Drawable> cache = new HashMap<>();
    private static final HashSet<String> ignore = new HashSet<>();

    private static final String ICONS1_URL = "https://github.com/vKolerts/quest_icons/raw/master/450/";
    private static final String ICONS2_URL = "https://raw.githubusercontent.com/lvonasek/binary/master/QuestPiLauncher/icons/";

    public static void clearCache() {
        ignore.clear();
        cache.clear();
    }

    public static void setIcon(final Activity context, final ImageView imageView, final String pkg, final String name) {
        if (cache.containsKey(pkg)) {
            imageView.setImageDrawable(cache.get(pkg));
            return;
        }

        final File file = pkg2path(context, pkg);
        if (file.exists()) {
            Drawable drawable = Drawable.createFromPath(file.getAbsolutePath());
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
                cache.put(pkg, drawable);
                return;
            }
        }
        IconDownloader.downloadIcon(context, pkg, name, () -> {
            Drawable drawable = Drawable.createFromPath(file.getAbsolutePath());
            if (drawable != null) {
                imageView.setImageDrawable(drawable);
                cache.put(pkg, drawable);
            }
        });
    }

    private static boolean downloadFile(String url, File outputFile) {
        try {
            InputStream is = new URL(url).openStream();
            DataInputStream dis = new DataInputStream(is);

            int length;
            byte[] buffer = new byte[65536];
            FileOutputStream fos = new FileOutputStream(outputFile);
            while ((length = dis.read(buffer))>0) {
                fos.write(buffer, 0, length);
            }
            fos.flush();
            fos.close();

            if (outputFile.length() >= 64 * 1024) {
                Bitmap bitmap = BitmapFactory.decodeFile(outputFile.getAbsolutePath());
                if (bitmap != null) {
                    try {
                        fos = new FileOutputStream(outputFile);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                        fos.close();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void downloadIcon(final Activity context, String pkg, String name, final Runnable callback) {
        final File file = pkg2path(context, pkg);
        if (file.exists()) {
            return;
        }
        new Thread(() -> {
            String autogen = null;
            if (ignore.contains(file.getName())) {
                //ignored icon
            } else if (downloadFile(ICONS1_URL + pkg + ".jpg", file)) {
                context.runOnUiThread(callback::run);
            } else if (downloadFile(ICONS2_URL + pkg.toLowerCase(Locale.US) + ".jpg", file)) {
                context.runOnUiThread(callback::run);
            } else {
                int count = 0;
                File info = new File(context.getApplicationInfo().dataDir, "applab.info");
                if (downloadFile(ICONS2_URL + "applab.info", info)) {
                    try {
                        FileInputStream fis = new FileInputStream(info);
                        Scanner sc = new Scanner(fis);
                        while (sc.hasNext()) {
                            String line = sc.nextLine();
                            if (line.contains(name)) {
                                Scanner lsc = new Scanner(line);
                                autogen = lsc.next();
                                lsc.close();
                                count++;
                            }
                        }
                        sc.close();
                        fis.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (count == 0) {
                    Log.d("Missing icon", file.getName());
                    ignore.add(file.getName());
                } else if (count == 1) {
                    if (downloadFile(ICONS2_URL + autogen + ".jpg", file)) {
                        context.runOnUiThread(callback::run);
                    } else {
                        Log.d("Missing icon", file.getName());
                        ignore.add(file.getName());
                    }
                } else if (count >= 2) {
                    Log.d("Too many icons", file.getName());
                    ignore.add(file.getName());
                }
            }
        }).start();
    }

    public static File pkg2path(Context context, String pkg) {
        return new File(context.getApplicationInfo().dataDir, pkg + ".jpg");
    }
}
