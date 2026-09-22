package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.modulo06.echoesmoon.utils.QuestTracker;

/**
 * Requisito 6: Video de encerramento.
 *
 * So e criada depois da terceira fala em Aharin (ver AharinScreen, que ja
 * chama "new EarthEndingScreen(game)" quando dialogIndex passa da ultima
 * fala). Usa o "Plano B" aceito no enunciado: EndingScreen com 4 frases,
 * 2 segundos cada, fundo preto, total >= 8s reais (medidos por delta, nao
 * por frames), e depois volta sozinho para o MenuScreen — sem precisar
 * de qualquer input do jogador.
 *
 * Se quiser usar um video real (assets/video/ending.webm) no lugar deste
 * Plano B, veja o comentario no fim da classe — so funciona se a
 * dependencia gdx-video estiver no build.gradle.
 */
public class EarthEndingScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont fontPhrase;
    private BitmapFont fontFooter;

    private float timer = 0f;
    private boolean advancing = false;

    // Requisito 6 (Plano B): exatamente 4 frases, 2s cada = 8s totais.
    private final String[] phrases = {
        "A Terra reaparece no horizonte escuro do espaco...",
        "Cada lua percorrida guardava um eco da sua jornada.",
        "\"A era nao se impoe. Ela se escolhe.\"",
        "ECHOES OF THE MOON"
    };
    private final float PHRASE_DURATION = 2f;
    private final float FADE_TIME = 0.4f;
    private final float TOTAL_DURATION = phrases.length * PHRASE_DURATION; // 8s

    public EarthEndingScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        fontPhrase = new BitmapFont();
        fontPhrase.getData().setScale(1.6f);
        fontFooter = new BitmapFont();
        fontFooter.getData().setScale(1.0f);

        // Limpa o save atual ao concluir o jogo com sucesso
        QuestTracker.getInstance().resetProgress();
    }

    @Override
    public void render(float delta) {
        timer += delta;

        // Fundo preto (pedido explicitamente no Plano B)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        int index = Math.min(phrases.length - 1, (int) (timer / PHRASE_DURATION));
        float localTime = timer - index * PHRASE_DURATION;

        // Envelope de fade in/out dentro de cada frase (efeito "fade" pedido no enunciado)
        float alpha;
        if (localTime < FADE_TIME) {
            alpha = localTime / FADE_TIME;
        } else if (localTime > PHRASE_DURATION - FADE_TIME) {
            alpha = (PHRASE_DURATION - localTime) / FADE_TIME;
        } else {
            alpha = 1f;
        }
        alpha = MathUtils.clamp(alpha, 0f, 1f);

        // "Terra vista do espaco" — um circulo azul/verde simples ao fundo, na primeira frase
        if (index == 0) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.1f, 0.3f, 0.7f, alpha);
            shapeRenderer.circle(400, 420, 90);
            shapeRenderer.setColor(0.15f, 0.55f, 0.25f, alpha);
            shapeRenderer.circle(365, 450, 22);
            shapeRenderer.circle(430, 400, 16);
            shapeRenderer.end();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        fontPhrase.setColor(1f, 1f, 1f, alpha);
        String phrase = phrases[index];
        float textX = 400 - (phrase.length() * 4.5f);
        fontPhrase.draw(batch, phrase, Math.max(30, textX), 250);

        fontFooter.setColor(0.6f, 0.6f, 0.6f, 0.8f);
        fontFooter.draw(batch, "Missao Concluida", 340, 60);
        batch.end();

        // O fim acontece sozinho: apos 8s totais, volta ao menu sem input do jogador.
        if (!advancing && timer >= TOTAL_DURATION) {
            advancing = true;
            game.setScreen(new MenuScreen(game));
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
        fontPhrase.dispose();
        fontFooter.dispose();
    }

    /*
     * ===== Video real (opcional) =====
     * Com a dependencia gdx-video no build.gradle e assets/video/ending.webm,
     * a API oficial e:
     *
     *   VideoPlayer vp = VideoPlayerCreator.createVideoPlayer();
     *   vp.play(Gdx.files.internal("video/ending.webm"));
     *   // a cada frame: vp.update(); desenhar vp.getTexture();
     *   // quando !vp.isPlaying(), chame game.setScreen(new MenuScreen(game)).
     *
     * Sem essa dependencia, nao referencie VideoPlayer aqui, ou o projeto
     * para de compilar. Por isso esta classe usa somente o Plano B.
     */
}
