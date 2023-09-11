package de.manu.m1;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Getter;

public class PasswordsManager implements ITabManager {

    @Getter
    public static class PasswordEntry {
        @NotNull
        private final String name;

        @Nullable
        private final String[] websites;

        @NotNull
        private String password;

        public PasswordEntry(@NotNull String name, @Nullable String[] websites, @NotNull String password) {
            this.name = name;
            this.websites = websites;
            this.password = password;
        }

        public void setPassword(@NotNull String password) {
            this.password = password;
        }
    }

    /**
     * Not final, because it is reset on every {@link PasswordsManager#OnTabOpen()}
     */
    private List<PasswordEntry> passwordEntries;
    private final MainActivity activity;
    private final LayoutInflater layoutInflater;
    private final LinearLayout layout;
    private final ClipboardManager clipboardManager;
    private int currentEntryCardIndex = 0;

    public PasswordsManager(MainActivity activity) {
        this.activity = activity;
        this.layoutInflater = LayoutInflater.from(activity);
        this.clipboardManager = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        this.layout = activity.findViewById(R.id.tab_passwords_scroll_layout);
    }

    @Override
    public void OnTabOpen() {
        this.passwordEntries = loadFromStorage();
        this.passwordEntries.forEach(this::createEntry);
    }

    @Override
    public void OnTabClose() {
        this.passwordEntries.clear();
        layout.removeAllViews();
        currentEntryCardIndex = 0;
    }

    private String generatePassword() {
        char[] chars = new char[]{ 'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z','1','2','3','4','5','6','7','8','9','0' };
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<10; i++) {
            int charIndex = ThreadLocalRandom.current().nextInt(chars.length);
            builder.append(chars[charIndex]);
        }
        return builder.toString();
    }

    private void createEntry(PasswordEntry entry) {
        ViewGroup cardParent = (ViewGroup) layoutInflater.inflate(R.layout.card_password, layout);
        ViewGroup card = (ViewGroup) cardParent.getChildAt(currentEntryCardIndex++);

        // full width
        cardParent.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
        cardParent.requestLayout();

        // set text values
        ((TextView) card.getChildAt(0)).setText(entry.name);
        ((TextView) card.getChildAt(1)).setText(entry.websites != null ? String.join("\n", entry.websites) : "");

        // register buttons
        Button copyButton = (Button) card.getChildAt(2);
        Button showButton = (Button) card.getChildAt(3);
        Button resetButton = (Button) card.getChildAt(4);
        Button deleteButton = (Button) card.getChildAt(5);

        copyButton.setOnClickListener(view -> {
            ClipData clipData = ClipData.newPlainText(String.format("password-%s", entry.name), entry.password);
            clipboardManager.setPrimaryClip(clipData);
        });

        final boolean[] hidingPassword = {true};
        showButton.setOnClickListener(view -> {
            if (hidingPassword[0])
                showButton.setText(entry.password);
            else
                showButton.setText(R.string.tab_passwords_action_show);

            hidingPassword[0] = !hidingPassword[0];
        });

        resetButton.setOnClickListener(view -> {
            activity.getInputManager().getInput(
                    generatePassword(),
                    value -> {
                        entry.setPassword(value);
                        saveToStorage();
                        if (!hidingPassword[0])
                            showButton.setText(entry.password);
                    },
                    null
            );
        });

        deleteButton.setOnClickListener(view -> {
            Log.i("PasswordManager", String.format("deleting password-entry %s", entry.name));
            layout.removeView(card);
            passwordEntries.remove(entry);
            saveToStorage();
        });

    }

    private void saveToStorage() {
        String storageJson = activity.getGson().toJson(passwordEntries);

        try (FileOutputStream fos = activity.openFileOutput("passwords.json", Context.MODE_PRIVATE)) {
            fos.write(storageJson.getBytes());
        } catch (FileNotFoundException e) {
            Log.e("PasswordManager", "saveToStorage failed, config file doesn't exist.", e);
        } catch (IOException e) {
            Log.e("PasswordManager", "saveToStorage failed, IOException", e);
        }
    }

    @NotNull
    private List<PasswordEntry> loadFromStorage() {
        Path path = Paths.get(activity.getFilesDir().toString(), "passwords.json");
        byte[] buffer = null;

        List<PasswordEntry> defaultEntries = new ArrayList<>(Arrays.asList(
                new PasswordEntry(
                        activity.getString(R.string.tab_passwords_template_header),
                        activity.getString(R.string.tab_passwords_template_websites).split("\n"),
                        activity.getString(R.string.tab_passwords_template_password)
                )
        ));

        if (!Files.exists(path) || Files.isDirectory(path))
            return defaultEntries;

        try {
            buffer = Files.readAllBytes(path);
        } catch (IOException e) {
            Log.e("PasswordManager", "loadFromStorage failed because of IOException", e);
        }

        if (buffer == null || buffer.length == 0)
            return defaultEntries;

        String storageJson = new String(buffer);
        return activity.getGson().fromJson(storageJson, List.class);

    }

}
