// File: ManagerList.java
package com.example.timekeeping;

import android.content.DialogInterface;
import android.content.Intent;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ManagerList extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ManagerUserAdapter managerAdapter;
    private List<ManagerUserData> managerList;

    private FirebaseFirestore db;
    private CollectionReference managersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_list);

        // Thiết lập padding cho window insets (nếu cần)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Kiểm tra xem người dùng đã đăng nhập chưa
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            // Người dùng chưa đăng nhập, chuyển đến màn hình đăng nhập
            Intent intent = new Intent(ManagerList.this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
        managersRef = db.collection("users"); // Đảm bảo sử dụng đúng tên collection

        // Khởi tạo RecyclerView
        recyclerView = findViewById(R.id.recyclerViewManagers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        managerList = new ArrayList<>();
        managerAdapter = new ManagerUserAdapter(managerList);
        recyclerView.setAdapter(managerAdapter);

        // Lấy dữ liệu từ Firestore
        fetchManagers();

        // Thiết lập ItemTouchHelper cho Swipe-to-Update Status
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void fetchManagers() {
        // Sử dụng Listener để theo dõi thời gian thực với điều kiện role = "Manager"
        managersRef
                .whereEqualTo("role", "Manager") // Thêm điều kiện lọc
                .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w("ManagerList", "Listen failed.", e);
                            Toast.makeText(ManagerList.this, "Lỗi khi lấy dữ liệu.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (snapshots != null) {
                            Log.d("ManagerList", "Received " + snapshots.size() + " documents.");
                            managerList.clear();
                            for (QueryDocumentSnapshot doc : snapshots) {
                                if (doc.exists()) {
                                    ManagerUserData user = doc.toObject(ManagerUserData.class);
                                    user.setId(doc.getId()); // Đặt ID nếu cần
                                    managerList.add(user);
                                    Log.d("ManagerList", "Added user: " + user.getName() + ", Status: " + user.getStatus());
                                }
                            }
                            managerAdapter.notifyDataSetChanged();
                            Log.d("ManagerList", "Adapter notified.");
                        } else {
                            Log.d("ManagerList", "Current data: null");
                        }
                    }
                });
    }

    // Define SimpleCallback cho ItemTouchHelper
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            // Không hỗ trợ di chuyển mục
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            // Lấy vị trí của mục bị swipe
            int position = viewHolder.getAdapterPosition();
            ManagerUserData userToUpdate = managerList.get(position);
            String userId = userToUpdate.getId();

            if (userId != null && !userId.isEmpty()) {
                // Hiển thị hộp thoại xác nhận trước khi cập nhật status
                new AlertDialog.Builder(ManagerList.this)
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có muốn thay đổi trạng thái của " + userToUpdate.getName() + " không?")
                        .setPositiveButton("Có", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                // Đặt trạng thái mới
                                String newStatus = "enable".equalsIgnoreCase(userToUpdate.getStatus()) ? "disable" : "enable";

                                // Cập nhật trường status trong Firestore
                                managersRef.document(userId)
                                        .update("status", newStatus)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(ManagerList.this, "Đã cập nhật trạng thái thành công.", Toast.LENGTH_SHORT).show();
                                            Log.d("ManagerList", "Updated status for user: " + userToUpdate.getName() + " to " + newStatus);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(ManagerList.this, "Lỗi khi cập nhật trạng thái.", Toast.LENGTH_SHORT).show();
                                            Log.w("ManagerList", "Error updating status for user: " + userToUpdate.getName(), e);
                                            // Khôi phục mục bị swipe
                                            managerAdapter.notifyItemChanged(position);
                                        });
                            }
                        })
                        .setNegativeButton("Không", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Khôi phục mục bị swipe
                                managerAdapter.notifyItemChanged(position);
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .show();
            } else {
                Toast.makeText(ManagerList.this, "Không thể cập nhật người dùng này.", Toast.LENGTH_SHORT).show();
                Log.w("ManagerList", "User ID is null or empty");
                managerAdapter.notifyItemChanged(position);
            }
        }
    };
}