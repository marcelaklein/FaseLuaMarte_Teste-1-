package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.modulo06.echoesmoon.utils.QuestTracker;

public class MenuScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;
    private BitmapFont fontTitle;

    public MenuScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();

        font = new BitmapFont();
        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(2.0f);
    }

    @Override
    public void render(float delta) {
        // Verifica se existem saves nos slots 1 e 2
        Preferences prefs1 = Gdx.app.getPreferences("EchoesMoonSave1");
        Preferences prefs2 = Gdx.app.getPreferences("EchoesMoonSave2");
        boolean hasSave1 = prefs1.contains("currentStage");
        boolean hasSave2 = prefs2.contains("currentStage");

        // Novo Jogo Slot 1 (Tecla 1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.N)) {
            Gdx.app.getPreferences("EchoesMoonSave1").clear();
            Gdx.app.getPreferences("EchoesMoonSave1").flush();
            QuestTracker.getInstance().resetProgress();
            game.setScreen(new IntroVideoScreen(game)); // Vai para o vídeo de intro >= 6s
            return;
        }

        // Novo Jogo Slot 2 (Tecla 2)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            Gdx.app.getPreferences("EchoesMoonSave2").clear();
            Gdx.app.getPreferences("EchoesMoonSave2").flush();
            QuestTracker.getInstance().resetProgress();
            game.setScreen(new IntroVideoScreen(game));
            return;
        }

        // Continuar Slot 1 (Tecla C)
        if (Gdx.input.isKeyJustPressed(Input.Keys.C) && hasSave1) {
            // Copia os dados do slot 1 para o save padrão do jogo
            copyPrefs("EchoesMoonSave1", "EchoesMoonSave");
            QuestTracker.getInstance().loadProgress();
            game.setScreen(createScreenForStage(QuestTracker.getInstance().getCurrentStage()));
            return;
        }

        // Continuar Slot 2 (Tecla V)
        if (Gdx.input.isKeyJustPressed(Input.Keys.V) && hasSave2) {
            // Copia os dados do slot 2 para o save padrão do jogo
            copyPrefs("EchoesMoonSave2", "EchoesMoonSave");
            QuestTracker.getInstance().loadProgress();
            game.setScreen(createScreenForStage(QuestTracker.getInstance().getCurrentStage()));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, "ECHOES OF THE MOON", 210, 460);

        font.setColor(Color.WHITE);
        font.draw(batch, "Pressione [1] Novo Jogo (Slot 1)  |  Pressione [2] Novo Jogo (Slot 2)", 150, 340);

        if (hasSave1) {
            font.setColor(Color.GREEN);
            font.draw(batch, "[C] Continuar Slot 1 (Fase: " + prefs1.getString("currentStage", "LUA") + ")", 180, 290);
        } else {
            font.setColor(Color.DARK_GRAY);
            font.draw(batch, "[C] Continuar Slot 1 (Vazio)", 180, 290);
        }

        if (hasSave2) {
            font.setColor(Color.GREEN);
            font.draw(batch, "[V] Continuar Slot 2 (Fase: " + prefs2.getString("currentStage", "LUA") + ")", 180, 250);
        } else {
            font.setColor(Color.DARK_GRAY);
            font.draw(batch, "[V] Continuar Slot 2 (Vazio)", 180, 250);
        }

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Pressione [ESC] para SAIR", 305, 180);

        batch.end();
    }

    private void copyPrefs(String sourceName, String destName) {
        Preferences src = Gdx.app.getPreferences(sourceName);
        Preferences dest = Gdx.app.getPreferences(destName);
        dest.clear();
        dest.put(src.get());
        dest.flush();
    }

    private Screen createScreenForStage(String stage) {
        if (stage == null) stage = QuestTracker.STAGE_LUA;
        if (stage.equals(QuestTracker.STAGE_MARTE)) return new MarsScreen(game);
        if (stage.equals(QuestTracker.STAGE_TITA)) return new TitanScreen(game);
        if (stage.equals(QuestTracker.STAGE_CALISTO)) return new CallistoScreen(game);
        if (stage.equals(QuestTracker.STAGE_AHARIN)) return new AharinScreen(game);
        return new GameScreen(game, QuestTracker.getInstance().getMoonFragments());
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
        fontTitle.dispose();
    }
}
