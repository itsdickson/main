package com.example.dickson.lighthausproject;

import android.app.ProgressDialog;
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
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.dickson.lighthausproject.AccountActivity.LoginActivity;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;

import static android.content.ContentValues.TAG;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private String BLUETOOTH_SETTINGS = "bluetooth";
    private String PERMISSIONS_SETTINGS = "permissions";

    private Camera mCamera = null;
    private SurfaceView mPreview = null;
    private SurfaceHolder mPreviewHolder = null;
    private ImageView capturedImage;
    private EditText idPhoto;
    static FirebaseAuth mAuth;
    private StorageReference mStorage;
    private ProgressDialog mProgressDialog;
    private boolean isCameraActivated;
    private boolean isCameraTurnedOn;
    private boolean isCameraConfigured = false;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static int flag = 0;

    public static File tempFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        isCameraActivated = false;
        isCameraTurnedOn = false;

        mProgressDialog = new ProgressDialog(this);
        mStorage = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        //Instantiating XML elements for usage
        Button startBtn = (Button) findViewById(R.id.startBtn);
        Button focusBtn = (Button) findViewById(R.id.focusBtn);
        Button sendBtn = (Button) findViewById(R.id.sendBtn);

        mPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        mPreviewHolder = mPreview.getHolder();
        mPreviewHolder.addCallback(surfaceCallback);
        mPreviewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        // Initialisation of displayed image
        capturedImage = (ImageView) findViewById(R.id.imageCaptured);
        capturedImage.setVisibility(View.INVISIBLE);
        idPhoto = (EditText) findViewById(R.id.idPhoto);
        idPhoto.setVisibility(View.INVISIBLE);

        capturedImage.setImageResource(R.drawable.ic_menu_camera);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBtIntent);
        }

        checkPairedStatus();

        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCamera == null) {
                    Log.i("LOG", "Camera is not on");
                    Toast toast = Toast.makeText(MainActivity.this, "Please allow camera permissions from settings", Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0,0);
                    toast.show();
                    return;
                }
                if (!isCameraActivated) {
                    Log.i("LOG", "Camera activating...");
                    activateCamera();
                }

                if (!isCameraTurnedOn) {
                    Log.i("LOG", "Turning on Camera...");
                    turnCamera();
                }
                flag = 0;
            }
        });

        focusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCameraTurnedOn) {
                    Log.i("LOG", "Focusing Camera...");
                    focusCamera();
//                    mCamera.takePicture(null, null, mPicture);
                    flag = 1;
                }
            }
        });

        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (idPhoto.getText().toString().isEmpty()) {
                    Toast.makeText(MainActivity.this, "Please input photo id", Toast.LENGTH_SHORT).show();
                } else {
                    if (flag == 0) {
                        Toast.makeText(MainActivity.this, "No Image Captured", Toast.LENGTH_SHORT).show();
                    } else {
                        mPreview.setVisibility(View.INVISIBLE);
                        capturedImage.setVisibility(View.INVISIBLE);
                        mProgressDialog.setMessage("Uploading...");
                        mProgressDialog.show();
                        Uri file = Uri.fromFile(tempFile);
                        if (mAuth.getCurrentUser() != null) {
                            String userDetails = mAuth.getCurrentUser().getEmail();
                            StorageReference filepath = mStorage.child(userDetails).child(idPhoto.getText().toString());
                            filepath.putFile(file).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(MainActivity.this, "Upload Done!", Toast.LENGTH_SHORT).show();
                                    mProgressDialog.dismiss();
                                    idPhoto.setVisibility(View.INVISIBLE);
                                    flag = 0;
                                    isCameraActivated = false;
                                    isCameraTurnedOn = false;
                                }
                            });
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isCameraActivated) {
            Log.i("LOG", "Activating Camera: onResume");
            activateCamera();
        }
        mPreview.setVisibility(View.INVISIBLE);
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
            openSettings(BLUETOOTH_SETTINGS);
            return true;
        }
        if(id == R.id.permissions) {
            openSettings(PERMISSIONS_SETTINGS);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        if (isCameraTurnedOn) {
            mCamera.stopPreview();
        }
        releaseCamera();              // release the camera immediately on pause event
        mCamera = null;
        isCameraTurnedOn = false;

        super.onPause();
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {
            Intent galleryIntent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(galleryIntent);

        } else if (id == R.id.nav_signOut) {
            mAuth.getInstance().signOut();
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
        }


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void openSettings(String setting) {
        switch (setting) {
            case "bluetooth":
                Intent intentOpenBluetoothSettings = new Intent();
                intentOpenBluetoothSettings.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(intentOpenBluetoothSettings);
                break;
            case "permissions":
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + getPackageName()));
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
        }
    }

    public void checkPairedStatus() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        AlertDialog.Builder builder1 = new AlertDialog.Builder(this);
        builder1.setMessage("Proceed to pair new devices?");
        builder1.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                Intent settingsIntent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
                startActivity(settingsIntent);
            }
        });
        builder1.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog alert1 = builder1.create();
        alert1.show();
        ArrayList<BluetoothDevice> list = new ArrayList<>();
        list.addAll(pairedDevices);
    }

    private void activateCamera() {
        Log.i("Log", "Camera Activated");

        mPreview.setVisibility(View.INVISIBLE);
        capturedImage.setVisibility(View.INVISIBLE);
        mCamera = getCameraInstance();
        isCameraActivated = true;
    }

    private void turnCamera() {
        mPreview.setVisibility(View.VISIBLE);
        capturedImage.setVisibility(View.INVISIBLE);
        isCameraTurnedOn = true;
    }

    private void releaseCamera() {
        if (mCamera != null){
            Log.i("Log", "Camera Released");
            mCamera.release();        // release the camera for other applications
        }
        mPreview.setVisibility(View.INVISIBLE);
        isCameraActivated = false;
        isCameraTurnedOn = false;
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
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            capturedImage.setImageBitmap(rotatedBitmap);
            capturedImage.setVisibility(View.VISIBLE);
            System.out.println("hey6: " + bitmap.getHeight() + " " + bitmap.getWidth());
            mPreview.setVisibility(View.INVISIBLE);

            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            idPhoto.setVisibility(View.VISIBLE);


            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , bos);
            byte[] bitmapdata = bos.toByteArray();

            tempFile = pictureFile;

            scanMedia(pictureFile);
            if (pictureFile == null){
                Log.d("Debug", "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(bitmapdata);
                fos.flush();
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
        // Take picture immediately after focus
        mCamera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if (success) {
                    mCamera.takePicture(null, null, mPicture);
                    Log.i("Log", "Photo Taken!");
                    isCameraTurnedOn = false;
                } else {
                    focusCamera();
                    Log.i("Log", "Retaking Photo...");
                }
            }
        });
    }

    private void scanMedia(File file) {
        Uri uri = Uri.fromFile(file);
        Intent scanFileIntent = new Intent(
                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }

    /** Create a File for saving an image or video */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "LightHausProject");
        Log.i("Log", "Photo Saved!");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("LightHausProject", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mPreviewHolder.getSurface() == null){
                // preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e){
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or
            // reformatting changes here

            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
                initPreview(width, height);
                mCamera.startPreview();

            } catch (Exception e){
                Log.d(TAG, "Error starting camera preview: " + e.getMessage());
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
        }
    };

    private void initPreview(int width, int height) {
        if (mCamera != null && mPreviewHolder.getSurface() != null) {
            try {
                mCamera.setPreviewDisplay(mPreviewHolder);
                mCamera.setDisplayOrientation(90);
            } catch (Throwable t) {
            }

            if (!isCameraConfigured) {
                Camera.Parameters parameters = mCamera.getParameters();
                Camera.Size size = getOptimalPreviewSize(width, height, parameters);

                if (size != null) {
                    System.out.println("test : " + size.width + " " + size.height);
                    parameters.setPreviewSize(size.width, size.height);
                    if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        Log.i("Info", "AUTO ACTIVATED");
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    } else if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        Log.i("Info", "CONTINUOUS_PICTURE ACTIVATED");
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    }
                    mCamera.setParameters(parameters);
                    double ratio = size.width*1.0 / size.height;
                    System.out.println("size:" + size.width + " " + size.height + " " + ratio);

                    int displayWidth = getWindowManager().getDefaultDisplay().getWidth();
                    System.out.println("size:" + (int)(displayWidth*ratio) + " " + (displayWidth) );

                    RelativeLayout.LayoutParams testLayout = new RelativeLayout.LayoutParams(displayWidth, (int)(displayWidth*ratio));
                    testLayout.addRule(RelativeLayout.CENTER_HORIZONTAL);
                    testLayout.addRule(RelativeLayout.CENTER_VERTICAL);
//
                    mPreview.setLayoutParams(testLayout);
                    System.out.println("hey3: " + mPreview.getLayoutParams().width + " " + mPreview.getLayoutParams().height);
                    isCameraConfigured = true;
                }
            }
        }
    }

    private Camera.Size getOptimalPreviewSize(int width, int height,
                                              Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result=size;
                }
                else {
                    int resultArea=result.width * result.height;
                    int newArea=size.width * size.height;

                    if (newArea > resultArea) {
                        result=size;
                    }
                }
            }
        }

        System.out.println("result: " + result.width + " " + result.height);

        return(result);
    }
}