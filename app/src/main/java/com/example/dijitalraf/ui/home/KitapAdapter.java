package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.example.dijitalraf.data.FavoritesHelper;
import com.google.android.material.chip.Chip;

import java.util.List;

public class KitapAdapter extends RecyclerView.Adapter<KitapAdapter.KitapViewHolder> {

    public interface OnFavoriteChangedListener {
        void onFavoriteChanged(Kitap kitap, int position);
    }

    private final Context appContext;
    private final List<Kitap> kitapListesi;
    private OnFavoriteChangedListener favoriteChangedListener;

    public KitapAdapter(Context context, List<Kitap> kitapListesi) {
        this.appContext = context.getApplicationContext();
        this.kitapListesi = kitapListesi;
    }

    public void setOnFavoriteChangedListener(OnFavoriteChangedListener listener) {
        this.favoriteChangedListener = listener;
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

        boolean fav = kitap.isFavorite();
        holder.ivFavorite.setImageResource(fav ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24);

        holder.ivFavorite.setOnClickListener(v -> {
            if (kitap.getId() == null) {
                return;
            }
            boolean wasFav = kitap.isFavorite();
            boolean newFav = !wasFav;

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

        public KitapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKitapAdi = itemView.findViewById(R.id.tvKitapAdi);
            tvYazar = itemView.findViewById(R.id.tvYazar);
            chipTur = itemView.findViewById(R.id.chipTur);
            ivCover = itemView.findViewById(R.id.ivCover);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
        }
    }
}
