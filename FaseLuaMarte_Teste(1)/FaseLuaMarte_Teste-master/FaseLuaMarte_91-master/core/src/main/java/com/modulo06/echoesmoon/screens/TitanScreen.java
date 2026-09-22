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

public class TitanScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, bossTex, partTex, bgTex, scientistTex, ammoTex;
    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle scientist;
    private Rectangle portalCalisto;
    private float playerSpeed = 300f;

    private float o2 = 100f;
    private float energy = 100f;
    private int partsCollected = 0;
    private final int TOTAL_PARTS = 4;
    private boolean titaMissoesOk = false;

    private int ammo = 20;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.4f;
    private final float BULLET_RANGE = 500f;

    // Requisito 17: o Boss de Tita nao existe mais desde o inicio da fase.
    // Ele e criado apenas em checkAndSpawnTitanBoss(), depois que a
    // "missao de Tita" (coletar as 4 pecas) e cumprida.
    private Enemy titanBoss = null;
    private Vector2 lastMoveDir = new Vector2(-1, 0);

    // ===== Requisito 23: Tempestade com timer =====
    enum StormState { CALMO, ALERTA, TEMPESTADE }
    private StormState stormState = StormState.CALMO;
    private float stormTimer = 0f;
    private final float CALM_DURATION = 12f;
    private final float ALERT_DURATION = 3f;
    private final float STORM_DURATION = 8f;
    private Rectangle abrigo;

    // ===== Requisito 24: Drone companheiro =====
    private Rectangle droneItemPickup;
    private boolean droneItemCollected = false;
    private boolean droneActive = false;
    private Vector2 dronePos;
    private float droneAttackCooldown = 0f;
    private final float DRONE_ATTACK_COOLDOWN = 2f;
    private final float DRONE_FOLLOW_LERP = 3.2f;
    private final float DRONE_LIGHT_RADIUS = 220f;

    public enum DialogState { CLOSED, OPEN, FINISHED }
    private DialogState dialogState = DialogState.CLOSED;
    private String[] dialogLines = {
        "CIENTISTA: Chegamos em Tita!",
        "CIENTISTA: Colete as 4 pecas espalhadas pela base. O Boss (20 HP) so vai despertar depois que voce reunir todas elas."
    };
    private int dialogIndex = 0;

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

    class Part {
        Rectangle rect;
        boolean collected = false;
        public Part(float x, float y) {
            this.rect = new Rectangle(x, y, 40, 40);
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
                if (dist < 900f) {
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

    private Array<Part> parts;
    private Array<Enemy> enemies;
    private Array<Bullet> bullets;
    private Array<AmmoPickup> ammoPickups;

    public TitanScreen(Game game) {
        this.game = game;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        scientist = new Rectangle(300, 200, 64, 96);
        portalCalisto = new Rectangle(1800, 1800, 120, 120);

        parts = new Array<>();
        enemies = new Array<>();
        bullets = new Array<>();
        ammoPickups = new Array<>();

        parts.add(new Part(500, 500));
        parts.add(new Part(1500, 400));
        parts.add(new Part(800, 1500));
        parts.add(new Part(1600, 1600));

        ammoPickups.add(new AmmoPickup(400, 400, 10));
        ammoPickups.add(new AmmoPickup(1200, 1200, 10));

        // Requisito 23: abrigo que protege da tempestade
        abrigo = new Rectangle(150, 150, 180, 180);

        // Requisito 24: item que precisa estar no inventario para chamar o drone
        droneItemPickup = new Rectangle(700, 900, 40, 40);
        dronePos = new Vector2(player.x - 60, player.y - 60);

        // Requisito 17: o Boss de Tita NAO e mais adicionado aqui no inicio
        // da fase. Ele so aparece via checkAndSpawnTitanBoss(), apos as 4
        // pecas serem coletadas (ver metodo abaixo).
        enemies.add(new Enemy(600, 800, EnemyType.CHASER, null, 3, 130f, 64, 64));

        loadGame();

        playerTex = safeLoadTexture("player_gun.png");
        scientistTex = safeLoadTexture("cientista.png");
        bgTex = safeLoadTexture("fundo_tita.png");
        partTex = safeLoadTexture("crystal.png");
        enemyTex = safeLoadTexture("alien_tita.png");
        bossTex = safeLoadTexture("chefao_tita.png");
        if (bossTex == null) bossTex = enemyTex;
        ammoTex = safeLoadTexture("ammo.png");

        // Caso a fase seja recarregada (ex: [C] Continuar) com as 4 pecas
        // ja coletadas mas o boss ainda nao derrotado, ele reaparece aqui
        // silenciosamente (sem reabrir a caixa de dialogo).
        checkAndSpawnTitanBoss(false);

        dialogState = DialogState.OPEN;
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
        this.partsCollected = qt.getTitanParts();
        this.titaMissoesOk = qt.isTitanMissionComplete();
        // Requisito 24: se o jogador ja tem o item DRONE, o pickup no mapa nao aparece mais
        this.droneItemCollected = qt.hasItem("DRONE");
    }

    private void saveGame() {
        QuestTracker qt = QuestTracker.getInstance();
        qt.setO2(o2);
        qt.setEnergy(energy);
        qt.setAmmo(ammo);
        qt.setTitanParts(partsCollected);
        qt.setTitanMissionComplete(titaMissoesOk);
        qt.setCurrentStage(QuestTracker.STAGE_TITA);
    }

    /**
     * Requisito 17 (Boss de Tita apos missoes de Tita).
     * So cria o Boss quando: ainda nao existe, a missao de Tita ainda nao
     * foi concluida, e as 4 pecas ja foram coletadas. "announce" = true
     * abre uma caixa de dialogo avisando o jogador (usado quando a ultima
     * peca e coletada em tempo real); "announce" = false recoloca o boss
     * em cena silenciosamente (usado ao recarregar a fase).
     */
    private void checkAndSpawnTitanBoss(boolean announce) {
        if (titanBoss == null && !titaMissoesOk && partsCollected >= TOTAL_PARTS) {
            titanBoss = new Enemy(1500, 1500, EnemyType.BOSS, null, 20, 140f, 128, 128);
            enemies.add(titanBoss);

            if (announce) {
                dialogLines = new String[]{
                    "CIENTISTA: Voce reuniu todas as pecas!",
                    "CIENTISTA: Cuidado, o Boss de Tita despertou e esta agressivo!"
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

        // Requisito 23: a cor do ceu muda conforme o estado da tempestade
        if (stormState == StormState.TEMPESTADE) {
            Gdx.gl.glClearColor(0.15f, 0.15f, 0.22f, 1);
        } else if (stormState == StormState.ALERTA) {
            Gdx.gl.glClearColor(0.25f, 0.2f, 0.1f, 1);
        } else {
            Gdx.gl.glClearColor(0.05f, 0.15f, 0.2f, 1);
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(titaMissoesOk ? Color.GREEN : Color.GRAY);
        shapeRenderer.rect(portalCalisto.x, portalCalisto.y, portalCalisto.width, portalCalisto.height);

        // Requisito 23: abrigo (protege da tempestade)
        shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 0.5f);
        shapeRenderer.rect(abrigo.x, abrigo.y, abrigo.width, abrigo.height);

        // Requisito 24: item do drone, se ainda nao coletado
        if (!droneItemCollected) {
            shapeRenderer.setColor(Color.PINK);
            shapeRenderer.rect(droneItemPickup.x, droneItemPickup.y, droneItemPickup.width, droneItemPickup.height);
        }

        // Requisito 24: luz do drone (ilumina a escuridao de Tita)
        if (droneActive) {
            shapeRenderer.setColor(1f, 1f, 0.6f, 0.15f);
            shapeRenderer.circle(dronePos.x, dronePos.y, DRONE_LIGHT_RADIUS, 40);
        }
        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);

        if (scientistTex != null) batch.draw(scientistTex, scientist.x, scientist.y, scientist.width, scientist.height);
        font.draw(batch, "CIENTISTA [E]", scientist.x, scientist.y + scientist.height + 15);

        if (titaMissoesOk) {
            font.draw(batch, "PORTAL PARA CALISTO [ATIVO]", portalCalisto.x, portalCalisto.y + portalCalisto.height + 15);
        } else if (titanBoss != null) {
            font.draw(batch, "PORTAL BLOQUEADO (Derrote o Boss)", portalCalisto.x, portalCalisto.y + portalCalisto.height + 15);
        } else {
            font.draw(batch, "PORTAL BLOQUEADO (Colete as " + TOTAL_PARTS + " pecas para enfrentar o Boss)", portalCalisto.x - 40, portalCalisto.y + portalCalisto.height + 15);
        }

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex != null) batch.draw(ammoTex, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
        }

        if (playerTex != null) batch.draw(playerTex, player.x, player.y, player.width, player.height);

        if (!droneItemCollected) {
            font.setColor(Color.PINK);
            font.draw(batch, "DRONE (item)", droneItemPickup.x - 10, droneItemPickup.y + droneItemPickup.height + 15);
        }

        if (droneActive) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "DRONE", dronePos.x - 10, dronePos.y + 40);
        }

        for (Enemy e : enemies) {
            if (e.type == EnemyType.BOSS && bossTex != null) {
                batch.draw(bossTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
                font.draw(batch, "BOSS DE TITA (HP: " + e.health + ")", e.rect.x, e.rect.y + e.rect.height + 20);
            } else if (enemyTex != null) {
                batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        for (Part p : parts) {
            if (!p.collected && partTex != null) batch.draw(partTex, p.rect.x, p.rect.y, p.rect.width, p.rect.height);
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
        font.draw(batch, "FASE: TITA | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, 580);
        font.draw(batch, "Pecas: " + partsCollected + "/" + TOTAL_PARTS + " | Chave Tita: " + (QuestTracker.getInstance().hasItem("CHAVE_TITA") ? "SIM" : "NAO"), 20, 555);

        // Requisito 23: HUD da tempestade
        String stormLabel;
        int secondsLeft;
        if (stormState == StormState.CALMO) {
            stormLabel = "CEU LIMPO";
            secondsLeft = (int) Math.ceil(CALM_DURATION - stormTimer);
            font.setColor(Color.LIGHT_GRAY);
        } else if (stormState == StormState.ALERTA) {
            stormLabel = "ALERTA DE TEMPESTADE";
            secondsLeft = (int) Math.ceil(ALERT_DURATION - stormTimer);
            font.setColor(Color.ORANGE);
        } else {
            stormLabel = "TEMPESTADE";
            secondsLeft = (int) Math.ceil(STORM_DURATION - stormTimer);
            font.setColor(Color.RED);
        }
        font.draw(batch, stormLabel + ": " + Math.max(0, secondsLeft) + "s", 20, 530);

        // Requisito 24: HUD do drone
        font.setColor(Color.PINK);
        String droneStatus = !droneItemCollected ? "nao encontrado"
            : (droneActive ? "ATIVO [C] dispensar" : "guardado [C] chamar");
        font.draw(batch, "Drone: " + droneStatus, 20, 505);

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

    /**
     * Requisito 23: ciclo completo da tempestade — CALMO -> ALERTA (3s) ->
     * TEMPESTADE (8s) -> CALMO -> ... Enquanto em TEMPESTADE, se o jogador
     * nao estiver dentro do abrigo, ele perde O2/energia extra. Dentro do
     * abrigo, o efeito para completamente.
     */
    private void updateStorm(float delta) {
        stormTimer += delta;

        switch (stormState) {
            case CALMO:
                if (stormTimer >= CALM_DURATION) {
                    stormState = StormState.ALERTA;
                    stormTimer = 0f;
                }
                break;
            case ALERTA:
                if (stormTimer >= ALERT_DURATION) {
                    stormState = StormState.TEMPESTADE;
                    stormTimer = 0f;
                }
                break;
            case TEMPESTADE:
                if (stormTimer >= STORM_DURATION) {
                    stormState = StormState.CALMO;
                    stormTimer = 0f;
                }
                break;
        }

        if (stormState == StormState.TEMPESTADE && !player.overlaps(abrigo)) {
            o2 -= 12f * delta;
            energy -= 10f * delta;
        }
    }

    /**
     * Requisito 24: drone companheiro. Segue o jogador com um leve atraso
     * (lerp), o que cria uma curva ao virar em L, em vez de ficar colado.
     * Se estiver perto de um inimigo, ataca a cada 2s; senao, so ilumina
     * a area ao redor (util na escuridao de Tita).
     */
    private void updateDrone(float delta) {
        if (!droneActive) return;

        Vector2 targetPos = new Vector2(
            player.x - lastMoveDir.x * 70f,
            player.y - lastMoveDir.y * 70f
        );
        dronePos.x += (targetPos.x - dronePos.x) * Math.min(1f, DRONE_FOLLOW_LERP * delta);
        dronePos.y += (targetPos.y - dronePos.y) * Math.min(1f, DRONE_FOLLOW_LERP * delta);

        if (droneAttackCooldown > 0) droneAttackCooldown -= delta;

        Enemy nearest = null;
        float nearestDist = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            float dist = dronePos.dst(e.rect.x, e.rect.y);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = e;
            }
        }

        if (nearest != null && nearestDist < DRONE_LIGHT_RADIUS && droneAttackCooldown <= 0) {
            nearest.health--;
            droneAttackCooldown = DRONE_ATTACK_COOLDOWN;
            if (nearest.health <= 0) {
                if (nearest.type == EnemyType.BOSS) {
                    titaMissoesOk = true;
                    QuestTracker.getInstance().setTitanMissionComplete(true);
                    QuestTracker.getInstance().addItem("CHAVE_TITA");
                }
                enemies.removeValue(nearest, true);
            }
        }
        // Se nao ha inimigo por perto, o drone apenas ilumina (ver renderizacao do circulo de luz)
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
                }
            }
            return;
        }

        updateStorm(delta);
        updateDrone(delta);

        if (!droneItemCollected && player.overlaps(droneItemPickup)) {
            droneItemCollected = true;
            QuestTracker.getInstance().addItem("DRONE");
        }

        if (droneItemCollected && Gdx.input.isKeyJustPressed(Input.Keys.C)) {
            droneActive = !droneActive;
            if (droneActive) {
                // Reaparece perto do jogador ao ser chamado
                dronePos.set(player.x - 60, player.y - 60);
            }
        }

        if (player.overlaps(portalCalisto) && titaMissoesOk && QuestTracker.getInstance().hasItem("CHAVE_TITA")) {
            o2 = 100f; energy = 100f;
            QuestTracker.getInstance().saveCheckpoint(QuestTracker.STAGE_CALISTO, o2, energy, ammo);
            game.setScreen(new CallistoScreen(game));
            return;
        }

        if (attackCooldown > 0) attackCooldown -= delta;

        float moveX = 0, moveY = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) moveX -= playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) moveX += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) moveY += playerSpeed * delta;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) moveY -= playerSpeed * delta;

        if (moveX != 0 || moveY != 0) {
            lastMoveDir.set(moveX, moveY).nor();
        }

        player.x += moveX; player.y += moveY;
        player.x = MathUtils.clamp(player.x, 0, WORLD_WIDTH - player.width);
        player.y = MathUtils.clamp(player.y, 0, WORLD_HEIGHT - player.height);

        camera.position.set(
            MathUtils.clamp(player.x, camera.viewportWidth / 2f, WORLD_WIDTH - camera.viewportWidth / 2f),
            MathUtils.clamp(player.y, camera.viewportHeight / 2f, WORLD_HEIGHT - camera.viewportHeight / 2f), 0
        );

        o2 -= 1.2f * delta;
        energy -= 1.0f * delta;

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
                o2 -= 20f * delta;
                energy -= 20f * delta;
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
                            titaMissoesOk = true;
                            QuestTracker.getInstance().setTitanMissionComplete(true);
                            QuestTracker.getInstance().addItem("CHAVE_TITA");
                        }
                        enemies.removeIndex(j);
                    }
                    break;
                }
            }
        }

        for (Part p : parts) {
            if (!p.collected && player.overlaps(p.rect)) {
                p.collected = true;
                partsCollected++;
                saveGame();
                // Requisito 17: assim que a ultima peca e coletada, o
                // Boss de Tita e criado e o jogador e avisado.
                checkAndSpawnTitanBoss(true);
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
        if (partTex != null) partTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (ammoTex != null) ammoTex.dispose();
    }
}
