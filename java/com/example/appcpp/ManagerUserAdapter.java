// File: ManagerUserAdapter.java
package com.example.appcpp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ManagerUserAdapter extends RecyclerView.Adapter<ManagerUserAdapter.ManagerViewHolder> {

    private List<ManagerUserData> managerList;

    // Constructor
    public ManagerUserAdapter(List<ManagerUserData> managerList) {
        this.managerList = managerList;
    }

    @NonNull
    @Override
    public ManagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout item_manager_users.xml
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_users, parent, false);
        return new ManagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ManagerViewHolder holder, int position) {
        // Bind dữ liệu vào các TextView
        ManagerUserData user = managerList.get(position);
        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText("Email: " + user.getEmail());
        holder.textViewRole.setText("Vai trò: " + user.getRole());
        holder.textViewCompany.setText("Công ty: " + user.getCompany());

        // Hiển thị status
        String status = user.getStatus();
        holder.textViewStatus.setText("Status: " + status);

        // Thay đổi màu sắc dựa trên status
        if ("enable".equalsIgnoreCase(status)) {
            holder.textViewStatus.setTextColor(Color.parseColor("#388E3C")); // Xanh lá
        } else if ("disable".equalsIgnoreCase(status)) {
            holder.textViewStatus.setTextColor(Color.parseColor("#D32F2F")); // Đỏ
        } else {
            holder.textViewStatus.setTextColor(Color.parseColor("#000000")); // Đen cho các giá trị khác
        }
    }

    @Override
    public int getItemCount() {
        return managerList.size();
    }

    // Phương thức để xóa mục tại vị trí position
    public void removeItem(int position) {
        managerList.remove(position);
        notifyItemRemoved(position);
    }

    // ViewHolder
    public static class ManagerViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole, textViewCompany, textViewStatus;

        public ManagerViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
            textViewCompany = itemView.findViewById(R.id.textViewCompany);
            textViewStatus = itemView.findViewById(R.id.textViewStatus); // Liên kết TextViewStatus
        }
    }

    // Cập nhật danh sách
    public void setManagerList(List<ManagerUserData> managerList) {
        this.managerList = managerList;
        notifyDataSetChanged();
    }
}
