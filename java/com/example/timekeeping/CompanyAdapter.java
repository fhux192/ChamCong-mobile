package com.example.timekeeping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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

        // Nếu bạn muốn xử lý sự kiện click vào item
        holder.itemView.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Bạn chọn: " + company.getCompanyName(), Toast.LENGTH_SHORT).show();
            // Có thể mở chi tiết công ty hoặc thực hiện các hành động khác
        });
    }

    @Override
    public int getItemCount() {
        return companyList.size();
    }

    public static class CompanyViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;

        public CompanyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewName = itemView.findViewById(R.id.textViewCompanyName);
        }
    }

    /**
     * Cập nhật danh sách công ty toàn bộ
     */
    public void setCompanyList(List<CompanyData> newList) {
        this.companyList = newList;
        notifyDataSetChanged();
    }
}
