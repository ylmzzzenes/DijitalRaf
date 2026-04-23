package com.example.dijitalraf.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;

import java.util.List;

public class KitapAdapter extends RecyclerView.Adapter<KitapAdapter.KitapViewHolder> {

    private List<Kitap> kitapListesi;

    public KitapAdapter(List<Kitap> kitapListesi) {
        this.kitapListesi = kitapListesi;
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
        holder.tvYazar.setText("Yazar: " + kitap.getYazar());
        holder.tvTur.setText("Tür: " + kitap.getTur());
    }

    @Override
    public int getItemCount() {
        return kitapListesi.size();
    }

    public static class KitapViewHolder extends RecyclerView.ViewHolder {
        TextView tvKitapAdi, tvYazar, tvTur;

        public KitapViewHolder(@NonNull View itemView) {
            super(itemView);
            tvKitapAdi = itemView.findViewById(R.id.tvKitapAdi);
            tvYazar = itemView.findViewById(R.id.tvYazar);
            tvTur = itemView.findViewById(R.id.tvTur);
        }
    }
}