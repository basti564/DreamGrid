package com.basti564.dreamgrid.ui;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.basti564.dreamgrid.ImageUtils;
import com.basti564.dreamgrid.MainActivity;
import com.basti564.dreamgrid.R;
import com.basti564.dreamgrid.SettingsProvider;
import com.basti564.dreamgrid.platforms.AbstractPlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

interface IconCallback {
    void setIconImage();
}

class IconTask extends AsyncTask {
    private ApplicationInfo currentApp;
    private ImageView imageView;
    private Drawable appIcon;

    @Override
    protected Object doInBackground(Object[] objects) {
        final AppsAdapter owner = (AppsAdapter) objects[0];
        final ApplicationInfo currentApp = (ApplicationInfo) objects[1];
        final MainActivity mainActivityContext = (MainActivity) objects[2];
        final AbstractPlatform appPlatform = AbstractPlatform.getPlatform(currentApp);
        imageView = (ImageView) objects[3];

        try {
            appIcon = appPlatform.loadIcon(mainActivityContext, currentApp);
        } catch (Resources.NotFoundException | PackageManager.NameNotFoundException e) {
            Log.e("DreamGrid", "Error loading icon for app: " + currentApp.packageName, e);
        }
        return null;
    }
    @Override
    protected void onPostExecute(Object result) {
        imageView.setImageDrawable(appIcon);
    }
}

public class AppsAdapter extends BaseAdapter{
    private static Drawable iconDrawable;
    private static File iconFile;
    private static String packageName;
    private static long lastClickTime;
    private final MainActivity mainActivityContext;
    private final List<ApplicationInfo> appList;
    private final boolean isEditMode;
    private final boolean showTextLabels;
    private final int itemScale;
    private final SettingsProvider settingsProvider;

    public AppsAdapter(MainActivity context, boolean editMode, int scale, boolean names) {
        mainActivityContext = context;
        isEditMode = editMode;
        showTextLabels = names;
        itemScale = scale;
        settingsProvider = SettingsProvider.getInstance(mainActivityContext);

        ArrayList<String> sortedGroups = settingsProvider.getAppGroupsSorted(false);
        ArrayList<String> sortedSelectedGroups = settingsProvider.getAppGroupsSorted(true);
        boolean isFirstGroupSelected = !sortedSelectedGroups.isEmpty() && !sortedGroups.isEmpty() && sortedSelectedGroups.get(0).compareTo(sortedGroups.get(0)) == 0;
        appList = settingsProvider.getInstalledApps(context, sortedSelectedGroups, isFirstGroupSelected);
    }

    private static class ViewHolder {
        LinearLayout layout;
        ImageView imageView;
        TextView textView;
        ProgressBar progressBar;
    }

    public int getCount() { return appList.size(); }

    public Object getItem(int position) {
        return appList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        final ApplicationInfo currentApp = appList.get(position);
        LayoutInflater layoutInflater = (LayoutInflater) mainActivityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (convertView == null) {
            // Create a new ViewHolder and inflate the layout
            convertView = layoutInflater.inflate(R.layout.lv_app, parent, false);
            holder = new ViewHolder();
            holder.layout = convertView.findViewById(R.id.layout);
            holder.imageView = convertView.findViewById(R.id.imageLabel);
            holder.textView = convertView.findViewById(R.id.textLabel);
            holder.progressBar = convertView.findViewById(R.id.progress_bar);

            // Set clipToOutline to true on imageView (Workaround for bug)
            holder.imageView.setClipToOutline(true);

            // Set size of items
            ViewGroup.LayoutParams params = holder.layout.getLayoutParams();
            params.width = itemScale;
            params.height = itemScale;
            holder.layout.setLayoutParams(params);

            convertView.setTag(holder);
        } else {
            // ViewHolder already exists, reuse it
            holder = (ViewHolder) convertView.getTag();
        }

        // set value into textview
        PackageManager packageManager = mainActivityContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mainActivityContext, currentApp.packageName, currentApp.loadLabel(packageManager));
        holder.textView.setText(name);
        holder.textView.setVisibility(showTextLabels ? View.VISIBLE : View.GONE);

        if (isEditMode) {
            // short click for app details, long click to activate drag and drop
            holder.layout.setOnTouchListener((view, motionEvent) -> {
                if ((motionEvent.getAction() == MotionEvent.ACTION_DOWN) ||
                        (motionEvent.getAction() == MotionEvent.ACTION_POINTER_DOWN)) {
                    packageName = currentApp.packageName;
                    lastClickTime = System.currentTimeMillis();
                    ClipData dragData = ClipData.newPlainText(name, name);
                    View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(view);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        view.startDragAndDrop(dragData, shadowBuilder, view, 0);
                    } else {
                        view.startDrag(dragData, shadowBuilder, view, 0);
                    }
                }
                return false;
            });

