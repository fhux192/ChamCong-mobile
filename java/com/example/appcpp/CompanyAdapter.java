package com.example.appcpp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CompanyAdapter extends RecyclerView.Adapter<CompanyAdapter.CompanyViewHolder> {

    private List<CompanyData> companyList;

    public CompanyAdapter(List<CompanyData> companyList) {
        this.companyList = companyList;
    }

    @NonNull
    @Override
    public CompanyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate layout của item (ví dụ item_company_card.xml hoặc item_company.xml tùy bạn)
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company, parent, false);
        return new CompanyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CompanyViewHolder holder, int position) {
        CompanyData company = companyList.get(position);

        // Thêm prefix tiếng Việt cho các trường
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
            // Gắn ID cho TextView
            textViewName = itemView.findViewById(R.id.textViewCompanyName);
            textViewLat  = itemView.findViewById(R.id.textViewLatitude);
            textViewLng  = itemView.findViewById(R.id.textViewLongitude);
        }
    }

    // Nếu cần cập nhật danh sách mới
    public void setCompanyList(List<CompanyData> newList) {
        this.companyList = newList;
        notifyDataSetChanged();
    }
}
