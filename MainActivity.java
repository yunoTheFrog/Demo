/*
****************************************************************************
* Copyright(c) 2014 NXP Semiconductors                                     *
* All rights are reserved.                                                 *
*                                                                          *
* Software that is described herein is for illustrative purposes only.     *
* This software is supplied "AS IS" without any warranties of any kind,    *
* and NXP Semiconductors disclaims any and all warranties, express or      *
* implied, including all implied warranties of merchantability,            *
* fitness for a particular purpose and non-infringement of intellectual    *
* property rights.  NXP Semiconductors assumes no responsibility           *
* or liability for the use of the software, conveys no license or          *
* rights under any patent, copyright, mask work right, or any other        *
* intellectual property rights in or to any products. NXP Semiconductors   *
* reserves the right to make changes in the software without notification. *
* NXP Semiconductors also makes no representation or warranty that such    *
* application will be suitable for the specified use without further       *
* testing or modification.                                                 *
*                                                                          *
* Permission to use, copy, modify, and distribute this software and its    *
* documentation is hereby granted, under NXP Semiconductors' relevant      *
* copyrights in the software, without fee, provided that it is used in     *
* conjunction with NXP Semiconductor products(UCODE I2C, NTAG I2C).        *
* This  copyright, permission, and disclaimer notice must appear in all    *
* copies of this code.                                                     *
****************************************************************************
*/
package com.nxp.DINRailDemo.activities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.nxp.DINRailDemo.adapters.TabsAdapter;
import com.nxp.DINRailDemo.fragments.ConfigToolFragment;
import com.nxp.DINRailDemo.fragments.DiagToolFragment;
import com.nxp.DINRailDemo.fragments.FlashMemoryFragment;
import com.nxp.DINRailDemo.reader.Ntag_I2C_Demo;
import com.nxp.DINRailDemo.R;

public class MainActivity extends FragmentActivity {
    public final static String EXTRA_MESSAGE = "com.nxp.nfc_demo.MESSAGE";
    public final static int AUTH_REQUEST = 0;
    public static Ntag_I2C_Demo demo;
    private TabHost mTabHost;
    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;
    private PendingIntent mPendingIntent;
    private NfcAdapter mAdapter;
    public static String PACKAGE_NAME;
    private TextView filePath;
    private TextView dataRateCallback;
    private static ProgressDialog dialog;
    private static flashTask task;
    private boolean isAppFW = true;
    private int indexFW = 0;
    private byte[] bytesToFlash = null;
    private static Context mContext;
    private static String appVersion = "";
    private static Intent mIntent;
    // Current used password
    private static byte[] mPassword;

