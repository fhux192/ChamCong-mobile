package com.example.timekeeping;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ManagerRegister extends AppCompatActivity {

    // =============== FIELDS ===============
    private static final String TAG = "ManagerRegister";
    private TextInputEditText editTextEmail, editTextName;
    private Spinner spinnerCompany;
    private MaterialCardView buttonRegister;
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private List<String> companyList = new ArrayList<>();
    private FirebaseFirestore firestore;

    // =============== ACTIVITY LIFECYCLE ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_register);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // INITIALIZE FIREBASE AUTH AND FIRESTORE
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // BIND UI COMPONENTS
        editTextEmail = findViewById(R.id.email);
        editTextName = findViewById(R.id.name);
        spinnerCompany = findViewById(R.id.spinnerCompany);
        buttonRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);
        Spinner spinner = findViewById(R.id.spinnerCompany);

        // CREATE CUSTOM SPINNER ADAPTER
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, companyList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // LOAD COMPANY LIST
        loadCompanyList();

        // SET REGISTER BUTTON CLICK LISTENER
        buttonRegister.setOnClickListener(view -> {
            String name = String.valueOf(editTextName.getText()).trim();
            if (TextUtils.isEmpty(name)) {
                Toast.makeText(ManagerRegister.this, "Vui lòng nhập tên.", Toast.LENGTH_SHORT).show();
                return;
            }
            handleRegister(name);
        });
    }

    // =============== LOAD COMPANY LIST ===============
    private void loadCompanyList() {
        DatabaseReference companiesRef = FirebaseDatabase.getInstance().getReference("companies");
        companiesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                companyList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String companyName = snapshot.child("companyName").getValue(String.class);
                    if (companyName != null) {
                        companyList.add(companyName);
                    }
                }
                if (companyList.isEmpty()) {
                    Toast.makeText(ManagerRegister.this, "Không có công ty nào trong cơ sở dữ liệu.", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Company list is empty.");
                } else {
                    Log.d(TAG, "Loaded companies: " + companyList);
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ManagerRegister.this, android.R.layout.simple_spinner_item, companyList);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerCompany.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(ManagerRegister.this, "Lỗi tải danh sách công ty: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading companies: ", databaseError.toException());
            }
        });
    }

    // =============== HANDLE REGISTER ===============
    private void handleRegister(String name) {
        progressBar.setVisibility(View.VISIBLE);

        String email = String.valueOf(editTextEmail.getText()).trim();
        String selectedCompany = spinnerCompany.getSelectedItem() != null ? spinnerCompany.getSelectedItem().toString() : "";

        Log.d(TAG, "Attempting to register user: " + email + ", Company: " + selectedCompany);

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(ManagerRegister.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            Log.w(TAG, "Email is empty.");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Định dạng email không hợp lệ");
            editTextEmail.requestFocus();
            progressBar.setVisibility(View.GONE);
            Log.w(TAG, "Invalid email format: " + email);
            return;
        }
        if (TextUtils.isEmpty(selectedCompany)) {
            Toast.makeText(ManagerRegister.this, "Vui lòng chọn một công ty hợp lệ.", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            Log.w(TAG, "No company selected.");
            return;
        }

        checkEmailInFirestore(email, name, selectedCompany);
    }

    // =============== CHECK EMAIL IN FIRESTORE ===============
    private void checkEmailInFirestore(String email, String name, String selectedCompany) {
        Log.d(TAG, "Checking if email exists in Firestore: " + email);
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            Toast.makeText(ManagerRegister.this, "Email này đã được đăng ký trên Firestore. Vui lòng sử dụng email khác.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            Log.w(TAG, "Email already exists in Firestore: " + email);
                        } else {
                            Log.d(TAG, "Email không tồn tại trong Firestore. Tiếp tục đăng ký.");
                            createUser(email, name, selectedCompany);
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi kiểm tra email.";
                        Toast.makeText(ManagerRegister.this, "Lỗi kiểm tra email: " + errorMessage, Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                        Log.e(TAG, "Error checking email in Firestore: ", task.getException());
                    }
                });
    }

    // =============== CREATE USER ===============
    private void createUser(String email, String name, String selectedCompany) {
        String randomPassword = generateRandomPassword();
        Log.d(TAG, "Generated random password for user: " + randomPassword);

        mAuth.createUserWithEmailAndPassword(email, randomPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User created successfully in Firebase Authentication: " + email);
                        FirebaseUser currentUser = mAuth.getCurrentUser();
                        if (currentUser != null) {
                            Log.d(TAG, "Saving user data to databases for UID: " + currentUser.getUid());
                            saveUserToDatabase(currentUser.getUid(), email, name, selectedCompany);
                        } else {
                            Toast.makeText(ManagerRegister.this, "Không thể lấy thông tin người dùng hiện tại.", Toast.LENGTH_SHORT).show();
                            progressBar.setVisibility(View.GONE);
                            Log.e(TAG, "Current user is null after successful registration.");
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                            Toast.makeText(ManagerRegister.this, "Email này đã được đăng ký. Vui lòng sử dụng email khác.", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "FirebaseAuthUserCollisionException: Email already in use: " + email);
                        } else {
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại.";
                            Toast.makeText(ManagerRegister.this, "Đăng ký thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                            Log.e(TAG, "User creation failed: ", task.getException());
                        }
                    }
                });
    }

    // =============== GENERATE RANDOM PASSWORD ===============
    private String generateRandomPassword() {
        int length = 10;
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        return password.toString();
    }

    // =============== SAVE USER TO DATABASE ===============
    private void saveUserToDatabase(String uid, String email, String name, String company) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", uid);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("company", company);
        userMap.put("role", "Manager");
        userMap.put("status", "disable");

        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.child(uid).setValue(userMap).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User data saved to Realtime Database for UID: " + uid);
                saveUserToFirestore(uid, userMap);
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi khi lưu dữ liệu.";
                Toast.makeText(ManagerRegister.this, "Không thể lưu dữ liệu người dùng vào Realtime Database: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error saving user data to Realtime Database: ", task.getException());
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    // =============== SAVE USER TO FIRESTORE ===============
    private void saveUserToFirestore(String uid, Map<String, Object> userMap) {
        Log.d(TAG, "Saving user data to Firestore for UID: " + uid);
        firestore.collection("users").document(uid).set(userMap)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "User data saved to Firestore for UID: " + uid);
                        String email = userMap.get("email").toString();
                        sendPasswordResetLink(email);
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi khi lưu vào Firestore.";
                        Toast.makeText(ManagerRegister.this, "Lưu Firestore thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error saving user data to Firestore: ", task.getException());
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    // =============== SEND PASSWORD RESET LINK ===============
    private void sendPasswordResetLink(String email) {
        Log.d(TAG, "Sending password reset link to: " + email);
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(ManagerRegister.this, "Liên kết đặt mật khẩu đã được gửi tới email: " + email, Toast.LENGTH_LONG).show();
                Log.d(TAG, "Password reset email sent to: " + email);
                Intent intent = new Intent(ManagerRegister.this, Admin.class);
                startActivity(intent);
                finish();
            } else {
                String errorMessage = task.getException() != null ? task.getException().getMessage() : "Không thể gửi liên kết đặt mật khẩu.";
                Toast.makeText(ManagerRegister.this, "Lỗi gửi liên kết: " + errorMessage, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error sending password reset email: ", task.getException());
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}
