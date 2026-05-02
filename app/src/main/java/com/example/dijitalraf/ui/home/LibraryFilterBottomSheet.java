package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class LibraryFilterBottomSheet extends BottomSheetDialogFragment {

    public static LibraryFilterBottomSheet newInstance() {
        return new LibraryFilterBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_library_filters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BooksViewModel booksViewModel = new ViewModelProvider(requireActivity()).get(BooksViewModel.class);
        LibraryFilterViewModel filterViewModel = new ViewModelProvider(requireActivity())
                .get(LibraryFilterViewModel.class);

        TextInputEditText etAuthor = view.findViewById(R.id.etFilterAuthor);
        MaterialAutoCompleteTextView actGenre = view.findViewById(R.id.actFilterGenre);
        RadioGroup rgRead = view.findViewById(R.id.rgRead);
        MaterialRadioButton rbReadAll = view.findViewById(R.id.rbReadAll);
        MaterialRadioButton rbReadYes = view.findViewById(R.id.rbReadYes);
        MaterialRadioButton rbReadNo = view.findViewById(R.id.rbReadNo);
        RadioGroup rgFavorite = view.findViewById(R.id.rgFavorite);
        MaterialRadioButton rbFavAll = view.findViewById(R.id.rbFavAll);
        MaterialRadioButton rbFavYes = view.findViewById(R.id.rbFavYes);
        MaterialRadioButton rbFavNo = view.findViewById(R.id.rbFavNo);
        Spinner spinnerMinStars = view.findViewById(R.id.spinnerMinStars);
        TextInputEditText etYear = view.findViewById(R.id.etFilterYear);
        TextInputLayout tilYear = view.findViewById(R.id.tilFilterYear);
        MaterialButton btnApply = view.findViewById(R.id.btnSheetApply);
        MaterialButton btnSheetClear = view.findViewById(R.id.btnSheetClear);

        ArrayAdapter<CharSequence> starsAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.library_filter_min_stars_labels,
                android.R.layout.simple_spinner_dropdown_item
        );
        spinnerMinStars.setAdapter(starsAdapter);

        List<Kitap> books = booksViewModel.getBooks().getValue();
        List<String> genreRows = new ArrayList<>();
        genreRows.add(getString(R.string.library_filter_genre_all));
        TreeSet<String> sorted = new TreeSet<>(java.text.Collator.getInstance(new Locale("tr", "TR")));
        if (books != null) {
            for (Kitap k : books) {
                if (k == null || k.getId() == null) {
                    continue;
                }
                String t = k.getTur();
                if (t != null && !t.trim().isEmpty()) {
                    sorted.add(t.trim());
                }
            }
        }
        genreRows.addAll(sorted);
        ArrayAdapter<String> genreAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                genreRows
        );
        actGenre.setAdapter(genreAdapter);

        LibraryFilterSpec spec = filterViewModel.getSpec().getValue();
        if (spec == null) {
            spec = new LibraryFilterSpec();
        }
        etAuthor.setText(spec.authorContains);
        if (!TextUtils.isEmpty(spec.genreExact)) {
            actGenre.setText(spec.genreExact, false);
        } else {
            actGenre.setText(getString(R.string.library_filter_genre_all), false);
        }
        if (Boolean.TRUE.equals(spec.readOkundu)) {
            rbReadYes.setChecked(true);
        } else if (Boolean.FALSE.equals(spec.readOkundu)) {
            rbReadNo.setChecked(true);
        } else {
            rbReadAll.setChecked(true);
        }
        if (Boolean.TRUE.equals(spec.favorite)) {
            rbFavYes.setChecked(true);
        } else if (Boolean.FALSE.equals(spec.favorite)) {
            rbFavNo.setChecked(true);
        } else {
            rbFavAll.setChecked(true);
        }
        if (spec.minStars != null && spec.minStars >= 1 && spec.minStars <= 5) {
            spinnerMinStars.setSelection(spec.minStars);
        } else {
            spinnerMinStars.setSelection(0);
        }
        if (spec.yearExact != null) {
            etYear.setText(String.valueOf(spec.yearExact));
        }

        btnSheetClear.setOnClickListener(v -> {
            LibraryFilterSpec cur = filterViewModel.getSpec().getValue();
            LibraryFilterSpec n = new LibraryFilterSpec();
            if (cur != null) {
                n.quickSearch = cur.quickSearch;
            }
            filterViewModel.setSpec(n);
            dismiss();
        });

        btnApply.setOnClickListener(v -> {
            LibraryFilterSpec cur = filterViewModel.getSpec().getValue();
            LibraryFilterSpec n = cur != null ? cur.copy() : new LibraryFilterSpec();

            n.authorContains = etAuthor.getText() != null ? etAuthor.getText().toString().trim() : "";

            String genrePick = actGenre.getText() != null ? actGenre.getText().toString().trim() : "";
            if (genrePick.isEmpty() || genrePick.equals(getString(R.string.library_filter_genre_all))) {
                n.genreExact = "";
            } else {
                n.genreExact = genrePick;
            }

            int readId = rgRead.getCheckedRadioButtonId();
            if (readId == R.id.rbReadYes) {
                n.readOkundu = true;
            } else if (readId == R.id.rbReadNo) {
                n.readOkundu = false;
            } else {
                n.readOkundu = null;
            }

            int favId = rgFavorite.getCheckedRadioButtonId();
            if (favId == R.id.rbFavYes) {
                n.favorite = true;
            } else if (favId == R.id.rbFavNo) {
                n.favorite = false;
            } else {
                n.favorite = null;
            }

            int starPos = spinnerMinStars.getSelectedItemPosition();
            n.minStars = starPos <= 0 ? null : starPos;

            tilYear.setError(null);
            String yearRaw = etYear.getText() != null ? etYear.getText().toString().trim() : "";
            if (yearRaw.isEmpty()) {
                n.yearExact = null;
            } else {
                try {
                    int y = Integer.parseInt(yearRaw);
                    if (y < 1000 || y > 2999) {
                        tilYear.setError(getString(R.string.library_filter_year_invalid));
                        return;
                    }
                    n.yearExact = y;
                } catch (NumberFormatException e) {
                    tilYear.setError(getString(R.string.library_filter_year_invalid));
                    return;
                }
            }

            filterViewModel.setSpec(n);
            dismiss();
        });
    }
}
