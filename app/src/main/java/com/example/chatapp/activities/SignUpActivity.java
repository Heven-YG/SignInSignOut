package com.example.chatapp.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivitySignUpBinding;
import com.example.chatapp.firebase.FcmTokenManager;
import com.example.chatapp.firebase.FirebaseErrorMessages;
import com.example.chatapp.firebase.SessionManager;
import com.example.chatapp.firebase.UserRepository;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.ImageUtils;
import com.example.chatapp.utilities.InsetsUtils;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.ValidationUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a Firebase Authentication account and its Firestore profile at
 * {@code users/{uid}}.
 *
 * <p>The screen also runs in a profile-completion mode, used when an authenticated user has no
 * profile document: either because profile saving failed right after the account was created,
 * or because sign-in found no profile. In that mode only the name and photo are editable and
 * saving can be retried; the user can also sign out instead. Passwords go only to Firebase
 * Authentication and are never stored.</p>
 */
public class SignUpActivity extends AppCompatActivity {

    private static final String STATE_ENCODED_IMAGE = "encodedImage";
    private static final String STATE_COMPLETE_PROFILE = "completeProfileMode";
    private static final String STATE_SAVE_FAILED = "saveFailed";

    private ActivitySignUpBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth auth;
    private ExecutorService imageExecutor;
    private ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher;

    /** Base64 avatar, or an empty string for the default avatar. */
    private String encodedImage = "";
    private boolean completeProfileMode;
    private boolean saveFailed;
    private boolean requestInProgress;
    private boolean imageProcessing;

