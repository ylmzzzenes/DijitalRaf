package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.BuildConfig;
import com.example.dijitalraf.R;
import com.example.dijitalraf.data.AiRecommendationService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DashboardFragment extends Fragment {

    private static final String PREFS_DASHBOARD = "dashboard_prefs";
    private static final String KEY_LAST_AI = "last_ai_recommendation";
    private static final String KEY_LAST_CHAT_SNIPPET = "last_chat_snippet";
    private static final int DASHBOARD_BOOKS_MAX = 24;

    private BooksViewModel viewModel;
    private AiRecommendationService aiService;
    private MaterialButton btnAiRecommend;
    private MaterialButton btnAiAssistant;
    private TextView tvAiPreview;
    private TextView tvChatPreview;
    private TextView tvWelcome;
    private TextView tvAiHistoryLabel;
    private TextView tvChatHistoryLabel;
    private RecyclerView rvReadBooks;
    private RecyclerView rvToReadBooks;
    private TextView tvReadBooksEmpty;
    private TextView tvToReadBooksEmpty;
    private DashboardBookRowAdapter readAdapter;
    private DashboardBookRowAdapter toReadAdapter;
    private SharedPreferences dashboardPrefs;

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
        dashboardPrefs = requireContext().getSharedPreferences(PREFS_DASHBOARD, Context.MODE_PRIVATE);

        aiService = new AiRecommendationService();
        btnAiRecommend = view.findViewById(R.id.btnAiRecommend);
        btnAiAssistant = view.findViewById(R.id.btnAiAssistant);
        tvAiPreview = view.findViewById(R.id.tvAiPreview);
        tvChatPreview = view.findViewById(R.id.tvChatPreview);
        tvWelcome = view.findViewById(R.id.tvWelcome);
        tvAiHistoryLabel = view.findViewById(R.id.tvAiHistoryLabel);
        tvChatHistoryLabel = view.findViewById(R.id.tvChatHistoryLabel);
        rvReadBooks = view.findViewById(R.id.rvReadBooks);
        rvToReadBooks = view.findViewById(R.id.rvToReadBooks);
        tvReadBooksEmpty = view.findViewById(R.id.tvReadBooksEmpty);
        tvToReadBooksEmpty = view.findViewById(R.id.tvToReadBooksEmpty);

        readAdapter = new DashboardBookRowAdapter(kitap -> {
            if (kitap.getId() != null) {
                startActivity(BookDetailActivity.newIntent(requireContext(), kitap.getId()));
            } else if (requireActivity() instanceof HomeActivity) {
                ((HomeActivity) requireActivity()).openBookSection(true);
            }
        });
        toReadAdapter = new DashboardBookRowAdapter(kitap -> {
            if (kitap.getId() != null) {
                startActivity(BookDetailActivity.newIntent(requireContext(), kitap.getId()));
            } else if (requireActivity() instanceof HomeActivity) {
                ((HomeActivity) requireActivity()).openBookSection(false);
            }
        });

        rvReadBooks.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvReadBooks.setAdapter(readAdapter);
        rvToReadBooks.setLayoutManager(
                new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvToReadBooks.setAdapter(toReadAdapter);

        btnAiRecommend.setOnClickListener(v -> getAiRecommendations());
        tvAiHistoryLabel.setOnClickListener(v ->
                showHistoryOverlay(R.string.ai_history_dialog_title, KEY_LAST_AI, R.string.ai_no_history_yet)
        );
        tvAiHistoryLabel.setOnLongClickListener(v -> {
            showHistoryOverlay(R.string.ai_history_dialog_title, KEY_LAST_AI, R.string.ai_no_history_yet);
            return true;
        });
        btnAiAssistant.setOnClickListener(v -> {
            if (requireActivity() instanceof HomeActivity) {
                ((HomeActivity) requireActivity()).openChatAssistant();
            }
        });
        tvChatHistoryLabel.setOnClickListener(v ->
                showHistoryOverlay(
                        R.string.ai_chat_history_dialog_title,
                        KEY_LAST_CHAT_SNIPPET,
                        R.string.ai_chat_no_history_yet
                )
        );
        tvChatHistoryLabel.setOnLongClickListener(v -> {
            showHistoryOverlay(
                    R.string.ai_chat_history_dialog_title,
                    KEY_LAST_CHAT_SNIPPET,
                    R.string.ai_chat_no_history_yet
            );
            return true;
        });

        loadAiPreviewFromPrefs();
        loadChatPreviewFromPrefs();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            DatabaseReference ref = FirebaseDatabase
                    .getInstance("https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app")
                    .getReference("users")
                    .child(uid);

            ref.child("fullName").get().addOnSuccessListener(snapshot -> {
                if (snapshot.exists()) {
                    String fullName = snapshot.getValue(String.class);
                    if (fullName != null && !fullName.trim().isEmpty()) {
                        tvWelcome.setText(getString(R.string.welcome_with_name, fullName.trim()));
                    } else {
                        tvWelcome.setText(R.string.welcome_generic);
                    }
                } else {
                    tvWelcome.setText(R.string.welcome_generic);
                }
            }).addOnFailureListener(e -> tvWelcome.setText(R.string.welcome_generic));
        } else {
            tvWelcome.setText(R.string.welcome_generic);
        }

        viewModel.getBooks().observe(getViewLifecycleOwner(), this::applyBookRows);
    }

    private void applyBookRows(List<Kitap> books) {
        List<Kitap> read = new ArrayList<>();
        List<Kitap> toRead = new ArrayList<>();
        if (books != null) {
            for (Kitap k : books) {
                if (k.getId() == null) {
                    continue;
                }
                if (k.isOkundu()) {
                    read.add(k);
                } else {
                    toRead.add(k);
                }
            }
        }
        sortByUpdatedDesc(read);
        sortByUpdatedDesc(toRead);
        capList(read);
        capList(toRead);

        readAdapter.setBooks(read);
        toReadAdapter.setBooks(toRead);

        boolean readEmpty = read.isEmpty();
        tvReadBooksEmpty.setVisibility(readEmpty ? View.VISIBLE : View.GONE);
        rvReadBooks.setVisibility(readEmpty ? View.GONE : View.VISIBLE);

        boolean toReadEmpty = toRead.isEmpty();
        tvToReadBooksEmpty.setVisibility(toReadEmpty ? View.VISIBLE : View.GONE);
        rvToReadBooks.setVisibility(toReadEmpty ? View.GONE : View.VISIBLE);
    }

    private static void sortByUpdatedDesc(List<Kitap> list) {
        Collections.sort(list, (a, b) -> Long.compare(b.getUpdatedAt(), a.getUpdatedAt()));
    }

    private static void capList(List<Kitap> list) {
        while (list.size() > DASHBOARD_BOOKS_MAX) {
            list.remove(list.size() - 1);
        }
    }

    private void loadAiPreviewFromPrefs() {
        String saved = dashboardPrefs.getString(KEY_LAST_AI, "");
        if (saved != null && !saved.trim().isEmpty()) {
            tvAiPreview.setText(saved.trim());
            tvAiPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            tvAiPreview.setText(R.string.ai_preview_placeholder);
            tvAiPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void loadChatPreviewFromPrefs() {
        String saved = dashboardPrefs.getString(KEY_LAST_CHAT_SNIPPET, "");
        if (saved != null && !saved.trim().isEmpty()) {
            tvChatPreview.setText(saved.trim());
            tvChatPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        } else {
            tvChatPreview.setText(R.string.ai_chat_preview_placeholder);
            tvChatPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    /** AI öneri ve sohbet kartlarında geçmiş metni gösterir; boşsa Toast. */
    private void showHistoryOverlay(int titleRes, String prefsKey, int emptyToastRes) {
        String saved = dashboardPrefs.getString(prefsKey, "");
        if (saved == null || saved.trim().isEmpty()) {
            Toast.makeText(requireContext(), emptyToastRes, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(titleRes)
                .setMessage(saved.trim())
                .setPositiveButton(R.string.dialog_close, (d, w) -> d.dismiss())
                .show();
    }

    private void getAiRecommendations() {
        String apiKey = BuildConfig.OPENROUTER_API_KEY != null
                ? BuildConfig.OPENROUTER_API_KEY.trim()
                : "";
        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_openrouter_key_missing, Toast.LENGTH_LONG).show();
            return;
        }

        if (viewModel.getBooks().getValue() == null || viewModel.getBooks().getValue().isEmpty()) {
            Toast.makeText(requireContext(), "Öneri almak için önce birkaç kitap ekleyin.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiRecommend.setEnabled(false);
        btnAiRecommend.setText(R.string.ai_analyzing);
        tvAiPreview.setText(R.string.ai_analyzing);
        tvAiPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));

        aiService.getRecommendations(
                apiKey,
                viewModel.getBooks().getValue(),
                new AiRecommendationService.AiCallback() {
                    @Override
                    public void onSuccess(String result) {
                        btnAiRecommend.setEnabled(true);
                        btnAiRecommend.setText(R.string.ai_get_recommendation);
                        if (result != null && !result.trim().isEmpty()) {
                            dashboardPrefs.edit().putString(KEY_LAST_AI, result.trim()).apply();
                            tvAiPreview.setText(result.trim());
                            tvAiPreview.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
                        } else {
                            loadAiPreviewFromPrefs();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        btnAiRecommend.setEnabled(true);
                        btnAiRecommend.setText(R.string.ai_get_recommendation);
                        loadAiPreviewFromPrefs();
                        Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }
}
