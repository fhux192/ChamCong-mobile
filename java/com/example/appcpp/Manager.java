package com.example.appcpp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;

public class Manager extends AppCompatActivity {

    private Button buttonSignOut;
    private Button btnAddEmployee;
    // Tên file SharedPreferences và các key cần thiết
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager);

        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Liên kết các thành phần giao diện
        buttonSignOut = findViewById(R.id.sign_out);
        btnAddEmployee =findViewById(R.id.add_employee);

        // Thiết lập sự kiện khi người dùng nhấn vào nút Sign Out
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignOut();
            }
        });

        // Thiết lập padding cho các thanh hệ thống
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    /**
     * Xử lý sự kiện đăng xuất
     */
    private void handleSignOut() {
        // Đăng xuất khỏi Firebase
        mAuth.signOut();

        // Xóa thông tin lưu trữ (nếu cần)
        clearPreferences();

        // Hiển thị thông báo đăng xuất thành công
        Toast.makeText(Manager.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

        // Chuyển hướng trở lại trang đăng nhập
        Intent intent = new Intent(Manager.this, Login.class);
        // Đảm bảo rằng người dùng không thể quay lại Activity Manager bằng nút back
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
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
}
