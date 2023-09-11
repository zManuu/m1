package de.manu.m1;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class InputManager {

    private Consumer<String> acceptListener;
    private Runnable cancelListener;
    private boolean isOpened;
    private final MainActivity activity;
    private final InputMethodManager inputManager;

    public InputManager(MainActivity activity) {
        this.activity = activity;
        this.inputManager = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Log.e("InputManager", "constructor hide overlay timeout failed", e);
            }

            toggleOverlay(false);
        });
    }

    public void getInput(String header, Consumer<String> onAccepted, @Nullable Runnable onCanceled) {
        if (this.isOpened) {
            Log.w("InputManager", String.format("Tried to open input %s, but another is still opened.", header));
            return;
        }

        this.acceptListener = onAccepted;
        this.cancelListener = onCanceled;
        this.isOpened = true;

        // set header
        ((EditText) activity.findViewById(R.id.input_field)).setText(header);

        toggleOverlay(true);
    }

    public void accept(String inputValue) {
        if (this.acceptListener == null) {
            Log.w("InputManager", String.format("Tried to call accept listener with input-value of %s, but no listener was registered.", inputValue));
            return;
        }

        this.acceptListener.accept(inputValue);
        this.acceptListener = null;
        this.cancelListener = null;
        this.isOpened = false;

        toggleOverlay(false);
    }

    public void cancel() {
        if (this.cancelListener == null) {
            Log.w("InputManager", "Tried to call cancel listener, but no listener was registered.");

            if (this.isOpened) {
                Log.w("InputManager", "the warning above probably was created because you didn't specify a closeListener. if this is intended, you can ignore this warning.");
                this.acceptListener = null;
                this.cancelListener = null;
                this.isOpened = false;
                toggleOverlay(false);
            }

            return;
        }

        this.cancelListener.run();
        this.acceptListener = null;
        this.cancelListener = null;
        this.isOpened = false;

        toggleOverlay(false);
    }

    private void toggleOverlay(boolean state) {
        View container = activity.findViewById(R.id.input_container_background);
        container.setVisibility(state ? View.VISIBLE : View.GONE);

        // hide keyboard
        if (!state) {
            View tempView = activity.getCurrentFocus() != null ? activity.getCurrentFocus() : new View(activity);
            inputManager.hideSoftInputFromWindow(tempView.getWindowToken(),0);
        }
    }

}
