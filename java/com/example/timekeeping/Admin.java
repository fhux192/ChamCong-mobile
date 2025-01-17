// src/main/java/com/example/appcpp/Admin.java
package com.example.timekeeping;

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

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class Admin extends AppCompatActivity {

    private MaterialCardView buttonSignOut;
    private MaterialCardView buttonAddCompany; // Nút Thêm Công Ty
    private MaterialCardView buttonAddManager; // Nút Thêm Quản Lý
    private MaterialCardView buttonViewManagerList;
    private MaterialCardView buttonViewCompanyList;
    // Tên file SharedPreferences và các key cần thiết
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);
        // Khởi tạo Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Liên kết các thành phần giao diện
        buttonSignOut = findViewById(R.id.sign_out);
        buttonAddCompany = findViewById(R.id.add_company);
        buttonAddManager = findViewById(R.id.add_manager); // Liên kết nút Thêm Quản Lý

        buttonViewManagerList = findViewById(R.id.manager_list);
        buttonViewCompanyList = findViewById(R.id.company_list);

        // Thiết lập sự kiện khi người dùng nhấn vào nút Sign Out
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignOut();
            }
        });

        // Thiết lập sự kiện khi người dùng nhấn vào nút Thêm Công Ty
        buttonAddCompany.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAddCompany();
            }
        });

        // Thiết lập sự kiện khi người dùng nhấn vào nút Thêm Quản Lý
        buttonAddManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAddManager();
            }
        });

        buttonViewManagerList.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) { handleViewManagerList();}
        });

        buttonViewCompanyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleViewCompanyList();
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
    private void handleViewManagerList(){
        Intent intent = new Intent(Admin.this, ManagerList.class);
        startActivity(intent);
    }

    private void handleViewCompanyList(){
        Intent intent = new Intent(Admin.this, CompanyList.class);
        startActivity(intent);
    }

    private void handleSignOut() {
        // Đăng xuất khỏi Firebase
        mAuth.signOut();

        // Xóa thông tin lưu trữ (nếu cần)
        clearPreferences();

        // Hiển thị thông báo đăng xuất thành công
        Toast.makeText(Admin.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();

        // Chuyển hướng trở lại trang đăng nhập
        Intent intent = new Intent(Admin.this, Login.class);
        // Đảm bảo rằng người dùng không thể quay lại Activity Admin bằng nút back
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Xử lý sự kiện Thêm Công Ty
     */
    private void handleAddCompany() {
        Intent intent = new Intent(Admin.this, CompanyRegister.class);
        startActivity(intent);
    }

    /**
     * Xử lý sự kiện Thêm Quản Lý
     */
    private void handleAddManager() {
        Intent intent = new Intent(Admin.this, ManagerRegister.class);
        startActivity(intent);
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