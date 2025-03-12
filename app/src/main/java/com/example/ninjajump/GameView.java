package com.example.ninjajump;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.media.SoundPool;
import android.media.AudioAttributes;
import android.os.Build;


import com.example.ninjajump.R;

import java.util.ArrayList;
import java.util.Random;

public class GameView extends View {

    private Paint paint;
    private Bitmap topBitmap, platformBitmap,breakablePlatformBitmap,brokenPlatformBitmap,backgroundBitmap1;
    private float characterX, characterY, characterSpeedY;
    private float screenWidth, screenHeight;
    private ArrayList<Platform> platforms;
    private boolean gameOver = false;
    private final Handler handler = new Handler();
    private Runnable gameLoop;
    private int score = 0;
    private int highScore = 0;
    private SharedPreferences preferences;
    private GameOverListener gameOverListener;
    private SoundPool soundPool;
    private int jumpSoundId;
    private int breakSoundId;

    private boolean isOnPlatform = false;

    private float jumpHeight = 30; // Sabit zıplama yüksekliği
    private float speedMultiplier = 1.0f; // Hız çarpanı
    private int platformCount = 5; // Platform sayısı
    private float platformGap; // Platformlar arasındaki mesafe

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        paint = new Paint();

        // Drawable klasöründen görselleri yükle
        topBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.top_image);
        platformBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.platform_image);
        breakablePlatformBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.breakable_platform);
        brokenPlatformBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.broken_platform);
        backgroundBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.background_image1);


        // Görselleri ölçeklendir
        topBitmap = Bitmap.createScaledBitmap(topBitmap, 150, 170, false);
        platformBitmap = Bitmap.createScaledBitmap(platformBitmap, 250, 60, false);
        breakablePlatformBitmap = Bitmap.createScaledBitmap(breakablePlatformBitmap, 250, 60, false);
        brokenPlatformBitmap = Bitmap.createScaledBitmap(brokenPlatformBitmap, 250, 100, false);

        characterSpeedY = 20;
        platforms = new ArrayList<>();

        preferences = context.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE);
        highScore = preferences.getInt("HighScore", 0);
        // SoundPool'u başlat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(5, android.media.AudioManager.STREAM_MUSIC, 0);
        }

        // Ses dosyalarını yükle
        jumpSoundId = soundPool.load(context, R.raw.jump_sound, 1);
        breakSoundId = soundPool.load(context, R.raw.break_sound, 1);

        gameLoop = new Runnable() {
            @Override
            public void run() {
                if (!gameOver) {
                    updateGame();
                    invalidate();
                    handler.postDelayed(this, 16);
                }
            }
        };
        handler.post(gameLoop);
    }

    private void setupPlatforms() {
        Random random = new Random();
        platforms.clear();

        platformGap = screenHeight / (platformCount + 1); // Platformlar arasındaki sabit mesafe
        int breakableCount = 0;
        ArrayList<Platform> tempPlatforms = new ArrayList<>();

        // Öncelikle normal platformları oluştur
        for (int i = 0; i < platformCount; i++) {
            float x = random.nextInt((int) (screenWidth - platformBitmap.getWidth())); // Yatay rastgele pozisyon
            float y = screenHeight - ((i + 1) * platformGap); // Dikey sabit aralık

            tempPlatforms.add(new Platform(x, y, false)); // Normal platform olarak ekle
        }

        // Normal platformlar arasına kırılan platformları ekle
        for (int i = 0; i < tempPlatforms.size() - 1; i++) { // Son platformdan önceki tüm platformlar için
            if (breakableCount < 2 && random.nextFloat() < 0.5) { // %50 olasılıkla
                float x = random.nextInt((int) (screenWidth - platformBitmap.getWidth())); // Yatay rastgele pozisyon
                float y = (tempPlatforms.get(i).y + tempPlatforms.get(i + 1).y) / 2; // İki platform arasına yerleştir

                Platform breakablePlatform = new Platform(x, y, true);
                breakablePlatform.isBroken = false; // Yeni kırılabilir platform her zaman sağlam başlar
                platforms.add(breakablePlatform);
                breakableCount++;
            }
            platforms.add(tempPlatforms.get(i)); // Normal platformu ekle
        }

        // Son normal platformu da ekle
        platforms.add(tempPlatforms.get(tempPlatforms.size() - 1));

        // Karakterin başlangıç konumunu ayarla
        if (!platforms.isEmpty()) {
            Platform firstPlatform = platforms.get(0);
            characterX = firstPlatform.x + platformBitmap.getWidth() / 2;
            characterY = firstPlatform.y - topBitmap.getHeight();
        }
    }



    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        setupPlatforms();

        // Arka planı ekran boyutuna göre ölçeklendir
        if (backgroundBitmap1 != null) {
            backgroundBitmap1 = Bitmap.createScaledBitmap(backgroundBitmap1, w, h, true);
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Arka planı çiz
        if (backgroundBitmap1 != null) {
            canvas.drawBitmap(backgroundBitmap1, 0, 0, paint);
        }

        // Platformları çiz
        for (Platform platform : platforms) {
            if (platform.isBreakable) {
                if (platform.isBroken) {
                    canvas.drawBitmap(brokenPlatformBitmap, platform.x, platform.y, paint); // Kırılmış platform
                } else {
                    canvas.drawBitmap(breakablePlatformBitmap, platform.x, platform.y, paint); // Sağlam kırılabilir platform
                }
            } else {
                canvas.drawBitmap(platformBitmap, platform.x, platform.y, paint); // Normal platform
            }
        }

        // Karakteri çiz
        canvas.drawBitmap(topBitmap, characterX - topBitmap.getWidth() / 2,
                characterY - topBitmap.getHeight() / 2, paint);

        // Puan ve yüksek skoru çiz
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(50);
        canvas.drawText("Score: " + score, 50, 100, paint);
        canvas.drawText("High Score: " + highScore, 50, 200, paint);

        // Oyun bitti ekranı
        if (gameOver) {
            paint.setTextSize(100);
            canvas.drawText("Game Over", screenWidth / 4, screenHeight / 2, paint);
            canvas.drawText("Tap to Restart", screenWidth / 6, screenHeight / 2 + 100, paint);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            characterX = event.getX();
        }
        return true;
    }


    private void updateGame() {
        if (gameOver) return;

        // Hız çarpanını artır
        if (score % 10 == 0 && score > 0) { // Her 10 puanda bir hız artışı
            speedMultiplier += 0.005f;
        }

        // Karakterin hareketi
        characterY -= characterSpeedY * speedMultiplier; // Zıplama hızı
        characterSpeedY -= 0.5f * speedMultiplier; // Yerçekimi

        int platformWidth = platformBitmap.getWidth();
        int platformHeight = platformBitmap.getHeight();
        int characterWidth = topBitmap.getWidth();
        int characterHeight = topBitmap.getHeight();

        isOnPlatform = false;
        for (Platform platform : platforms) {
            boolean isColliding = (characterX < platform.x + platformWidth &&
                    characterX + characterWidth * 0.3f > platform.x + platformWidth * 0.1f &&
                    characterY + characterHeight * 0.3f < platform.y + platformHeight * 0.9f &&
                    characterY + characterHeight * 0.7f > platform.y + platformHeight * 0.1f);


            if (isColliding && characterSpeedY < 0) {
                if (platform.isBreakable && !platform.isBroken) {
                    platform.isBroken = true; // Platformu kırılmış olarak işaretle
                    if (!platform.soundPlayed) { // Daha önce ses çalınmamışsa
                        soundPool.play(breakSoundId, 1, 1, 0, 0, 1); // Kırılma sesi çal
                        platform.soundPlayed = true; // Sesin çalındığını işaretle
                    }; // Kırılma sesi
                } else if (!platform.isBroken) {
                    soundPool.play(jumpSoundId, 1, 1, 0, 0, 1);


                    characterY = platform.y - characterHeight;
                    characterSpeedY = 20; // Zıplama hızını sıfırla
                    score++;

                    // Yüksek skoru güncelle
                    if (score > highScore) {
                        highScore = score;
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt("HighScore", highScore);
                        editor.apply();
                    }

                    isOnPlatform = true;
                }
            }
        }

        // Platformları hareket ettir
        for (Platform platform : platforms) {
            if (platform.y > screenHeight) {
                platform.y -= platformCount * platformGap; // Sabit aralıkla yeniden doğar
                platform.x = new Random().nextInt((int) (screenWidth - platformBitmap.getWidth()));
                if (platform.isBreakable) {
                    platform.isBroken = false;// Yeniden doğduğunda sağlam olur
                    platform.soundPlayed = false; // Sesin tekrar çalınmasına izin ver
                }
            }
        }

        // Ekran kaydırma
        if (characterY < screenHeight / 2) {
            float diff = screenHeight / 2 - characterY;
            characterY = screenHeight / 2;
            for (Platform platform : platforms) {
                platform.y += diff * speedMultiplier; // Platform hareketi hızlanır
            }
        }

        if (characterY > screenHeight || characterX < 0 || characterX > screenWidth) {
            gameOver = true;
            if (gameOverListener != null) gameOverListener.onGameOver();
        }
    }

    public void restartGame() {
        characterSpeedY = 20;
        score = 0;
        speedMultiplier = 1.0f; // Hız çarpanını sıfırla
        setupPlatforms();
        gameOver = false;
        handler.post(gameLoop);
    }

    public void setGameOverListener(GameOverListener listener) {
        this.gameOverListener = listener;
    }
    public void releaseSoundPool() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    private static class Platform {
        public boolean soundPlayed;
        float x, y; // Platformun pozisyonu
        boolean isBreakable; // Platformun kırılabilir olup olmadığını belirtir
        boolean isBroken = false; // Platformun kırılma durumu (başlangıçta sağlam)

        Platform(float x, float y) {
            this.x = x;
            this.y = y;
            this.isBreakable = false; // Varsayılan olarak kırılmaz
        }

        Platform(float x, float y, boolean isBreakable) {
            this.x = x;
            this.y = y;
            this.isBreakable = isBreakable; // Kırılabilir mi?
        }

        void breakPlatform() {
            if (isBreakable) {
                isBroken = true; // Platform kırıldı
            }
        }
    }


    public interface GameOverListener {
        void onGameOver();
    }
}