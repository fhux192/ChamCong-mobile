package com.example.appcpp;

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

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class CompanyRegister extends AppCompatActivity {

    private TextInputEditText editTextCompanyName, editTextLatitude, editTextLongitude;
    private Button buttonRegister;
    private ProgressBar progressBar;
    private DatabaseReference databaseReference;
    private Handler handler = new Handler(); // Để trì hoãn kiểm tra
    private Runnable checkDuplicateTask; // Tác vụ trì hoãn

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_register);

        // Khởi tạo Firebase Realtime Database
        databaseReference = FirebaseDatabase.getInstance().getReference("companies");

        // Liên kết các thành phần giao diện
        editTextCompanyName = findViewById(R.id.companyName);
        editTextLatitude = findViewById(R.id.latitude);
        editTextLongitude = findViewById(R.id.longitude);
        buttonRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progressBar);

        // Lắng nghe sự thay đổi văn bản của trường tên công ty
        editTextCompanyName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String companyName = s.toString().trim();
                if (!TextUtils.isEmpty(companyName)) {
                    if (checkDuplicateTask != null) {
                        handler.removeCallbacks(checkDuplicateTask);
                    }
                    checkDuplicateTask = () -> checkCompanyNameInRealTime(companyName);
                    handler.postDelayed(checkDuplicateTask, 500); // Đợi 500ms trước khi kiểm tra
                } else {
                    editTextCompanyName.setError(null); // Xóa lỗi nếu trống
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Xử lý sự kiện khi người dùng nhấn nút đăng ký
        buttonRegister.setOnClickListener(view -> registerCompany());
    }

    /**
     * Xử lý sự kiện đăng ký công ty
     */
    private void registerCompany() {
        progressBar.setVisibility(View.VISIBLE);

        // Lấy thông tin từ các trường nhập liệu
        String companyName = editTextCompanyName.getText().toString().trim();
        String latitudeStr = editTextLatitude.getText().toString().trim();
        String longitudeStr = editTextLongitude.getText().toString().trim();

        // Kiểm tra dữ liệu hợp lệ
        if (TextUtils.isEmpty(companyName)) {
            showToastAndHideProgress("Vui lòng nhập tên công ty");
            return;
        }
        if (TextUtils.isEmpty(latitudeStr)) {
            showToastAndHideProgress("Vui lòng nhập vĩ độ");
            return;
        }
        if (TextUtils.isEmpty(longitudeStr)) {
            showToastAndHideProgress("Vui lòng nhập kinh độ");
            return;
        }

        // Kiểm tra giá trị vĩ độ và kinh độ
        double latitude, longitude;
        try {
            latitude = Double.parseDouble(latitudeStr);
            longitude = Double.parseDouble(longitudeStr);
        } catch (NumberFormatException e) {
            showToastAndHideProgress("Vĩ độ và kinh độ phải là số hợp lệ");
            return;
        }

        if (latitude < -90 || latitude > 90) {
            showToastAndHideProgress("Vĩ độ phải nằm trong khoảng -90 đến 90");
            return;
        }

        if (longitude < -180 || longitude > 180) {
            showToastAndHideProgress("Kinh độ phải nằm trong khoảng -180 đến 180");
            return;
        }

        // Kiểm tra xem tên công ty có trùng không
        checkDuplicateCompanyName(companyName, latitude, longitude);
    }

    /**
     * Kiểm tra tên công ty có bị trùng không trong thời gian thực
     */
    private void checkCompanyNameInRealTime(String companyName) {
        databaseReference.orderByChild("companyName").equalTo(companyName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            editTextCompanyName.setError("Tên công ty đã tồn tại");
                        } else {
                            editTextCompanyName.setError(null); // Xóa lỗi nếu hợp lệ
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(CompanyRegister.this, "Lỗi kiểm tra tên công ty: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Kiểm tra tên công ty khi đăng ký
     */
    private void checkDuplicateCompanyName(String companyName, double latitude, double longitude) {
        databaseReference.orderByChild("companyName").equalTo(companyName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(CompanyRegister.this, "Tên công ty đã tồn tại. Vui lòng nhập tên khác.", Toast.LENGTH_SHORT).show();
                        } else {
                            saveCompany(companyName, latitude, longitude);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(CompanyRegister.this, "Lỗi kiểm tra tên công ty: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * Lưu công ty vào Realtime Database
     */
    private void saveCompany(String companyName, double latitude, double longitude) {
        String companyId = databaseReference.push().getKey();

        Map<String, Object> companyData = new HashMap<>();
        companyData.put("companyName", companyName);
        companyData.put("latitude", latitude);
        companyData.put("longitude", longitude);

        if (companyId != null) {
            databaseReference.child(companyId).setValue(companyData).addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                if (task.isSuccessful()) {
                    Toast.makeText(CompanyRegister.this, "Đăng ký công ty thành công", Toast.LENGTH_SHORT).show();
                    editTextCompanyName.setText("");
                    editTextLatitude.setText("");
                    editTextLongitude.setText("");
                } else {
                    Toast.makeText(CompanyRegister.this, "Lỗi khi lưu công ty: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            progressBar.setVisibility(View.GONE);
            Toast.makeText(this, "Không thể tạo ID công ty", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Hiển thị thông báo lỗi và ẩn ProgressBar
     */
    private void showToastAndHideProgress(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        progressBar.setVisibility(View.GONE);
    }
}
