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

import com.google.firebase.auth.FirebaseAuth;

public class Manager extends AppCompatActivity {

    // =============== FIELDS ===============
    private Button buttonSignOut;
    private Button btnAddEmployee;
    private static final String PREFS_NAME = "MyPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_REMEMBER_ME = "remember_me";
    private FirebaseAuth mAuth;

    // =============== ACTIVITY LIFECYCLE ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manager);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // INITIALIZE FIREBASE AUTH
        mAuth = FirebaseAuth.getInstance();

        // BIND UI COMPONENTS
        buttonSignOut = findViewById(R.id.sign_out);
        btnAddEmployee = findViewById(R.id.add_employee);

        // SET CLICK LISTENER FOR SIGN OUT BUTTON
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handleSignOut();
            }
        });

        // APPLY WINDOW INSETS
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // =============== HELPER METHODS ===============
    private void handleSignOut() {
        // SIGN OUT FROM FIREBASE
        mAuth.signOut();

        // CLEAR SAVED PREFERENCES
        clearPreferences();

        // SHOW SUCCESS MESSAGE AND REDIRECT TO LOGIN
        Toast.makeText(Manager.this, "Đăng xuất thành công", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Manager.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void clearPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_EMAIL);
        editor.remove(KEY_REMEMBER_ME);
        editor.apply();
    }
}
