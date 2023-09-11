package de.manu.m1;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.tabs.TabLayout;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private InputManager inputManager;
    private Gson gson;
    private final Map<Integer, ITabManager> tabs = new HashMap<>();
    private @IdRes int openedTabId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.gson = new Gson();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
    }

    public void exitWelcome(View view) {
        inputManager = new InputManager(this);

        setContentView(R.layout.activity_home);

        tabs.put(R.id.tab_passwords, new PasswordsManager(this));
        tabs.put(R.id.tab_recipes, new RecipesManager());

        showTab(R.id.tab_passwords);

        TabLayout tabLayout = findViewById(R.id.home_tabLayout);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {

            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // hide / show tabs
                switch (tab.getPosition()) {
                    case 0:
                        showTab(R.id.tab_passwords);
                        break;
                    case 1:
                        showTab(R.id.tab_recipes);
                        break;
                    default:
                        hideTab();
                        break;
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void hideTab() {
        if (openedTabId == -1)
            return;

        ConstraintLayout tab = findViewById(openedTabId);
        tab.setVisibility(View.GONE);

        ITabManager tabManager = tabs.get(openedTabId);
        if (tabManager == null)
            Log.w("M1Activity", String.format("hideTab->tabManager==null for tabId %d", openedTabId));
        else
            tabManager.OnTabClose();
    }

    private void showTab(@IdRes int tabId) {
        if (openedTabId == tabId)
            return;

        if (openedTabId != -1)
            hideTab();

        ConstraintLayout tab = findViewById(tabId);
        tab.setVisibility(View.VISIBLE);

        ITabManager tabManager = tabs.get(tabId);
        if (tabManager == null)
            Log.w("M1Activity", String.format("showTab->tabManager==null for tabId %d", tabId));
        else
            tabManager.OnTabOpen();

        openedTabId = tabId;
    }

    public void inputAccept(View view) {
        this.inputManager.accept(
                ((EditText) findViewById(R.id.input_field)).getText().toString()
        );
    }

    public void inputCancel(View view) {
        this.inputManager.cancel();
    }

    public InputManager getInputManager() {
        return inputManager;
    }

    public Gson getGson() {
        return gson;
    }
}