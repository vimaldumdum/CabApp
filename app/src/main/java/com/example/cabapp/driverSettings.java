package com.example.cabapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class driverSettings extends AppCompatActivity {

    private Button confirm, back;
    private TextView nameText, phoneText, carText;
    private ImageView profileImage;

    private String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    private Uri profileImageUri;

    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_settings);

        confirm = findViewById(R.id.driverConfirm);
        back = findViewById(R.id.driverBack);

        nameText = findViewById(R.id.nameDriver);
        phoneText = findViewById(R.id.phoneDriver);
        carText = findViewById(R.id.carDriver);

        profileImage = findViewById(R.id.profilePictureDriver);

        databaseReference = FirebaseDatabase.getInstance().getReference().child("users").child("driver").child(userId);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                return;
            }
        });

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent, 101);
            }
        });

        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveDriverInfo();
                finish();
                return;
            }
        });

        getUserInfo();
    }

    public void saveDriverInfo() {

        Map map = new HashMap();
        String name = nameText.getText().toString().trim();
        String phone = phoneText.getText().toString().trim();
        String car = carText.getText().toString().trim();

        map.put("name", name);
        map.put("phone", phone);
        map.put("car", car);

        databaseReference.updateChildren(map);
        Toast.makeText(this, "info saved", Toast.LENGTH_SHORT).show();

        if (profileImageUri != null) {
            profileImage.setDrawingCacheEnabled(true);
            profileImage.buildDrawingCache();

            Bitmap bitmap = ((BitmapDrawable) profileImage.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);

            byte[] data = baos.toByteArray();

            final StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("profilePictures").child(userId);

            UploadTask uploadTask = storageReference.putBytes(data);

            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(driverSettings.this, "Image upload Failed", Toast.LENGTH_SHORT).show();
                }
            });
            uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            Uri downloadUri = uri;
                            String uriString = downloadUri.toString();
                            Map map = new HashMap();
                            map.put("profilePicture", uriString);
                            databaseReference.updateChildren(map);
                            Toast.makeText(driverSettings.this, "upload successful", Toast.LENGTH_SHORT).show();
                            Toast.makeText(driverSettings.this, uriString, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            });

        }


    }

    public void getUserInfo() {

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && snapshot.getChildrenCount() > 0) {

                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();

                    if (map.get("name") != null) {
                        nameText.setText(map.get("name").toString());
                    }
                    if (map.get("phone") != null) {
                        phoneText.setText(map.get("phone").toString());
                    }
                    if (map.get("car") != null) {
                        carText.setText(map.get("car").toString());
                    }
                    if (map.get("profilePicture") != null) {
                        Uri imageUri = Uri.parse(map.get("profilePicture").toString());
                        Glide.with(driverSettings.this).load(imageUri).into(profileImage);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            profileImageUri = data.getData();
            profileImage.setImageURI(profileImageUri);
        }
    }
}