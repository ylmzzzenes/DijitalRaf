package com.example.dijitalraf.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;

import java.util.ArrayList;
import java.util.List;

public class DashboardBookRowAdapter extends RecyclerView.Adapter<DashboardBookRowAdapter.Holder> {

    public interface Listener {
        void onRowClick();
    }

    private final List<Kitap> items = new ArrayList<>();
    private final Listener listener;

    public DashboardBookRowAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setBooks(List<Kitap> books) {
        items.clear();
        if (books != null) {
            items.addAll(books);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_dashboard_book, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Kitap kitap = items.get(position);
        ImageView iv = holder.ivCover;

        String imageUrl = kitap.getImageUrl();
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(iv.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_menu_book_24)
                    .error(R.drawable.ic_menu_book_24)
                    .centerCrop()
                    .into(iv);
        } else {
            Glide.with(iv.getContext()).clear(iv);
            iv.setImageResource(R.drawable.ic_menu_book_24);
        }

        holder.itemView.setOnClickListener(v -> listener.onRowClick());
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final ImageView ivCover;

        Holder(@NonNull View itemView) {
            super(itemView);
            ivCover = itemView.findViewById(R.id.ivCover);
        }
    }
}
