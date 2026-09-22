package com.modulo06.echoesmoon.screens;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4; // <--- IMPORT ADICIONADO AQUI
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.modulo06.echoesmoon.utils.QuestTracker;

public class CallistoScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, bossForm1Tex, bossForm2Tex, bossForm3Tex, ammoTex;
    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle portalAharin;
    private float playerSpeed = 300f;

    private float o2 = 100f;
    private float energy = 100f;
    private int ammo = 25;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.4f;
    private final float BULLET_RANGE = 500f;

    // Mecânica do Chefe de 3 Mutações
    private int bossMutationForm = 1; // 1, 2 ou 3
    private int bossHealth = 20;
    private boolean mortoFinal = false;
    private float dashTimer = 0f;

    class Bullet {
        Rectangle rect;
        Vector2 velocity;
        Vector2 origin;
        public Bullet(float x, float y, Vector2 dir, float speed) {
            this.rect = new Rectangle(x - 6, y - 6, 12, 12);
            this.velocity = new Vector2(dir).nor().scl(speed);
            this.origin = new Vector2(x, y);
        }
        public void update(float delta) {
            rect.x += velocity.x * delta;
            rect.y += velocity.y * delta;
        }
        public float travelled() {
            return origin.dst(rect.x + rect.width / 2f, rect.y + rect.height / 2f);
        }
    }

    class AmmoPickup {
        Rectangle rect;
        int amount;
        boolean collected = false;
        public AmmoPickup(float x, float y, int amount) {
            this.rect = new Rectangle(x, y, 40, 40);
            this.amount = amount;
        }
    }

    class MutatingBoss {
        Rectangle rect;
        float speed;
        int health;

        public MutatingBoss(float x, float y, int health, float speed) {
            this.rect = new Rectangle(x, y, 128, 128);
            this.health = health;
            this.speed = speed;
        }

        public void update(float delta, Vector2 playerPos) {
            float currentSpeed = speed;
            if (bossMutationForm == 2) currentSpeed = speed * 1.3f;
            else if (bossMutationForm == 3) {
                currentSpeed = speed * 1.6f;
                dashTimer += delta;
                if (dashTimer > 3f) {
                    Vector2 dashDir = new Vector2(playerPos.x - rect.x, playerPos.y - rect.y).nor();
                    rect.x += dashDir.x * 250f;
                    rect.y += dashDir.y * 250f;
                    dashTimer = 0f;
                }
            }

            float dist = playerPos.dst(rect.x, rect.y);
            if (dist < 1000f) {
                Vector2 dir = new Vector2(playerPos.x - rect.x, playerPos.y - rect.y).nor();
                rect.x += dir.x * currentSpeed * delta;
                rect.y += dir.y * currentSpeed * delta;
            }
        }
    }

    private MutatingBoss boss;
    private Array<Bullet> bullets;
    private Array<AmmoPickup> ammoPickups;

    public CallistoScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        portalAharin = new Rectangle(1800, 1800, 120, 120);

        bullets = new Array<>();
        ammoPickups = new Array<>();
        ammoPickups.add(new AmmoPickup(500, 500, 10));
        ammoPickups.add(new AmmoPickup(1200, 1200, 10));

        boss = new MutatingBoss(1500, 1500, 20, 100f);

        loadGame();

        playerTex = safeLoadTexture("player_gun.png");
        bossForm1Tex = safeLoadTexture("chefao_calisto_f1.png");
        bossForm2Tex = safeLoadTexture("chefao_calisto_f2.png");
        bossForm3Tex = safeLoadTexture("chefao_calisto_f3.png");
        ammoTex = safeLoadTexture("ammo.png");
    }

    private Texture safeLoadTexture(String path) {
        try {
            if (Gdx.files.internal(path).exists()) return new Texture(path);
        } catch (Exception ignored) {}
        return null;
    }

    private void loadGame() {
        QuestTracker qt = QuestTracker.getInstance();
        qt.loadProgress();
        this.o2 = qt.getO2();
        this.energy = qt.getEnergy();
        this.ammo = qt.getAmmo();
        this.mortoFinal = qt.isCallistoMissionComplete();
        if (mortoFinal) bossMutationForm = 3;
    }

    private void saveGame() {
        QuestTracker qt = QuestTracker.getInstance();
        qt.setO2(o2);
        qt.setEnergy(energy);
        qt.setAmmo(ammo);
        qt.setCallistoMissionComplete(mortoFinal);
        qt.setCurrentStage(QuestTracker.STAGE_CALISTO);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.1f, 0.2f, 0.4f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        boolean hasLightKey = QuestTracker.getInstance().hasItem("CHAVE_LUZ");

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(hasLightKey ? Color.GOLD : Color.DARK_GRAY);
        shapeRenderer.rect(portalAharin.x, portalAharin.y, portalAharin.width, portalAharin.height);
        shapeRenderer.end();
        batch.begin();

        if (hasLightKey) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "PORTAL PARA AHARIN [ONLINE]", portalAharin.x - 20, portalAharin.y + portalAharin.height + 15);
        } else {
            font.setColor(Color.RED);
            font.draw(batch, "PORTAL BLOQUEADO (Exige CHAVE DE LUZ)", portalAharin.x - 40, portalAharin.y + portalAharin.height + 15);
        }

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex != null) batch.draw(ammoTex, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
        }

        if (playerTex != null) batch.draw(playerTex, player.x, player.y, player.width, player.height);

        if (!mortoFinal && boss != null) {
            Texture currentBossTex = bossForm1Tex;
            if (bossMutationForm == 2) currentBossTex = bossForm2Tex;
            else if (bossMutationForm == 3) currentBossTex = bossForm3Tex;

            if (currentBossTex != null) {
                batch.draw(currentBossTex, boss.rect.x, boss.rect.y, boss.rect.width, boss.rect.height);
            }
            font.setColor(Color.CYAN);
            font.draw(batch, "MUTACAO BOSS - Forma: " + bossMutationForm + "/3 (HP: " + bossHealth + ")", boss.rect.x - 20, boss.rect.y + boss.rect.height + 20);
        }

        batch.end();
        renderHUD();
    }

    private void renderHUD() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, 800, 600);
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "FASE: CALISTO (Gelo-Azul) | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, 580);
        font.draw(batch, "Chave de Luz: " + (QuestTracker.getInstance().hasItem("CHAVE_LUZ") ? "ADQUIRIDA" : "FALTA") + " | Forma Atual Chefe: " + (mortoFinal ? "Derrotado" : bossMutationForm), 20, 555);
        batch.end();
    }

    private void update(float delta) {
        if (o2 <= 0 || energy <= 0) {
            game.setScreen(new GameOverScreen(game));
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            saveGame();
            game.setScreen(new PauseMenuScreen(game, this));
            return;
        }

        boolean hasLightKey = QuestTracker.getInstance().hasItem("CHAVE_LUZ");
        if (player.overlaps(portalAharin) && hasLightKey) {
            o2 = 100f; energy = 100f;
            QuestTracker.getInstance().saveCheckpoint(QuestTracker.STAGE_AHARIN, o2, energy, ammo);
            game.setScreen(new AharinScreen(game));
            return;
        }

        if (attackCooldown > 0) attackCooldown -= delta;

        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= playerSpeed * delta;

        player.x += moveX; player.y += moveY;
        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
        player.y = MathUtils.clamp(player.y, 0, WORLD_HEIGHT - player.height);

        camera.position.set(
            MathUtils.clamp(player.x, camera.viewportWidth / 2f, WORLD_WIDTH - camera.viewportWidth / 2f),
            MathUtils.clamp(player.y, camera.viewportHeight / 2f, WORLD_HEIGHT - camera.viewportHeight / 2f), 0
        );

        o2 -= 1.3f * delta;
        energy -= 1.1f * delta;

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && player.overlaps(a.rect)) {
                a.collected = true;
                ammo = Math.min(maxAmmo, ammo + a.amount);
                saveGame();
            }
        }

        if (!mortoFinal && boss != null) {
            boss.update(delta, new Vector2(player.x, player.y));
            if (player.overlaps(boss.rect)) {
                o2 -= 25f * delta;
                energy -= 25f * delta;
            }
        }

        if (Gdx.input.isButtonPressed(Input.Buttons.LEFT) && attackCooldown <= 0 && ammo > 0) {
            ammo--;
            attackCooldown = COOLDOWN_TIME;
            Vector3 mouseWorldPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
            camera.unproject(mouseWorldPos);
            float spawnX = player.x + player.width / 2f;
            float spawnY = player.y + player.height / 2f;
            Vector2 dir = new Vector2(mouseWorldPos.x - spawnX, mouseWorldPos.y - spawnY);
            if (dir.len2() > 0.0001f) {
                bullets.add(new Bullet(spawnX, spawnY, dir, 800f));
            }
        }

        for (int i = bullets.size - 1; i >= 0; i--) {
            Bullet b = bullets.get(i);
            b.update(delta);
            if (b.travelled() > BULLET_RANGE) {
                bullets.removeIndex(i);
                continue;
            }
            if (!mortoFinal && boss != null && b.rect.overlaps(boss.rect)) {
                boss.health--;
                bullets.removeIndex(i);
                if (boss.health <= 0) {
                    if (bossMutationForm == 1) {
                        bossMutationForm = 2;
                        boss.health = 20;
                    } else if (bossMutationForm == 2) {
                        bossMutationForm = 3;
                        boss.health = 20;
                    } else if (bossMutationForm == 3) {
                        mortoFinal = true;
                        QuestTracker.getInstance().setCallistoMissionComplete(true);
                        QuestTracker.getInstance().addItem("CHAVE_LUZ");
                    }
                }
                break;
            }
        }
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() { saveGame(); }
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); font.dispose();
        if (playerTex != null) playerTex.dispose();
        if (bossForm1Tex != null) bossForm1Tex.dispose();
        if (bossForm2Tex != null) bossForm2Tex.dispose();
        if (bossForm3Tex != null) bossForm3Tex.dispose();
        if (ammoTex != null) ammoTex.dispose();
    }
}
