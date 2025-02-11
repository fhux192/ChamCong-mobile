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

    // =============== FIELDS ===============
    private RecyclerView recyclerView;
    private ManagerUserAdapter managerAdapter;
    private List<ManagerUserData> managerList;

    // =============== FIRESTORE REFERENCES ===============
    private FirebaseFirestore db;
    private CollectionReference managersRef;

    // =============== ACTIVITY LIFECYCLE ===============
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager_list);

        // APPLY WINDOW INSETS
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // CHECK USER SIGN-IN STATUS
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(ManagerList.this, Login.class);
            startActivity(intent);
            finish();
            return;
        }

        // INITIALIZE FIRESTORE
        db = FirebaseFirestore.getInstance();
        managersRef = db.collection("users");

        // SETUP RECYCLER VIEW
        recyclerView = findViewById(R.id.recyclerViewManagers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        managerList = new ArrayList<>();
        managerAdapter = new ManagerUserAdapter(managerList);
        recyclerView.setAdapter(managerAdapter);

        // FETCH DATA FROM FIRESTORE
        fetchManagers();

        // INITIALIZE ITEM TOUCH HELPER FOR SWIPE-TO-UPDATE
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    // =============== FETCH MANAGERS DATA ===============
    private void fetchManagers() {
        managersRef
                .whereEqualTo("role", "Manager")
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
                                    user.setId(doc.getId());
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

    // =============== ITEM TOUCH HELPER CALLBACK ===============
    ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getAdapterPosition();
            ManagerUserData userToUpdate = managerList.get(position);
            String userId = userToUpdate.getId();

            if (userId != null && !userId.isEmpty()) {
                new AlertDialog.Builder(ManagerList.this)
                        .setTitle("Xác nhận")
                        .setMessage("Bạn có muốn thay đổi trạng thái của " + userToUpdate.getName() + " không?")
                        .setPositiveButton("Có", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newStatus = "enable".equalsIgnoreCase(userToUpdate.getStatus()) ? "disable" : "enable";
                                managersRef.document(userId)
                                        .update("status", newStatus)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(ManagerList.this, "Đã cập nhật trạng thái thành công.", Toast.LENGTH_SHORT).show();
                                            Log.d("ManagerList", "Updated status for user: " + userToUpdate.getName() + " to " + newStatus);
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(ManagerList.this, "Lỗi khi cập nhật trạng thái.", Toast.LENGTH_SHORT).show();
                                            Log.w("ManagerList", "Error updating status for user: " + userToUpdate.getName(), e);
                                            managerAdapter.notifyItemChanged(position);
                                        });
                            }
                        })
                        .setNegativeButton("Không", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
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
