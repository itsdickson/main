package com.example.dickson.lighthausproject.AccountActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.example.dickson.lighthausproject.MainActivity;
import com.example.dickson.lighthausproject.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import static android.content.ContentValues.TAG;
import com.google.firebase.auth.FirebaseUser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by Junyang on 14/12/2017.
 */

public class LoginActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {
    static FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private EditText editEmail, editPass;
    private Button signUp, signIn;
    private CheckBox visibility;
    private ProgressDialog mProgressDialog;
    private String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
    private int validEmailFlag = 0;

    private static final int PERMISSION_REQUEST_CAMERA = 0;
    private static final int PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContextCompat.checkSelfPermission(LoginActivity.this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                    android.Manifest.permission.CAMERA)) {

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(LoginActivity.this,
                        new String[]{android.Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            }
        }

        if (ContextCompat.checkSelfPermission(LoginActivity.this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE)) {

                // Show an explanation to the user *asynchronously* — don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(LoginActivity.this,
                        new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);

            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (ContextCompat.checkSelfPermission(LoginActivity.this,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(LoginActivity.this,
                    android.Manifest.permission.CAMERA)) {

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(LoginActivity.this,
                        new String[]{android.Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
            }

        }


        editEmail = (EditText) findViewById(R.id.etEmail);
        editPass = (EditText) findViewById(R.id.etPassword);
        editPass.setTransformationMethod(new PasswordTransformationMethod());
        signUp = (Button) findViewById(R.id.signupBtn);
        signIn = (Button) findViewById(R.id.signinBtn);
        visibility = (CheckBox) findViewById(R.id.visibleBtn);

        visibility.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    editPass.setTransformationMethod(null);
                } else {
                    editPass.setTransformationMethod(new PasswordTransformationMethod());
                }
                editPass.setSelection(editPass.getText().length());
            }
        });

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null) {
            Intent i = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(i);
        }
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null) {
                    // User is signed in
                    Log.d(TAG, "User is signed in" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "User is signed out");
                }
            }
        };

        mProgressDialog = new ProgressDialog(this);

        signUp.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignUpActivity.class));
            }
        });

        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editEmail.getText().toString();
                String password = editPass.getText().toString();

                if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Please fill in email/password", Toast.LENGTH_SHORT).show();
                    return;
                }

                Pattern pattern = Pattern.compile(emailRegex);
                Matcher matcher = pattern.matcher(email);

                if (!matcher.matches()) {
                    Toast.makeText(getApplicationContext(), "The email address is invalid!", Toast.LENGTH_SHORT).show();
                    return;
                }
                else {
                    validEmailFlag = 1;
                }

                if (validEmailFlag == 1) {
                    mProgressDialog.setMessage("Logging in...");
                    mProgressDialog.show();

                    //authenticate user
                    mAuth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener(LoginActivity.this, new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    mProgressDialog.dismiss();
                                    if (!task.isSuccessful()) {
                                        try {
                                            throw task.getException();
                                        } catch (FirebaseAuthException e) {
                                            switch (e.getErrorCode()) {
                                                case "ERROR_USER_NOT_FOUND":
                                                    Toast.makeText(LoginActivity.this, "User not found.", Toast.LENGTH_SHORT).show();
                                                    break;
                                                case "ERROR_WRONG_PASSWORD":
                                                    Toast.makeText(LoginActivity.this, "Invalid password.", Toast.LENGTH_SHORT).show();
                                                    break;
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                        startActivity(intent);
                                        Toast.makeText(LoginActivity.this, "Successfully signed in.", Toast.LENGTH_SHORT).show();
                                        finish();
                                    }
                                }
                            });
                }
            }
        });
    }
}