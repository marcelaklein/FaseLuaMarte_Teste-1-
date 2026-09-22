package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class IntroVideoScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private float timer = 0f;
    private static final float MIN_DURATION = 6.0f; // Exige >= 6 segundos

    public IntroVideoScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.5f);
    }

    @Override
    public void render(float delta) {
        timer += delta;

        Gdx.gl.glClearColor(0.02f, 0.02f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        font.setColor(Color.CYAN);
        font.draw(batch, "[CUTSCENE / VIDEO DE INTRODUCAO]", 210, 380);
        font.setColor(Color.WHITE);
        font.draw(batch, "A historia de Echoes of the Moon...", 240, 330);

        font.setColor(Color.GRAY);
        font.draw(batch, "Tempo decorrido: " + String.format("%.1f", timer) + "s / 6.0s", 260, 250);

        if (timer >= MIN_DURATION) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "Pressione ESPACO para iniciar o jogo", 220, 180);
        }
        batch.end();

        // Só avança após os 6 segundos e o pressionamento de ESPACO (ou automaticamente)
        if (timer >= MIN_DURATION && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            game.setScreen(new GameScreen(game, 0));
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
