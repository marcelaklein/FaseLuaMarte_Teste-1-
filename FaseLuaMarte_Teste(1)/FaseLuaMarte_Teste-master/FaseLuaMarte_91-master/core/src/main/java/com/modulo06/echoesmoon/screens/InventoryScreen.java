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
import com.badlogic.gdx.math.Matrix4;
import com.modulo06.echoesmoon.utils.QuestTracker;
import java.util.Map;

public class InventoryScreen implements Screen {

    private Game game;
    private Screen previousScreen;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont fontTitle;
    private BitmapFont fontText;

    public InventoryScreen(Game game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        fontTitle = new BitmapFont();
        fontTitle.getData().setScale(2.0f);

        fontText = new BitmapFont();
        fontText.getData().setScale(1.3f);
    }

    @Override
    public void render(float delta) {
        // Fecha o inventário ao apertar I, TAB ou ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.I) ||
            Gdx.input.isKeyJustPressed(Input.Keys.TAB) ||
            Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(previousScreen);
            return;
        }

        // Fundo escuro semi-transparente sobre a fase atual
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, 800, 600);
        shapeRenderer.setProjectionMatrix(hudMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.2f, 0.95f);
        shapeRenderer.rect(100, 50, 600, 500);
        shapeRenderer.end();

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();

        // Título do Inventário
        fontTitle.setColor(Color.CYAN);
        fontTitle.draw(batch, "INVENTARIO DO SOLDADO", 230, 500);

        // Status das Chaves Coletadas
        QuestTracker qt = QuestTracker.getInstance();

        fontText.setColor(Color.WHITE);
        fontText.draw(batch, "=== ITENS E CHAVES ===", 150, 420);

        drawItemStatus(batch, "Chave da Lua (CHAVE_LUA):", qt.hasItem("CHAVE_LUA"), 380);
        drawItemStatus(batch, "Chave de Marte (CHAVE_MARTE):", qt.hasItem("CHAVE_MARTE"), 340);
        drawItemStatus(batch, "Chave de Tita (CHAVE_TITA):", qt.hasItem("CHAVE_TITA"), 300);
        drawItemStatus(batch, "Chave de Luz (CHAVE_LUZ):", qt.hasItem("CHAVE_LUZ"), 260);
        drawItemStatus(batch, "Drone Companheiro (DRONE):", qt.hasItem("DRONE"), 220);

        // Requisito 22: materiais e itens craftados (com quantidade)
        fontText.setColor(Color.WHITE);
        fontText.draw(batch, "=== MATERIAIS E CRAFTS ===", 150, 175);
        float matY = 150f;
        Map<String, Integer> materials = qt.getAllMaterials();
        if (materials.isEmpty()) {
            fontText.setColor(Color.GRAY);
            fontText.draw(batch, "(nenhum material coletado)", 150, matY);
        } else {
            for (Map.Entry<String, Integer> entry : materials.entrySet()) {
                if (entry.getValue() == null || entry.getValue() <= 0) continue;
                fontText.setColor(Color.LIGHT_GRAY);
                fontText.draw(batch, entry.getKey() + ": " + entry.getValue(), 150, matY);
                matY -= 25f;
            }
        }

        // Progresso atual
        fontText.setColor(Color.YELLOW);
        fontText.draw(batch, "Fase Atual: " + qt.getCurrentStage(), 450, 190);
        fontText.draw(batch, "Arma da Lua Construida: " + (qt.isWeaponBuilt() ? "SIM" : "NAO"), 450, 150);

        // Rodapé
        fontText.setColor(Color.LIGHT_GRAY);
        fontText.draw(batch, "Pressione [I], [TAB] ou [ESC] para voltar ao jogo", 180, 80);

        batch.end();
    }

    private void drawItemStatus(SpriteBatch batch, String label, boolean collected, float y) {
        fontText.setColor(Color.WHITE);
        fontText.draw(batch, label, 150, y);

        if (collected) {
            fontText.setColor(Color.GREEN);
            fontText.draw(batch, "[ADQUIRIDO]", 520, y);
        } else {
            fontText.setColor(Color.RED);
            fontText.draw(batch, "[FALTANDO]", 520, y);
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
        shapeRenderer.dispose();
        fontTitle.dispose();
        fontText.dispose();
    }
}
