package com.example.chatapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.chatapp.R;
import com.example.chatapp.databinding.ActivityMainBinding;
import com.example.chatapp.firebase.FcmTokenManager;
import com.example.chatapp.firebase.FirebaseErrorMessages;
import com.example.chatapp.firebase.SessionManager;
import com.example.chatapp.firebase.UserProfile;
import com.example.chatapp.firebase.UserRepository;
import com.example.chatapp.utilities.Constants;
import com.example.chatapp.utilities.ImageUtils;
import com.example.chatapp.utilities.InsetsUtils;
import com.example.chatapp.utilities.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Authenticated home screen showing the signed-in user's name, email, and profile image.
 *
 * <p>Access requires a signed-in Firebase user; otherwise the user is sent to
 * {@link SignInActivity}. A cached profile is shown immediately only if it belongs to the
 * current Firebase user, and the latest profile is then fetched from Firestore. This screen
 * also registers the FCM token for restored sessions and asks for notification permission on
 * Android 13 and later.</p>
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_PERMISSION_ASKED = "notificationPermissionAsked";

    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth auth;
    private String userId;
    private boolean showingCachedProfile;
    private boolean signingOut;
    private boolean permissionAsked;

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    this::onNotificationPermissionResult);

    private final FirebaseAuth.AuthStateListener authStateListener = firebaseAuth -> {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (!signingOut && (user == null || !user.getUid().equals(userId))) {
            preferenceManager.clear();
            openSignIn();
        }
    };

    /**
     * Verifies the Firebase session, shows any valid cached profile, and starts loading the
     * latest profile.
     *
     * @param savedInstanceState previously saved state, or {@code null}
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager = new PreferenceManager(this);
        auth = FirebaseAuth.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            // Firebase Auth is the source of truth; a saved flag alone never grants access.
            preferenceManager.clear();
            openSignIn();
            return;
        }
        userId = user.getUid();
        if (savedInstanceState != null) {
            permissionAsked = savedInstanceState.getBoolean(STATE_PERMISSION_ASKED, false);
        }

        WindowCompat.enableEdgeToEdge(getWindow());
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        InsetsUtils.applySystemBarAndImePadding(binding.getRoot());

        binding.buttonRetry.setOnClickListener(v -> loadProfile());
        binding.buttonSignOut.setOnClickListener(v -> signOut());

        if (preferenceManager.hasCachedProfileFor(userId)) {
            displayProfile(
                    preferenceManager.getString(Constants.KEY_NAME),
                    preferenceManager.getString(Constants.KEY_EMAIL),
                    preferenceManager.getString(Constants.KEY_IMAGE));
            showingCachedProfile = true;
        } else {
            // The cache is missing or belongs to another account; never show it.
            preferenceManager.clear();
            displayProfile(null, user.getEmail(), null);
        }

        loadProfile();
        FcmTokenManager.registerCurrentToken(this);
        requestNotificationPermissionIfNeeded();
    }

    /**
     * Starts watching for the session ending or changing while this screen is visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
        if (binding != null) {
            auth.addAuthStateListener(authStateListener);
        }
    }

    /**
     * Stops watching authentication state while the screen is not visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
        auth.removeAuthStateListener(authStateListener);
    }

    /**
     * Remembers whether the notification permission was already requested on this screen.
     *
     * @param outState bundle to write into
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PERMISSION_ASKED, permissionAsked);
    }

    /**
     * Fetches the profile from Firestore. A missing profile opens the profile-completion
     * flow. Errors show a retry option, either inline or, when a cached profile is already
     * shown, as a snackbar.
     */
    private void loadProfile() {
        final String requestedUserId = userId;
        binding.layoutError.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);
        UserRepository.loadProfile(requestedUserId).addOnCompleteListener(task -> {
            if (isFinishing() || isDestroyed() || signingOut) {
                return;
            }
            FirebaseUser current = auth.getCurrentUser();
            if (current == null || !requestedUserId.equals(current.getUid())) {
                return;
            }
            binding.progressBar.setVisibility(View.GONE);
            if (!task.isSuccessful()) {
                String reason = FirebaseErrorMessages.from(this, task.getException());
                if (showingCachedProfile) {
                    Snackbar.make(binding.getRoot(), R.string.error_profile_refresh,
                                    Snackbar.LENGTH_LONG)
                            .setAction(R.string.action_retry, v -> loadProfile())
                            .show();
                } else {
                    binding.textError.setText(getString(R.string.error_profile_load, reason));
                    binding.layoutError.setVisibility(View.VISIBLE);
                }
                return;
            }
            UserProfile profile = task.getResult();
            if (profile == null) {
                openCompleteProfile();
                return;
            }
            preferenceManager.saveUserProfile(requestedUserId, profile.getName(),
                    profile.getEmail(), profile.getImage());
            displayProfile(profile.getName(), profile.getEmail(), profile.getImage());
            showingCachedProfile = true;
        });
    }

    /**
     * Shows profile values, falling back to the default avatar when there is no valid image.
     *
     * @param name  display name, or {@code null}
     * @param email email address, or {@code null}
     * @param image Base64 avatar, or {@code null}/empty for the default avatar
     */
    private void displayProfile(@Nullable String name, @Nullable String email,
                                @Nullable String image) {
        binding.textName.setText(name == null ? "" : name);
        binding.textEmail.setText(email == null ? "" : email);
        Bitmap bitmap = ImageUtils.decodeBase64(image);
        if (bitmap != null) {
            binding.imageProfile.setImageBitmap(bitmap);
        } else {
            binding.imageProfile.setImageResource(R.drawable.ic_default_avatar);
        }
    }

    /**
     * Signs out: removes the FCM device association, signs out of Firebase, clears the
     * cache, and opens sign-in in a fresh task so Back cannot return here. Repeated taps are
     * ignored.
     */
    private void signOut() {
        if (signingOut) {
            return;
        }
        signingOut = true;
        binding.buttonSignOut.setEnabled(false);
        binding.buttonSignOut.setText(R.string.signing_out);
        binding.buttonRetry.setEnabled(false);
        SessionManager.signOut(this, this::openSignIn);
    }

    /**
     * Asks for the notification permission on Android 13 and later, once per screen
     * instance, if it has not been granted yet.
     */
    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || permissionAsked) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        permissionAsked = true;
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    /**
     * Tells the user how to turn notifications back on if they declined.
     *
     * @param granted whether the permission was granted
     */
    private void onNotificationPermissionResult(boolean granted) {
        if (!granted && binding != null) {
            Snackbar.make(binding.getRoot(), R.string.notification_permission_denied,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * Opens the profile-completion flow for an authenticated user who has no profile.
     */
    private void openCompleteProfile() {
        Intent intent = new Intent(this, SignUpActivity.class)
                .putExtra(Constants.EXTRA_COMPLETE_PROFILE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /**
     * Opens the sign-in screen in a fresh task and closes this screen.
     */
    private void openSignIn() {
        Intent intent = new Intent(this, SignInActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
