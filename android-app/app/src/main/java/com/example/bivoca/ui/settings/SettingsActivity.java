package com.example.bivoca.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bivoca.R;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;

public final class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        NavigationDrawerController.attach(this, NavigationRoute.SETTINGS, R.id.buttonSettingsMenu);
        bind(R.id.settingsStudy, SettingsDetailActivity.SECTION_STUDY);
        bind(R.id.settingsAudio, SettingsDetailActivity.SECTION_AUDIO);
        bind(R.id.settingsReminder, SettingsDetailActivity.SECTION_REMINDER);
        bind(R.id.settingsDisplay, SettingsDetailActivity.SECTION_DISPLAY);
        bind(R.id.settingsAbout, SettingsDetailActivity.SECTION_ABOUT);
    }

    private void bind(int viewId, String section) {
        findViewById(viewId).setOnClickListener(view -> startActivity(
                new Intent(this, SettingsDetailActivity.class)
                        .putExtra(SettingsDetailActivity.EXTRA_SECTION, section)));
    }
}
