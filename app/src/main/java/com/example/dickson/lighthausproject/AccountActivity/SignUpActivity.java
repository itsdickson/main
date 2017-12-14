package com.example.dickson.lighthausproject.AccountActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.dickson.lighthausproject.MainActivity;
import com.example.dickson.lighthausproject.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

/**
 * Created by Junyang on 14/12/2017.
 */

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText editEmail, editPass;
    private Button signUp;
    private ProgressDialog mProgressDialog;
    private CheckBox visibility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        editEmail = (EditText) findViewById(R.id.etEmail);
        editPass = (EditText) findViewById(R.id.etPassword);
        editPass.setTransformationMethod(new PasswordTransformationMethod());
        signUp = (Button) findViewById(R.id.signupBtn);
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
        mProgressDialog = new ProgressDialog(this);

        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mProgressDialog.setMessage("Signing up!");
                mProgressDialog.show();
                String email = editEmail.getText().toString();
                String password = editPass.getText().toString();

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
                    return;
                }
                //create user
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                mProgressDialog.dismiss();
                                if (!task.isSuccessful()) {
                                    Toast.makeText(SignUpActivity.this, "Authentication failed." + task.getException(),
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                                    Toast.makeText(SignUpActivity.this, "Successfully signed up." ,Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                        });
            }
        });
    }
}

