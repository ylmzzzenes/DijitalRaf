package com.example.dijitalraf.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class BookQuoteAdapter extends RecyclerView.Adapter<BookQuoteAdapter.Holder> {

    public interface Listener {
        void onEdit(@NonNull String quoteId, @NonNull String text);

        void onDelete(@NonNull String quoteId, @NonNull String text);
    }

    public static final class Item {
        public final String id;
        public final String text;
        public final long createdAt;

        public Item(@NonNull String id, @NonNull String text, long createdAt) {
            this.id = id;
            this.text = text;
            this.createdAt = createdAt;
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final Listener listener;

    public BookQuoteAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void setItems(@NonNull List<Item> next) {
        items.clear();
        items.addAll(next);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_kitap_alinti, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Item item = items.get(position);
        holder.tvText.setText(item.text);
        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item.id, item.text));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item.id, item.text));
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView tvText;
        final MaterialButton btnEdit;
        final MaterialButton btnDelete;

        Holder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvQuoteText);
            btnEdit = itemView.findViewById(R.id.btnQuoteEdit);
            btnDelete = itemView.findViewById(R.id.btnQuoteDelete);
        }
    }
}
