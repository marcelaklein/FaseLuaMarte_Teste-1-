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
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.modulo06.echoesmoon.utils.QuestTracker;

public class MarsScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, bossTex, crystalTex, bgTex, scientistTex, ammoTex;
    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle scientist;
    private Rectangle portalTitan;
    private float playerSpeed = 300f;

    private float o2 = 100f;
    private float energy = 100f;
    private int crystalsCollected = 0;
    private final int TOTAL_CRYSTALS = 4;
    private boolean marteMissoesOk = false;

    private int ammo = 15;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.5f;
    private final float BULLET_RANGE = 480f;

    // Requisito 16: o Boss de Marte nao existe mais desde o inicio da fase.
    // Ele e criado apenas em checkAndSpawnMarsBoss(), depois que a
    // "missao de Marte" (coletar os 4 cristais) e cumprida.
    private Enemy marsBoss = null;

    public enum DialogState { CLOSED, OPEN, FINISHED }
    private DialogState dialogState = DialogState.CLOSED;
    private String[] dialogLines = {
        "CIENTISTA: Bem-vindo a Marte!",
        "CIENTISTA: Colete os 4 cristais espalhados pelo terreno. O Boss de Marte so vai despertar depois que voce reunir todos eles."
    };
    private int dialogIndex = 0;
    private boolean talkedToScientist = false;

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

    class Crystal {
        Rectangle rect;
        boolean collected = false;
        public Crystal(float x, float y) {
            this.rect = new Rectangle(x, y, 48, 48);
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

    enum EnemyType { PATROL, CHASER, BOSS }

    class Enemy {
        Rectangle rect;
        float speed;
        EnemyType type;
        Vector2 patrolA, patrolB;
        boolean movingToB = true;
        int health;

        public Enemy(float x, float y, EnemyType type, Vector2 patrolB, int health, float speed, float w, float h) {
            this.rect = new Rectangle(x, y, w, h);
            this.type = type;
            this.patrolA = new Vector2(x, y);
            this.patrolB = patrolB != null ? patrolB : new Vector2(x + 250, y);
            this.health = health;
            this.speed = speed;
        }

        public void update(float delta, Vector2 playerPos) {
            if (type == EnemyType.CHASER || type == EnemyType.BOSS) {
                float dist = playerPos.dst(rect.x, rect.y);
                if (dist < 700f) {
                    Vector2 dir = new Vector2(playerPos.x - rect.x, playerPos.y - rect.y).nor();
                    rect.x += dir.x * speed * delta;
                    rect.y += dir.y * speed * delta;
                }
            } else {
                Vector2 target = movingToB ? patrolB : patrolA;
                Vector2 dir = new Vector2(target.x - rect.x, target.y - rect.y);
                if (dir.len() < 10f) movingToB = !movingToB;
                else {
                    dir.nor();
                    rect.x += dir.x * speed * delta;
                    rect.y += dir.y * speed * delta;
                }
            }
        }
    }

    private Array<Crystal> crystals;
    private Array<Enemy> enemies;
    private Array<Bullet> bullets;
    private Array<AmmoPickup> ammoPickups;

    public MarsScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        scientist = new Rectangle(320, 200, 64, 96);
        portalTitan = new Rectangle(320, 340, 120, 120);

        crystals = new Array<>();
        enemies = new Array<>();
        bullets = new Array<>();
        ammoPickups = new Array<>();

        ammoPickups.add(new AmmoPickup(500, 300, 10));
        ammoPickups.add(new AmmoPickup(1200, 1200, 10));

        crystals.add(new Crystal(600, 600));
        crystals.add(new Crystal(1400, 400));
        crystals.add(new Crystal(800, 1500));
        crystals.add(new Crystal(1600, 1600));

        // Requisito 16: o Boss de Marte NAO e mais adicionado aqui no inicio
        // da fase. Ele so aparece via checkAndSpawnMarsBoss(), apos os 4
        // cristais serem coletados (ver metodo abaixo).
        enemies.add(new Enemy(500, 1000, EnemyType.CHASER, null, 2, 130f, 64, 64));

        loadGame();

        playerTex = safeLoadTexture("player_gun.png");
        scientistTex = safeLoadTexture("cientista.png");
        bgTex = safeLoadTexture("fundo_marte.png");
        crystalTex = safeLoadTexture("crystal.png");
        enemyTex = safeLoadTexture("alien_marte.png");
        bossTex = safeLoadTexture("chefao_marte.png");
        if (bossTex == null) bossTex = enemyTex;
        ammoTex = safeLoadTexture("ammo.png");

        // Caso a fase seja recarregada (ex: [C] Continuar) com os 4 cristais
        // ja coletados mas o boss ainda nao derrotado, ele reaparece aqui
        // silenciosamente (sem reabrir a caixa de dialogo).
        checkAndSpawnMarsBoss(false);

        if (!talkedToScientist) dialogState = DialogState.OPEN;
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
        this.crystalsCollected = qt.getMarsCrystals();
        this.talkedToScientist = qt.isMarsScientistTalked();
        this.marteMissoesOk = qt.isMarsMissionComplete();
    }

    private void saveGame() {
        QuestTracker qt = QuestTracker.getInstance();
        qt.setO2(o2);
        qt.setEnergy(energy);
        qt.setAmmo(ammo);
        qt.setMarsCrystals(crystalsCollected);
        qt.setMarsScientistTalked(talkedToScientist);
        qt.setMarsMissionComplete(marteMissoesOk);
        qt.setCurrentStage(QuestTracker.STAGE_MARTE);
    }

    /**
     * Requisito 16 (Boss de Marte apos missoes de Marte).
     * So cria o Boss quando: ainda nao existe, a missao de Marte ainda nao
     * foi concluida, e os 4 cristais ja foram coletados. "announce" = true
     * abre uma caixa de dialogo avisando o jogador (usado quando o ultimo
     * cristal e coletado em tempo real); "announce" = false recoloca o
     * boss em cena silenciosamente (usado ao recarregar a fase).
     */
    private void checkAndSpawnMarsBoss(boolean announce) {
        if (marsBoss == null && !marteMissoesOk && crystalsCollected >= TOTAL_CRYSTALS) {
            marsBoss = new Enemy(1500, 1500, EnemyType.BOSS, null, 20, 110f, 128, 128);
            enemies.add(marsBoss);

            if (announce) {
                dialogLines = new String[]{
                    "CIENTISTA: Voce reuniu todos os cristais!",
                    "CIENTISTA: Cuidado, o Boss de Marte despertou para proteger o portal!"
                };
                dialogIndex = 0;
                dialogState = DialogState.OPEN;
            }
            saveGame();
        }
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.2f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(marteMissoesOk ? Color.CYAN : Color.GRAY);
        shapeRenderer.rect(portalTitan.x, portalTitan.y, portalTitan.width, portalTitan.height);
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);

        if (scientistTex != null) batch.draw(scientistTex, scientist.x, scientist.y, scientist.width, scientist.height);
        font.draw(batch, "CIENTISTA [E]", scientist.x, scientist.y + scientist.height + 15);

        if (marteMissoesOk) {
            font.draw(batch, "PORTAL PARA TITA [ATIVO]", portalTitan.x, portalTitan.y + portalTitan.height + 15);
        } else if (marsBoss != null) {
            font.draw(batch, "PORTAL BLOQUEADO (Derrote o Boss)", portalTitan.x, portalTitan.y + portalTitan.height + 15);
        } else {
            font.draw(batch, "PORTAL BLOQUEADO (Colete os " + TOTAL_CRYSTALS + " cristais para enfrentar o Boss)", portalTitan.x - 60, portalTitan.y + portalTitan.height + 15);
        }

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex != null) batch.draw(ammoTex, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
        }

        if (playerTex != null) batch.draw(playerTex, player.x, player.y, player.width, player.height);

        for (Enemy e : enemies) {
            if (e.type == EnemyType.BOSS && bossTex != null) {
                batch.draw(bossTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
                font.draw(batch, "BOSS DE MARTE (HP: " + e.health + ")", e.rect.x, e.rect.y + e.rect.height + 20);
            } else if (enemyTex != null) {
                batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        for (Crystal c : crystals) {
            if (!c.collected && crystalTex != null) batch.draw(crystalTex, c.rect.x, c.rect.y, c.rect.width, c.rect.height);
        }
        batch.end();
        renderHUD();

        if (dialogState == DialogState.OPEN) renderDialogBox();
    }

    private void renderHUD() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, 800, 600);
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "FASE: MARTE | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, 580);
        font.draw(batch, "Cristais: " + crystalsCollected + "/" + TOTAL_CRYSTALS + " | Chave Marte: " + (QuestTracker.getInstance().hasItem("CHAVE_MARTE") ? "SIM" : "NAO"), 20, 555);
        batch.end();
    }

    private void renderDialogBox() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, 800, 600);
        shapeRenderer.setProjectionMatrix(hudMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.85f);
        shapeRenderer.rect(50, 20, 700, 120);
        shapeRenderer.end();

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.WHITE);
        if (dialogLines != null && dialogIndex < dialogLines.length) {
            font.draw(batch, dialogLines[dialogIndex], 70, 110);
        }
        font.setColor(Color.YELLOW);
        font.draw(batch, "[ESPACO] continuar", 550, 45);
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

        if (dialogState == DialogState.OPEN) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                dialogIndex++;
                if (dialogLines == null || dialogIndex >= dialogLines.length) {
                    dialogState = DialogState.CLOSED;
                    talkedToScientist = true;
                }
            }
            return;
        }

        if (player.overlaps(portalTitan) && marteMissoesOk) {
            o2 = 100f; energy = 100f;
            QuestTracker.getInstance().saveCheckpoint(QuestTracker.STAGE_TITA, o2, energy, ammo);
            game.setScreen(new TitanScreen(game));
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

        o2 -= 1.1f * delta;
        energy -= 0.9f * delta;

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && player.overlaps(a.rect)) {
                a.collected = true;
                ammo = Math.min(maxAmmo, ammo + a.amount);
                saveGame();
            }
        }

        Vector2 playerPos = new Vector2(player.x, player.y);
        for (Enemy e : enemies) {
            e.update(delta, playerPos);
            if (player.overlaps(e.rect)) {
                o2 -= 18f * delta;
                energy -= 18f * delta;
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
            for (int j = enemies.size - 1; j >= 0; j--) {
                Enemy e = enemies.get(j);
                if (b.rect.overlaps(e.rect)) {
                    e.health--;
                    bullets.removeIndex(i);
                    if (e.health <= 0) {
                        if (e.type == EnemyType.BOSS) {
                            marteMissoesOk = true;
                            QuestTracker.getInstance().setMarsMissionComplete(true);
                            QuestTracker.getInstance().addItem("CHAVE_MARTE");
                        }
                        enemies.removeIndex(j);
                    }
                    break;
                }
            }
        }

        for (Crystal c : crystals) {
            if (!c.collected && player.overlaps(c.rect)) {
                c.collected = true;
                crystalsCollected++;
                saveGame();
                // Requisito 16: assim que o ultimo cristal e coletado, o
                // Boss de Marte e criado e o jogador e avisado.
                checkAndSpawnMarsBoss(true);
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
        if (scientistTex != null) scientistTex.dispose();
        if (enemyTex != null) enemyTex.dispose();
        if (bossTex != null) bossTex.dispose();
        if (crystalTex != null) crystalTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (ammoTex != null) ammoTex.dispose();
    }
}
