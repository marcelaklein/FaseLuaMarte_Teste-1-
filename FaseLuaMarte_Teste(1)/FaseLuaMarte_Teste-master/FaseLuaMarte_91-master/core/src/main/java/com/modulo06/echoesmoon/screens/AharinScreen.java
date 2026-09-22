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
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class AharinScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private String[] dialogLines = {
        "ENTIDADE DE LUZ: Saudações, viajante. Chegaste ao limiar de Rigel e Aharin.",
        "ENTIDADE DE LUZ: Tua coragem restaurou o equilíbrio através dos mundos.",
        "ENTIDADE DE LUZ: É hora de retornar ao nosso lar primordial. A Terra aguarda."
    };
    private int dialogIndex = 0;

    public AharinScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
    }

    @Override
    public void render(float delta) {
        // Paleta Azul-Branco (Aharin)
        Gdx.gl.glClearColor(0.8f, 0.9f, 1.0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            dialogIndex++;
            if (dialogIndex >= dialogLines.length) {
                // Ao terminar as 3 falas, dispara o encerramento / retorno à Terra
                game.setScreen(new EarthEndingScreen(game));
                return;
            }
        }

        camera.update();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.85f);
        shapeRenderer.rect(50, 200, 700, 200);
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.CYAN);
        font.draw(batch, "FASE: AHARIN (Santuário de Rigel)", 280, 440);

        font.setColor(Color.WHITE);
        if (dialogIndex < dialogLines.length) {
            font.draw(batch, dialogLines[dialogIndex], 80, 320);
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, "[ESPACO] Avancar diálogo (" + (dialogIndex + 1) + "/3)", 520, 230);
        batch.end();
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        font.dispose();
    }
}