            // drag and drop
            holder.layout.setOnDragListener((view, event) -> {
                if (currentApp.packageName.compareTo(packageName) == 0) {
                    if (event.getAction() == DragEvent.ACTION_DRAG_STARTED) {
                        view.setVisibility(View.INVISIBLE);
                    } else if (event.getAction() == DragEvent.ACTION_DRAG_ENDED) {
                        mainActivityContext.reloadUI();
                    } else if (event.getAction() == DragEvent.ACTION_DROP) {
                        if (System.currentTimeMillis() - lastClickTime < 250) {
                            try {
                                showAppDetails(currentApp);
                            } catch (PackageManager.NameNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            mainActivityContext.reloadUI();
                        }
                    }
                    return event.getAction() != DragEvent.ACTION_DROP;
                }
                return true;
            });
        } else {
            holder.layout.setOnClickListener(view -> {
                holder.progressBar.setVisibility(View.VISIBLE);
                mainActivityContext.openApp(currentApp);
            });
            holder.layout.setOnLongClickListener(view -> {
                try {
                    showAppDetails(currentApp);
                } catch (PackageManager.NameNotFoundException e) {
                    throw new RuntimeException(e);
                }
                return false;
            });
        }

        // set application icon
        Log.i("DreamGrid", "loading icon for app: " + currentApp.packageName);

        holder.imageView.setImageDrawable(Drawable.createFromPath("@drawable/bkg_app"));
        new IconTask().execute(this, currentApp, mainActivityContext, holder.imageView);

        return convertView;
    }

    public void onIconReady() {
        Log.e("DreamGrid", "OnIconReady");

        // callback code goes here
    }
    // The callback interface

    // The class that takes the callback
//
//// Option 1:
//
//    class Callback implements MyCallback {
//        void callbackCall() {
//            // callback code goes here
//        }
//    }
//
//    worker.callback = new Callback();

    public void onImageSelected(String path, ImageView selectedImageView) {
        AbstractPlatform.clearIconCache();
        if (path != null) {
            Bitmap bitmap = ImageUtils.getResizedBitmap(BitmapFactory.decodeFile(path), 450);
            ImageUtils.saveBitmap(bitmap, iconFile);
            selectedImageView.setImageBitmap(bitmap);
        } else {
            selectedImageView.setImageDrawable(iconDrawable);
            AbstractPlatform.updateIcon(iconFile, packageName);
            //No longer sets icon here but that should be fine
        }
        mainActivityContext.reloadUI();
        this.notifyDataSetChanged(); // for real time updates
    }

    private void showAppDetails(ApplicationInfo currentApp) throws PackageManager.NameNotFoundException {
        // set layout
        Context context = mainActivityContext;
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setView(R.layout.dialog_app_details);
        AlertDialog appDetailsDialog = dialogBuilder.create();
        appDetailsDialog.getWindow().setBackgroundDrawableResource(R.drawable.bkg_dialog);
        appDetailsDialog.show();

        // info action
        appDetailsDialog.findViewById(R.id.info).setOnClickListener(view13 -> mainActivityContext.openAppDetails(currentApp.packageName));

        // set name
        PackageManager packageManager = mainActivityContext.getPackageManager();
        String name = SettingsProvider.getAppDisplayName(mainActivityContext, currentApp.packageName, currentApp.loadLabel(packageManager));
        final EditText appNameEditText = appDetailsDialog.findViewById(R.id.app_name);
        appNameEditText.setText(name);
        appDetailsDialog.findViewById(R.id.ok).setOnClickListener(view12 -> {
            settingsProvider.setAppDisplayName(context, currentApp, appNameEditText.getText().toString());
            mainActivityContext.reloadUI();
            appDetailsDialog.dismiss();
        });

        // load icon
        ImageView tempImage = appDetailsDialog.findViewById(R.id.app_icon);
        AbstractPlatform appPlatform = AbstractPlatform.getPlatform(currentApp);
        tempImage.setImageDrawable(appPlatform.loadIcon(mainActivityContext, currentApp));

        tempImage.setClipToOutline(true);

        tempImage.setOnClickListener(iconPickerView -> {
            iconDrawable = currentApp.loadIcon(packageManager);
            packageName = currentApp.packageName;
            iconFile = AbstractPlatform.packageToPath(mainActivityContext, currentApp.packageName);
            if (iconFile.exists()) {
                iconFile.delete();
            }
            mainActivityContext.setSelectedImageView(tempImage);
            ImageUtils.showImagePicker(mainActivityContext, MainActivity.PICK_ICON_CODE);
        });
    }

    public String getSelectedPackage() {
        return packageName;
    }
}
