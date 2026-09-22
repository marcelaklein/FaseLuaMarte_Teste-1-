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

public class VictoryCutsceneScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private float timer = 0f;

    public VictoryCutsceneScreen(Game game) {
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
        Gdx.gl.glClearColor(0.0f, 0.1f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        font.setColor(Color.GREEN);
        font.draw(batch, "[CUTSCENE DE ENCERRAMENTO - POS-AHARIN]", 180, 380);
        font.setColor(Color.WHITE);
        font.draw(batch, "A Rainha de Aharin foi derrotada e a paz retorna...", 200, 330);

        font.setColor(Color.YELLOW);
        font.draw(batch, "Pressione ESPACO para ver o relatorio final", 230, 220);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE) || timer >= 7.0f) {
            game.setScreen(new VictoryScreen(game));
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
