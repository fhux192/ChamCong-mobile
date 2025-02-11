package com.example.timekeeping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder> {

    // =============== FIELDS ===============
    private List<CompanyData> companyList;

    // =============== CONSTRUCTOR ===============
    public CompanyAdapter(List<CompanyData> companyList) {
        this.companyList = companyList;
    }

    // =============== CREATE VIEW HOLDER ===============
    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new CompanyViewHolder(view);
    }

    // =============== BIND DATA TO VIEW HOLDER ===============
    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        CompanyData company = companyList.get(position);
        holder.textViewName.setText("Tên: " + company.getCompanyName());
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Bạn chọn: " + company.getCompanyName(), Toast.LENGTH_SHORT).show();
        });
    }

    // =============== RETURN ITEM COUNT ===============
    @Override
    public int getItemCount() {
        return companyList.size();
    }

    // =============== VIEW HOLDER CLASS ===============
    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;

        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewCompanyName);
        }
    }

    // =============== UPDATE COMPANY LIST ===============
    public void setCompanyList(List<CompanyData> newList) {
        this.companyList = newList;
        notifyDataSetChanged();
    }
}
