package com.parassidhu.pdfpinner;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.*;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class MainActivity extends AppCompatActivity {

    private Button chooseBtn, pinFiles;
    private RadioButton blue,red;
    private ArrayList<String> docPaths = new ArrayList<>();
    private ArrayList<ListItem> listItems = new ArrayList<>();
    private int image;
    private RecyclerView file_list;
    private DataAdapter dataAdapter;
    private TextView info, pinInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        initViews();

    }

    private void choosePDF() {
        String[] pdf = {".pdf"};
        FilePickerBuilder.getInstance()
                .setActivityTheme(R.style.AppTheme)
                .addFileSupport("PDF", pdf)
                .enableDocSupport(false)
                .enableSelectAll(true)
                .showFolderView(false)
                .pickFile(MainActivity.this);
    }

    private void checkPerm() {
        hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.READ_EXTERNAL_STORAGE};
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        } else {
            choosePDF();
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
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
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            choosePDF();
        } else {
            Toast.makeText(this, "Permissions for accessing External Storage not granted! To choose a PDF file, it's required." +
                    "Please go to Settings->Apps to grant Storage permission.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
        chooseBtn = findViewById(R.id.chooseBtn);
        file_list = findViewById(R.id.file_list);
        file_list.setLayoutManager(new LinearLayoutManager(this));
        pinFiles = findViewById(R.id.pinFiles);
        blue = findViewById(R.id.blueIcon);
        red = findViewById(R.id.redIcon);
        image = R.drawable.pdf;
        chooseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPerm();
            }
        });
        info = findViewById(R.id.info);
        pinInfo = findViewById(R.id.pinInfo);

        pinFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < listItems.size(); i++) {
                    addShortcut(listItems.get(i).getPath(), listItems.get(i).getName());
                }
                Toast.makeText(MainActivity.this, "Process finished!", Toast.LENGTH_SHORT).show();
            }
        });
        blue.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(blue.isChecked())
                image=R.drawable.pdf;
                else image=R.drawable.pdf2;
            }
        });
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
            String name = docPaths.get(i);
            listItems.add(new ListItem(name, name.substring(name.lastIndexOf("/") + 1, name.lastIndexOf("."))));
            //   addShortcut(docPaths.get(i));
        }
        addDataToList(listItems);
    }

    private void addDataToList(ArrayList<ListItem> listItems) {

        dataAdapter = new DataAdapter(this, listItems);
        file_list.setAdapter(dataAdapter);
        if(isOreo())
            info.setVisibility(View.VISIBLE);
        pinInfo.setVisibility(View.VISIBLE);
    }

    private void addShortcut(String path1, String pdfName) {
        File file = new File(path1);

        if (file.exists()) {
            Uri path = Uri.fromFile(file);
            Intent shortcutIntent = new Intent(Intent.ACTION_VIEW);
            shortcutIntent.setDataAndType(path, "application/pdf");
            Intent addIntent = new Intent();
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, pdfName);
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this.getApplicationContext(),image));
            addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            getApplicationContext().sendBroadcast(addIntent);
        } else {
            Toast.makeText(this, "Some error occurred!", Toast.LENGTH_SHORT).show();
        }

        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(Uri.fromFile(file),"application/pdf");

        if (Build.VERSION.SDK_INT > 25) {
            ShortcutManager shortcutManager;
            shortcutManager = getSystemService(ShortcutManager.class);
            if(blue.isChecked()) {
                image = R.drawable.pdf;
                Toast.makeText(this, "blue", Toast.LENGTH_SHORT).show();
            }
            else {image=R.drawable.pdf2;
                Toast.makeText(this, "Red", Toast.LENGTH_SHORT).show();}
            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, pdfName)
                    .setShortLabel(pdfName)
                    .setLongLabel(pdfName)
                    .setIcon(Icon.createWithResource(this,image))
                    .setIntent(pdfIntent)
                    .build();

            shortcutManager.requestPinShortcut(shortcut,null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isOreo() {
        if (Build.VERSION.SDK_INT > 25)
            return true;
        else return false;
    }
}
