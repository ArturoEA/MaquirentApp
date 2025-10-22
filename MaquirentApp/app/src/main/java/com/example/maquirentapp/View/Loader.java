package com.example.maquirentapp.View;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.maquirentapp.MainActivity;
import com.example.maquirentapp.R;

public class Loader extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ly_loader);

//         Espera 1.5 segundos antes de lanzar la actividad principal
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(Loader.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, 1500);
    }


}