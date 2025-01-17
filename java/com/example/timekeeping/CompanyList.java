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

/**
 * Activity hiển thị danh sách công ty, cho phép vuốt xóa.
 * Sử dụng ValueEventListener để tự động tải lại khi xóa thành công.
 */
public class CompanyList extends AppCompatActivity {

    private RecyclerView recyclerViewCompanies;
    private CompanyAdapter companyAdapter;
    private List<CompanyData> companyList;

    // Tham chiếu đến node "companies" trên Firebase
    private DatabaseReference companiesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_list);

        // Xử lý WindowInsets nếu cần
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

        // Đăng ký listener để tự động cập nhật khi dữ liệu thay đổi (realtime)
        fetchCompaniesData();

        // Tạo khả năng vuốt xóa
        initSwipeToDelete();
    }

    /**
     * Đăng ký ValueEventListener để lắng nghe thay đổi tại node "companies".
     * Mỗi lần xóa công ty (hoặc thêm, sửa), onDataChange() sẽ tự gọi -> cập nhật danh sách.
     */
    private void fetchCompaniesData() {
        companiesRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                // Clear danh sách cục bộ rồi thêm lại
                companyList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CompanyData company = snapshot.getValue(CompanyData.class);
                    if (company != null) {
                        company.setId(snapshot.getKey());
                        companyList.add(company);
                    }
                }
                // Cập nhật giao diện
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

    /**
     * Áp dụng ItemTouchHelper để vuốt xóa
     */
    private void initSwipeToDelete() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,  // Không hỗ trợ drag
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT // Vuốt trái/phải
        ) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // Không xử lý kéo item
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                CompanyData companyToDelete = companyList.get(position);

                // Xác nhận xóa
                showDeleteDialog(companyToDelete, position);
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerViewCompanies);
    }

    /**
     * Hiển thị Dialog xác nhận xóa,
     * Xóa trên Firebase -> onDataChange() tự gọi -> reload UI
     */
    private void showDeleteDialog(CompanyData companyToDelete, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa công ty")
                .setMessage("Bạn có chắc muốn xóa \"" + companyToDelete.getCompanyName() + "\" không?")
                .setPositiveButton("Có", (dialog, which) -> {
                    // Lấy key
                    String id = companyToDelete.getId();
                    if (id != null && !id.isEmpty()) {
                        // Xóa trên Firebase
                        companiesRef.child(id).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    // KHÔNG cần gọi fetchCompaniesData()
                                    // Vì onDataChange() sẽ tự động chạy -> reload
                                    Toast.makeText(CompanyList.this,
                                            "Đã xóa công ty thành công.",
                                            Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(CompanyList.this,
                                            "Xóa không thành công: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    // Khôi phục item (do xóa thất bại)
                                    companyAdapter.notifyItemChanged(position);
                                });
                    } else {
                        Toast.makeText(CompanyList.this,
                                "Không có id. Không thể xóa!",
                                Toast.LENGTH_SHORT).show();
                        // Khôi phục item
                        companyAdapter.notifyItemChanged(position);
                    }
                })
                .setNegativeButton("Không", (dialog, which) -> {
                    // Người dùng hủy xóa -> khôi phục item
                    companyAdapter.notifyItemChanged(position);
                })
                .setCancelable(false)
                .show();
    }
}
