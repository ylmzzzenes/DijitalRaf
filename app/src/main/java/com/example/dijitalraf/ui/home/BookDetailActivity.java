package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatRatingBar;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookDetailActivity extends AppCompatActivity {

    private static final String EXTRA_BOOK_ID = "extra_book_id";

    private static final int DESCRIPTION_COLLAPSED_LINES = 6;

    private static final String DATABASE_URL =
            "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

    public static Intent newIntent(@NonNull Context context, @NonNull String bookId) {
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(EXTRA_BOOK_ID, bookId);
        return intent;
    }

    private MaterialToolbar toolbar;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvGenre;
    private TextView tvDescription;
    private TextView tvPageCount;
    private TextView tvPublishedDate;
    private MaterialButton btnToggleDescription;
    private View progress;
    private View scrollContent;
    private MaterialButton btnFavorite;
    private MaterialSwitch switchRead;
    private TextInputEditText etPersonalNote;
    private MaterialButton btnSavePersonalNote;
    private MaterialButton btnDeletePersonalNote;
    private AppCompatRatingBar ratingBar;
    private boolean ratingBarProgrammatic;
    private boolean switchReadProgrammatic;

    private String bookIdArg;
    private BooksViewModel booksViewModel;
    @Nullable
    private DatabaseReference bookRef;
    @Nullable
    private ValueEventListener bookListener;

    private RecyclerView recyclerQuotes;
    private TextView tvQuotesEmpty;
    private MaterialButton btnAddQuote;
    private BookQuoteAdapter quoteAdapter;
    @Nullable
    private DatabaseReference quotesRef;
    @Nullable
    private ValueEventListener quotesListener;

    private String lastDescriptionRaw = "";
    private boolean descriptionExpanded;
    private boolean lastFavorite;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        booksViewModel = new ViewModelProvider(this).get(BooksViewModel.class);

        toolbar = findViewById(R.id.toolbar);
        ivCover = findViewById(R.id.ivCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvGenre = findViewById(R.id.tvGenre);
        tvDescription = findViewById(R.id.tvDescription);
        tvPageCount = findViewById(R.id.tvPageCount);
        tvPublishedDate = findViewById(R.id.tvPublishedDate);
        btnToggleDescription = findViewById(R.id.btnToggleDescription);
        progress = findViewById(R.id.progressLoading);
        scrollContent = findViewById(R.id.scrollContent);
        btnFavorite = findViewById(R.id.btnFavorite);
        switchRead = findViewById(R.id.switchRead);
        etPersonalNote = findViewById(R.id.etPersonalNote);
        btnSavePersonalNote = findViewById(R.id.btnSavePersonalNote);
        btnDeletePersonalNote = findViewById(R.id.btnDeletePersonalNote);
        ratingBar = findViewById(R.id.ratingBar);
        recyclerQuotes = findViewById(R.id.recyclerQuotes);
        tvQuotesEmpty = findViewById(R.id.tvQuotesEmpty);
        btnAddQuote = findViewById(R.id.btnAddQuote);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);
        if (TextUtils.isEmpty(bookId)) {
            Toast.makeText(this, R.string.book_detail_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        bookIdArg = bookId.trim();
        setupPersonalNoteActions();
        setupRatingBarListener();
        setupFavoriteButton();
        setupReadSwitch();
        setupQuotesSection();
        attachBookRealtimeListener(bookIdArg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookRef != null && bookListener != null) {
            bookRef.removeEventListener(bookListener);
        }
        bookRef = null;
        bookListener = null;
        if (quotesRef != null && quotesListener != null) {
            quotesRef.removeEventListener(quotesListener);
        }
        quotesRef = null;
        quotesListener = null;
    }

    private void attachBookRealtimeListener(@NonNull String bookId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.book_detail_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.INVISIBLE);

        bookRef = FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("books")
                .child(user.getUid())
                .child(bookId);

        attachQuotesListener();

        bookListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                progress.setVisibility(View.GONE);
                scrollContent.setVisibility(View.VISIBLE);

                if (!snapshot.exists()) {
                    Toast.makeText(BookDetailActivity.this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                Kitap kitap = snapshot.getValue(Kitap.class);
                if (kitap == null) {
                    Toast.makeText(BookDetailActivity.this, R.string.book_detail_load_error, Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                kitap.setId(bookId);
                bind(kitap);
                applyUserActionsMerged(kitap);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progress.setVisibility(View.GONE);
                Toast.makeText(
                        BookDetailActivity.this,
                        getString(R.string.error_generic, error.getMessage()),
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }
        };
        bookRef.addValueEventListener(bookListener);
    }

    private void bind(@NonNull Kitap kitap) {
        String title = nonEmptyOrFallback(kitap.getKitapAdi(), R.string.info_not_available);
        String author = nonEmptyOrFallback(kitap.getYazar(), R.string.info_not_available);
        String genre = nonEmptyOrFallback(kitap.getTur(), R.string.info_not_available);

        toolbar.setTitle(title);
        tvTitle.setText(title);
        tvAuthor.setText(getString(R.string.book_detail_author_line, author));
        tvGenre.setText(genre);

        bindDescription(kitap.getAciklama());

        String pages = kitap.getSayfaSayisi();
        if (TextUtils.isEmpty(pages) || pages.trim().isEmpty()) {
            tvPageCount.setText(R.string.info_not_available);
        } else {
            tvPageCount.setText(getString(R.string.book_detail_pages_value, pages.trim()));
        }

        String published = kitap.getYayinTarihi();
        if (TextUtils.isEmpty(published) || published.trim().isEmpty()) {
            tvPublishedDate.setText(R.string.info_not_available);
        } else {
            tvPublishedDate.setText(published.trim());
        }

        String imageUrl = kitap.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.ic_menu_book_24)
                    .error(R.drawable.ic_menu_book_24)
                    .centerCrop()
                    .into(ivCover);
        } else {
            Glide.with(this).clear(ivCover);
            ivCover.setImageResource(R.drawable.ic_menu_book_24);
        }
    }

    /**
     * Sunucudan gelen kitap ile cihazdaki tek kayıtlı kullanıcı durumunu birleştirir; yerel kayıt varsa önceliklidir.
     */
    private void applyUserActionsMerged(@NonNull Kitap kitap) {
        BookDetailUserActionsStore.Snapshot local =
                BookDetailUserActionsStore.load(this, bookIdArg);

        String mergedNote = kitap.getNote() != null ? kitap.getNote() : "";
        int mergedStars = kitap.getYildiz();
        boolean mergedFavorite = kitap.isFavorite();
        boolean mergedRead = kitap.isOkundu();

        if (local != null) {
            mergedNote = local.note;
            mergedStars = local.stars;
            mergedFavorite = local.favorite;
            mergedRead = local.read;
        }

        if (!etPersonalNote.hasFocus()) {
            etPersonalNote.setText(mergedNote);
            etPersonalNote.setSelection(mergedNote.length());
        }

        ratingBarProgrammatic = true;
        ratingBar.setRating(mergedStars);
        ratingBarProgrammatic = false;

        lastFavorite = mergedFavorite;
        applyFavoriteButtonUi(lastFavorite);
        applyReadSwitchFromKitap(mergedRead);

        if (local == null) {
            persistSnapshotFromUi();
        }
    }

    private void persistSnapshotFromUi() {
        if (bookIdArg == null) {
            return;
        }
        BookDetailUserActionsStore.Snapshot s = new BookDetailUserActionsStore.Snapshot();
        s.note = etPersonalNote.getText() != null ? etPersonalNote.getText().toString() : "";
        s.stars = Math.max(0, Math.min(5, (int) ratingBar.getRating()));
        s.favorite = lastFavorite;
        s.read = switchRead.isChecked();
        BookDetailUserActionsStore.save(this, bookIdArg, s);
    }

    private void bindDescription(@Nullable String descHtml) {
        String raw = descHtml == null ? "" : descHtml.trim();
        if (TextUtils.isEmpty(raw)) {
            lastDescriptionRaw = "";
            descriptionExpanded = false;
            tvDescription.setText(R.string.book_detail_empty_description);
            tvDescription.setMaxLines(Integer.MAX_VALUE);
            btnToggleDescription.setVisibility(View.GONE);
            btnToggleDescription.setOnClickListener(null);
            return;
        }

        boolean contentChanged = !raw.equals(lastDescriptionRaw);
        if (contentChanged) {
            lastDescriptionRaw = raw;
            descriptionExpanded = false;
        }

        CharSequence styled = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY);

        if (contentChanged || !descriptionExpanded) {
            tvDescription.setMaxLines(Integer.MAX_VALUE);
            tvDescription.setText(styled);
            btnToggleDescription.setVisibility(View.GONE);
            btnToggleDescription.setOnClickListener(null);
            tvDescription.post(this::applyDescriptionLineClamp);
        } else {
            btnToggleDescription.setVisibility(View.VISIBLE);
            btnToggleDescription.setText(R.string.book_detail_read_less);
            btnToggleDescription.setOnClickListener(v -> toggleDescriptionExpanded());
        }
    }

    private void applyDescriptionLineClamp() {
        Layout layout = tvDescription.getLayout();
        if (layout == null) {
            return;
        }
        int lines = layout.getLineCount();
        if (lines > DESCRIPTION_COLLAPSED_LINES) {
            if (!descriptionExpanded) {
                tvDescription.setMaxLines(DESCRIPTION_COLLAPSED_LINES);
            }
            btnToggleDescription.setVisibility(View.VISIBLE);
            btnToggleDescription.setText(descriptionExpanded
                    ? R.string.book_detail_read_less
                    : R.string.book_detail_read_more);
            btnToggleDescription.setOnClickListener(v -> toggleDescriptionExpanded());
        } else {
            btnToggleDescription.setVisibility(View.GONE);
            btnToggleDescription.setOnClickListener(null);
        }
    }

    private void toggleDescriptionExpanded() {
        descriptionExpanded = !descriptionExpanded;
        if (descriptionExpanded) {
            tvDescription.setMaxLines(Integer.MAX_VALUE);
            btnToggleDescription.setText(R.string.book_detail_read_less);
        } else {
            tvDescription.setMaxLines(DESCRIPTION_COLLAPSED_LINES);
            btnToggleDescription.setText(R.string.book_detail_read_more);
        }
    }

    @NonNull
    private String nonEmptyOrFallback(@Nullable String value, int emptyRes) {
        if (value == null || value.trim().isEmpty()) {
            return getString(emptyRes);
        }
        return value.trim();
    }

    private void setupFavoriteButton() {
        btnFavorite.setOnClickListener(v -> {
            if (bookIdArg == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }
            boolean next = !lastFavorite;
            lastFavorite = next;
            booksViewModel.updateBookFavorite(bookIdArg, next);
            applyFavoriteButtonUi(next);
            persistSnapshotFromUi();
            Toast.makeText(
                    this,
                    next ? R.string.favorite_added : R.string.favorite_removed,
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    private void applyFavoriteButtonUi(boolean favorite) {
        btnFavorite.setText(favorite ? R.string.book_detail_favorite_remove : R.string.book_detail_favorite_add);
        btnFavorite.setIcon(ContextCompat.getDrawable(
                this,
                favorite ? R.drawable.ic_favorite_24 : R.drawable.ic_favorite_border_24
        ));
    }

    private void setupReadSwitch() {
        switchRead.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (switchReadProgrammatic || bookIdArg == null
                    || FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }
            if (isChecked) {
                switchReadProgrammatic = true;
                switchRead.setChecked(false);
                switchReadProgrammatic = false;
                MarkAsReadDialogHelper.runWithConfirmationIfMarkingRead(BookDetailActivity.this, true, () -> {
                    switchReadProgrammatic = true;
                    switchRead.setChecked(true);
                    switchReadProgrammatic = false;
                    booksViewModel.updateBookOkundu(bookIdArg, true);
                    persistSnapshotFromUi();
                    Toast.makeText(this, R.string.marked_as_read, Toast.LENGTH_SHORT).show();
                });
            } else {
                booksViewModel.updateBookOkundu(bookIdArg, false);
                persistSnapshotFromUi();
                Toast.makeText(this, R.string.marked_as_to_read, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyReadSwitchFromKitap(boolean okundu) {
        switchReadProgrammatic = true;
        switchRead.setChecked(okundu);
        switchReadProgrammatic = false;
    }

    private void setupRatingBarListener() {
        ratingBar.setOnRatingBarChangeListener((bar, rating, fromUser) -> {
            if (ratingBarProgrammatic || !fromUser || bookIdArg == null) {
                return;
            }
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }
            int stars = Math.max(0, Math.min(5, (int) rating));
            booksViewModel.updateBookYildiz(bookIdArg, stars);
            persistSnapshotFromUi();
            Toast.makeText(this, R.string.rating_saved, Toast.LENGTH_SHORT).show();
        });
    }

    private void setupPersonalNoteActions() {
        btnSavePersonalNote.setOnClickListener(v -> {
            if (bookIdArg == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }
            String text = etPersonalNote.getText() != null
                    ? etPersonalNote.getText().toString().trim()
                    : "";
            int stars = Math.max(0, Math.min(5, (int) ratingBar.getRating()));
            booksViewModel.updateBookNote(bookIdArg, text);
            booksViewModel.updateBookYildiz(bookIdArg, stars);
            persistSnapshotFromUi();
            Toast.makeText(this, R.string.book_detail_actions_saved, Toast.LENGTH_SHORT).show();
        });

        btnDeletePersonalNote.setOnClickListener(v -> {
            if (bookIdArg == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
                return;
            }
            String current = etPersonalNote.getText() != null
                    ? etPersonalNote.getText().toString().trim()
                    : "";
            if (current.isEmpty()) {
                Toast.makeText(this, R.string.personal_note_already_empty, Toast.LENGTH_SHORT).show();
                return;
            }
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.personal_note_delete_confirm)
                    .setNegativeButton(R.string.dialog_close, (d, w) -> d.dismiss())
                    .setPositiveButton(R.string.action_delete_note, (d, w) -> {
                        booksViewModel.updateBookNote(bookIdArg, "");
                        etPersonalNote.setText("");
                        persistSnapshotFromUi();
                        Toast.makeText(this, R.string.personal_note_deleted, Toast.LENGTH_SHORT).show();
                    })
                    .show();
        });
    }

    private void setupQuotesSection() {
        recyclerQuotes.setLayoutManager(new LinearLayoutManager(this));
        recyclerQuotes.setItemAnimator(new DefaultItemAnimator());
        recyclerQuotes.setNestedScrollingEnabled(false);
        quoteAdapter = new BookQuoteAdapter(new BookQuoteAdapter.Listener() {
            @Override
            public void onEdit(@NonNull String quoteId, @NonNull String text) {
                showQuoteEditorDialog(true, quoteId, text);
            }

            @Override
            public void onDelete(@NonNull String quoteId, @NonNull String text) {
                showDeleteQuoteDialog(quoteId);
            }
        });
        recyclerQuotes.setAdapter(quoteAdapter);
        btnAddQuote.setOnClickListener(v -> showQuoteEditorDialog(false, null, null));
    }

    private void attachQuotesListener() {
        if (bookRef == null) {
            return;
        }
        if (quotesRef != null && quotesListener != null) {
            quotesRef.removeEventListener(quotesListener);
        }
        quotesRef = bookRef.child("quotes");
        quotesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applyQuotesSnapshot(snapshot);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Aynı kitap dinleyicisi hata verdiyse zaten toast/finish olabilir; burada sessiz kal.
            }
        };
        quotesRef.addValueEventListener(quotesListener);
    }

    private void applyQuotesSnapshot(@NonNull DataSnapshot snapshot) {
        List<BookQuoteAdapter.Item> rows = new ArrayList<>();
        for (DataSnapshot child : snapshot.getChildren()) {
            KitapAlinti a = child.getValue(KitapAlinti.class);
            if (a == null || a.getText() == null) {
                continue;
            }
            String t = a.getText().trim();
            if (t.isEmpty()) {
                continue;
            }
            String key = child.getKey();
            if (key == null) {
                continue;
            }
            rows.add(new BookQuoteAdapter.Item(key, t, a.getCreatedAt()));
        }
        Collections.sort(rows, (a, b) -> Long.compare(b.createdAt, a.createdAt));
        quoteAdapter.setItems(rows);
        boolean empty = rows.isEmpty();
        tvQuotesEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerQuotes.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showQuoteEditorDialog(boolean edit, @Nullable String quoteId, @Nullable String initialText) {
        View form = getLayoutInflater().inflate(R.layout.dialog_kitap_alinti_edit, null, false);
        TextInputEditText etQuote = form.findViewById(R.id.etQuote);
        if (initialText != null) {
            etQuote.setText(initialText);
            etQuote.setSelection(initialText.length());
        }
        int titleRes = edit ? R.string.book_quotes_dialog_edit_title : R.string.book_quotes_dialog_add_title;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(titleRes)
                .setView(form)
                .setNegativeButton(R.string.dialog_cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.book_quotes_save, null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positive.setOnClickListener(v -> {
                if (bookIdArg == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
                    dialog.dismiss();
                    return;
                }
                String raw = etQuote.getText() != null ? etQuote.getText().toString().trim() : "";
                if (raw.isEmpty()) {
                    Toast.makeText(BookDetailActivity.this, R.string.book_quotes_error_empty, Toast.LENGTH_SHORT)
                            .show();
                    return;
                }
                if (edit && quoteId != null) {
                    booksViewModel.updateBookQuote(bookIdArg, quoteId, raw);
                } else {
                    booksViewModel.addBookQuote(bookIdArg, raw);
                }
                Toast.makeText(BookDetailActivity.this, R.string.book_quotes_saved, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private void showDeleteQuoteDialog(@NonNull String quoteId) {
        if (bookIdArg == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setMessage(R.string.book_quotes_delete_confirm)
                .setNegativeButton(R.string.dialog_cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.action_delete_generic, (d, w) -> {
                    booksViewModel.deleteBookQuote(bookIdArg, quoteId);
                    Toast.makeText(this, R.string.book_quotes_deleted, Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
