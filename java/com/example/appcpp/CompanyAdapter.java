package com.example.appcpp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter hiển thị danh sách CompanyData lên RecyclerView
 */
public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder> {

    private List<CompanyData> companyList;

    public CompanyAdapter(List<CompanyData> companyList) {
        this.companyList = companyList;
    }

    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new CompanyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        CompanyData company = companyList.get(position);

        holder.textViewName.setText("Tên: " + company.getCompanyName());
        holder.textViewLat.setText("Vĩ độ: " + company.getLatitude());
        holder.textViewLng.setText("Kinh độ: " + company.getLongitude());
    }

    @Override
    public int getItemCount() {
        return companyList.size();
    }

    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName, textViewLat, textViewLng;

        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewCompanyName);
            textViewLat  = itemView.findViewById(R.id.textViewLatitude);
            textViewLng  = itemView.findViewById(R.id.textViewLongitude);
        }
    }

    /**
     * Cập nhật data toàn bộ
     */
    public void setCompanyList(List<CompanyData> newList) {
        this.companyList = newList;
        notifyDataSetChanged();
    }
}
