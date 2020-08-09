package com.example.cabapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class customerLogin extends AppCompatActivity{

    FirebaseAuth mAuth = FirebaseAuth.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_login);

        Button login, register;
        final EditText email, password ;

        login = findViewById(R.id.customerLogin);
        register = findViewById(R.id.customerRegister);
        email = findViewById(R.id.customerEmail);
        password = findViewById(R.id.customerPassword);

        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String emailString, passwordString;

                emailString = email.getText().toString();
                passwordString = password.getText().toString();

                mAuth.createUserWithEmailAndPassword(emailString, passwordString).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(!task.isSuccessful()){

                            Toast.makeText(customerLogin.this, "Something went wrong", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            String userId = mAuth.getUid();
                            Toast.makeText(customerLogin.this, userId, Toast.LENGTH_SHORT).show();
                            DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child("customer").child(userId);
                            databaseReference.setValue(true);
                            updateUI();
                        }

                    }
                });

            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String emailString, passwordString;

                emailString = email.getText().toString();
                passwordString = password.getText().toString();

                mAuth.signInWithEmailAndPassword(emailString, passwordString).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {

                        if(!task.isSuccessful()){
                            Toast.makeText(customerLogin.this, "Something went wrong", Toast.LENGTH_SHORT).show();

                        }
                        else{
                            updateUI();
                        }
                    }
                });
            }
        });
    }

    public void updateUI(){

        Intent customerMapIntent = new Intent(this, customerMap.class);
        startActivity(customerMapIntent);
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = mAuth.getCurrentUser();
        if(user!=null)
            updateUI();
    }
}