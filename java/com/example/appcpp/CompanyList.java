package com.example.appcpp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class CompanyList extends AppCompatActivity {

    private RecyclerView recyclerViewCompanies;
    private CompanyAdapter companyAdapter;
    private List<CompanyData> companyList;

    // Tham chiếu đến node "companies" trong Realtime Database
    private DatabaseReference companiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        // Nếu cần xử lý Window Insets (status bar, navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.mainContainerCompanyList),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
        );

        // Khởi tạo tham chiếu node "companies"
        companiesRef = FirebaseDatabase.getInstance().getReference("companies");

        // Thiết lập RecyclerView
        recyclerViewCompanies = findViewById(R.id.recyclerViewCompanies);
        recyclerViewCompanies.setLayoutManager(new LinearLayoutManager(this));

        companyList = new ArrayList<>();
        companyAdapter = new CompanyAdapter(companyList);
        recyclerViewCompanies.setAdapter(companyAdapter);

        // Gọi hàm lấy dữ liệu
        fetchCompaniesData();
    }

    private void fetchCompaniesData() {
        companiesRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                companyList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    // Parse snapshot thành CompanyData
                    CompanyData company = snapshot.getValue(CompanyData.class);
                    if (company != null) {
                        companyList.add(company);
                    }
                }
                companyAdapter.notifyDataSetChanged();
                Log.d("CompanyListActivity", "Companies loaded: " + companyList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(
                        CompanyList.this,
                        "Lỗi khi lấy dữ liệu: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT
                ).show();
                Log.e("CompanyListActivity", "onCancelled", databaseError.toException());
            }
        });
    }
}
