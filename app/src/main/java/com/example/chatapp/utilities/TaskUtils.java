package com.example.chatapp.utilities;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.concurrent.TimeoutException;

/**
 * Helpers for Google Play services {@link Task} objects.
 */
public final class TaskUtils {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    /** Prevents instantiation of this utility class. */
    private TaskUtils() {
    }

    /**
     * Wraps a task so it fails with a {@link TimeoutException} if it has not finished within
     * the given time. Firestore writes made while offline do not complete until the server
     * confirms them, so this keeps the UI from waiting forever. The original operation is not
     * cancelled and may still finish later.
     *
     * @param task          task to watch
     * @param timeoutMillis maximum wait in milliseconds
     * @param <T>           result type of the task
     * @return a task that completes with the original result, error, or a timeout
     */
    @NonNull
    public static <T> Task<T> withTimeout(@NonNull Task<T> task, long timeoutMillis) {
        TaskCompletionSource<T> source = new TaskCompletionSource<>();
        Runnable timeout = () -> source.trySetException(
                new TimeoutException("Operation timed out"));
        MAIN_HANDLER.postDelayed(timeout, timeoutMillis);
        task.addOnCompleteListener(completed -> {
            MAIN_HANDLER.removeCallbacks(timeout);
            if (completed.isSuccessful()) {
                source.trySetResult(completed.getResult());
            } else {
                Exception error = completed.getException();
                source.trySetException(error != null
                        ? error : new IllegalStateException("Operation failed"));
            }
        });
        return source.getTask();
    }
}
