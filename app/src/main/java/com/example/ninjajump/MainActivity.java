package com.example.ninjajump;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;
    private Button restartButton;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameView != null) {
            gameView.releaseSoundPool(); // SoundPool'u serbest bırak
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // GameView oluştur ve ekle
        gameView = new GameView(this, null);
        findViewById(R.id.game_container).post(() -> {
            ((android.widget.FrameLayout) findViewById(R.id.game_container)).addView(gameView);
        });

        // Restart düğmesini tanımla
        restartButton = findViewById(R.id.restart_button);
        restartButton.setOnClickListener(v -> {
            restartButton.setVisibility(View.GONE); // Restart düğmesini gizle
            gameView.restartGame(); // Oyunu yeniden başlat
        });

        // GameView'de oyun bitişini dinle
        gameView.setGameOverListener(() -> runOnUiThread(() -> {
            restartButton.setVisibility(View.VISIBLE); // Restart düğmesini görünür yap
        }));
    }
}