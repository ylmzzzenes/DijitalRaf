package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.example.dijitalraf.data.FavoritesHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends Fragment {

    private BooksViewModel viewModel;
    private final List<Kitap> sourceBooks = new ArrayList<>();
    private final List<Kitap> favoriteBooks = new ArrayList<>();
    private KitapAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerBooks;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favorites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BooksViewModel.class);
        recyclerBooks = view.findViewById(R.id.recyclerBooks);
        emptyState = view.findViewById(R.id.emptyInclude);

        TextView tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        TextView tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvEmptyTitle.setText(R.string.empty_favorites_title);
        tvEmptyMessage.setText(R.string.empty_favorites_message);
        MaterialButton btnEmpty = view.findViewById(R.id.btnEmptyAction);
        btnEmpty.setVisibility(View.VISIBLE);
        btnEmpty.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), KitapEkleActivity.class)));

        recyclerBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBooks.setItemAnimator(new DefaultItemAnimator());
        adapter = new KitapAdapter(requireContext(), favoriteBooks);
        adapter.setOnBookClickListener((kitap, position) -> {
            if (kitap.getId() == null) {
                return;
            }
            boolean next = !kitap.isOkundu();
            kitap.setOkundu(next);
            viewModel.persistKitap(kitap);
            Snackbar.make(
                    recyclerBooks,
                    next ? R.string.marked_as_read : R.string.marked_as_to_read,
                    Snackbar.LENGTH_SHORT
            ).show();
        });
        recyclerBooks.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= favoriteBooks.size()) {
                    return;
                }
                Kitap k = favoriteBooks.get(pos);
                if (k.getId() != null) {
                    FavoritesHelper.setFavorite(k.getId(), false);
                   k.setFavorite(false);
                   applyFavoriteFilter();
                    Snackbar.make(recyclerBooks, R.string.favorite_removed, Snackbar.LENGTH_SHORT).show();
                } else {
                    adapter.notifyItemChanged(pos);
                }
            }
        });
        touchHelper.attachToRecyclerView(recyclerBooks);

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            sourceBooks.clear();
            if (books != null) {
                sourceBooks.addAll(books);
            }
            applyFavoriteFilter();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!sourceBooks.isEmpty()) {
            applyFavoriteFilter();
        }
    }

    private void applyFavoriteFilter() {
        favoriteBooks.clear();
        for (Kitap k : sourceBooks) {
            if (k.getId() != null && k.isFavorite()) {
                favoriteBooks.add(k);
            }
        }
        adapter.notifyDataSetChanged();
        boolean showEmpty = favoriteBooks.isEmpty();
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        recyclerBooks.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }
}
