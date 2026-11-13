package com.example.chatapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignInBinding;
import com.example.chatapp.firebase.FcmTokenManager;
import com.example.chatapp.firebase.FirebaseErrorMessages;
import com.example.chatapp.firebase.UserProfile;
import com.example.chatapp.firebase.UserRepository;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.InsetsUtils;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.ValidationUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Launcher screen that signs the user in with email and password.
 *
 * <p>If Firebase already has a signed-in user, the screen forwards straight to
 * {@link MainActivity}. The saved preference flag is never enough on its own: a stale flag
 * without a Firebase user is cleared. After a successful sign-in the user's Firestore profile
 * is loaded and cached; if the profile is missing, the user is sent to the profile-completion
 * flow in {@link SignUpActivity}.</p>
 */
public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth auth;
    private boolean requestInProgress;

    /**
     * Restores an existing session if present, otherwise builds the sign-in form.
     *
     * @param savedInstanceState previously saved state, or {@code null}
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(this);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            openAndClearTask(MainActivity.class);
            return;
        }
        // A saved flag without a Firebase user is stale and must not grant access.
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)) {
            preferenceManager.clear();
        }

        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtils.applySystemBarAndImePadding(binding.getRoot());
        setListeners();
    }

    /**
     * Connects the buttons, the keyboard "done" action, and error clearing on edit.
     */
    private void setListeners() {
        binding.buttonSignIn.setOnClickListener(v -> attemptSignIn());
        binding.buttonCreateAccount.setOnClickListener(v ->
                startActivity(new Intent(this, SignUpActivity.class)));
        binding.inputPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptSignIn();
                return true;
            }
            return false;
        });
        binding.inputEmail.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.emailLayout.setError(null);
            }
        });
        binding.inputPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.passwordLayout.setError(null);
            }
        });
    }

    /**
     * Validates the form and, if valid, signs in with Firebase Authentication. Ignored while
     * a request is already running, which prevents duplicate submissions.
     */
    private void attemptSignIn() {
        if (requestInProgress) {
            return;
        }
        String email = textOf(binding.inputEmail.getText()).trim();
        String password = textOf(binding.inputPassword.getText());
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        ValidationUtils.Result result = ValidationUtils.validateSignIn(email, password);
        if (result != ValidationUtils.Result.VALID) {
            showValidationError(result);
            return;
        }

        setLoading(true);
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (task.isSuccessful() && user != null) {
                loadProfile(user);
            } else {
                setLoading(false);
                showMessage(FirebaseErrorMessages.from(this, task.getException()));
            }
        });
    }

    /**
     * Loads the signed-in user's profile, then caches it and opens {@link MainActivity}. A
     * missing profile opens the profile-completion flow. If loading fails, the user is signed
     * out again so the app is not left in a half-signed-in state, and the form is restored.
     *
     * @param user the newly signed-in Firebase user
     */
    private void loadProfile(@NonNull FirebaseUser user) {
        final String userId = user.getUid();
        UserRepository.loadProfile(userId).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            FirebaseUser current = auth.getCurrentUser();
            if (current == null || !userId.equals(current.getUid())) {
                setLoading(false);
                return;
            }
            if (!task.isSuccessful()) {
                auth.signOut();
                preferenceManager.clear();
                setLoading(false);
                showMessage(getString(R.string.error_profile_load_on_sign_in,
                        FirebaseErrorMessages.from(this, task.getException())));
                return;
            }
            UserProfile profile = task.getResult();
            FcmTokenManager.registerCurrentToken(this);
            if (profile == null) {
                Intent intent = new Intent(this, SignUpActivity.class)
                        .putExtra(Constants.EXTRA_COMPLETE_PROFILE, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            preferenceManager.saveUserProfile(userId, profile.getName(),
                    profile.getEmail(), profile.getImage());
            openAndClearTask(MainActivity.class);
        });
    }

    /**
     * Shows a validation error on the matching field and moves focus there.
     *
     * @param result the validation problem to display
     */
    private void showValidationError(@NonNull ValidationUtils.Result result) {
        switch (result) {
            case EMAIL_REQUIRED:
                binding.emailLayout.setError(getString(R.string.error_email_required));
                binding.inputEmail.requestFocus();
                break;
            case EMAIL_INVALID:
                binding.emailLayout.setError(getString(R.string.error_email_invalid));
                binding.inputEmail.requestFocus();
                break;
            case PASSWORD_REQUIRED:
                binding.passwordLayout.setError(getString(R.string.error_password_required));
                binding.inputPassword.requestFocus();
                break;
            default:
                showMessage(getString(R.string.error_generic));
                break;
        }
    }

    /**
     * Switches between the loading state and the editable form.
     *
     * @param loading {@code true} to show progress and disable input
     */
    private void setLoading(boolean loading) {
        requestInProgress = loading;
        binding.buttonSignIn.setEnabled(!loading);
        binding.buttonSignIn.setText(loading ? "" : getString(R.string.action_sign_in));
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.inputEmail.setEnabled(!loading);
        binding.inputPassword.setEnabled(!loading);
        binding.buttonCreateAccount.setEnabled(!loading);
    }

    /**
     * Shows a short message at the bottom of the screen.
     *
     * @param message text to display
     */
    private void showMessage(@NonNull String message) {
        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
    }

    /**
     * Starts an activity as the only screen in a new task, so Back cannot return here.
     *
     * @param destination activity to open
     */
    private void openAndClearTask(@NonNull Class<?> destination) {
        Intent intent = new Intent(this, destination)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Converts editable text to a non-null string.
     *
     * @param text field contents, possibly {@code null}
     * @return the text, or an empty string
     */
    @NonNull
    private static String textOf(@Nullable CharSequence text) {
        return text == null ? "" : text.toString();
    }
}
