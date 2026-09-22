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
import com.modulo06.echoesmoon.utils.QuestTracker;

public class PauseMenuScreen implements Screen {

    private Game game;
    private Screen previousScreen;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont fontTitle;
    private BitmapFont fontOptions;
    private String saveStatusMessage = "";
    private float messageTimer = 0f;

    public PauseMenuScreen(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(2.0f);

        fontOptions = new BitmapFont();
        fontOptions.getData().setScale(1.3f);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        QuestTracker qt = QuestTracker.getInstance();

        batch.begin();
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, "JOGO PAUSADO", 290, 470);

        fontOptions.setColor(Color.LIGHT_GRAY);
        fontOptions.draw(batch, "Fase atual: " + qt.getCurrentStage()
            + "   |   O2: " + (int) qt.getO2() + "%"
            + "   |   Municao: " + qt.getAmmo(), 190, 400);

        fontOptions.setColor(Color.YELLOW);
        fontOptions.draw(batch, "[ENTER] Retornar ao Jogo", 250, 330);
        fontOptions.draw(batch, "[S] Salvar o Jogo", 250, 285);
        fontOptions.draw(batch, "[ESC] Sair para o Menu Principal", 250, 240);

        if (!saveStatusMessage.isEmpty()) {
            fontOptions.setColor(Color.GREEN);
            fontOptions.draw(batch, saveStatusMessage, 270, 170);
            messageTimer -= delta;
            if (messageTimer <= 0) {
                saveStatusMessage = "";
            }
        }
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(previousScreen);
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            saveGameManually();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // Salva antes de sair, para o "Continuar" funcionar
            QuestTracker.getInstance().saveProgress();
            game.setScreen(new MenuScreen(game));
        }
    }

    private void saveGameManually() {
        QuestTracker.getInstance().saveProgress();
        saveStatusMessage = "Jogo Salvo com Sucesso!";
        messageTimer = 2.0f;
    }

    @Override public void show() {}
    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        fontTitle.dispose();
        fontOptions.dispose();
    }
}
