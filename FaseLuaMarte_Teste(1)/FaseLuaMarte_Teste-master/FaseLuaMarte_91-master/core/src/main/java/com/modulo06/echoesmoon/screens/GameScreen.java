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

public class GameScreen implements Screen {

    private Game game;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Texture playerTex, enemyTex, bossTex, fragmentTex, bgTex, benchTex, guideTex, ammoTex;

    private final float WORLD_WIDTH = 2000f;
    private final float WORLD_HEIGHT = 2000f;

    private Rectangle player;
    private Rectangle workbench;
    private Rectangle guide;
    private Rectangle portalMars;
    private float playerSpeed = 300f;

    // Requisito 22: Bancada de crafting (estacao separada da bancada de montar a arma)
    private Rectangle craftBench;
    private boolean craftMenuOpen = false;
    private String craftMessage = "";
    private float craftMessageTimer = 0f;

    class Recipe {
        String matA; int qtyA;
        String matB; int qtyB;
        String output; int outputQty;
        Recipe(String matA, int qtyA, String matB, int qtyB, String output, int outputQty) {
            this.matA = matA; this.qtyA = qtyA;
            this.matB = matB; this.qtyB = qtyB;
            this.output = output; this.outputQty = outputQty;
        }
        boolean canCraft(QuestTracker qt) {
            return qt.hasMaterial(matA, qtyA) && qt.hasMaterial(matB, qtyB);
        }
        void craft(QuestTracker qt) {
            if (!canCraft(qt)) return;
            qt.consumeMaterial(matA, qtyA);
            qt.consumeMaterial(matB, qtyB);
            qt.addMaterial(output, outputQty);
        }
        String describe() {
            return matA + " x" + qtyA + " + " + matB + " x" + qtyB + " -> " + output + " x" + outputQty;
        }
    }

    private Array<Recipe> recipes;

    class MaterialPickup {
        Rectangle rect;
        String material;
        int amount;
        boolean collected = false;
        public MaterialPickup(float x, float y, String material, int amount) {
            this.rect = new Rectangle(x, y, 36, 36);
            this.material = material;
            this.amount = amount;
        }
    }

    private Array<MaterialPickup> materialPickups;

    private float o2 = 100f;
    private float energy = 100f;
    private int fragmentsCollected = 0;
    private final int TOTAL_FRAGMENTS = 4;
    private boolean weaponBuilt = false;
    private boolean moonMissoesOk = false;

    private int ammo = 20;
    private int maxAmmo = 30;
    private float attackCooldown = 0f;
    private final float COOLDOWN_TIME = 0.4f;
    private final float BULLET_RANGE = 500f;

