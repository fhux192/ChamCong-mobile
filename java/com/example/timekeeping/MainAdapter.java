package com.example.timekeeping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;

public class MainAdapter extends FirebaseRecyclerAdapter<MainModel, MainAdapter.myViewHolder> {

    // =============== CONSTRUCTOR ===============
    public MainAdapter(@NonNull FirebaseRecyclerOptions<MainModel> options) {
        super(options);
    }

    // =============== BIND VIEW HOLDER ===============
    @Override
    protected void onBindViewHolder(@NonNull myViewHolder holder, int position, @NonNull MainModel model) {
        holder.name.setText(model.getName());
        // holder.embedding.setText(model.getEmbedding().toString());
    }

    // =============== CREATE VIEW HOLDER ===============
    @NonNull
    @Override
    public myViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_user, parent, false);
        return new myViewHolder(view);
    }

    // =============== VIEW HOLDER CLASS ===============
    class myViewHolder extends RecyclerView.ViewHolder {
        TextView name, embedding;

        public myViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.name);
            embedding = itemView.findViewById(R.id.embedding);
        }
    }
}
