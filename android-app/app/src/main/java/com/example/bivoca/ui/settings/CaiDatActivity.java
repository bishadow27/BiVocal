package com.example.bivoca.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bivoca.R;
import com.example.bivoca.ui.navigation.NavigationDrawerController;
import com.example.bivoca.ui.navigation.NavigationRoute;

public final class CaiDatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cai_dat);
        NavigationDrawerController.attach(this, NavigationRoute.SETTINGS, R.id.buttonSettingsMenu);
        bind(R.id.settingsStudy, ChiTietCaiDatActivity.SECTION_STUDY);
        bind(R.id.settingsAudio, ChiTietCaiDatActivity.SECTION_AUDIO);
        bind(R.id.settingsReminder, ChiTietCaiDatActivity.SECTION_REMINDER);
        bind(R.id.settingsAbout, ChiTietCaiDatActivity.SECTION_ABOUT);
        findViewById(R.id.settingsGoogleLink).setOnClickListener(view -> startActivity(
                new Intent(this, com.example.bivoca.ui.profile.HoSoCaNhanActivity.class)
                        .putExtra(com.example.bivoca.ui.profile.HoSoCaNhanActivity.EXTRA_OPEN_GOOGLE_LINK, true)));
    }

    private void bind(int viewId, String section) {
        findViewById(viewId).setOnClickListener(view -> startActivity(
                new Intent(this, ChiTietCaiDatActivity.class)
                        .putExtra(ChiTietCaiDatActivity.EXTRA_SECTION, section)));
    }
}
