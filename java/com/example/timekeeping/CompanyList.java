package com.example.timekeeping;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class CompanyList extends AppCompatActivity {

    // =============== UI ELEMENTS ===============
    private RecyclerView recyclerViewCompanies;
    private CompanyAdapter companyAdapter;
    private List<CompanyData> companyList;

    // =============== FIREBASE REFERENCE ===============
    private DatabaseReference companiesRef;

    // =============== ACTIVITY LIFECYCLE METHODS ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        // =============== APPLY WINDOW INSETS ===============
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.mainContainerCompanyList),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                    return insets;
                }
        );

        // =============== INITIALIZE FIREBASE REFERENCE ===============
        companiesRef = FirebaseDatabase.getInstance().getReference("companies");

        // =============== SETUP RECYCLER VIEW ===============
        recyclerViewCompanies = findViewById(R.id.recyclerViewCompanies);
        recyclerViewCompanies.setLayoutManager(new LinearLayoutManager(this));
        companyList = new ArrayList<>();
        companyAdapter = new CompanyAdapter(companyList);
        recyclerViewCompanies.setAdapter(companyAdapter);

        // =============== FETCH DATA FROM FIREBASE ===============
        fetchCompaniesData();

        // =============== INIT SWIPE TO DELETE ===============
        initSwipeToDelete();
    }

    // =============== FETCH COMPANIES DATA ===============
    private void fetchCompaniesData() {
        companiesRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                companyList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CompanyData company = snapshot.getValue(CompanyData.class);
                    if (company != null) {
                        company.setId(snapshot.getKey());
                        companyList.add(company);
                    }
                }
                companyAdapter.notifyDataSetChanged();
                Log.d("CompanyListActivity", "Companies loaded: " + companyList.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CompanyList.this,
                        "Lỗi khi lấy dữ liệu: " + databaseError.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e("CompanyListActivity", "onCancelled", databaseError.toException());
            }
        });
    }

    // =============== INIT SWIPE TO DELETE ===============
    private void initSwipeToDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                CompanyData companyToDelete = companyList.get(position);
                showDeleteDialog(companyToDelete, position);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerViewCompanies);
    }

    // =============== SHOW DELETE DIALOG ===============
    private void showDeleteDialog(CompanyData companyToDelete, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa công ty")
                .setMessage("Bạn có chắc muốn xóa \"" + companyToDelete.getCompanyName() + "\" không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    String id = companyToDelete.getId();
                    if (id != null && !id.isEmpty()) {
                        companiesRef.child(id).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(CompanyList.this,
                                            "Đã xóa công ty thành công.",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(CompanyList.this,
                                            "Xóa không thành công: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    companyAdapter.notifyItemChanged(position);
                                });
                    } else {
                        Toast.makeText(CompanyList.this,
                                "Không có id. Không thể xóa!",
                                Toast.LENGTH_SHORT).show();
                        companyAdapter.notifyItemChanged(position);
                    }
                })
                .setNegativeButton("Không", (dialog, which) -> {
                    companyAdapter.notifyItemChanged(position);
                })
                .setCancelable(false)
                .show();
    }
}
