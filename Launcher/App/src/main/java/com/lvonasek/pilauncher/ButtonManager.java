package com.lvonasek.pilauncher;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

public class ButtonManager extends AccessibilityService implements Runnable
{
    private boolean mRunning = false;

    public void onAccessibilityEvent(AccessibilityEvent event)
    {
    }

    public void onInterrupt() {}

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (!mRunning) {
                    AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                    int volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                    if (volume > 0) {
                        SharedPreferences.Editor e = pref.edit();
                        e.putInt(SettingsProvider.KEY_VOLUME_RESTORE, volume);
                        e.commit();
                    }
                    mRunning = true;
                    new Thread(this).start();
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                SharedPreferences.Editor e = pref.edit();
                e.putInt(SettingsProvider.KEY_VOLUME_RESTORE, -1);
                e.commit();
                mRunning = false;
            }
        }
        return super.onKeyEvent(event);
    }

    protected void onServiceConnected()
    {
        super.onServiceConnected();
    }

    public static void restoreVolume(Context context, boolean reset) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int volume = pref.getInt(SettingsProvider.KEY_VOLUME_RESTORE, -1);
        if (volume >= 0) {
            if (isAccesibilityInitialized(context)) {
                AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI);
            }
            if (reset) {
                SharedPreferences.Editor e = pref.edit();
                e.putInt(SettingsProvider.KEY_VOLUME_RESTORE, -1);
                e.commit();
            }
        }
    }

    public static boolean isAccesibilityInitialized(Context context)
    {
        try {
            android.provider.Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);

            String settingValue = android.provider.Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                return settingValue.contains(context.getPackageName());
            }
        } catch (android.provider.Settings.SettingNotFoundException e) {
            return false;
        }
        return false;
    }

    public static void requestAccessibility(Context context) {
        Intent localIntent = new Intent("android.settings.ACCESSIBILITY_SETTINGS");
        localIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(localIntent);
    }

    @Override
    public void run() {
        long timestamp = System.currentTimeMillis();
        while (mRunning) {
            try {
                Thread.sleep(10);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (System.currentTimeMillis() - timestamp > 2000) {
                MainActivity.reset(getApplicationContext());
                mRunning = false;
            }
        }
    }
}