    public enum DialogState { CLOSED, OPEN, FINISHED }
    private DialogState dialogState = DialogState.CLOSED;
    private String[] dialogLines = {
        "GUIA: Bem-vindo a Base da Lua, soldado!",
        "GUIA: Colete os 4 fragmentos e traga-os a bancada para montar sua arma.",
        "GUIA: Apos montar a arma, o Boss da Lua aparecera para proteger a saida!"
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

    class Fragment {
        Rectangle rect;
        boolean collected = false;
        public Fragment(float x, float y) {
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

    private Array<Fragment> fragments;
    private Array<Enemy> enemies;
    private Array<Bullet> bullets;
    private Array<AmmoPickup> ammoPickups;
    private Enemy moonBoss = null;

    public GameScreen(Game game, int startingFragments) {
        this.game = game;
        this.fragmentsCollected = startingFragments;

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 600);
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();

        player = new Rectangle(200, 200, 64, 96);
        guide = new Rectangle(120, 200, 64, 96);
        workbench = new Rectangle(300, 200, 80, 80);
        craftBench = new Rectangle(500, 260, 80, 80);
        portalMars = new Rectangle(1800, 1800, 120, 120);

        fragments = new Array<>();
        enemies = new Array<>();
        bullets = new Array<>();
        ammoPickups = new Array<>();

        // Requisito 22: receitas da bancada de crafting e materiais coletaveis
        recipes = new Array<>();
        recipes.add(new Recipe("GELO", 1, "PECA", 1, "FILTRO_O2", 1));
        recipes.add(new Recipe("METAL", 1, "CIRCUITO", 1, "MUNICAO_X3", 1));

        materialPickups = new Array<>();
        materialPickups.add(new MaterialPickup(560, 500, "GELO", 1));
        materialPickups.add(new MaterialPickup(650, 500, "PECA", 1));
        materialPickups.add(new MaterialPickup(1300, 700, "METAL", 1));
        materialPickups.add(new MaterialPickup(1380, 700, "CIRCUITO", 1));

        fragments.add(new Fragment(500, 500));
        fragments.add(new Fragment(1500, 400));
        fragments.add(new Fragment(800, 1600));
        fragments.add(new Fragment(1600, 1500));

        ammoPickups.add(new AmmoPickup(400, 300, 10));
        ammoPickups.add(new AmmoPickup(1200, 1200, 10));

        enemies.add(new Enemy(600, 600, EnemyType.PATROL, new Vector2(1000, 600), 2, 100f, 64, 64));
        enemies.add(new Enemy(1400, 800, EnemyType.CHASER, null, 2, 110f, 64, 64));

        loadGame();

        playerTex = safeLoadTexture("player.png");
        enemyTex = safeLoadTexture("alien.png");
        bossTex = safeLoadTexture("chefao_lua.png");
        if (bossTex == null) bossTex = enemyTex;
        fragmentTex = safeLoadTexture("fragment.png");
        bgTex = safeLoadTexture("fundo_lua.png");
        benchTex = safeLoadTexture("bancada.png");
        guideTex = safeLoadTexture("guia.png");
        ammoTex = safeLoadTexture("ammo.png");

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
        this.fragmentsCollected = qt.getMoonFragments();
        this.weaponBuilt = qt.isWeaponBuilt();
        this.moonMissoesOk = qt.isMoonMissionComplete();
    }

    private void saveGame() {
        QuestTracker qt = QuestTracker.getInstance();
        qt.setO2(o2);
        qt.setEnergy(energy);
        qt.setAmmo(ammo);
        qt.setMoonFragments(fragmentsCollected);
        qt.setWeaponBuilt(weaponBuilt);
        qt.setMoonMissionComplete(moonMissoesOk);
        qt.setCurrentStage(QuestTracker.STAGE_LUA);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        if (bgTex != null) batch.draw(bgTex, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (moonMissoesOk) {
            shapeRenderer.setColor(Color.MAGENTA);
        } else {
            shapeRenderer.setColor(Color.DARK_GRAY);
        }
        shapeRenderer.rect(portalMars.x, portalMars.y, portalMars.width, portalMars.height);

        shapeRenderer.setColor(Color.BLUE);
        shapeRenderer.rect(workbench.x, workbench.y, workbench.width, workbench.height);

        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.rect(craftBench.x, craftBench.y, craftBench.width, craftBench.height);

        shapeRenderer.setColor(Color.LIGHT_GRAY);
        for (MaterialPickup m : materialPickups) {
            if (!m.collected) shapeRenderer.rect(m.rect.x, m.rect.y, m.rect.width, m.rect.height);
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        font.setColor(Color.WHITE);

        if (guideTex != null) batch.draw(guideTex, guide.x, guide.y, guide.width, guide.height);
        font.draw(batch, "GUIA [E]", guide.x, guide.y + guide.height + 15);

        if (benchTex != null) batch.draw(benchTex, workbench.x, workbench.y, workbench.width, workbench.height);
        font.draw(batch, weaponBuilt ? "BANCADA (Arma Pronta) [R] Recarregar" : "BANCADA [E] (Montar Arma) [R] Recarregar", workbench.x - 30, workbench.y + workbench.height + 15);

        font.setColor(Color.ORANGE);
        font.draw(batch, "BANCADA DE CRAFTING [E] / [R] Recarregar", craftBench.x - 30, craftBench.y + craftBench.height + 15);

        for (MaterialPickup m : materialPickups) {
            if (!m.collected) {
                font.setColor(Color.WHITE);
                font.draw(batch, m.material, m.rect.x, m.rect.y + m.rect.height + 12);
            }
        }

        if (moonMissoesOk) {
            font.draw(batch, "PORTAL PARA MARTE [ATIVO]", portalMars.x - 20, portalMars.y + portalMars.height + 15);
        } else {
            font.draw(batch, "PORTAL BLOQUEADO (Derrote o Boss)", portalMars.x - 30, portalMars.y + portalMars.height + 15);
        }

        for (AmmoPickup a : ammoPickups) {
            if (!a.collected && ammoTex != null) batch.draw(ammoTex, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
        }

        if (playerTex != null) batch.draw(playerTex, player.x, player.y, player.width, player.height);

        for (Enemy e : enemies) {
            if (e.type == EnemyType.BOSS && bossTex != null) {
                batch.draw(bossTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
                font.draw(batch, "BOSS DA LUA (HP: " + e.health + ")", e.rect.x - 10, e.rect.y + e.rect.height + 20);
            } else if (enemyTex != null) {
                batch.draw(enemyTex, e.rect.x, e.rect.y, e.rect.width, e.rect.height);
            }
        }

        for (Fragment f : fragments) {
            if (!f.collected && fragmentTex != null) batch.draw(fragmentTex, f.rect.x, f.rect.y, f.rect.width, f.rect.height);
        }

        for (Bullet b : bullets) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(b.rect.x, b.rect.y, b.rect.width, b.rect.height);
            shapeRenderer.end();
        }

        batch.end();
        renderHUD();

        if (dialogState == DialogState.OPEN) {
            renderDialogBox();
        }

        if (craftMenuOpen) {
            renderCraftMenu();
        }
    }

    private void renderCraftMenu() {
        QuestTracker qt = QuestTracker.getInstance();
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, 800, 600);

        shapeRenderer.setProjectionMatrix(hudMatrix);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.05f, 0.05f, 0.1f, 0.92f);
        shapeRenderer.rect(120, 120, 560, 380);
        shapeRenderer.end();

        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.CYAN);
        font.draw(batch, "BANCADA DE CRAFTING", 300, 470);

        font.setColor(Color.LIGHT_GRAY);
        font.draw(batch, "Materiais: GELO " + qt.getMaterialCount("GELO")
            + " | PECA " + qt.getMaterialCount("PECA")
            + " | METAL " + qt.getMaterialCount("METAL")
            + " | CIRCUITO " + qt.getMaterialCount("CIRCUITO"), 150, 430);

        for (int i = 0; i < recipes.size; i++) {
            Recipe r = recipes.get(i);
            boolean can = r.canCraft(qt);
            font.setColor(can ? Color.GREEN : Color.RED);
            font.draw(batch, "[" + (i + 1) + "] " + r.describe(), 150, 380 - i * 40);
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, "Pressione [1]/[2] para craftar. [E] ou [ESC] para fechar.", 150, 250);

        if (!craftMessage.isEmpty()) {
            font.setColor(Color.WHITE);
            font.draw(batch, craftMessage, 150, 200);
        }
        batch.end();
    }

    private void renderHUD() {
        Matrix4 hudMatrix = new Matrix4().setToOrtho2D(0, 0, 800, 600);
        batch.setProjectionMatrix(hudMatrix);
        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "FASE: LUA | O2: " + (int)o2 + "% | Energia: " + (int)energy + "% | Municao: " + ammo + "/" + maxAmmo, 20, 580);
        font.draw(batch, "Fragmentos: " + fragmentsCollected + "/" + TOTAL_FRAGMENTS + " | Chave Lua: " + (QuestTracker.getInstance().hasItem("CHAVE_LUA") ? "SIM" : "NAO"), 20, 555);
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

        if (craftMessageTimer > 0) {
            craftMessageTimer -= delta;
            if (craftMessageTimer <= 0) craftMessage = "";
        }

        if (craftMenuOpen) {
            QuestTracker qt = QuestTracker.getInstance();
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) && recipes.size > 0) {
                attemptCraft(recipes.get(0), qt);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) && recipes.size > 1) {
                attemptCraft(recipes.get(1), qt);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.E) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                craftMenuOpen = false;
            }
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

        if (player.overlaps(guide) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            dialogLines = new String[]{
                "GUIA: Continue coletando os fragmentos.",
                "GUIA: Assim que terminar, monte a arma na bancada."
            };
            dialogIndex = 0;
            dialogState = DialogState.OPEN;
            return;
        }

        if (player.overlaps(craftBench) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            craftMenuOpen = true;
            return;
        }

        if ((player.overlaps(workbench) || player.overlaps(craftBench)) && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            o2 = 100f;
            energy = 100f;
            saveGame();
        }

        if (player.overlaps(workbench) && Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            if (fragmentsCollected >= TOTAL_FRAGMENTS && !weaponBuilt) {
                weaponBuilt = true;
                QuestTracker.getInstance().setWeaponBuilt(true);

                if (moonBoss == null && !moonMissoesOk) {
                    moonBoss = new Enemy(1500, 1500, EnemyType.BOSS, null, 20, 100f, 128, 128); // HP Reduzido para 20
                    enemies.add(moonBoss);
                }

                dialogLines = new String[]{
                    "GUIA: Arma construida com sucesso!",
                    "GUIA: Cuidado! O Boss da Lua apareceu no setor leste!"
                };
                dialogIndex = 0;
                dialogState = DialogState.OPEN;
                saveGame();
            }
        }

        if (player.overlaps(portalMars) && moonMissoesOk) {
            o2 = 100f; energy = 100f;
            QuestTracker.getInstance().saveCheckpoint(QuestTracker.STAGE_MARTE, o2, energy, ammo);
            game.setScreen(new MarsScreen(game));
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

        o2 -= 1.0f * delta;
        energy -= 0.8f * delta;

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
                o2 -= 15f * delta;
                energy -= 15f * delta;
            }
        }

        if (weaponBuilt && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && attackCooldown <= 0 && ammo > 0) {
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
                            moonMissoesOk = true;
                            QuestTracker.getInstance().setMoonMissionComplete(true);
                            QuestTracker.getInstance().addItem("CHAVE_LUA");
                        }
                        enemies.removeIndex(j);
                    }
                    break;
                }
            }
        }

        for (Fragment f : fragments) {
            if (!f.collected && player.overlaps(f.rect)) {
                f.collected = true;
                fragmentsCollected++;
                saveGame();
            }
        }

        for (MaterialPickup m : materialPickups) {
            if (!m.collected && player.overlaps(m.rect)) {
                m.collected = true;
                QuestTracker.getInstance().addMaterial(m.material, m.amount);
            }
        }
    }

    /**
     * Requisito 22: tenta craftar a receita informada.
     * Se faltar material, NAO consome nada e mostra uma mensagem
     * ("Faltam materiais"). Se der certo, consome os 2 materiais (I antes)
     * e adiciona 1 item novo (I depois: materiais -1 e item novo +1).
     */
    private void attemptCraft(Recipe r, QuestTracker qt) {
        if (r.canCraft(qt)) {
            r.craft(qt);
            craftMessage = "Craftado: " + r.output + " x" + r.outputQty + "!";
        } else {
            craftMessage = "Faltam materiais para essa receita.";
        }
        craftMessageTimer = 2.5f;
    }

    @Override public void show() {}
    @Override public void resize(int w, int h) {}
    @Override public void pause() { saveGame(); }
    @Override public void resume() {}
    @Override public void hide() {}
    @Override
    public void dispose() {
        batch.dispose(); shapeRenderer.dispose(); font.dispose();
        if (playerTex != null) playerTex.dispose();
        if (enemyTex != null) enemyTex.dispose();
        if (bossTex != null) bossTex.dispose();
        if (fragmentTex != null) fragmentTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (benchTex != null) benchTex.dispose();
        if (guideTex != null) guideTex.dispose();
        if (ammoTex != null) ammoTex.dispose();
    }
}
