package com.example.timekeeping;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    private EditText editTextEmail;
    private Button submitBtn;
    private FirebaseAuth mAuth; // Thêm FirebaseAuth

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // Ánh xạ các view
        editTextEmail = findViewById(R.id.email);
        submitBtn = findViewById(R.id.submit);

        // Khởi tạo FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        // Xử lý đặt lại mật khẩu khi nhấn nút
        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String email = editTextEmail.getText().toString().trim();
                if (isValidEmail(email)) {
                    sendPasswordResetEmail(email);
                } else {
                    editTextEmail.setError("Vui lòng nhập email hợp lệ");
                }
            }
        });

        // Xử lý Edge to Edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Kiểm tra định dạng email hợp lệ
    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Gửi email đặt lại mật khẩu
    private void sendPasswordResetEmail(String email) {
        // Hiển thị thông báo đang xử lý
        Toast.makeText(ForgotPassword.this, "Đang gửi email...", Toast.LENGTH_SHORT).show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPassword.this, "Email đặt lại mật khẩu đã được gửi.", Toast.LENGTH_LONG).show();
                        // Có thể kết thúc activity hoặc chuyển hướng người dùng về màn hình đăng nhập
                        finish();
                    } else {
                        Toast.makeText(ForgotPassword.this, "Gửi email thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}