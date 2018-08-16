package com.parassidhu.pdfpin;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class MainActivity extends AppCompatActivity {

    public static final String SHORTCUT_VALUE = "yes";
    public static final String SHORTCUT_KEY = "shortcut";
    public static final String SHORTCUT_ID = "id1";

    private Button chooseBtn, pinFiles, btnPDF1, btnPDF2;
    private RadioButton blue;
    private ArrayList<String> docPaths = new ArrayList<>();
    private ArrayList<ListItem> listItems = new ArrayList<>();
    private int image, num = 0;
    private RecyclerView file_list;
    private DataAdapter dataAdapter;
    private TextView info, pinInfo, centerInfo, dev;
    private RadioGroup radios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initViews();
        initAds();
        pinDynamicShortcut();

        // Check if the app is opened using long-press shortcut menu
        try {
            Intent intent = getIntent();
            if (intent.getExtras() != null) {
                if (intent.getExtras().getString(SHORTCUT_KEY).equals(SHORTCUT_VALUE))
                    chooseBtn.callOnClick();
            }
        } catch (Exception e) {
        }
    }

    //Shows the choose files picker
    private void choosePDF() {
        String[] pdf = {".pdf"};
        FilePickerBuilder.getInstance()
                .addFileSupport("PDF", pdf)
                .setActivityTheme(R.style.LibAppTheme)
                .enableDocSupport(false)
                .enableSelectAll(true)
                .pickFile(MainActivity.this);
    }

    private void checkPerm() {
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            choosePDF();
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            choosePDF();
        } else {
            Toast.makeText(this, "Permissions for accessing External Storage not granted! To choose a PDF file, it's required." +
                    "Please go to Settings->Apps to grant Storage permission.", Toast.LENGTH_SHORT).show();
        }
    }

    //Initializes all the mess. btnPDFx represents both icons.
    private void initViews() {
        chooseBtn = findViewById(R.id.chooseBtn);
        file_list = findViewById(R.id.file_list);
        file_list.setLayoutManager(new LinearLayoutManager(this));
        pinFiles = findViewById(R.id.pinFiles);
        blue = findViewById(R.id.blueIcon);
        image = R.drawable.pdf;

        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPerm();
            }
        });

        info = findViewById(R.id.info);
        pinInfo = findViewById(R.id.pinInfo);
        radios = findViewById(R.id.radios);
        btnPDF1 = findViewById(R.id.btnPDF1);
        btnPDF2 = findViewById(R.id.btnPDF2);
        centerInfo = findViewById(R.id.centerInfo);
        dev = findViewById(R.id.dev);
        toggle(0);

        pinFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isOreo()) {
                    for (int i = 0; i < listItems.size(); i++) {
                        addShortcut(listItems.get(i).getPath(), listItems.get(i).getName());
                    }
                    Toast.makeText(MainActivity.this, "Success!", Toast.LENGTH_SHORT).show();
                } else {
                    oneByOne();
                }
            }
        });

        blue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (blue.isChecked())
                    image = R.drawable.pdf;
                else image = R.drawable.pdf2;
            }
        });
    }

    private void oneByOne() {
        // Could've used For-Loop but purposefully didn't do so
        if (num < listItems.size() - 1) {
            if (listItems.size() > 1) {
                pinFiles.setText(R.string.pin_next);
                addShortcutInOreo(listItems.get(num).getPath(), listItems.get(num).getName());
                num++;
            } else {
                addShortcutInOreo(listItems.get(num).getPath(), listItems.get(num).getName());
                successMessage();
            }
        } else if (num == listItems.size() - 1) {
            pinFiles.setText("Pin File(s)");
            addShortcutInOreo(listItems.get(num).getPath(), listItems.get(num).getName());
            num = 0;
            successMessage();
        }
    }

    private void successMessage() {
        Toast.makeText(this, "All selected files have been pinned!", Toast.LENGTH_SHORT).show();
    }

    //Custom toggle to set views' visibility
    private void toggle(int val) {
        if (val == 0) {
            radios.setVisibility(View.GONE);
            btnPDF1.setVisibility(View.GONE);
            btnPDF2.setVisibility(View.GONE);
            pinFiles.setVisibility(View.GONE);
            centerInfo.setVisibility(View.VISIBLE);
            dev.setVisibility(View.GONE);
        } else {
            radios.setVisibility(View.VISIBLE);
            btnPDF1.setVisibility(View.VISIBLE);
            btnPDF2.setVisibility(View.VISIBLE);
            pinFiles.setVisibility(View.VISIBLE);
            centerInfo.setVisibility(View.GONE);
            dev.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case FilePickerConst.REQUEST_CODE_DOC:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    docPaths = new ArrayList<>();
                    docPaths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                }
                break;
        }

        listItems.clear();
        for (int i = 0; i < docPaths.size(); i++) {
            String path = docPaths.get(i);
            // Example: root/DCIM/Pictures/IMG_8004.JPG
            String name = path.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."));
            listItems.add(new ListItem(path, name));
        }
        addDataToList(listItems);
    }

    //Adds the selected PDFs to RecyclerView
    private void addDataToList(ArrayList<ListItem> listItems) {
        num = 0;
        pinFiles.setText("Pin File(s)");
        dataAdapter = new DataAdapter(this, listItems);
        file_list.setAdapter(dataAdapter);

        // Show Oreo specific information
        if (isOreo())
            info.setVisibility(View.VISIBLE);

        pinInfo.setVisibility(View.VISIBLE);

        if (listItems.size() != 0)
            toggle(1);
        else {
            toggle(0);
            info.setVisibility(View.GONE);
            pinInfo.setVisibility(View.GONE);
            dev.setVisibility(View.GONE);
        }
    }

    private void addShortcut(String path1, String pdfName) {
        File file = new File(path1);
        if (file.exists()) {
            Uri path = Uri.fromFile(file);
            Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
            shortcutIntent.setDataAndTypeAndNormalize(path, "application/pdf");

            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, pdfName);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this.getApplicationContext(), image));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            getApplicationContext().sendBroadcast(addIntent);
        } else {
            errorMessage(null);
        }
    }

    private void addShortcutInOreo(String path1, String pdfName) {
        try {
            File file = new File(path1);
            Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
            pdfIntent.setDataAndType(Uri.fromFile(file), "application/pdf");

            if (Build.VERSION.SDK_INT > 25) {
                ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

                // Check which icon is to be pinned
                if (blue.isChecked())
                    image = R.drawable.pdf;
                else image = R.drawable.pdf2;

                // Create a shortcut
                ShortcutInfo shortcut = new ShortcutInfo.Builder(this, pdfName)
                        .setShortLabel(pdfName)
                        .setLongLabel(pdfName)
                        .setIcon(Icon.createWithResource(this, image))
                        .setIntent(pdfIntent)
                        .build();

                shortcutManager.requestPinShortcut(shortcut, null);
            }
        } catch (Exception e) {
            errorMessage(e);
        }
    }

    private void errorMessage(Exception e) {
        if (e != null)
            Toast.makeText(this, "Some error occurred: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        else Toast.makeText(this, "Some error occurred!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // Rate and Review is clicked
        if (id == R.id.action_settings) {
            Uri uri = Uri.parse("market://details?id=" + getApplicationContext().getPackageName());
            Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            try {
                startActivity(goToMarket);
            } catch (ActivityNotFoundException e) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://play.google.com/store/apps/details?id=" + getApplicationContext().getPackageName())));
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isOreo() {
        if (Build.VERSION.SDK_INT > 25)
            return true;
        else return false;
    }

    private void initAds() {
        MobileAds.initialize(this, BuildConfig.BANNER_KEY);

        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    // This is a Nougat specific shortcut which is shown on long press of app shortcut
    private void pinDynamicShortcut() {
        // If it's Nougat
        if (Build.VERSION.SDK_INT > 24) {
            // Get instance of ShortcutManager Service
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            Intent intent = new Intent(Intent.ACTION_MAIN, Uri.EMPTY, this, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

            // This extra will be later used to check if there exists a shortcut or no
            intent.putExtra(SHORTCUT_KEY, SHORTCUT_VALUE);

            // Create a shortcut
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, SHORTCUT_ID)
                    .setShortLabel("Choose File(s)")
                    .setIcon(Icon.createWithResource(this, R.drawable.choose_files))
                    .setIntent(intent)
                    .build();

            // Ask the service to pin the shortcut
            shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
        }
    }
}
