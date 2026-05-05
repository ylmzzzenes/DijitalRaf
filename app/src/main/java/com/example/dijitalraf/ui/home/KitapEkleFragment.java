package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.example.dijitalraf.auth.EmailVerificationHelper;
import com.example.dijitalraf.core.constants.DatabasePaths;
import com.example.dijitalraf.data.model.BookMetadata;
import com.example.dijitalraf.data.repository.OpenLibraryRepository;
import com.example.dijitalraf.di.AppContainer;
import com.example.dijitalraf.ui.util.UiMessages;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.HashMap;
import java.util.Map;

public class KitapEkleFragment extends Fragment {

    private MaterialToolbar toolbar;
    private TextInputEditText etKitapAdi, etYazar, etTur;
    private TextInputLayout tilKitapAdi, tilYazar, tilTur;
    private MaterialButton btnKaydet, btnAra;
    private TextView tvPreviewTitle, tvPreviewAuthor;
    private Chip chipPreviewGenre;
    private LinearProgressIndicator progressApi;
    private BooksViewModel booksViewModel;
    private OpenLibraryRepository openLibraryRepository;
    private TextWatcher previewWatcher;

    private String selectedImageUrl = "";
    private String apiAciklama = "";
    private String apiSayfaSayisi = "";
    private String apiYayinTarihi = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_kitap_ekle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            dismissKitapEkleOverlay();
            return;
        }
        if (EmailVerificationHelper.mustVerifyEmail(FirebaseAuth.getInstance().getCurrentUser())) {
            UiMessages.snackbar(
                    requireActivity(),
                    R.string.feature_locked_email_unverified,
                    Snackbar.LENGTH_LONG,
                    snackbarAnchorFab());
            dismissKitapEkleOverlay();
            return;
        }

        initComponents(view);
        setupToolbar();
        registerEventHandlers();
        updatePreview();
    }

    private void initComponents(@NonNull View view) {
        initViews(view);
        booksViewModel = new ViewModelProvider(requireActivity()).get(BooksViewModel.class);
        openLibraryRepository = AppContainer.from(requireContext()).getOpenLibraryRepository();

        previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updatePreview();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
    }

    private void registerEventHandlers() {
        etKitapAdi.addTextChangedListener(previewWatcher);
        etYazar.addTextChangedListener(previewWatcher);
        etTur.addTextChangedListener(previewWatcher);

        btnKaydet.setOnClickListener(v -> kitapKaydet());
        btnAra.setOnClickListener(v -> kitapAra());
    }

    private void dismissKitapEkleOverlay() {
        if (getActivity() instanceof HomeActivity) {
            ((HomeActivity) getActivity()).dismissKitapEkleOverlay();
        }
    }

    private void initViews(@NonNull View root) {
        toolbar = root.findViewById(R.id.toolbar);
        etKitapAdi = root.findViewById(R.id.etKitapAdi);
        etYazar = root.findViewById(R.id.etYazar);
        etTur = root.findViewById(R.id.etTur);
        tilKitapAdi = root.findViewById(R.id.tilKitapAdi);
        tilYazar = root.findViewById(R.id.tilYazar);
        tilTur = root.findViewById(R.id.tilTur);
        btnKaydet = root.findViewById(R.id.btnKaydet);
        btnAra = root.findViewById(R.id.btnAra);
        tvPreviewTitle = root.findViewById(R.id.tvPreviewTitle);
        tvPreviewAuthor = root.findViewById(R.id.tvPreviewAuthor);
        chipPreviewGenre = root.findViewById(R.id.chipPreviewGenre);
        progressApi = root.findViewById(R.id.progressApi);
    }

    private void updatePreview() {
        String title = etKitapAdi.getText() != null ? etKitapAdi.getText().toString().trim() : "";
        String author = etYazar.getText() != null ? etYazar.getText().toString().trim() : "";
        String genre = etTur.getText() != null ? etTur.getText().toString().trim() : "";

        tvPreviewTitle.setText(title.isEmpty() ? getString(R.string.hint_book_title) : title);
        tvPreviewAuthor.setText(author.isEmpty() ? getString(R.string.hint_author) : author);

        if (genre.isEmpty()) {
            chipPreviewGenre.setVisibility(View.GONE);
        } else {
            chipPreviewGenre.setVisibility(View.VISIBLE);
            chipPreviewGenre.setText(genre);
        }
    }

    private void setupToolbar() {
        toolbar.setNavigationOnClickListener(v -> dismissKitapEkleOverlay());
    }

    private void setApiLoading(boolean loading) {
        btnAra.setEnabled(!loading);
        progressApi.setVisibility(loading ? View.VISIBLE : View.GONE);

        if (loading) {
            progressApi.setIndeterminate(true);
        }
    }

    private void kitapAra() {
        String query = etKitapAdi.getText() != null ? etKitapAdi.getText().toString().trim() : "";

        if (TextUtils.isEmpty(query)) {
            tilKitapAdi.setError(getString(R.string.error_enter_title_first));
            UiMessages.snackbar(this, R.string.error_search_empty, Snackbar.LENGTH_SHORT, snackbarAnchorFab());
            etKitapAdi.requestFocus();
            return;
        }

        selectedImageUrl = "";
        apiAciklama = "";
        apiSayfaSayisi = "";
        apiYayinTarihi = "";
        tilKitapAdi.setError(null);
        setApiLoading(true);

        searchBookFromOpenLibrary(query);
    }

    private void searchBookFromOpenLibrary(final String queryTrimmed) {
        openLibraryRepository.searchBooks(queryTrimmed, new OpenLibraryRepository.Callback() {
            @Override
            public void onSuccess(@NonNull BookMetadata metadata) {
                requireActivity().runOnUiThread(() -> {
                    setApiLoading(false);
                    if (!isAdded()) {
                        return;
                    }
                    etKitapAdi.setText(metadata.title);
                    etYazar.setText(metadata.author);
                    etTur.setText(metadata.genre);
                    selectedImageUrl = metadata.coverUrl;
                    apiAciklama = metadata.description;
                    apiSayfaSayisi = metadata.pageCount;
                    apiYayinTarihi = metadata.publishedDate;
                    updatePreview();
                    UiMessages.snackbar(
                            KitapEkleFragment.this,
                            R.string.book_info_loaded,
                            Snackbar.LENGTH_SHORT,
                            snackbarAnchorFab());
                });
            }

            @Override
            public void onNotFound() {
                requireActivity().runOnUiThread(() -> {
                    setApiLoading(false);
                    if (!isAdded()) {
                        return;
                    }
                    UiMessages.snackbar(
                            KitapEkleFragment.this,
                            R.string.open_library_no_results,
                            Snackbar.LENGTH_LONG,
                            snackbarAnchorFab());
                });
            }

            @Override
            public void onError(@NonNull String message) {
                requireActivity().runOnUiThread(() -> {
                    setApiLoading(false);
                    if (!isAdded()) {
                        return;
                    }
                    UiMessages.snackbar(
                            KitapEkleFragment.this,
                            getString(R.string.api_error, message),
                            Snackbar.LENGTH_LONG,
                            snackbarAnchorFab());
                });
            }
        });
    }

    private void kitapKaydet() {
        String kitapAdi = etKitapAdi.getText() != null ? etKitapAdi.getText().toString().trim() : "";
        String yazar = etYazar.getText() != null ? etYazar.getText().toString().trim() : "";
        String tur = etTur.getText() != null ? etTur.getText().toString().trim() : "";

        tilKitapAdi.setError(null);
        tilYazar.setError(null);
        tilTur.setError(null);

        if (kitapAdi.isEmpty()) {
            tilKitapAdi.setError(getString(R.string.field_required));
            etKitapAdi.requestFocus();
            return;
        }

        if (yazar.isEmpty()) {
            tilYazar.setError(getString(R.string.field_required));
            etYazar.requestFocus();
            return;
        }

        if (tur.isEmpty()) {
            tilTur.setError(getString(R.string.field_required));
            etTur.requestFocus();
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> kitap = new HashMap<>();
        kitap.put(DatabasePaths.FIELD_BOOK_TITLE, kitapAdi);
        kitap.put(DatabasePaths.FIELD_BOOK_AUTHOR, yazar);
        kitap.put(DatabasePaths.FIELD_BOOK_GENRE, tur);
        kitap.put(DatabasePaths.FIELD_BOOK_IMAGE_URL, selectedImageUrl);
        kitap.put(DatabasePaths.FIELD_BOOK_FAVORITE, false);
        kitap.put(DatabasePaths.FIELD_BOOK_READ, false);
        kitap.put(DatabasePaths.FIELD_BOOK_NOTE, "");
        kitap.put(DatabasePaths.FIELD_BOOK_STARS, 0);
        kitap.put(DatabasePaths.FIELD_CREATED_AT, now);
        kitap.put(DatabasePaths.FIELD_UPDATED_AT, now);

        if (apiAciklama != null && !apiAciklama.trim().isEmpty()) {
            kitap.put(DatabasePaths.FIELD_BOOK_DESCRIPTION, apiAciklama.trim());
        }
        if (apiSayfaSayisi != null && !apiSayfaSayisi.trim().isEmpty()) {
            kitap.put(DatabasePaths.FIELD_BOOK_PAGE_COUNT, apiSayfaSayisi.trim());
        }
        if (apiYayinTarihi != null && !apiYayinTarihi.trim().isEmpty()) {
            kitap.put(DatabasePaths.FIELD_BOOK_PUBLISHED_DATE, apiYayinTarihi.trim());
        }

        booksViewModel.addBook(kitap)
                .addOnSuccessListener(unused -> {
                    UiMessages.snackbar(
                            requireActivity(),
                            R.string.book_added,
                            Snackbar.LENGTH_SHORT,
                            snackbarAnchorFab());
                    dismissKitapEkleOverlay();
                })
                .addOnFailureListener(e ->
                        UiMessages.snackbar(
                                KitapEkleFragment.this,
                                getString(R.string.error_generic, e.getMessage()),
                                Snackbar.LENGTH_LONG,
                                snackbarAnchorFab())
                );
    }

    @Nullable
    private View snackbarAnchorFab() {
        if (getActivity() == null) {
            return null;
        }
        return getActivity().findViewById(R.id.fabAddBook);
    }
}