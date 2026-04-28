package com.example.dijitalraf.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.google.android.material.chip.Chip;

import java.util.List;

public class KitapCardAdapter extends RecyclerView.Adapter<KitapCardAdapter.CardViewHolder> {

    private final List<Kitap> kitapListesi;

    public KitapCardAdapter(List<Kitap> kitapListesi) {
        this.kitapListesi = kitapListesi;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_kitap_grid, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Kitap kitap = kitapListesi.get(position);
        holder.tvKitapAdi.setText(kitap.getKitapAdi());
        holder.tvYazar.setText(kitap.getYazar());
        String tur = kitap.getTur() != null ? kitap.getTur() : "";
        holder.chipTur.setText(tur);
        holder.chipTur.setVisibility(tur.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return kitapListesi.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        final TextView tvKitapAdi;
        final TextView tvYazar;
        final Chip chipTur;
        final ImageView ivCover;

        CardViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKitapAdi = itemView.findViewById(R.id.tvKitapAdi);
            tvYazar = itemView.findViewById(R.id.tvYazar);
            chipTur = itemView.findViewById(R.id.chipTur);
            ivCover = itemView.findViewById(R.id.ivCover);
        }
    }
}
