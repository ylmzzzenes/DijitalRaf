package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
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
import com.google.firebase.auth.FirebaseAuth;
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

    private final String databaseUrl = "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

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
        MaterialButton btnEmpty = view.findViewById(R.id.btnEmptyAction);

        tvEmptyTitle.setText(R.string.empty_library_title);
        tvEmptyMessage.setText(R.string.empty_library_message);

        btnEmpty.setVisibility(View.VISIBLE);
        btnEmpty.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), KitapEkleActivity.class))
        );

        recyclerBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBooks.setItemAnimator(new DefaultItemAnimator());

        adapter = new KitapAdapter(requireContext(), kitapListesi);
        recyclerBooks.setAdapter(adapter);

        setupSwipeActions();

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
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

    private void setupSwipeActions() {
        ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT
        ) {
            private final ColorDrawable deleteBackground = new ColorDrawable(Color.parseColor("#D32F2F"));
            private final Drawable deleteIcon = ContextCompat.getDrawable(
                    requireContext(),
                    android.R.drawable.ic_menu_delete
            );

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onChildDraw(@NonNull Canvas canvas,
                                    @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX,
                                    float dY,
                                    int actionState,
                                    boolean isCurrentlyActive) {

                View itemView = viewHolder.itemView;

                if (dX < 0) {
                    deleteBackground.setBounds(
                            itemView.getRight() + (int) dX,
                            itemView.getTop(),
                            itemView.getRight(),
                            itemView.getBottom()
                    );
                    deleteBackground.draw(canvas);

                    if (deleteIcon != null) {
                        deleteIcon.setTint(Color.WHITE);

                        int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
                        int iconTop = itemView.getTop() + iconMargin;
                        int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
                        int iconRight = itemView.getRight() - iconMargin;
                        int iconLeft = iconRight - deleteIcon.getIntrinsicWidth();

                        deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
                        deleteIcon.draw(canvas);
                    }
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION || position >= kitapListesi.size()) {
                    return;
                }

                Kitap deletedBook = kitapListesi.get(position);

                if (deletedBook.getId() == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (direction == ItemTouchHelper.LEFT) {
                    deleteBookWithUndo(deletedBook, position);
                } else {
                    toggleFavorite(deletedBook, position);
                }
            }
        });

        touchHelper.attachToRecyclerView(recyclerBooks);
    }

    private void deleteBookWithUndo(Kitap deletedBook, int position) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        kitapListesi.remove(position);
        adapter.notifyItemRemoved(position);
        updateEmpty();

        FirebaseDatabase
                .getInstance(databaseUrl)
                .getReference("books")
                .child(uid)
                .child(deletedBook.getId())
                .removeValue();

        Snackbar.make(recyclerBooks, R.string.book_deleted, Snackbar.LENGTH_LONG)
                .setAction("GERİ AL", v -> {
                    kitapListesi.add(position, deletedBook);
                    adapter.notifyItemInserted(position);
                    recyclerBooks.scrollToPosition(position);
                    updateEmpty();

                    FirebaseDatabase
                            .getInstance(databaseUrl)
                            .getReference("books")
                            .child(uid)
                            .child(deletedBook.getId())
                            .setValue(deletedBook);
                })
                .show();
    }

    private void toggleFavorite(Kitap kitap, int position) {
        boolean wasFav = kitap.isFavorite();

        FavoritesHelper.toggle(kitap.getId(), wasFav);
        kitap.setFavorite(!wasFav);

        adapter.notifyItemChanged(position);

        Snackbar.make(
                recyclerBooks,
                wasFav ? R.string.favorite_removed : R.string.favorite_added,
                Snackbar.LENGTH_SHORT
        ).show();
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