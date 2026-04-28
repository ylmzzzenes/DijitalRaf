package com.example.dijitalraf.ui.home;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.example.dijitalraf.data.FavoritesHelper;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private BooksViewModel viewModel;
    private final List<Kitap> kitapListesi = new ArrayList<>();
    private KitapAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerBooks;
    private View progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BooksViewModel.class);
        recyclerBooks = view.findViewById(R.id.recyclerBooks);
        emptyState = view.findViewById(R.id.emptyInclude);
        progress = view.findViewById(R.id.progressLoading);

        TextView tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        TextView tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvEmptyTitle.setText(R.string.empty_library_title);
        tvEmptyMessage.setText(R.string.empty_library_message);
        MaterialButton btnEmpty = view.findViewById(R.id.btnEmptyAction);
        btnEmpty.setVisibility(View.VISIBLE);
        btnEmpty.setOnClickListener(v ->
                startActivity(new android.content.Intent(requireContext(), KitapEkleActivity.class)));

        recyclerBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBooks.setItemAnimator(new DefaultItemAnimator());
        adapter = new KitapAdapter(requireContext(), kitapListesi);
        recyclerBooks.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || pos >= kitapListesi.size()) {
                    return;
                }
                Kitap k = kitapListesi.get(pos);
                if (k.getId() == null) {
                    adapter.notifyItemChanged(pos);
                    return;
                }
                if (direction == ItemTouchHelper.LEFT) {
                    kitapListesi.remove(pos);
                    adapter.notifyItemRemoved(pos);
                    FirebaseDatabase.getInstance().getReference("kitaplar").child(k.getId()).removeValue();
                    Snackbar.make(recyclerBooks, R.string.book_deleted, Snackbar.LENGTH_SHORT).show();
                } else {
                    boolean wasFav = FavoritesHelper.isFavorite(requireContext(), k.getId());
                    FavoritesHelper.toggle(requireContext(), k.getId());
                    adapter.notifyItemChanged(pos);
                    Snackbar.make(recyclerBooks,
                            wasFav ? R.string.favorite_removed : R.string.favorite_added,
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        touchHelper.attachToRecyclerView(recyclerBooks);

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }
            updateEmpty();
        });

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            kitapListesi.clear();
            if (books != null) {
                kitapListesi.addAll(books);
            }
            adapter.notifyDataSetChanged();
            updateEmpty();
        });
    }

    private void updateEmpty() {
        Boolean loading = viewModel.getLoading().getValue();
        boolean isLoading = Boolean.TRUE.equals(loading);
        if (isLoading) {
            emptyState.setVisibility(View.GONE);
            recyclerBooks.setVisibility(View.VISIBLE);
            return;
        }
        boolean showEmpty = kitapListesi.isEmpty();
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        recyclerBooks.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }
}
