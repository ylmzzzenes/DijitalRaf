package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import com.example.dijitalraf.data.AiRecommendationService;
import android.widget.Toast;

public class DashboardFragment extends Fragment {

    private BooksViewModel viewModel;
    private final List<Kitap> sourceBooks = new ArrayList<>();
    private final List<Kitap> displayedBooks = new ArrayList<>();
    private KitapCardAdapter adapter;
    private ChipGroup chipGroup;
    private TextInputEditText etSearch;
    private View progress;
    private View emptyState;
    private RecyclerView recyclerBooks;
    private String selectedFilter = "__ALL__";
    private AiRecommendationService aiService;
    private MaterialButton btnAiRecommend;
    private TextView tvAiResult;

    private static final String OPENROUTER_API_KEY = "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";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BooksViewModel.class);
        chipGroup = view.findViewById(R.id.chipGroupFilter);
        etSearch = view.findViewById(R.id.etSearch);
        progress = view.findViewById(R.id.progressLoading);
        emptyState = view.findViewById(R.id.emptyInclude);
        recyclerBooks = view.findViewById(R.id.recyclerBooks);

        recyclerBooks.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        recyclerBooks.setItemAnimator(new DefaultItemAnimator());
        adapter = new KitapCardAdapter(displayedBooks);
        recyclerBooks.setAdapter(adapter);

        aiService = new AiRecommendationService();
        btnAiRecommend = view.findViewById(R.id.btnAiRecommend);
        tvAiResult = view.findViewById(R.id.tvAiResult);

        btnAiRecommend.setOnClickListener(v -> getAiRecommendations());



        TextView tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);
        TextView tvEmptyMessage = view.findViewById(R.id.tvEmptyMessage);
        tvEmptyTitle.setText(R.string.empty_library_title);
        tvEmptyMessage.setText(R.string.empty_library_message);
        MaterialButton btnEmpty = view.findViewById(R.id.btnEmptyAction);
        btnEmpty.setVisibility(View.VISIBLE);
        btnEmpty.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), KitapEkleActivity.class)));

        viewModel.getLoading().observe(getViewLifecycleOwner(), loading -> {
            if (Boolean.TRUE.equals(loading)) {
                progress.setVisibility(View.VISIBLE);
            } else {
                progress.setVisibility(View.GONE);
            }
            updateEmptyState(view);
        });

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> {
            sourceBooks.clear();
            if (books != null) {
                sourceBooks.addAll(books);
            }
            rebuildCategoryChips();
            applyFiltersAndSearch();
            updateEmptyState(view);
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFiltersAndSearch();
                updateEmptyState(view);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                return;
            }
            Chip chip = group.findViewById(checkedId);
            if (chip != null && chip.getTag() != null) {
                selectedFilter = String.valueOf(chip.getTag());
                applyFiltersAndSearch();
                updateEmptyState(view);
            }
        });
    }

    private void rebuildCategoryChips() {
        chipGroup.removeAllViews();
        chipGroup.clearCheck();

        Chip all = new Chip(requireContext());
        all.setId(View.generateViewId());
        all.setText(R.string.filter_all);
        all.setTag("__ALL__");
        all.setCheckable(true);
        chipGroup.addView(all);

        TreeSet<String> categories = new TreeSet<>();
        for (Kitap k : sourceBooks) {
            if (k.getTur() != null && !k.getTur().trim().isEmpty()) {
                categories.add(k.getTur().trim());
            }
        }
        for (String c : categories) {
            Chip chip = new Chip(requireContext());
            chip.setId(View.generateViewId());
            chip.setText(c);
            chip.setTag(c);
            chip.setCheckable(true);
            chipGroup.addView(chip);
        }

        chipGroup.check(all.getId());
        selectedFilter = "__ALL__";
    }

    private void applyFiltersAndSearch() {
        displayedBooks.clear();
        String q = etSearch.getText().toString().trim().toLowerCase(Locale.getDefault());
        for (Kitap k : sourceBooks) {
            if (!"__ALL__".equals(selectedFilter)) {
                String tur = k.getTur() != null ? k.getTur() : "";
                if (!selectedFilter.equals(tur)) {
                    continue;
                }
            }
            if (!q.isEmpty()) {
                String hay = ((k.getKitapAdi() != null ? k.getKitapAdi() : "") + " "
                        + (k.getYazar() != null ? k.getYazar() : "")).toLowerCase(Locale.getDefault());
                if (!hay.contains(q)) {
                    continue;
                }
            }
            displayedBooks.add(k);
        }
        adapter.notifyDataSetChanged();
    }

    private void updateEmptyState(View root) {
        Boolean loading = viewModel.getLoading().getValue();
        boolean isLoading = Boolean.TRUE.equals(loading);
        if (isLoading) {
            emptyState.setVisibility(View.GONE);
            recyclerBooks.setVisibility(View.VISIBLE);
            return;
        }
        boolean showEmpty = displayedBooks.isEmpty();
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
        recyclerBooks.setVisibility(showEmpty ? View.GONE : View.VISIBLE);
    }

    private void getAiRecommendations(){
        if(sourceBooks.isEmpty()){
            Toast.makeText(requireContext(), "Öneri almak için önce birkaç kitap ekleyin.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiRecommend.setEnabled(false);
        btnAiRecommend.setText("Öneriler hazırlanıyor...");
        tvAiResult.setText("Ai kitap zevkinizi analiz ediyor");

        aiService.getRecommendations(
          OPENROUTER_API_KEY,
          sourceBooks,
                new AiRecommendationService.AiCallback() {
                    @Override
                    public void onSuccess(String result) {
                        btnAiRecommend.setEnabled(true);
                        btnAiRecommend.setText("AI Öneri Al");
                        tvAiResult.setText(result);
                    }

                    @Override
                    public void onError(String error) {
                        btnAiRecommend.setEnabled(true);
                        btnAiRecommend.setText("AI Öneri Al");
                        tvAiResult.setText("Öneri alınamadı.");
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    }
          }
        );
    }
}
