package com.example.timekeeping;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button; // Đảm bảo import Button
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.timekeeping.MainActivityCPP;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonEmployee; // Định nghĩa buttonEmployee là Button
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView textViewRegisterNow;
    private CheckBox checkboxRememberMe;

    // Tên file SharedPreferences
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // Khởi tạo Firebase Auth và Firestore
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Liên kết các thành phần giao diện
        editTextEmail = findViewById(R.id.email);
        editTextPassword = findViewById(R.id.password);
        buttonLogin = findViewById(R.id.btn_login);
        buttonEmployee = findViewById(R.id.btn_employee);
        progressBar = findViewById(R.id.progressBar);
        textViewRegisterNow = findViewById(R.id.registerNow);
        checkboxRememberMe = findViewById(R.id.checkboxRememberMe);

        // Kiểm tra nếu các view không tồn tại để tránh NullPointerException
        if (editTextEmail == null || editTextPassword == null || buttonLogin == null ||
                buttonEmployee == null || progressBar == null || textViewRegisterNow == null ||
                checkboxRememberMe == null) {
            Toast.makeText(this, "Một số thành phần giao diện không được tìm thấy.", Toast.LENGTH_LONG).show();
            finish(); // Dừng Activity nếu thiếu view
            return;
        }

        // Tải thông tin đã lưu (nếu có)
        loadPreferences();

        // Thiết lập sự kiện khi người dùng nhấn vào "Register Now"
        textViewRegisterNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Ẩn bàn phím nếu đang mở
                hideKeyboard();

                // Chuyển hướng đến màn hình đăng ký
                Intent intent = new Intent(Login.this, ForgotPassword.class);
                startActivity(intent);
            }
        });

        // Thiết lập sự kiện khi người dùng nhấn vào nút đăng nhập
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(); // Ẩn bàn phím khi nhấn nút Đăng nhập
                handleLogin();
            }
        });

        // Thiết lập sự kiện khi người dùng nhấn vào nút "Nhân Viên"
        buttonEmployee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(); // Ẩn bàn phím khi nhấn nút Nhân Viên
                handleEmployeeLogin();
            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        // Sign out any existing user to ensure fresh login
        mAuth.signOut();

        // Kiểm tra xem người dùng đã đăng nhập hay chưa (sẽ luôn là null sau signOut)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null){
            // Nên không bao giờ vào đây vì đã sign out
            mAuth.signOut();
        }
    }

    /**
     * Xử lý sự kiện đăng nhập
     */
    private void handleLogin() {
        // Hiển thị ProgressBar
        progressBar.setVisibility(View.VISIBLE);

        // Lấy thông tin email và mật khẩu từ người dùng
        String email = String.valueOf(editTextEmail.getText()).trim();
        String password = String.valueOf(editTextPassword.getText()).trim();

        // Kiểm tra xem email có rỗng không
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(Login.this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra định dạng email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Định dạng email không hợp lệ");
            editTextEmail.requestFocus();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra xem mật khẩu có rỗng không
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(Login.this, "Vui lòng nhập mật khẩu", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Kiểm tra độ dài mật khẩu
        if (password.length() < 6) {
            editTextPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            editTextPassword.requestFocus();
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Thực hiện đăng nhập với Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Ẩn ProgressBar khi hoàn thành
                        progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            // Đăng nhập thành công
                            // Chờ đến khi kiểm tra vai trò và trạng thái để hiển thị thông báo
                            // Do đó, không hiển thị Toast "Đăng nhập thành công" ở đây

                            // Lưu thông tin nếu "Remember Me" được chọn
                            if (checkboxRememberMe.isChecked()) {
                                savePreferences(email);
                            } else {
                                clearPreferences();
                            }

                            // Kiểm tra vai trò của người dùng và chuyển hướng
                            checkUserRole(mAuth.getCurrentUser());
                        } else {
                            // Đăng nhập thất bại
                            String errorMessage = task.getException() != null ? task.getException().getMessage() : "Xác thực thất bại.";
                            Toast.makeText(Login.this, "Xác thực thất bại: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Xử lý sự kiện khi nhấn nút "Nhân Viên"
     */
    private void handleEmployeeLogin() {
        // Chuyển hướng trực tiếp đến MainActivityCPP mà không cần đăng nhập
        Intent intent = new Intent(Login.this, MainActivityCPP.class);
        startActivity(intent);
        finish();
    }

    /**
     * Kiểm tra vai trò của người dùng sau khi đăng nhập và chuyển hướng tương ứng
     * Ngoài ra kiểm tra nếu vai trò là Manager thì phải có status là "enable"
     * @param user Người dùng hiện tại
     */
    private void checkUserRole(FirebaseUser user) {
        if (user == null) {
            Toast.makeText(this, "Người dùng không hợp lệ.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = user.getUid();
        DocumentReference docRef = db.collection("users").document(uid);
        docRef.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String role = document.getString("role");
                                String status = document.getString("status"); // Lấy status

                                if (role != null) {
                                    if (role.equalsIgnoreCase("Quản Trị Viên")) {
                                        // Chuyển hướng đến AdminActivity
                                        Toast.makeText(Login.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Login.this, Admin.class);
                                        startActivity(intent);
                                        finish();
                                    } else if (role.equalsIgnoreCase("Manager")) {
                                        // Kiểm tra nếu status là "enable"
                                        if ("enable".equalsIgnoreCase(status)) {
                                            // Chuyển hướng đến ManagerActivity
                                            Toast.makeText(Login.this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(Login.this, MainActivityCPP.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Nếu status không phải "enable", từ chối đăng nhập
                                            Toast.makeText(Login.this, "Tài khoản của bạn đã bị vô hiệu hóa!", Toast.LENGTH_LONG).show();
                                            mAuth.signOut(); // Đăng xuất người dùng
                                        }
                                    } else {
                                        // Vai trò không xác định
                                        Toast.makeText(Login.this, "Vai trò không được xác định.", Toast.LENGTH_SHORT).show();
                                        mAuth.signOut();
                                        Intent intent = new Intent(Login.this, Login.class);
                                        startActivity(intent);
                                        finish();
                                    }
                                } else {
                                    // Trường role không tồn tại
                                    Toast.makeText(Login.this, "Trường vai trò không tồn tại.", Toast.LENGTH_SHORT).show();
                                    mAuth.signOut();
                                    Intent intent = new Intent(Login.this, Login.class);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                // Tài liệu không tồn tại
                                Toast.makeText(Login.this, "Người dùng không tồn tại.", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                Intent intent = new Intent(Login.this, Login.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            // Lỗi Firestore
                            String error = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định.";
                            Toast.makeText(Login.this, "Lỗi khi lấy vai trò: " + error, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Lưu thông tin email vào SharedPreferences
     */
    private void savePreferences(String email) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_EMAIL, email);
        editor.putBoolean(KEY_REMEMBER_ME, true);
        editor.apply();
    }

    /**
     * Xóa thông tin email khỏi SharedPreferences
     */
    private void clearPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }

    /**
     * Tải thông tin đã lưu từ SharedPreferences
     */
    private void loadPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);

        if (isRemembered) {
            editTextEmail.setText(savedEmail);
            checkboxRememberMe.setChecked(true);
        }
    }

    /**
     * Thiết lập UI để ẩn bàn phím khi chạm ra ngoài EditText
     */
    private void setupUI(View view) {
        // Nếu không phải EditText, thêm OnTouchListener để ẩn bàn phím
        if (!(view instanceof TextInputEditText)) {
            view.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    hideKeyboard();
                    return false;
                }
            });
        }

        // Nếu là ViewGroup, duyệt đệ quy để thêm OnTouchListener
        if (view instanceof android.view.ViewGroup) {
            for (int i = 0; i < ((android.view.ViewGroup) view).getChildCount(); i++) {
                View innerView = ((android.view.ViewGroup) view).getChildAt(i);
                setupUI(innerView);
            }
        }
    }

    /**
     * Phương thức hỗ trợ để ẩn bàn phím
     */
    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}