    public static Intent getmIntent() {
        return mIntent;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Application package name to be used by the AAR record
        PACKAGE_NAME = getApplicationContext().getPackageName();
        String languageToLoad = "en";
        Locale locale = new Locale(languageToLoad);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.locale = locale;
        getBaseContext().getResources().updateConfiguration(config,
                getBaseContext().getResources().getDisplayMetrics());
        setContentView(R.layout.activity_main);
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mTabsAdapter = new TabsAdapter(this, mTabHost, mViewPager);

        mTabsAdapter.addTab(
                mTabHost.newTabSpec("Zero Power Config").setIndicator(
                        getString(R.string.zeropowerconfig)), ConfigToolFragment.class,
                null);
        mTabsAdapter.addTab(
                mTabHost.newTabSpec("Diagnosis").setIndicator(
                        getString(R.string.diagnosis)), DiagToolFragment.class,
                null);

        mTabsAdapter.addTab(
                mTabHost.newTabSpec("Flash").setIndicator(
                        getString(R.string.Flash)), FlashMemoryFragment.class,
                null);

        mContext = getApplicationContext();

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
        }
        // Get App version
        appVersion = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
            appVersion = pInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        // Notifier to be used for the demo changing
        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
                if (demo.isReady()) {
                    demo.finishAllTasks();
                    if (tabId.equalsIgnoreCase("Zero Power Config") && demo.isConnected()) {
                        launchDemo(tabId);
                    }
                }
                mTabsAdapter.onTabChanged(tabId);
            }
        });


        // Initialize the demo in order to handle tab change events
        demo = new Ntag_I2C_Demo(null, this);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        setNfcForeground();
        checkNFC();
    }

    @SuppressLint("InlinedApi")
    private void checkNFC() {
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                new AlertDialog.Builder(this)
                        .setTitle("NFC not enabled")
                        .setMessage("Go to Settings?")
                        .setPositiveButton("Yes",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        if (android.os.Build.VERSION.SDK_INT >= 16) {
                                            startActivity(new Intent(android.provider.Settings.ACTION_NFC_SETTINGS));
                                        } else {
                                            startActivity(new Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS));
                                        }
                                    }
                                })
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog,
                                                        int which) {
                                        System.exit(0);
                                    }
                                }).show();
            }
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("No NFC available. App is going to be closed.")
                    .setNeutralButton("Ok",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog,
                                                    int which) {
                                    System.exit(0);
                                }
                            }).show();
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (mAdapter != null) {
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPassword = null;
        mIntent = null;
    }

    private byte[] readFileAssets(int indexFW) throws IOException {
        byte[] data = null;
        AssetManager assManager = getApplicationContext().getAssets();
        InputStream is = null;
        try {

            switch (indexFW) {
                case 0:
                    is = assManager.open("demo.bin");
                    break;
                case 1:
                    is = assManager.open("demo_fast.bin");
                    break;
                case 2:
                    is = assManager.open("demo_slow.bin");
                    break;
                case 3:
                    is = assManager.open("blink.bin");
                    break;
                default:
                    break;
            }
            int byteCount = is.available();
            data = new byte[byteCount];
            is.read(data, 0, byteCount);
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return data;
    }

    private byte[] readFileMemory(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            return null;
        }

        // Open file
        RandomAccessFile f = new RandomAccessFile(file, "r");
        try {
            // Get and check length
            long longlength = f.length();
            int length = (int) longlength;
            if (length != longlength) {
                throw new IOException("File size >= 2 GB");
            }
            // Read file and return data
            byte[] data = new byte[length];
            f.readFully(data);
            return data;
        } finally {
            f.close();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if ( demo != null
                && demo.isReady()) {
            String currTab = mTabHost.getCurrentTabTag();
            if (currTab == "Flash") {
                try {
                    if (isAppFW) {
                        bytesToFlash = readFileAssets(indexFW);
                    } else {
                        String path = ((TextView) findViewById(R.id.file_path)).getText().toString();
                        bytesToFlash = readFileMemory(path);
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                    // Set bytesToFlash to null so that the task is not started
                    bytesToFlash = null;
                }
                if (bytesToFlash == null || bytesToFlash.length == 0) {
                    Toast.makeText(mContext, "Error could not open File",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            launchDemo(currTab);
        }
    }


    @Override
    protected void onNewIntent(Intent nfc_intent) {
        super.onNewIntent(nfc_intent);

        doProcess(nfc_intent);
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {

            case R.id.action_about:
                showAboutDialog();
                return true;
            case R.id.action_feedback:
                sendFeedback();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void doProcess(Intent nfc_intent) {
        mIntent = nfc_intent;
        Tag tag = nfc_intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        demo = new Ntag_I2C_Demo(tag, this);
        if (demo.isReady()) {
            // Retrieve Auth Status before doing any operation
            //mAuthStatus = obtainAuthStatus();
            String currTab = mTabHost.getCurrentTabTag();
            launchDemo(currTab);
        }
    }

    private void launchDemo(String currTab) {

        // ===========================================================================
        // Zero Power Configuration
        // ===========================================================================
        if (currTab.equalsIgnoreCase("Zero Power Config")) {
            try {
                demo.LINConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // ===========================================================================
        // Diagnosis
        // ===========================================================================
        if (currTab.equalsIgnoreCase("Diagnosis")) {
            try {
                demo.LINDiag();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // ===========================================================================
        // Flash
        // ===========================================================================
        if (currTab.equalsIgnoreCase("Flash")) {

            try {
                isAppFW = FlashMemoryFragment.isAppFW();
                if (isAppFW) {
                    indexFW = FlashMemoryFragment.getIndexFW();
                    bytesToFlash = readFileAssets(indexFW);
                } else {
                    String path = ((TextView) findViewById(R.id.file_path)).getText().toString();
                    bytesToFlash = readFileMemory(path);
                }
            } catch (IOException e) {
                e.printStackTrace();

                // Set bytesToFlash to null so that the task is not started
                bytesToFlash = null;
            }

            if (bytesToFlash == null || bytesToFlash.length == 0) {
                Toast.makeText(mContext, "Error could not open File",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Launch the thread
            task = new flashTask();
            task.execute();
        }
    }

    private class flashTask extends AsyncTask<Intent, Integer, Boolean> {
        private long timeToFlashFirmware = 0;
        public ProgressDialog dialog;

        @Override
        protected void onPostExecute(Boolean success) {
            // Inform the user about the task completion
            flashCompleted(success, bytesToFlash.length, timeToFlashFirmware);

            // Action completed
            dialog.dismiss();
        }

        @Override
        protected Boolean doInBackground(Intent... nfc_intent) {
            long RegTimeOutStart = System.currentTimeMillis();

            // Flash the new firmware
            boolean success = demo.Flash(bytesToFlash);

            // Flash firmware time statistics
            timeToFlashFirmware = System.currentTimeMillis() - RegTimeOutStart;

            return success;
        }

        @Override
        protected void onPreExecute() {
            // Show the progress dialog on the screen to inform about the action
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setTitle("Flashing");
            dialog.setMessage("Writing sector ...");
            dialog.setCancelable(false);
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setProgress(0);
            dialog.show();
        }

    }

    public void flashCompleted(boolean success, int bytes, long time) {
        if (success) {
            Toast.makeText(MainActivity.this, "Flash Completed", Toast.LENGTH_SHORT)
                    .show();
            String readTimeMessage = "";

            // Transmission Results
            readTimeMessage = readTimeMessage.concat("Flash Firmware\n");
            readTimeMessage = readTimeMessage.concat("Speed (" + bytes + " Byte / "
                    + time + " ms): "
                    + String.format("%.0f", bytes / (time / 1000.0))
                    + " Bytes/s");

            // Make the board input layout visible
            ((LinearLayout) findViewById(R.id.layoutFlashStatistics)).setVisibility(View.VISIBLE);
            dataRateCallback = (TextView) findViewById(R.id.flashfwdata_datarateCallback);
            dataRateCallback.setText(readTimeMessage);
        } else
            Toast.makeText(MainActivity.this, "Error during memory flash",
                    Toast.LENGTH_SHORT).show();
    }

    public static void setFLashDialogMax(int max) {
        task.dialog.setMax(max);
    }

    public static void updateFLashDialog() {
        task.dialog.incrementProgressBy(1);
    }


    /**
     * NDEF Demo execution is launched from its fragmend.
     */


    public void sendFeedback() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT,
                this.getString(R.string.email_titel_feedback));
        intent.putExtra(Intent.EXTRA_TEXT, "Android Version: "
                + android.os.Build.VERSION.RELEASE + "\nManufacurer: "
                + android.os.Build.MANUFACTURER + "\nModel: "
                + android.os.Build.MODEL + "\nBrand: " + android.os.Build.BRAND
                + "\nDisplay: " + android.os.Build.DISPLAY + "\nProduct: "
                + android.os.Build.PRODUCT + "\nIncremental: "
                + android.os.Build.VERSION.INCREMENTAL);
        intent.setData(Uri.parse(this.getString(R.string.support_email)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
    }

    public void showAboutDialog() {
        Intent intent = null;
        intent = new Intent(this, VersionInfoActivity.class);
        if (MainActivity.mIntent != null)
            intent.putExtras(MainActivity.mIntent);
        startActivity(intent);
    }

    public void setNfcForeground() {
        // Create a generic PendingIntent that will be delivered to this
        // activity. The NFC stack will fill
        // in the intent with the details of the discovered tag before
        // delivering it to this activity.
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(
                getApplicationContext(), getClass())
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    // ===========================================================================
    // NTAG I2C Plus getters and setters
    // ===========================================================================


}
