package com.example.timekeeping;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ManagerUserAdapter extends RecyclerView.Adapter<ManagerUserAdapter.ManagerViewHolder> {

    // =============== FIELDS ===============
    private List<ManagerUserData> managerList;

    // =============== CONSTRUCTOR ===============
    public ManagerUserAdapter(List<ManagerUserData> managerList) {
        this.managerList = managerList;
    }

    // =============== CREATE VIEW HOLDER ===============
    @NonNull
    @Override
    public ManagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manager_users, parent, false);
        return new ManagerViewHolder(view);
    }

    // =============== BIND DATA TO VIEW HOLDER ===============
    @Override
    public void onBindViewHolder(@NonNull ManagerViewHolder holder, int position) {
        ManagerUserData user = managerList.get(position);
        holder.textViewName.setText(user.getName());
        holder.textViewEmail.setText("Email: " + user.getEmail());
        holder.textViewRole.setText("Vai trò: " + user.getRole());
        holder.textViewCompany.setText("Công ty: " + user.getCompany());

        String status = user.getStatus();
        holder.textViewStatus.setText("Status: " + status);

        if ("enable".equalsIgnoreCase(status)) {
            holder.textViewStatus.setTextColor(Color.parseColor("#388E3C"));
        } else if ("disable".equalsIgnoreCase(status)) {
            holder.textViewStatus.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            holder.textViewStatus.setTextColor(Color.parseColor("#000000"));
        }
    }

    // =============== RETURN ITEM COUNT ===============
    @Override
    public int getItemCount() {
        return managerList.size();
    }

    // =============== REMOVE ITEM ===============
    public void removeItem(int position) {
        managerList.remove(position);
        notifyItemRemoved(position);
    }

    // =============== VIEW HOLDER CLASS ===============
    public static class ManagerViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewEmail, textViewRole, textViewCompany, textViewStatus;

        public ManagerViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewEmail = itemView.findViewById(R.id.textViewEmail);
            textViewRole = itemView.findViewById(R.id.textViewRole);
            textViewCompany = itemView.findViewById(R.id.textViewCompany);
            textViewStatus = itemView.findViewById(R.id.textViewStatus);
        }
    }

    // =============== UPDATE MANAGER LIST ===============
    public void setManagerList(List<ManagerUserData> managerList) {
        this.managerList = managerList;
        notifyDataSetChanged();
    }
}
