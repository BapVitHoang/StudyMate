package com.hcmute.studymate.view;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.hcmute.studymate.R;
import com.hcmute.studymate.controller.AuthController;
import com.hcmute.studymate.utils.AppContainer;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AuthController authController = AppContainer.authController();
        Class<?> destination = authController.isLoggedIn() ? NoteListActivity.class : LoginActivity.class;
        startActivity(new Intent(this, destination));
        finish();
    }
}
