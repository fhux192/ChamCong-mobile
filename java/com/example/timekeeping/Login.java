package com.example.timekeeping;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.textfield.TextInputEditText;

public class Login extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonEmployee;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewRegisterNow;
    private CheckBox checkboxRememberMe;

    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        buttonEmployee = findViewById(R.id.btn_employee);
        progressBar = findViewById(R.id.progressBar);
        textViewRegisterNow = findViewById(R.id.registerNow);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);

        if (editTextEmail == null || editTextPassword == null || buttonLogin == null ||
                buttonEmployee == null || progressBar == null || textViewRegisterNow == null ||
                checkboxRememberMe == null) {
            Toast.makeText(this, "Không tìm thấy một số thành phần giao diện.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadPreferences();

        textViewRegisterNow.setOnClickListener(v -> {
            hideKeyboard();
            Intent intent = new Intent(Login.this, ForgotPassword.class);
            startActivity(intent);
        });

        buttonLogin.setOnClickListener(view -> {
            hideKeyboard();
            handleLogin();
        });

        buttonEmployee.setOnClickListener(view -> {
            hideKeyboard();
            handleEmployeeLogin();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.signOut();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            mAuth.signOut();
        }
    }

    private void handleLogin() {
        progressBar.setVisibility(View.VISIBLE);

        String email = String.valueOf(editTextEmail.getText()).trim();
        String password = String.valueOf(editTextPassword.getText()).trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(Login.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Định dạng email không hợp lệ");
            editTextEmail.requestFocus();
            progressBar.setVisibility(View.GONE);
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(Login.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }
        if (password.length() < 6) {
            editTextPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            editTextPassword.requestFocus();
            progressBar.setVisibility(View.GONE);
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        if (checkboxRememberMe.isChecked()) {
                            savePreferences(email);
                        } else {
                            clearPreferences();
                        }
                        checkUserRole(mAuth.getCurrentUser());
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Xác thực không thành công.";
                        Toast.makeText(Login.this, "Đăng nhập thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleEmployeeLogin() {
        Intent intent = new Intent(Login.this, MainActivityCPP.class);
        startActivity(intent);
        finish();
    }

    private void checkUserRole(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Người dùng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    String role = document.getString("role");
                    String status = document.getString("status");

                    if ("Quản Trị Viên".equalsIgnoreCase(role)) {
                        Toast.makeText(Login.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(Login.this, Admin.class));
                        finish();
                    } else if ("Manager".equalsIgnoreCase(role)) {
                        if ("enable".equalsIgnoreCase(status)) {
                            Toast.makeText(Login.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(Login.this, MainActivityCPP.class));
                            finish();
                        } else {
                            Toast.makeText(Login.this, "Tài khoản của bạn đã bị vô hiệu hóa!", Toast.LENGTH_LONG).show();
                            mAuth.signOut();
                        }
                    } else {
                        Toast.makeText(Login.this, "Không xác định được quyền truy cập.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                        startActivity(new Intent(Login.this, Login.class));
                        finish();
                    }
                } else {
                    Toast.makeText(Login.this, "Không tìm thấy người dùng.", Toast.LENGTH_SHORT).show();
                    mAuth.signOut();
                    startActivity(new Intent(Login.this, Login.class));
                    finish();
                }
            } else {
                String error = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định.";
                Toast.makeText(Login.this, "Lỗi khi kiểm tra vai trò: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void savePreferences(String email) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.apply();
    }

    private void clearPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);

        if (isRemembered) {
            editTextEmail.setText(savedEmail);
            checkboxRememberMe.setChecked(true);
        }
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
