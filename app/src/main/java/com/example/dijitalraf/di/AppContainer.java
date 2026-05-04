package com.example.dijitalraf.di;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.dijitalraf.DijitalRafApplication;
import com.example.dijitalraf.core.constants.FirebaseConstants;
import com.example.dijitalraf.data.AiService;
import com.example.dijitalraf.data.remote.firebase.FirebaseAuthDataSource;
import com.example.dijitalraf.data.remote.firebase.FirebaseBooksDataSource;
import com.example.dijitalraf.data.remote.firebase.FirebaseStorageDataSource;
import com.example.dijitalraf.data.remote.firebase.FirebaseUserDataSource;
import com.example.dijitalraf.data.repository.AiRepository;
import com.example.dijitalraf.data.repository.AuthRepository;
import com.example.dijitalraf.data.repository.BooksRepository;
import com.example.dijitalraf.data.repository.DefaultAuthRepository;
import com.example.dijitalraf.data.repository.DefaultBooksRepository;
import com.example.dijitalraf.data.repository.DefaultOpenLibraryRepository;
import com.example.dijitalraf.data.repository.DefaultReadingGoalRepository;
import com.example.dijitalraf.data.repository.DefaultStorageRepository;
import com.example.dijitalraf.data.repository.DefaultUserRepository;
import com.example.dijitalraf.data.repository.OpenLibraryRepository;
import com.example.dijitalraf.data.repository.OpenRouterAiRepository;
import com.example.dijitalraf.data.repository.ReadingGoalRepository;
import com.example.dijitalraf.data.repository.StorageRepository;
import com.example.dijitalraf.data.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

/**
 * Manual dependency container for app-wide services.
 */
public final class AppContainer {

    private final Context appContext;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseDatabase realtimeDatabase;
    private final FirebaseStorage firebaseStorage;
    private final OkHttpClient defaultHttpClient;
    private final OkHttpClient openRouterHttpClient;

    private final AuthRepository authRepository;
    private final UserRepository userRepository;
    private final StorageRepository storageRepository;
    private final OpenLibraryRepository openLibraryRepository;
    private final AiRepository aiRepository;
    private final ReadingGoalRepository readingGoalRepository;

    public AppContainer(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.realtimeDatabase = FirebaseDatabase.getInstance(FirebaseConstants.RTDB_URL);
        this.firebaseStorage = FirebaseStorage.getInstance();
        this.defaultHttpClient = new OkHttpClient();
        this.openRouterHttpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        FirebaseAuthDataSource authDataSource = new FirebaseAuthDataSource(firebaseAuth);
        FirebaseUserDataSource userDataSource = new FirebaseUserDataSource(realtimeDatabase);
        FirebaseStorageDataSource storageDataSource = new FirebaseStorageDataSource(firebaseStorage);

        this.authRepository = new DefaultAuthRepository(authDataSource);
        this.userRepository = new DefaultUserRepository(userDataSource);
        this.storageRepository = new DefaultStorageRepository(storageDataSource);
        this.openLibraryRepository = new DefaultOpenLibraryRepository(defaultHttpClient);
        this.aiRepository = new OpenRouterAiRepository(new AiService(appContext, openRouterHttpClient));
        this.readingGoalRepository = new DefaultReadingGoalRepository(appContext);
    }

    @NonNull
    public static AppContainer from(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof DijitalRafApplication) {
            return ((DijitalRafApplication) appContext).getAppContainer();
        }
        throw new IllegalStateException("DijitalRafApplication is required for dependency access");
    }

    @NonNull
    public AuthRepository getAuthRepository() {
        return authRepository;
    }

    @NonNull
    public UserRepository getUserRepository() {
        return userRepository;
    }

    @NonNull
    public StorageRepository getStorageRepository() {
        return storageRepository;
    }

    @NonNull
    public OpenLibraryRepository getOpenLibraryRepository() {
        return openLibraryRepository;
    }

    @NonNull
    public AiRepository getAiRepository() {
        return aiRepository;
    }

    @NonNull
    public ReadingGoalRepository getReadingGoalRepository() {
        return readingGoalRepository;
    }

    @NonNull
    public BooksRepository createBooksRepository() {
        return new DefaultBooksRepository(new FirebaseBooksDataSource(firebaseAuth, realtimeDatabase));
    }
}
