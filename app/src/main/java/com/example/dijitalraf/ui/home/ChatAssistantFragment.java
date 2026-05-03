package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.BuildConfig;
import com.example.dijitalraf.R;
import com.example.dijitalraf.auth.EmailVerificationHelper;
import com.example.dijitalraf.data.AiService;
import com.example.dijitalraf.ui.util.UiMessages;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ChatAssistantFragment extends Fragment {

    private static final String PREFS_DASHBOARD = "dashboard_prefs";
    private static final String KEY_LAST_CHAT_SNIPPET = "last_chat_snippet";
    private static final int SNIPPET_MAX = 320;

    private BooksViewModel viewModel;
    private AiService aiService;
    private SharedPreferences prefs;
    private ChatMessagesAdapter adapter;
    private RecyclerView recyclerChat;
    private LinearLayout emptyState;
    private MaterialCardView cardError;
    private TextInputLayout tilMessage;
    private TextInputEditText etMessage;
    private MaterialButton btnSend;
    private boolean awaitingReply;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_assistant, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(BooksViewModel.class);
        aiService = new AiService(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_DASHBOARD, Context.MODE_PRIVATE);

        MaterialToolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(AppCompatResources.getDrawable(requireContext(), R.drawable.ic_home_24));
        toolbar.setNavigationContentDescription(R.string.chat_nav_home);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).openHomeDashboard();
            }
        });

        recyclerChat = view.findViewById(R.id.recyclerChat);
        emptyState = view.findViewById(R.id.emptyState);
        cardError = view.findViewById(R.id.cardError);
        tilMessage = view.findViewById(R.id.tilMessage);
        etMessage = view.findViewById(R.id.etMessage);
        btnSend = view.findViewById(R.id.btnSend);

        adapter = new ChatMessagesAdapter();
        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        recyclerChat.setLayoutManager(lm);
        recyclerChat.setAdapter(adapter);

        btnSend.setOnClickListener(v -> sendCurrentMessage());
        etMessage.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrentMessage();
                return true;
            }
            return false;
        });

        viewModel.getBooks().observe(getViewLifecycleOwner(), books -> updateEmptyState());

        updateEmptyState();
    }

    private void updateEmptyState() {
        boolean showEmpty = !adapter.hasUserMessage() && !awaitingReply;
        emptyState.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }

    private void scrollToBottom() {
        if (adapter.getItemCount() > 0) {
            recyclerChat.post(() ->
                    recyclerChat.smoothScrollToPosition(adapter.getItemCount() - 1));
        }
    }

    private void hideError() {
        cardError.setVisibility(View.GONE);
    }

    private void showError(@NonNull String message) {
        TextView tv = requireView().findViewById(R.id.tvError);
        tv.setText(message);
        cardError.setVisibility(View.VISIBLE);
    }

    private void sendCurrentMessage() {
        if (awaitingReply) {
            return;
        }
        String apiKey = BuildConfig.OPENROUTER_API_KEY != null
                ? BuildConfig.OPENROUTER_API_KEY.trim()
                : "";
        if (apiKey.isEmpty()) {
            UiMessages.snackbar(this, R.string.error_openrouter_key_missing, Snackbar.LENGTH_LONG, snackbarAnchorFab());
            showError(getString(R.string.error_openrouter_key_missing));
            return;
        }

        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) {
            tilMessage.setError(getString(R.string.chat_error_empty_message));
            return;
        }
        tilMessage.setError(null);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            UiMessages.snackbar(this, R.string.chat_error_not_signed_in, Snackbar.LENGTH_SHORT, snackbarAnchorFab());
            return;
        }

        if (EmailVerificationHelper.mustVerifyEmail(FirebaseAuth.getInstance().getCurrentUser())) {
            UiMessages.snackbar(this, R.string.feature_locked_email_unverified, Snackbar.LENGTH_LONG, snackbarAnchorFab());
            return;
        }

        hideError();
        emptyState.setVisibility(View.GONE);

        adapter.addRow(new ChatMessagesAdapter.Row(ChatMessagesAdapter.TYPE_USER, text));
        etMessage.setText("");
        scrollToBottom();

        adapter.addRow(new ChatMessagesAdapter.Row(ChatMessagesAdapter.TYPE_TYPING, ""));
        scrollToBottom();

        awaitingReply = true;
        btnSend.setEnabled(false);
        etMessage.setEnabled(false);

        JSONArray messages;
        try {
            messages = buildOpenRouterMessages(adapter.getRows());
        } catch (JSONException e) {
            awaitingReply = false;
            btnSend.setEnabled(true);
            etMessage.setEnabled(true);
            adapter.removeTypingIfAny();
            showError(getString(R.string.chat_error_build_request, e.getMessage()));
            updateEmptyState();
            return;
        }

        aiService.sendChatMessage(apiKey, messages, new AiService.LlmCallback() {
            @Override
            public void onSuccess(@NonNull String result) {
                if (!isAdded()) {
                    return;
                }
                awaitingReply = false;
                btnSend.setEnabled(true);
                etMessage.setEnabled(true);
                adapter.removeTypingIfAny();
                adapter.addRow(new ChatMessagesAdapter.Row(ChatMessagesAdapter.TYPE_ASSISTANT, result));
                saveSnippet(result);
                scrollToBottom();
                updateEmptyState();
            }

            @Override
            public void onError(@NonNull String error) {
                if (!isAdded()) {
                    return;
                }
                awaitingReply = false;
                btnSend.setEnabled(true);
                etMessage.setEnabled(true);
                adapter.removeTypingIfAny();
                showError(error);
                scrollToBottom();
                updateEmptyState();
            }
        });
    }

    private void saveSnippet(@NonNull String reply) {
        String snippet = reply.length() > SNIPPET_MAX ? reply.substring(0, SNIPPET_MAX) + "…" : reply;
        prefs.edit().putString(KEY_LAST_CHAT_SNIPPET, snippet).apply();
    }

    private JSONArray buildOpenRouterMessages(@NonNull List<ChatMessagesAdapter.Row> rows)
            throws JSONException {

        List<Kitap> books = viewModel.getBooks().getValue();
        String shelf = buildShelfContext(books);

        String systemContent = getString(R.string.chat_assistant_system_prompt) + "\n\n" + shelf;

        JSONArray messages = new JSONArray();
        JSONObject system = new JSONObject();
        system.put("role", "system");
        system.put("content", systemContent);
        messages.put(system);

        for (ChatMessagesAdapter.Row row : rows) {
            if (row.type == ChatMessagesAdapter.TYPE_TYPING) {
                continue;
            }
            JSONObject o = new JSONObject();
            if (row.type == ChatMessagesAdapter.TYPE_USER) {
                o.put("role", "user");
                o.put("content", row.text);
            } else {
                o.put("role", "assistant");
                o.put("content", row.text);
            }
            messages.put(o);
        }
        return messages;
    }

    @NonNull
    private String buildShelfContext(@Nullable List<Kitap> books) {
        if (books == null || books.isEmpty()) {
            return getString(R.string.chat_context_empty_shelf);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.chat_context_shelf_header));
        int n = 0;
        for (Kitap k : books) {
            if (k.getId() == null) {
                continue;
            }
            if (n >= 48) {
                break;
            }
            n++;
            sb.append("• ");
            sb.append(nullSafe(k.getKitapAdi()));
            sb.append(" — ");
            sb.append(nullSafe(k.getYazar()));
            sb.append(" — ");
            sb.append(nullSafe(k.getTur()));
            sb.append(k.isOkundu()
                    ? getString(R.string.chat_context_status_read) + "\n"
                    : getString(R.string.chat_context_status_to_read) + "\n");
        }
        return sb.toString();
    }

    private String nullSafe(String s) {
        return s == null || s.trim().isEmpty()
                ? getString(R.string.chat_context_placeholder)
                : s.trim();
    }

    @Nullable
    private View snackbarAnchorFab() {
        if (getActivity() == null) {
            return null;
        }
        return getActivity().findViewById(R.id.fabAddBook);
    }
}
