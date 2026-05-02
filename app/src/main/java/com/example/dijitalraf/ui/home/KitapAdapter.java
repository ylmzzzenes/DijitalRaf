package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;
import com.example.dijitalraf.data.FavoritesHelper;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class KitapAdapter extends RecyclerView.Adapter<KitapAdapter.KitapViewHolder> {

    public interface OnFavoriteChangedListener {
        void onFavoriteChanged(Kitap kitap, int position);
    }

    public interface OnBookClickListener {
        void onBookClick(Kitap kitap, int position);
    }

    public interface OnBookLongClickListener {
        void onBookLongClick(Kitap kitap, int position);
    }

    private final Context appContext;
    private final List<Kitap> kitapListesi;
    private OnFavoriteChangedListener favoriteChangedListener;
    private OnBookClickListener bookClickListener;
    private OnBookLongClickListener bookLongClickListener;

    public KitapAdapter(Context context, List<Kitap> kitapListesi) {
        this.appContext = context.getApplicationContext();
        this.kitapListesi = kitapListesi;
    }

    public void setOnFavoriteChangedListener(OnFavoriteChangedListener listener) {
        this.favoriteChangedListener = listener;
    }

    public void setOnBookClickListener(OnBookClickListener listener) {
        this.bookClickListener = listener;
    }

    public void setOnBookLongClickListener(OnBookLongClickListener listener) {
        this.bookLongClickListener = listener;
    }

    @NonNull
    @Override
    public KitapViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kitap, parent, false);
        return new KitapViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KitapViewHolder holder, int position) {
        Kitap kitap = kitapListesi.get(position);

        holder.tvKitapAdi.setText(kitap.getKitapAdi());
        holder.tvYazar.setText(kitap.getYazar());

        String tur = kitap.getTur() != null ? kitap.getTur() : "";
        holder.chipTur.setText(tur);
        holder.chipTur.setVisibility(tur.isEmpty() ? View.GONE : View.VISIBLE);

        boolean isRead = kitap.isOkundu();
        holder.ivReadStatus.setVisibility(isRead ? View.VISIBLE : View.GONE);
        holder.tvReadDate.setVisibility(isRead ? View.VISIBLE : View.GONE);
        holder.tvRating.setVisibility(isRead ? View.VISIBLE : View.GONE);
        if (isRead) {
            holder.tvReadDate.setText(formatReadDate(kitap.getUpdatedAt()));
            holder.tvRating.setText(buildRatingStars(kitap.isFavorite()));
        }

        String imageUrl = kitap.getImageUrl();

        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_add_24)
                    .error(R.drawable.ic_add_24)
                    .centerCrop()
                    .into(holder.ivCover);
        } else {
            holder.ivCover.setImageResource(R.drawable.ic_add_24);
        }

        boolean fav = kitap.isFavorite();
        holder.ivFavorite.setImageResource(
                fav ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24
        );

        holder.ivFavorite.setOnClickListener(v -> {
            if (kitap.getId() == null) return;

            boolean newFav = !kitap.isFavorite();

            FavoritesHelper.setFavorite(kitap.getId(), newFav);
            kitap.setFavorite(newFav);

            int pos = holder.getBindingAdapterPosition();

            if (pos != RecyclerView.NO_POSITION) {
                notifyItemChanged(pos);
            }

            if (favoriteChangedListener != null && pos != RecyclerView.NO_POSITION) {
                favoriteChangedListener.onFavoriteChanged(kitap, pos);
            }
        });

        holder.ivShare.setOnClickListener(v -> shareBook(v.getContext(), kitap));

        holder.itemView.setOnClickListener(v -> {
            if (bookClickListener == null) {
                return;
            }
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                bookClickListener.onBookClick(kitap, pos);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (bookLongClickListener == null) {
                return false;
            }
            int pos = holder.getBindingAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return false;
            }
            bookLongClickListener.onBookLongClick(kitap, pos);
            return true;
        });
    }

    private String formatReadDate(long updatedAt) {
        if (updatedAt <= 0L) {
            return "Okundu: -";
        }
        String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(new Date(updatedAt));
        return "Okundu: " + date;
    }

    private String buildRatingStars(boolean favorite) {
        return favorite ? "⭐⭐⭐⭐⭐" : "⭐⭐⭐⭐☆";
    }

    private void shareBook(Context context, Kitap kitap) {
        String kitapAdi = kitap.getKitapAdi() != null && !kitap.getKitapAdi().trim().isEmpty()
                ? kitap.getKitapAdi().trim()
                : "Kitap";

        String yazar = kitap.getYazar() != null && !kitap.getYazar().trim().isEmpty()
                ? kitap.getYazar().trim()
                : "Bilinmeyen yazar";

        String tur = kitap.getTur() != null ? kitap.getTur().trim() : "";
        String imageUrl = kitap.getImageUrl() != null ? kitap.getImageUrl().trim() : "";

        StringBuilder shareText = new StringBuilder();

        shareText.append("📚 Sana bir kitap önerim var!\n\n");
        shareText.append("📖 Kitap: ").append(kitapAdi).append("\n");
        shareText.append("✍️ Yazar: ").append(yazar);

        if (!tur.isEmpty()) {
            shareText.append("\n🏷️ Tür: ").append(tur);
        }

        if (!imageUrl.isEmpty()) {
            shareText.append("\n\n🖼️ Kapak görseli: ").append(imageUrl);
        }

        shareText.append("\n\n✨ DijitalRaf'tan paylaşıldı.");

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        context.startActivity(Intent.createChooser(shareIntent, "Kitap paylaş"));
    }

    @Override
    public int getItemCount() {
        return kitapListesi.size();
    }

    public static class KitapViewHolder extends RecyclerView.ViewHolder {
        final TextView tvKitapAdi;
        final TextView tvYazar;
        final Chip chipTur;
        final ImageView ivCover;
        final ImageButton ivFavorite;
        final ImageButton ivShare;
        final ImageView ivReadStatus;
        final TextView tvRating;
        final TextView tvReadDate;

        public KitapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKitapAdi = itemView.findViewById(R.id.tvKitapAdi);
            tvYazar = itemView.findViewById(R.id.tvYazar);
            chipTur = itemView.findViewById(R.id.chipTur);
            ivCover = itemView.findViewById(R.id.ivCover);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            ivShare = itemView.findViewById(R.id.ivShare);
            ivReadStatus = itemView.findViewById(R.id.ivReadStatus);
            tvRating = itemView.findViewById(R.id.tvRating);
            tvReadDate = itemView.findViewById(R.id.tvReadDate);
        }
    }
}