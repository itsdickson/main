package com.example.dickson.lighthausproject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dickson.lighthausproject.AccountActivity.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private Camera mCamera;
    private CameraPreview mPreview;
    private ImageView capturedImage;
    private FirebaseAuth mAuth;

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int MEDIA_TYPE_IMAGE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Instantiating XML elements for usage
        Button startBtn = (Button) findViewById(R.id.startBtn);
        Button focusBtn = (Button) findViewById(R.id.focusBtn);
        Button sendBtn = (Button) findViewById(R.id.sendBtn);
        TextView idPhoto = (TextView) findViewById(R.id.idPhoto);


        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = (FrameLayout) findViewById(R.id.cameraPreview);
        preview.addView(mPreview);
        preview.setVisibility(View.VISIBLE);
        mPreview.setVisibility(View.INVISIBLE);

        // Initialisation of displayed image
        capturedImage = (ImageView) findViewById(R.id.imageCaptured);
        capturedImage.setVisibility(View.INVISIBLE);

        idPhoto.setText("ID of image goes here");
        capturedImage.setImageResource(R.drawable.ic_menu_camera);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (mBluetoothAdapter == null) {
            // Checks if device supports Bluetooth
            Log.i("info:", "Bluetooth not supported");
        } else {
            // Checks if device is currently paired with other devices
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                checkPairedStatus();
            }
        }

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateCamera();
            }
        });

        focusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                focusCamera();
                mCamera.takePicture(null, null, mPicture);
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPreview.setVisibility(View.INVISIBLE);
                capturedImage.setVisibility(View.INVISIBLE);
            }
        });
    }

    // Condition where app's bluetooth is not turned on
    // After Alert Dialog to turn on bluetooth, upon resume of lifecycle, app checks for pairing
    @Override
    public void onResume() {
        super.onResume();
        if (mBluetoothAdapter == null) {
            Log.i("Info", "Bluetooth not supported");
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                checkPairedStatus();
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseCamera();              // release the camera immediately on pause event
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_signOut) {
            mAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void checkPairedStatus() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        System.out.println("size " + mBluetoothAdapter.getBondedDevices().size());
        if (pairedDevices == null || pairedDevices.size() == 0) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
            builder1.setMessage("No paired devices found, Proceed to pair?");
            builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                    startActivity(settingsIntent);
                }
            });
            AlertDialog alert1 = builder1.create();
            alert1.show();
        } else {
            ArrayList<BluetoothDevice> list = new ArrayList<>();
            list.addAll(pairedDevices);
        }
    }

    public void activateCamera() {
        mPreview.setVisibility(View.VISIBLE);
        capturedImage.setVisibility(View.INVISIBLE);
    }

    private void releaseCamera() {
        if (mCamera != null){
            Log.i("Log", "Camera Released");
            mCamera.release();        // release the camera for other applications
        }
    }

    /** A safe way to get an instance of the Camera object */
    public static Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open();
        } catch (Exception e) {
            // Camera is not available
        }
        return c;
    }

    Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            if (bitmap == null){
                Toast.makeText(MainActivity.this, "Empty", Toast.LENGTH_LONG).show();
                return;
            }
            capturedImage.setImageBitmap(scaleDownBitmapImage(bitmap, 300, 200));
            capturedImage.setVisibility(View.VISIBLE);
            mPreview.setVisibility(View.INVISIBLE);

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if (pictureFile == null){
                Log.d("Debug", "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.i("Log", "File Created");
            } catch (FileNotFoundException e) {
                Log.d("Debug", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("Debug", "Error accessing file: " + e.getMessage());
            }
        }
    };

    private void focusCamera() {
        Camera.Parameters params = mCamera.getParameters();
        if (params.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        mCamera.setParameters(params);
        // Take picture immediately after focus
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mCamera.takePicture(null, null, mPicture);
                    Log.i("Info", "Photo Taken!");
                }
            }
        });
    }

    private Bitmap scaleDownBitmapImage(Bitmap bitmap, int newWidth, int newHeight){
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
        Bitmap rotatedBitmap = Bitmap.createBitmap(resizedBitmap, 0, 0, newWidth, newHeight, matrix, true);
        return rotatedBitmap;
    }

    /** Create a file Uri for saving an image or video */
    private static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "LightHausProject");
        Log.i("Log", "Photo Saved!");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("LightHausProject", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }
}
