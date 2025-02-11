package com.example.timekeeping;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class Admin extends AppCompatActivity {

    // =============== UI ELEMENTS ===============
    private MaterialCardView buttonSignOut;
    private MaterialCardView buttonAddCompany;   
    private MaterialCardView buttonAddManager;      
    private MaterialCardView buttonViewManagerList;
    private MaterialCardView buttonViewCompanyList;

    // =============== SHARED PREFERENCES KEYS ===============
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";

    // =============== FIREBASE AUTHENTICATION ===============
    private FirebaseAuth mAuth;

    // =============== ACTIVITY LIFECYCLE METHODS ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Enable Edge-to-Edge mode
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);
        // Set UI flags for stable layout & fullscreen display
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // =============== INITIALIZE FIREBASE AUTHENTICATION ===============
        mAuth = FirebaseAuth.getInstance();

        // =============== BIND UI COMPONENTS ===============
        buttonSignOut = findViewById(R.id.sign_out);
        buttonAddCompany = findViewById(R.id.add_company);
        buttonAddManager = findViewById(R.id.add_manager);
        buttonViewManagerList = findViewById(R.id.manager_list);
        buttonViewCompanyList = findViewById(R.id.company_list);

        // =============== SET CLICK LISTENERS ===============
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignOut();
            }
        });

        buttonAddCompany.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAddCompany();
            }
        });

        buttonAddManager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleAddManager();
            }
        });

        buttonViewManagerList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleViewManagerList();
            }
        });

        buttonViewCompanyList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleViewCompanyList();
            }
        });

        // =============== APPLY WINDOW INSETS FOR SYSTEM BARS ===============
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // =============== HANDLE VIEW MANAGER LIST ===============
    private void handleViewManagerList() {
        Intent intent = new Intent(Admin.this, ManagerList.class);
        startActivity(intent);
    }

    // =============== HANDLE VIEW COMPANY LIST ===============
    private void handleViewCompanyList() {
        Intent intent = new Intent(Admin.this, CompanyList.class);
        startActivity(intent);
    }

    // =============== HANDLE SIGN OUT ===============
    private void handleSignOut() {
        // Sign out from Firebase Authentication
        mAuth.signOut();
        // Clear saved preferences (if any)
        clearPreferences();
        // Show sign out success message
        Toast.makeText(Admin.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        // Redirect to Login Activity ensuring user cannot navigate back
        Intent intent = new Intent(Admin.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    // =============== HANDLE ADD COMPANY ===============
    private void handleAddCompany() {
        Intent intent = new Intent(Admin.this, CompanyRegister.class);
        startActivity(intent);
    }

    // =============== HANDLE ADD MANAGER ===============
    private void handleAddManager() {
        Intent intent = new Intent(Admin.this, ManagerRegister.class);
        startActivity(intent);
    }

    // =============== CLEAR SAVED PREFERENCES ===============
    private void clearPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }
}
