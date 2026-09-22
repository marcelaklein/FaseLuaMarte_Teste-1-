package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Requisito 5: Video de intro (>= 6 segundos).
 *
 * Este projeto usa o "Plano B" aceito no enunciado: uma animacao com 6+
 * quadros (as 6 letras de "ECHOES" aparecendo uma a uma, junto com a Lua
 * e o capacete) + musica de fundo (se o arquivo existir) + o texto ECHOES.
 * A duracao MINIMA real e garantida por tempo (delta acumulado), nao por
 * frames de tela, entao o cronometro do professor sempre bate >= 6s.
 *
 * Se no seu projeto voce ja tiver a extensao gdx-video configurada e um
 * assets/video/intro.webm valido, veja o comentario no final da classe
 * com o trecho de codigo oficial (VideoPlayerCreator) para ligar o video
 * real no lugar do Plano B — nao ative isso sem adicionar a dependencia
 * gdx-video ao build.gradle, ou o projeto deixa de compilar.
 */
public class IntroScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont fontTitle;
    private BitmapFont fontBody;
    private Music music;

    private float timer = 0f;

    // 6 letras de "ECHOES" = 6 quadros de animacao, 1 por segundo.
    private final String TITLE = "ECHOES";
    private final float SECONDS_PER_LETTER = 1.0f;
    private final float MIN_DURATION = TITLE.length() * SECONDS_PER_LETTER; // 6s
    private final float HOLD_AFTER = 3f; // segura mais 3s com o titulo completo
    private final float TOTAL_DURATION = MIN_DURATION + HOLD_AFTER; // 9s (fim "sozinho")

    private boolean advancing = false;

    // Estrelas fixas (decorativas, parte da animacao)
    private final float[] starX = new float[40];
    private final float[] starY = new float[40];
    private final float[] starPhase = new float[40];

    public IntroScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(4.0f);
        fontBody = new BitmapFont();
        fontBody.getData().setScale(1.2f);

        MathUtils.random.setSeed(42); // estrelas sempre no mesmo lugar
        for (int i = 0; i < starX.length; i++) {
            starX[i] = MathUtils.random(0, 800);
            starY[i] = MathUtils.random(0, 600);
            starPhase[i] = MathUtils.random(0f, MathUtils.PI2);
        }

        music = safeLoadMusic("audio/intro_music.ogg");
        if (music == null) music = safeLoadMusic("audio/intro_music.mp3");
        if (music != null) {
            music.setLooping(false);
            music.setVolume(0.7f);
            music.play();
        }
    }

    private Music safeLoadMusic(String path) {
        try {
            if (Gdx.files.internal(path).exists()) {
                return Gdx.audio.newMusic(Gdx.files.internal(path));
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Override
    public void render(float delta) {
        timer += delta;

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.06f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // ===== Quadro: campo de estrelas (fundo, sempre visivel) =====
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < starX.length; i++) {
            float twinkle = 0.5f + 0.5f * MathUtils.sin(timer * 2f + starPhase[i]);
            shapeRenderer.setColor(1f, 1f, 1f, twinkle);
            shapeRenderer.circle(starX[i], starY[i], 2f);
        }

        // ===== Quadro: a Lua =====
        shapeRenderer.setColor(0.85f, 0.85f, 0.9f, 1f);
        shapeRenderer.circle(650, 470, 60);
        shapeRenderer.setColor(0.6f, 0.6f, 0.68f, 1f);
        shapeRenderer.circle(630, 490, 10);
        shapeRenderer.circle(670, 450, 14);
        shapeRenderer.circle(660, 500, 7);

        // ===== Quadro: o capacete (visor + contorno) =====
        shapeRenderer.setColor(0.85f, 0.87f, 0.9f, 1f);
        shapeRenderer.circle(400, 360, 90);
        shapeRenderer.setColor(0.1f, 0.6f, 0.9f, 1f);
        shapeRenderer.circle(400, 360, 60);
        shapeRenderer.setColor(1f, 1f, 1f, 0.25f);
        shapeRenderer.circle(378, 385, 18);
        shapeRenderer.end();

        // ===== Quadros 1 a 6: as letras de "ECHOES" aparecem uma a uma =====
        int lettersShown = Math.min(TITLE.length(), (int) (timer / SECONDS_PER_LETTER) + 1);
        String revealed = TITLE.substring(0, Math.max(0, lettersShown));

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, revealed, 180, 170);

        if (timer >= MIN_DURATION) {
            fontBody.setColor(Color.LIGHT_GRAY);
            fontBody.draw(batch, "OF THE MOON", 195, 110);

            fontBody.setColor(Color.YELLOW);
            fontBody.draw(batch, "[ENTER] Pular", 640, 30);
        }
        batch.end();

        // ===== Fim: apos MIN_DURATION o jogador pode pular; apos TOTAL_DURATION avanca sozinho =====
        if (!advancing) {
            boolean skipRequested = timer >= MIN_DURATION && Gdx.input.isKeyJustPressed(Input.Keys.ENTER);
            boolean naturalEnd = timer >= TOTAL_DURATION;
            if (skipRequested || naturalEnd) {
                advancing = true;
                goToMenu();
            }
        }
    }

    private void goToMenu() {
        if (music != null) music.stop();
        game.setScreen(new MenuScreen(game));
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        fontTitle.dispose();
        fontBody.dispose();
        if (music != null) music.dispose();
    }

    /*
     * ===== Video real (opcional) =====
     * Se voce adicionar a dependencia gdx-video ao build.gradle (core +
     * gdx-video-lwjgl3 no desktop) e colocar assets/video/intro.webm,
     * pode trocar o construtor/render por algo assim (API oficial):
     *
     *   VideoPlayer vp = VideoPlayerCreator.createVideoPlayer();
     *   vp.play(Gdx.files.internal("video/intro.webm"));
     *   // a cada frame: vp.update(); desenhar vp.getTexture();
     *   // quando !vp.isPlaying() (ou apos 6s), chame goToMenu().
     *
     * Sem essa dependencia adicionada, NAO descomente nem referencie
     * VideoPlayer diretamente no codigo, ou o projeto para de compilar.
     * Por isso este arquivo usa soh o Plano B, que ja e aceito.
     */
}
