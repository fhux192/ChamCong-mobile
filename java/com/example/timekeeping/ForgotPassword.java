package com.example.timekeeping;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPassword extends AppCompatActivity {

    // =============== FIELDS ===============
    private TextInputEditText editTextEmail;
    private MaterialCardView submitBtn;
    private FirebaseAuth mAuth;

    // =============== ACTIVITY LIFECYCLE ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        editTextEmail = findViewById(R.id.email);
        submitBtn = findViewById(R.id.submit);
        mAuth = FirebaseAuth.getInstance();

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

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // =============== HELPER METHODS ===============
    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void sendPasswordResetEmail(String email) {
        Toast.makeText(ForgotPassword.this, "Đang gửi email...", Toast.LENGTH_SHORT).show();
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ForgotPassword.this, "Email đặt lại mật khẩu đã được gửi.", Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        Toast.makeText(ForgotPassword.this, "Gửi email thất bại: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
