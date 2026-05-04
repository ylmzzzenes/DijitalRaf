package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class KitapAdapter extends ListAdapter<Kitap, KitapAdapter.KitapViewHolder> {

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
    private OnFavoriteChangedListener favoriteChangedListener;
    private OnBookClickListener bookClickListener;
    private OnBookLongClickListener bookLongClickListener;

    public KitapAdapter(Context context) {
        super(new KitapDiffCallback());
        this.appContext = context.getApplicationContext();
    }

    /** Fragment’teki liste ile adapter içeriğini senkronlar (DiffUtil). */
    public void submitFrom(@NonNull List<Kitap> source) {
        submitList(new ArrayList<>(source));
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
        Kitap kitap = getItem(position);

        holder.tvKitapAdi.setText(kitap.getKitapAdi());
        holder.tvYazar.setText(kitap.getYazar());

        String tur = kitap.getTur() != null ? kitap.getTur() : "";
        holder.chipTur.setText(tur);
        holder.chipTur.setVisibility(tur.isEmpty() ? View.GONE : View.VISIBLE);

        boolean isRead = kitap.isOkundu();
        holder.ivReadStatus.setVisibility(isRead ? View.VISIBLE : View.GONE);
        holder.tvReadDate.setVisibility(isRead ? View.VISIBLE : View.GONE);
        holder.tvRating.setVisibility(View.VISIBLE);
        holder.tvRating.setText(starsEmoji(kitap.getYildiz()));
        if (isRead) {
            holder.tvReadDate.setText(formatReadDate(kitap.getUpdatedAt()));
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
        holder.ivFavorite.setIconResource(fav ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);
        holder.ivFavorite.setIconTint(ColorStateList.valueOf(
                ContextCompat.getColor(holder.itemView.getContext(),
                        fav ? R.color.primary : R.color.text_secondary)));

        holder.ivFavorite.setOnClickListener(v -> {
            if (kitap.getId() == null) {
                return;
            }

            boolean newFav = !kitap.isFavorite();

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
            return appContext.getString(R.string.book_list_read_date_unknown);
        }
        String date = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                .format(new Date(updatedAt));
        return appContext.getString(R.string.book_detail_read_date, date);
    }

    /** 0–5 yıldız; 0 ise nötr gösterim. */
    private String starsEmoji(int yildiz) {
        int n = Math.max(0, Math.min(5, yildiz));
        if (n == 0) {
            return "☆☆☆☆☆";
        }
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < n; i++) {
            sb.append("⭐");
        }
        for (int i = n; i < 5; i++) {
            sb.append("☆");
        }
        return sb.toString();
    }

    private void shareBook(Context context, Kitap kitap) {
        String kitapAdi = kitap.getKitapAdi() != null && !kitap.getKitapAdi().trim().isEmpty()
                ? kitap.getKitapAdi().trim()
                : appContext.getString(R.string.share_book_title_fallback);

        String yazar = kitap.getYazar() != null && !kitap.getYazar().trim().isEmpty()
                ? kitap.getYazar().trim()
                : appContext.getString(R.string.share_book_unknown_author);

        String tur = kitap.getTur() != null ? kitap.getTur().trim() : "";
        String imageUrl = kitap.getImageUrl() != null ? kitap.getImageUrl().trim() : "";

        StringBuilder shareText = new StringBuilder();
        shareText.append(appContext.getString(R.string.share_book_opening));
        shareText.append(appContext.getString(R.string.share_book_line_book, kitapAdi)).append("\n");
        shareText.append(appContext.getString(R.string.share_book_line_author, yazar));

        if (!tur.isEmpty()) {
            shareText.append(appContext.getString(R.string.share_book_line_genre, tur));
        }

        int y = kitap.getYildiz();
        if (y > 0) {
            shareText.append(appContext.getString(R.string.share_book_line_rating, y));
        }

        if (!imageUrl.isEmpty()) {
            shareText.append(appContext.getString(R.string.share_book_line_cover, imageUrl));
        }

        shareText.append(appContext.getString(R.string.share_book_footer));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

        context.startActivity(Intent.createChooser(
                shareIntent,
                appContext.getString(R.string.share_chooser_title)));
    }

    public static class KitapViewHolder extends RecyclerView.ViewHolder {
        final TextView tvKitapAdi;
        final TextView tvYazar;
        final Chip chipTur;
        final ImageView ivCover;
        final MaterialButton ivFavorite;
        final MaterialButton ivShare;
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

    static final class KitapDiffCallback extends DiffUtil.ItemCallback<Kitap> {

        @Override
        public boolean areItemsTheSame(@NonNull Kitap oldItem, @NonNull Kitap newItem) {
            if (oldItem.getId() != null && newItem.getId() != null) {
                return oldItem.getId().equals(newItem.getId());
            }
            return oldItem == newItem;
        }

        @Override
        public boolean areContentsTheSame(@NonNull Kitap oldItem, @NonNull Kitap newItem) {
            return Objects.equals(oldItem.getKitapAdi(), newItem.getKitapAdi())
                    && Objects.equals(oldItem.getYazar(), newItem.getYazar())
                    && Objects.equals(oldItem.getTur(), newItem.getTur())
                    && oldItem.isFavorite() == newItem.isFavorite()
                    && oldItem.isOkundu() == newItem.isOkundu()
                    && oldItem.getYildiz() == newItem.getYildiz()
                    && Objects.equals(oldItem.getImageUrl(), newItem.getImageUrl())
                    && oldItem.getUpdatedAt() == newItem.getUpdatedAt();
        }
    }
}
