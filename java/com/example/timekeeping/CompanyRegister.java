package com.example.timekeeping;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CompanyRegister extends AppCompatActivity {

    // =============== FIELDS ===============
    private TextInputEditText editTextCompanyName;
    private MaterialCardView buttonRegister;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private Handler handler = new Handler();
    private Runnable checkDuplicateTask;

    // =============== ACTIVITY LIFECYCLE ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_register);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN);

        // INITIALIZE FIREBASE REFERENCE
        databaseReference = FirebaseDatabase.getInstance().getReference("companies");

        // BIND UI COMPONENTS
        editTextCompanyName = findViewById(R.id.companyName);
        buttonRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);

        // TEXT CHANGE LISTENER FOR REAL-TIME DUPLICATE CHECK
        editTextCompanyName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {  }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String companyName = s.toString().trim();
                if (!TextUtils.isEmpty(companyName)) {
                    if (checkDuplicateTask != null) {
                        handler.removeCallbacks(checkDuplicateTask);
                    }
                    checkDuplicateTask = () -> checkCompanyNameInRealTime(companyName);
                    handler.postDelayed(checkDuplicateTask, 500);
                } else {
                    editTextCompanyName.setError(null);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {  }
        });

        // BUTTON CLICK LISTENER
        buttonRegister.setOnClickListener(view -> registerCompany());
    }

    // =============== REGISTER COMPANY ===============
    private void registerCompany() {
        progressBar.setVisibility(View.VISIBLE);
        String companyName = editTextCompanyName.getText().toString().trim();
        if (TextUtils.isEmpty(companyName)) {
            showToastAndHideProgress("Vui lòng nhập tên công ty");
            return;
        }
        checkDuplicateCompanyName(companyName);
    }

    // =============== REAL-TIME DUPLICATE CHECK ===============
    private void checkCompanyNameInRealTime(String companyName) {
        databaseReference.orderByChild("companyName").equalTo(companyName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            editTextCompanyName.setError("Tên công ty đã tồn tại");
                        } else {
                            editTextCompanyName.setError(null);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(CompanyRegister.this, "Lỗi kiểm tra tên công ty: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =============== DUPLICATE CHECK WHEN REGISTER ===============
    private void checkDuplicateCompanyName(String companyName) {
        databaseReference.orderByChild("companyName").equalTo(companyName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(CompanyRegister.this, "Tên công ty đã tồn tại. Vui lòng nhập tên khác.", Toast.LENGTH_SHORT).show();
                        } else {
                            saveCompany(companyName);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CompanyRegister.this, "Lỗi kiểm tra tên công ty: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =============== SAVE COMPANY TO FIREBASE ===============
    private void saveCompany(String companyName) {
        String companyId = databaseReference.push().getKey();
        Map<String, Object> companyData = new HashMap<>();
        companyData.put("companyName", companyName);
        if (companyId != null) {
            databaseReference.child(companyId).setValue(companyData).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(CompanyRegister.this, "Đăng ký công ty thành công", Toast.LENGTH_SHORT).show();
                    editTextCompanyName.setText("");
                    Intent intent = new Intent(CompanyRegister.this, CompanyList.class);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(CompanyRegister.this, "Lỗi khi lưu công ty: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Không thể tạo ID công ty", Toast.LENGTH_SHORT).show();
        }
    }

    // =============== SHOW TOAST & HIDE PROGRESS ===============
    private void showToastAndHideProgress(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }
}
