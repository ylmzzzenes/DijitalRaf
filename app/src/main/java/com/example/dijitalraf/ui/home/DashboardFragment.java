package com.example.dijitalraf.ui.home;

import android.app.Notification;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.BuildConfig;
import com.example.dijitalraf.R;
import com.example.dijitalraf.data.AiRecommendationService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class DashboardFragment extends Fragment {

    private BooksViewModel viewModel;
    private AiRecommendationService aiService;
    private MaterialButton btnAiRecommend;
    private TextView tvAiResult;
    private View progress;
    private TextView tvTotalBooks, tvFavorites;
    private MaterialButton btnAiChat;

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

        aiService = new AiRecommendationService();
        btnAiRecommend = view.findViewById(R.id.btnAiRecommend);
        tvAiResult = view.findViewById(R.id.tvAiResult);

        btnAiRecommend.setOnClickListener(v -> getAiRecommendations());
        tvTotalBooks = view.findViewById(R.id.tvTotalBooks);
        tvFavorites = view.findViewById(R.id.tvFavorites);
        btnAiChat = view.findViewById(R.id.btnAiChat);
        TextView tvWelcome = view.findViewById(R.id.tvWelcome);

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
                    tvWelcome.setText("Hoş geldin, " + fullName + " 👋");
                } else {
                    tvWelcome.setText("Hoş geldin 👋");
                }
            }).addOnFailureListener(e -> {
                tvWelcome.setText("Hoş geldin 👋");
            });
        }

        viewModel.getBooks().observe(getViewLifecycleOwner(), books ->{
            if(books != null){
                tvTotalBooks.setText(String.valueOf(books.size()));

                int favCount = 0;

                for(Kitap k : books){
                    if(k.isFavorite()) favCount ++;
                }
                tvFavorites.setText(String.valueOf(favCount));
            }
        });

        btnAiChat.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "Chat yakında eklenecek", Toast.LENGTH_SHORT).show();
        });


    }
    private void getAiRecommendations(){
        String apiKey = BuildConfig.OPENROUTER_API_KEY != null
                ? BuildConfig.OPENROUTER_API_KEY.trim()
                : "";
        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(), R.string.error_openrouter_key_missing, Toast.LENGTH_LONG).show();
            return;
        }

        if(viewModel.getBooks().getValue() == null || viewModel.getBooks().getValue().isEmpty()){
            Toast.makeText(requireContext(), "Öneri almak için önce birkaç kitap ekleyin.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnAiRecommend.setEnabled(false);
        btnAiRecommend.setText("Öneriler hazırlanıyor...");
        tvAiResult.setText("Ai kitap zevkinizi analiz ediyor");

        aiService.getRecommendations(
          apiKey,
                viewModel.getBooks().getValue(),
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
