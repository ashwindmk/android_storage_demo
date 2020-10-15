package com.ashwin.android.storage.apptwo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "app-two";

    private static final int PERMISSION_REQUEST_CODE = 1024;
    private static final int PICKFILE_RESULT_CODE = 1025;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    private String getDirName() {
        return Environment.DIRECTORY_DOWNLOADS;
    }

    private String getFileName() {
        return "myfile1.txt";
    }

    private File getFile() {
        /**
         * Internal
         */
        // getDir
        // Absolute path: /data/user/0/com.ashwin.android.storage.appone/app_mydir/
        // Device path: /data/data/com.ashwin.android.storage.appone/app_mydir/
        //File dir = getDir("mydir", Context.MODE_PRIVATE);

        // getFilesDir

        // getCacheDir

        // API 21
        //File dir = getNoBackupFilesDir();

        // API 24
        //File dir = getDataDir();

        File dir = getObbDir();

        /**
         * External
         */
        // ExternalFilesDir
        // No permission required
        // Device path: /storage/emulated/0/Android/data/com.ashwin.android.storage.appone/files
        // User path: Cannot view
        //File dir = getExternalFilesDir(null);

        // (Deprecated in 10 (Q)) Environment.getExternalStorageDirectory: android:requestLegacyExternalStorage="true"
        // Permission required
        // Device path: /storage/emulated/0/
        // User path: Internal Storage/
        //File dir = Environment.getExternalStorageDirectory();

        // (Deprecated in 10 (Q)) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS): android:requestLegacyExternalStorage="true"
        // Permission required
        // Absolute path: /storage/emulated/0/Download/
        // Device path: /storage/self/primary/Download/
        // User path: Internal Storage/Download
        //File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        File file = new File(dir, getFileName());
        Log.w(TAG, "File | AbsolutePath: " + file.getAbsolutePath());
        return file;
    }

    public void permit(View view) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Write External Storage permission allows us to save files. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Permission Granted");
                } else {
                    Log.e(TAG, "Permission Denied");
                }
                break;
        }
    }

    public void check(View view) {
        Log.w(TAG, "check");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Uri contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL);
                String selection = MediaStore.MediaColumns.RELATIVE_PATH + " = ? AND " + MediaStore.MediaColumns.DISPLAY_NAME + " = ?";
                String[] selectionArgs = new String[]{getDirName() + "/", getFileName()};
                Cursor cursor = getContentResolver().query(contentUri, null, selection, selectionArgs, null);

                if (cursor.getCount() == 0) {
                    Log.e(TAG, "check | No record found");
                } else {
                    Uri uri = null;
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
                        uri = ContentUris.withAppendedId(contentUri, id);
                        break;
                    }
                    if (uri != null) {
                        Log.w(TAG, "check | File found");
                    } else {
                        Log.e(TAG, "check | File NOT found");
                    }
                }
            } else {
                File file = getFile();
                if (file.exists()) {
                    Log.w(TAG, "check | File found");
                } else {
                    Log.e(TAG, "check | File NOT found");
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "check | Exception while checking file", e);
        }
    }

    public void write(View view) {
        Log.w(TAG, "write");
        try {
            String text = (new Date()) + ": Hello world";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, getFileName());
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), contentValues);
                OutputStream os = getContentResolver().openOutputStream(uri);
                os.write(text.getBytes());
                os.close();
                Log.w(TAG, "write | uri: " + uri);  // E.g. content://media/external/file/1022
            } else {
                File file = getFile();
                if (file.exists()) {
                    Log.w(TAG, "write | File already exists, deleting...");
                    file.delete();
                }

                FileOutputStream os = null;
                os = new FileOutputStream(file);
                os.write(text.getBytes());
                os.close();
            }
            Log.w(TAG, "write | complete");
        } catch (Exception e) {
            Log.e(TAG, "write | Exception while writing to file", e);
        }
    }

    public void read(View view) {
        Log.w(TAG, "read");
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("text/plain");
                startActivityForResult(intent, PICKFILE_RESULT_CODE);
            } else {
                File file = getFile();
                try (BufferedReader br = new BufferedReader(new FileReader(file), 1024)) {
                    StringBuilder sb = new StringBuilder();
                    String s;
                    while ((s = br.readLine()) != null) {
                        sb.append(s);
                    }
                    Log.w(TAG, "read | content:\n\t" + sb);
                }
            }
        } catch (Throwable e) {
            Log.e(TAG, "read | File NOT found", e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data == null) {
            Log.e(TAG, "No file selected");
            return;
        }

        switch (requestCode) {
            case PICKFILE_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();  // E.g. content://com.android.providers.media.documents/document/document%3A1022
                        Log.w(TAG, "onActivityResult | uri: " + uri);
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                        try (BufferedReader br = new BufferedReader(inputStreamReader, 1024)) {
                            String s;
                            StringBuilder sb = new StringBuilder();
                            while ((s = br.readLine()) != null) {
                                sb.append(s);
                            }
                            Log.w(TAG, "onActivityResult | content:\n\t" + sb);
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "onActivityResult | Error while opening file", t);
                    }
                }
                break;
        }
    }

    public void delete(View view) {
        Log.w(TAG, "delete");
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                Log.w(TAG, "delete | TODO");
            } else {
                File file = getFile();
                if (file.delete()) {
                    Log.w(TAG, "delete | File deleted");
                } else {
                    Log.w(TAG, "delete | File NOT deleted");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "delete | Error in deleting file", e);
        }
    }
}