    /**
     * Builds the form, registers the photo picker, and restores mode and image after a
     * configuration change.
     *
     * @param savedInstanceState previously saved state, or {@code null}
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtils.applySystemBarAndImePadding(binding.getRoot());

        preferenceManager = new PreferenceManager(this);
        auth = FirebaseAuth.getInstance();
        imageExecutor = Executors.newSingleThreadExecutor();
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.PickVisualMedia(), this::onImagePicked);

        completeProfileMode = getIntent().getBooleanExtra(Constants.EXTRA_COMPLETE_PROFILE, false);
        if (savedInstanceState != null) {
            encodedImage = savedInstanceState.getString(STATE_ENCODED_IMAGE, "");
            completeProfileMode = savedInstanceState.getBoolean(
                    STATE_COMPLETE_PROFILE, completeProfileMode);
            saveFailed = savedInstanceState.getBoolean(STATE_SAVE_FAILED, false);
        }

        if (completeProfileMode && auth.getCurrentUser() == null) {
            // Profile completion requires an authenticated user.
            openSignIn();
            return;
        }

        binding.buttonSubmit.setText(R.string.action_create_account);
        binding.passwordLayout.setHelperText(getResources().getQuantityString(
                R.plurals.password_helper, Constants.MIN_PASSWORD_LENGTH,
                Constants.MIN_PASSWORD_LENGTH));
        showImagePreview();
        if (completeProfileMode) {
            applyCompleteProfileMode(saveFailed ? getString(R.string.error_profile_save_failed,
                    getString(R.string.error_generic)) : null);
        }
        setListeners();
    }

    /**
     * Saves the encoded image and current mode so they survive rotation.
     *
     * @param outState bundle to write into
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_ENCODED_IMAGE, encodedImage);
        outState.putBoolean(STATE_COMPLETE_PROFILE, completeProfileMode);
        outState.putBoolean(STATE_SAVE_FAILED, saveFailed);
    }

    /**
     * Stops the image worker thread.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageExecutor != null) {
            imageExecutor.shutdownNow();
        }
    }

    /**
     * Connects the image picker, submit, navigation, and sign-out actions.
     */
    private void setListeners() {
        binding.imageProfile.setOnClickListener(v -> pickImage());
        binding.textAddImage.setOnClickListener(v -> pickImage());
        binding.buttonSubmit.setOnClickListener(v -> submit());
        binding.buttonSignIn.setOnClickListener(v -> finish());
        binding.buttonUseDifferentAccount.setOnClickListener(v -> signOutAndRestart());
        binding.inputConfirmPassword.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit();
                return true;
            }
            return false;
        });
    }

    /**
     * Opens the system photo picker for images only. On older devices the picker falls back to
     * the document chooser, so no storage permission is required.
     */
    private void pickImage() {
        if (requestInProgress || imageProcessing) {
            return;
        }
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    /**
     * Handles the photo picker result by resizing and encoding the image on a worker thread.
     * A cancelled selection keeps the current image.
     *
     * @param uri the chosen image, or {@code null} if the user cancelled
     */
    private void onImagePicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        setImageProcessing(true);
        imageExecutor.execute(() -> {
            String result;
            try {
                result = ImageUtils.encodeProfileImage(getContentResolver(), uri);
            } catch (IOException | RuntimeException e) {
                result = null;
            }
            final String encoded = result;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setImageProcessing(false);
                if (encoded == null) {
                    showMessage(getString(R.string.error_image_invalid));
                    return;
                }
                encodedImage = encoded;
                showImagePreview();
            });
        });
    }

    /**
     * Shows the selected avatar, or the default avatar when none is selected.
     */
    private void showImagePreview() {
        Bitmap bitmap = ImageUtils.decodeBase64(encodedImage);
        if (bitmap != null) {
            binding.imageProfile.setImageBitmap(bitmap);
            binding.textAddImage.setText(R.string.change_image);
        } else {
            encodedImage = "";
            binding.imageProfile.setImageResource(R.drawable.ic_default_avatar);
            binding.textAddImage.setText(R.string.add_image);
        }
    }

    /**
     * Validates the form and creates the account, or saves the profile in completion mode.
     * Ignored while a request or image conversion is running.
     */
    private void submit() {
        if (requestInProgress || imageProcessing) {
            return;
        }
        clearFieldErrors();
        String name = textOf(binding.inputName.getText()).trim();

        if (completeProfileMode) {
            ValidationUtils.Result result = ValidationUtils.validateProfileCompletion(name);
            if (result != ValidationUtils.Result.VALID) {
                showValidationError(result);
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                showMessage(getString(R.string.error_session_expired));
                openSignIn();
                return;
            }
            String email = user.getEmail() != null
                    ? user.getEmail() : textOf(binding.inputEmail.getText()).trim();
            setLoading(true);
            saveProfile(user, name, email);
            return;
        }

        String email = textOf(binding.inputEmail.getText()).trim();
        String password = textOf(binding.inputPassword.getText());
        String confirm = textOf(binding.inputConfirmPassword.getText());
        ValidationUtils.Result result =
                ValidationUtils.validateSignUp(name, email, password, confirm);
        if (result != ValidationUtils.Result.VALID) {
            showValidationError(result);
            return;
        }
        setLoading(true);
        createAccount(name, email, password);
    }

    /**
     * Creates the Firebase Authentication account, then saves the profile.
     *
     * @param name     display name for the profile
     * @param email    account email
     * @param password account password, sent only to Firebase Authentication
     */
    private void createAccount(@NonNull String name, @NonNull String email,
                               @NonNull String password) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (task.isSuccessful() && user != null) {
                saveProfile(user, name, email);
                return;
            }
            setLoading(false);
            Exception error = task.getException();
            String message = FirebaseErrorMessages.from(this, error);
            if (error instanceof FirebaseAuthUserCollisionException) {
                binding.emailLayout.setError(message);
                binding.inputEmail.requestFocus();
            } else if (error instanceof FirebaseAuthWeakPasswordException) {
                binding.passwordLayout.setError(message);
                binding.inputPassword.requestFocus();
            } else {
                showMessage(message);
            }
        });
    }

    /**
     * Saves the profile document for {@code user}. On success the profile is cached, the FCM
     * token registered, and {@link MainActivity} opened. On failure the screen switches to
     * profile-completion mode with a retry button, so registration is never treated as
     * complete without a profile.
     *
     * @param user  the authenticated Firebase user
     * @param name  display name
     * @param email email address to store
     */
    private void saveProfile(@NonNull FirebaseUser user, @NonNull String name,
                             @NonNull String email) {
        final String userId = user.getUid();
        final String image = encodedImage;
        UserRepository.saveProfile(userId, name, email, image).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            FirebaseUser current = auth.getCurrentUser();
            if (current == null || !userId.equals(current.getUid())) {
                setLoading(false);
                openSignIn();
                return;
            }
            if (task.isSuccessful()) {
                saveFailed = false;
                preferenceManager.saveUserProfile(userId, name, email, image);
                FcmTokenManager.registerCurrentToken(this);
                Intent intent = new Intent(this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
                return;
            }
            saveFailed = true;
            setLoading(false);
            applyCompleteProfileMode(getString(R.string.error_profile_save_failed,
                    FirebaseErrorMessages.from(this, task.getException())));
        });
    }

    /**
     * Switches the screen to profile-completion mode: the email is fixed to the signed-in
     * account, password fields are hidden, and a sign-out option is shown.
     *
     * @param errorMessage message explaining a failed save, or {@code null} for none
     */
    private void applyCompleteProfileMode(@Nullable String errorMessage) {
        completeProfileMode = true;
        FirebaseUser user = auth.getCurrentUser();
        binding.textTitle.setText(R.string.complete_profile_title);
        binding.textSubtitle.setText(R.string.complete_profile_subtitle);
        if (user != null && user.getEmail() != null) {
            binding.inputEmail.setText(user.getEmail());
        }
        binding.inputEmail.setEnabled(false);
        binding.inputPassword.setText(null);
        binding.inputConfirmPassword.setText(null);
        binding.passwordLayout.setVisibility(View.GONE);
        binding.confirmPasswordLayout.setVisibility(View.GONE);
        binding.buttonSignIn.setVisibility(View.GONE);
        binding.buttonUseDifferentAccount.setVisibility(View.VISIBLE);
        binding.inputName.setImeOptions(EditorInfo.IME_ACTION_DONE);
        binding.buttonSubmit.setText(saveFailed
                ? R.string.action_retry_save_profile : R.string.action_save_profile);
        if (errorMessage != null) {
            binding.textStatus.setText(errorMessage);
            binding.textStatus.setVisibility(View.VISIBLE);
        } else {
            binding.textStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Signs out of the incomplete account and returns to the sign-in screen.
     */
    private void signOutAndRestart() {
        if (requestInProgress) {
            return;
        }
        setLoading(true);
        SessionManager.signOut(this, this::openSignIn);
    }

    /**
     * Opens the sign-in screen as a fresh task and closes this screen.
     */
    private void openSignIn() {
        Intent intent = new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Shows a validation error on the matching field and moves focus there.
     *
     * @param result the validation problem to display
     */
    private void showValidationError(@NonNull ValidationUtils.Result result) {
        switch (result) {
            case NAME_REQUIRED:
                binding.nameLayout.setError(getString(R.string.error_name_required));
                binding.inputName.requestFocus();
                break;
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
            case PASSWORD_TOO_SHORT:
                binding.passwordLayout.setError(getResources().getQuantityString(
                        R.plurals.error_password_too_short, Constants.MIN_PASSWORD_LENGTH,
                        Constants.MIN_PASSWORD_LENGTH));
                binding.inputPassword.requestFocus();
                break;
            case CONFIRM_PASSWORD_REQUIRED:
                binding.confirmPasswordLayout.setError(
                        getString(R.string.error_confirm_password_required));
                binding.inputConfirmPassword.requestFocus();
                break;
            case PASSWORDS_DO_NOT_MATCH:
                binding.confirmPasswordLayout.setError(
                        getString(R.string.error_passwords_do_not_match));
                binding.inputConfirmPassword.requestFocus();
                break;
            default:
                showMessage(getString(R.string.error_generic));
                break;
        }
    }

    /**
     * Removes all field errors before a new validation pass.
     */
    private void clearFieldErrors() {
        binding.nameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmPasswordLayout.setError(null);
    }

    /**
     * Switches between the loading state and the editable form.
     *
     * @param loading {@code true} to show progress and disable input
     */
    private void setLoading(boolean loading) {
        requestInProgress = loading;
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.buttonSubmit.setEnabled(!loading);
        if (loading) {
            binding.buttonSubmit.setText("");
        } else if (completeProfileMode) {
            binding.buttonSubmit.setText(saveFailed
                    ? R.string.action_retry_save_profile : R.string.action_save_profile);
        } else {
            binding.buttonSubmit.setText(R.string.action_create_account);
        }
        binding.inputName.setEnabled(!loading);
        binding.inputEmail.setEnabled(!loading && !completeProfileMode);
        binding.inputPassword.setEnabled(!loading);
        binding.inputConfirmPassword.setEnabled(!loading);
        binding.imageProfile.setEnabled(!loading);
        binding.textAddImage.setEnabled(!loading);
        binding.buttonSignIn.setEnabled(!loading);
        binding.buttonUseDifferentAccount.setEnabled(!loading);
    }

    /**
     * Disables submission while a picked image is being converted.
     *
     * @param processing {@code true} while the image worker is running
     */
    private void setImageProcessing(boolean processing) {
        imageProcessing = processing;
        binding.buttonSubmit.setEnabled(!processing && !requestInProgress);
        binding.imageProfile.setAlpha(processing ? 0.5f : 1f);
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
