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
import android.widget.Toast;

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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;

public class LibraryFragment extends Fragment {

    private static final String ARG_LIST_READ = "list_read";

    private BooksViewModel viewModel;
    private final List<Kitap> kitapListesi = new ArrayList<>();
    private KitapAdapter adapter;
    private View emptyState;
    private RecyclerView recyclerBooks;
    private View progress;
    private boolean listRead;

    private final String databaseUrl = "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

    public static LibraryFragment newInstance(boolean listReadBooks) {
        LibraryFragment fragment = new LibraryFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_LIST_READ, listReadBooks);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        listRead = args != null && args.getBoolean(ARG_LIST_READ, false);
    }

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

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(listRead ? R.string.nav_read_books : R.string.nav_to_read);

        recyclerBooks = view.findViewById(R.id.recyclerBooks);
        emptyState = view.findViewById(R.id.emptyInclude);
        progress = view.findViewById(R.id.progressLoading);

        TextView tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        TextView tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        MaterialButton btnEmpty = view.findViewById(R.id.btnEmptyAction);
        TextView tvInteractionNote = view.findViewById(R.id.tvInteractionNote);

        if (listRead) {
            tvEmptyTitle.setText(R.string.empty_read_books_title);
            tvEmptyMessage.setText(R.string.empty_read_books_message);
            tvInteractionNote.setText(R.string.library_long_press_hint_read);
            btnEmpty.setVisibility(View.GONE);
        } else {
            tvEmptyTitle.setText(R.string.empty_to_read_title);
            tvEmptyMessage.setText(R.string.empty_to_read_message);
            tvInteractionNote.setText(R.string.library_long_press_hint_to_read);
            btnEmpty.setVisibility(View.VISIBLE);
            btnEmpty.setOnClickListener(v ->
                    startActivity(new Intent(requireContext(), KitapEkleActivity.class))
            );
        }

        recyclerBooks.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerBooks.setItemAnimator(new DefaultItemAnimator());

        adapter = new KitapAdapter(requireContext(), kitapListesi);

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
        adapter.setOnBookLongClickListener((kitap, position) -> showBookDetailsDialog(kitap));

        recyclerBooks.setAdapter(adapter);

        setupSwipeActions();

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            progress.setVisibility(Boolean.TRUE.equals(loading) ? View.VISIBLE : View.GONE);
            updateEmpty();
        });

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            kitapListesi.clear();

            if (books != null) {
                for (Kitap k : books) {
                    if (k.isOkundu() == listRead) {
                        kitapListesi.add(k);
                    }
                }
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
            private final ColorDrawable deleteBackground =
                    new ColorDrawable(Color.parseColor("#D32F2F"));

            private final ColorDrawable favoriteBackground =
                    new ColorDrawable(Color.parseColor("#6B8E23"));

            private final Drawable deleteIcon = ContextCompat.getDrawable(
                    requireContext(),
                    android.R.drawable.ic_menu_delete
            );

            private final Drawable favoriteIcon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.ic_favorite_24
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
                    drawSwipeBackground(
                            canvas,
                            itemView,
                            deleteBackground,
                            deleteIcon,
                            false
                    );
                } else if (dX > 0) {
                    drawSwipeBackground(
                            canvas,
                            itemView,
                            favoriteBackground,
                            favoriteIcon,
                            true
                    );
                }

                super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();

                if (position == RecyclerView.NO_POSITION || position >= kitapListesi.size()) {
                    return;
                }

                Kitap selectedBook = kitapListesi.get(position);

                if (selectedBook.getId() == null) {
                    adapter.notifyItemChanged(position);
                    return;
                }

                if (direction == ItemTouchHelper.LEFT) {
                    deleteBookWithUndo(selectedBook, position);
                } else if (direction == ItemTouchHelper.RIGHT) {
                    toggleFavorite(selectedBook, position);
                }
            }
        });

        touchHelper.attachToRecyclerView(recyclerBooks);
    }

    private void drawSwipeBackground(Canvas canvas,
                                     View itemView,
                                     ColorDrawable background,
                                     Drawable icon,
                                     boolean fromLeft) {

        if (fromLeft) {
            background.setBounds(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom()
            );
        } else {
            background.setBounds(
                    itemView.getLeft(),
                    itemView.getTop(),
                    itemView.getRight(),
                    itemView.getBottom()
            );
        }

        background.draw(canvas);

        if (icon != null) {
            icon.setTint(Color.WHITE);

            int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + icon.getIntrinsicHeight();

            int iconLeft;
            int iconRight;

            if (fromLeft) {
                iconLeft = itemView.getLeft() + iconMargin;
                iconRight = iconLeft + icon.getIntrinsicWidth();
            } else {
                iconRight = itemView.getRight() - iconMargin;
                iconLeft = iconRight - icon.getIntrinsicWidth();
            }

            icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            icon.draw(canvas);
        }
    }

    private void deleteBookWithUndo(Kitap deletedBook, int position) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            adapter.notifyItemChanged(position);
            Toast.makeText(requireContext(), "Kullanıcı oturumu bulunamadı.", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = currentUser.getUid();

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
        boolean nextFavorite = !kitap.isFavorite();

        kitap.setFavorite(nextFavorite);
        viewModel.persistKitap(kitap);

        adapter.notifyItemChanged(position);

        Snackbar.make(
                recyclerBooks,
                nextFavorite ? R.string.favorite_added : R.string.favorite_removed,
                Snackbar.LENGTH_SHORT
        ).show();
    }

    private void showBookDetailsDialog(@NonNull Kitap kitap) {
        String title = safeValue(kitap.getKitapAdi());
        String author = safeValue(kitap.getYazar());
        String genre = safeValue(kitap.getTur());
        String readState = kitap.isOkundu()
                ? getString(R.string.marked_as_read)
                : getString(R.string.marked_as_to_read);
        String readDate = kitap.isOkundu()
                ? getString(R.string.book_detail_read_date, formatDate(kitap.getUpdatedAt()))
                : getString(R.string.book_detail_not_read_yet);
        String rating = kitap.isOkundu()
                ? (kitap.isFavorite() ? "⭐⭐⭐⭐⭐" : "⭐⭐⭐⭐☆")
                : "-";

        String message =
                getString(R.string.book_detail_author, author) + "\n" +
                getString(R.string.book_detail_genre, genre) + "\n" +
                getString(R.string.book_detail_status, readState) + "\n" +
                getString(R.string.book_detail_rating, rating) + "\n" +
                readDate;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.dialog_close, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String safeValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.trim();
    }

    private String formatDate(long millis) {
        if (millis <= 0L) {
            return "-";
        }
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(millis));
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