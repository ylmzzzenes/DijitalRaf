package com.example.dijitalraf.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;

import java.util.ArrayList;
import java.util.List;

public class ChatMessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_USER = 0;
    public static final int TYPE_ASSISTANT = 1;
    public static final int TYPE_TYPING = 2;

    public static final class Row {
        public final int type;
        @NonNull
        public final String text;

        public Row(int type, @NonNull String text) {
            this.type = type;
            this.text = text;
        }
    }

    private final List<Row> rows = new ArrayList<>();

    public void setRows(@NonNull List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        notifyDataSetChanged();
    }

    @NonNull
    public List<Row> getRows() {
        return new ArrayList<>(rows);
    }

    public void addRow(@NonNull Row row) {
        rows.add(row);
        notifyItemInserted(rows.size() - 1);
    }

    public void removeTypingIfAny() {
        for (int i = rows.size() - 1; i >= 0; i--) {
            if (rows.get(i).type == TYPE_TYPING) {
                rows.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    public boolean hasUserMessage() {
        for (Row r : rows) {
            if (r.type == TYPE_USER) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_chat_message_user, parent, false);
            return new MessageHolder(v);
        }
        if (viewType == TYPE_ASSISTANT) {
            View v = inflater.inflate(R.layout.item_chat_message_assistant, parent, false);
            return new MessageHolder(v);
        }
        View v = inflater.inflate(R.layout.item_chat_typing, parent, false);
        return new TypingHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof MessageHolder) {
            ((MessageHolder) holder).bind(row.text);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class MessageHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;

        MessageHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
        }

        void bind(String text) {
            tvMessage.setText(text);
        }
    }

    static final class TypingHolder extends RecyclerView.ViewHolder {
        TypingHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
