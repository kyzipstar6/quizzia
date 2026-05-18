import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.*;

public class CompanyMuseumGame extends Application {
    private static final double ROOM_SIZE = 24;
    private static final double PLAYER_Y = -1.65;
    private static final String[] PRIZES = {
            "100 EUR", "200 EUR", "300 EUR", "500 EUR", "1,000 EUR",
            "2,000 EUR", "4,000 EUR", "8,000 EUR", "16,000 EUR", "32,000 EUR",
            "64,000 EUR", "125,000 EUR", "500,000 EUR", "750,000 EUR", "1,000,000 EUR"
    };

    private final List<Company> companies = List.of(
            new Company("Ubisoft", "Ubisoft is a leading video game company known for popular franchises such as Assassin's Creed, Far Cry, and Rainbow Six. The company focuses on creating immersive gaming experiences across various platforms.", "https://www.ubisoft.com/", "5.5 billion USD (2023)", "18000", "Montreuil, France", "1.2 billion USD (2023)", "45.00 USD (2023)"),
            new Company("Electronic Arts (EA)", "Electronic Arts is a major video game publisher known for titles like FIFA, Madden NFL, and The Sims. EA focuses on delivering engaging gaming experiences across multiple platforms, including consoles, PC, and mobile.", "https://www.ea.com/", "6.0 billion USD (2023)", "10000", "Redwood City, California, USA", "1.5 billion USD (2023)", "120.00 USD (2023)"),
            new Company("Activision Blizzard", "Activision Blizzard is a leading video game company known for popular franchises such as Call of Duty, World of Warcraft, and Overwatch. The company focuses on creating engaging gaming experiences across various platforms.", "https://www.activisionblizzard.com/", "7.0 billion USD (2023)", "9000", "Santa Monica, California, USA", "1.8 billion USD (2023)", "85.00 USD (2023)"),
            new Company("Rockstar Games", "Rockstar Games is a renowned video game company known for popular franchises such as Grand Theft Auto, Red Dead Redemption, and Max Payne. The company focuses on creating immersive gaming experiences across various platforms.", "https://www.rockstargames.com/", "4.0 billion USD (2023)", "2000", "New York City, New York, USA", "900 million USD (2023)", "70.00 USD (2023)"),
            new Company("Nintendo", "Nintendo is a renowned video game company known for iconic franchises such as Mario, The Legend of Zelda, and Pokemon. The company focuses on creating innovative gaming experiences across its own platforms, including the Nintendo Switch.", "https://www.nintendo.com/", "12.0 billion USD (2023)", "6000", "Kyoto, Japan", "2.5 billion USD (2023)", "75.00 USD (2023)"),
            new Company("Sony Interactive Entertainment", "Sony Interactive Entertainment is a division of Sony Corporation responsible for the PlayStation brand. The company focuses on creating immersive gaming experiences through its consoles and exclusive game titles.", "https://www.playstation.com/", "15.0 billion USD (2023)", "8000", "San Mateo, California, USA", "3.0 billion USD (2023)", "100.00 USD (2023)"),
            new Company("Microsoft Xbox", "Microsoft Xbox is a division of Microsoft Corporation responsible for the Xbox gaming brand. The company focuses on delivering high-quality gaming experiences through its consoles, services, and exclusive game titles.", "https://www.xbox.com/", "20.0 billion USD (2023)", "12000", "Redmond, Washington, USA", "4.0 billion USD (2023)", "150.00 USD (2023)"),
            new Company("Take-Two Interactive", "Take-Two Interactive is a leading video game company known for popular franchises such as Grand Theft Auto, Red Dead Redemption, and NBA 2K. The company focuses on creating immersive gaming experiences across various platforms.", "https://www.take2games.com/", "3.5 billion USD (2023)", "5000", "New York City, New York, USA", "800 million USD (2023)", "75.00 USD (2023)"),
            new Company("Square Enix", "Square Enix is a renowned video game company known for popular franchises such as Final Fantasy, Dragon Quest, and Kingdom Hearts. The company focuses on creating immersive gaming experiences across various platforms.", "https://www.square-enix.com/", "2.0 billion USD (2023)", "4000", "Tokyo, Japan", "500 million USD (2023)", "50.00 USD (2023)")
    );

    private final Group world = new Group();
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate yawRotate = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitchRotate = new Rotate(0, Rotate.X_AXIS);
    private final Set<KeyCode> pressed = EnumSet.noneOf(KeyCode.class);
    private final List<Painting> paintings = new ArrayList<>();
    private final Set<Integer> delivered = new HashSet<>();
    private final Random random = new Random();

    private Stage stage;
    private BorderPane root;
    private Label pickedLabel;
    private Label deliveredLabel;
    private Label quizLabel;
    private Label promptLabel;
    private VBox companyCard;
    private StackPane quizOverlay;
    private Label questionLabel;
    private Label quizMessage;
    private VBox prizeList;
    private GridPane answersGrid;
    private Button fullscreenButton;
    private Button musicButton;
    private Button nextTrackButton;

    private Painting nearestPainting;
    private Painting carryingPainting;
    private boolean nearTable;
    private double playerX = 0;
    private double playerZ = 8;
    private double yaw = 0;
    private double pitch = 0;
    private double lastMouseX;
    private double lastMouseY;
    private boolean draggingMouse;

    private List<Question> quizQuestions = new ArrayList<>();
    private int quizIndex = 0;
    private boolean quizLocked = false;
    private boolean quizFinished = false;

    private MusicEngine musicEngine;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        root = new BorderPane();

        buildWorld();
        SubScene subScene = new SubScene(world, 1280, 760, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.rgb(21, 24, 33));
        subScene.setCamera(camera);
        subScene.widthProperty().bind(root.widthProperty());
        subScene.heightProperty().bind(root.heightProperty());

        StackPane stack = new StackPane(subScene, buildHud(), buildCompanyCard(), buildControls(), buildQuizOverlay());
        root.setCenter(stack);

        Scene scene = new Scene(root, 1280, 760, true);
        bindInput(scene, subScene);

        primaryStage.setTitle("Company Museum Game - JavaFX");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.show();

        updateCamera();
        updateHud();
        startLoop();
    }

    @Override
    public void stop() {
        if (musicEngine != null) {
            musicEngine.stop();
        }
    }

    private void buildWorld() {
        camera.setNearClip(0.05);
        camera.setFarClip(80);
        camera.getTransforms().addAll(yawRotate, pitchRotate);

        AmbientLight ambient = new AmbientLight(Color.rgb(130, 126, 116));
        PointLight mainLight = new PointLight(Color.rgb(255, 235, 190));
        mainLight.setTranslateX(2);
        mainLight.setTranslateY(-6);
        mainLight.setTranslateZ(-3);
        world.getChildren().addAll(ambient, mainLight);

        addRoom();
        addTable();
        addPaintings();
    }

    private void addRoom() {
        PhongMaterial floorMaterial = material(Color.rgb(107, 102, 91));
        Box floor = box(ROOM_SIZE, 0.18, ROOM_SIZE, 0, 0.08, 0, floorMaterial);
        world.getChildren().add(floor);

        PhongMaterial ceilingMaterial = material(Color.rgb(41, 43, 50));
        Box ceiling = box(ROOM_SIZE, 0.18, ROOM_SIZE, 0, -4.4, 0, ceilingMaterial);
        world.getChildren().add(ceiling);

        PhongMaterial wallMaterial = material(Color.rgb(214, 207, 189));
        world.getChildren().addAll(
                box(ROOM_SIZE, 4.4, 0.24, 0, -2.1, -12, wallMaterial),
                box(ROOM_SIZE, 4.4, 0.24, 0, -2.1, 12, wallMaterial),
                box(0.24, 4.4, ROOM_SIZE, -12, -2.1, 0, wallMaterial),
                box(0.24, 4.4, ROOM_SIZE, 12, -2.1, 0, wallMaterial)
        );

        PhongMaterial stripMaterial = material(Color.rgb(92, 72, 48));
        world.getChildren().addAll(
                box(ROOM_SIZE, 0.18, 0.12, 0, -0.08, -11.82, stripMaterial),
                box(ROOM_SIZE, 0.18, 0.12, 0, -0.08, 11.82, stripMaterial),
                box(0.12, 0.18, ROOM_SIZE, -11.82, -0.08, 0, stripMaterial),
                box(0.12, 0.18, ROOM_SIZE, 11.82, -0.08, 0, stripMaterial)
        );
    }

    private void addTable() {
        PhongMaterial tableMaterial = material(Color.rgb(118, 82, 51));
        world.getChildren().add(box(5.4, 0.28, 2.9, 0, -0.94, 0, tableMaterial));

        PhongMaterial legMaterial = material(Color.rgb(74, 51, 34));
        double[][] legs = {{-2.35, -1.1}, {2.35, -1.1}, {-2.35, 1.1}, {2.35, 1.1}};
        for (double[] leg : legs) {
            world.getChildren().add(box(0.25, 0.9, 0.25, leg[0], -0.45, leg[1], legMaterial));
        }

        Box marker = box(2.4, 0.08, 0.42, 0, -1.12, 1.46, material(Color.rgb(27, 24, 20)));
        world.getChildren().add(marker);
    }

    private void addPaintings() {
        double[][] positions = {
                {-7.8, -2.15, -11.82, 0},
                {0, -2.15, -11.82, 0},
                {7.8, -2.15, -11.82, 0},
                {-11.82, -2.15, -6.8, 90},
                {-11.82, -2.15, 0, 90},
                {-11.82, -2.15, 6.8, 90},
                {11.82, -2.15, -6.8, -90},
                {11.82, -2.15, 0, -90},
                {11.82, -2.15, 6.8, -90}
        };

        for (int i = 0; i < companies.size(); i++) {
            Company company = companies.get(i);
            double[] p = positions[i % positions.length];
            Group group = new Group();
            group.setTranslateX(p[0]);
            group.setTranslateY(p[1]);
            group.setTranslateZ(p[2]);
            group.getTransforms().add(new Rotate(p[3], Rotate.Y_AXIS));

            Box frame = new Box(2.42, 1.68, 0.16);
            frame.setMaterial(material(Color.rgb(124, 90, 45)));
            Box art = new Box(2.08, 1.34, 0.08);
            art.setTranslateZ(-0.08);
            PhongMaterial artMaterial = new PhongMaterial();
            artMaterial.setDiffuseMap(makePaintingImage(company, i));
            art.setMaterial(artMaterial);

            Box plaque = new Box(1.8, 0.22, 0.06);
            plaque.setTranslateY(1.08);
            plaque.setTranslateZ(-0.09);
            plaque.setMaterial(material(Color.rgb(32, 29, 23)));

            group.getChildren().addAll(frame, art, plaque);
            world.getChildren().add(group);
            paintings.add(new Painting(i, company, group));
        }
    }

    private Pane buildHud() {
        VBox hud = new VBox(10);
        hud.setMaxWidth(390);
        hud.setPadding(new Insets(14));
        hud.setAlignment(Pos.TOP_LEFT);
        hud.setStyle(panelStyle());
        hud.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.35)));

        Label title = label("Company Museum Heist", 17, true, Color.rgb(247, 243, 232));
        Label objective = label("Walk through the museum, inspect every picture, pick it up, and place it on the table.", 13, false, Color.rgb(199, 193, 175));
        objective.setWrapText(true);

        GridPane meters = new GridPane();
        meters.setHgap(8);
        meters.add(meter("Pictures", pickedLabel = meterValue()), 0, 0);
        meters.add(meter("On table", deliveredLabel = meterValue()), 1, 0);
        meters.add(meter("Quiz", quizLabel = meterValue()), 2, 0);

        promptLabel = label("Loading museum...", 13, true, Color.rgb(247, 243, 232));
        promptLabel.setWrapText(true);
        promptLabel.setMinHeight(40);

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        fullscreenButton = smallButton("Fullscreen");
        musicButton = smallButton("Music on");
        nextTrackButton = smallButton("Next track");
        fullscreenButton.setOnAction(e -> toggleFullscreen());
        musicButton.setOnAction(e -> toggleMusic());
        nextTrackButton.setOnAction(e -> nextTrack());
        actions.getChildren().addAll(fullscreenButton, musicButton, nextTrackButton);

        hud.getChildren().addAll(title, objective, meters, promptLabel, actions);
        StackPane wrapper = new StackPane(hud);
        wrapper.setPadding(new Insets(16));
        wrapper.setAlignment(Pos.TOP_LEFT);
        wrapper.setMouseTransparent(false);
        return wrapper;
    }

    private Pane buildCompanyCard() {
        companyCard = new VBox(8);
        companyCard.setMaxWidth(390);
        companyCard.setPadding(new Insets(16));
        companyCard.setStyle(panelStyle());
        companyCard.setVisible(false);
        companyCard.setManaged(false);
        companyCard.setEffect(new DropShadow(24, Color.rgb(0, 0, 0, 0.35)));

        StackPane wrapper = new StackPane(companyCard);
        wrapper.setPadding(new Insets(16));
        wrapper.setAlignment(Pos.TOP_RIGHT);
        wrapper.setMouseTransparent(true);
        return wrapper;
    }

    private Pane buildControls() {
        HBox controls = new HBox(8);
        controls.setPadding(new Insets(16));
        controls.setAlignment(Pos.BOTTOM_LEFT);
        controls.getChildren().addAll(controlKey("WASD"), controlKey("Mouse"), controlKey("E"), controlKey("Q"));

        StackPane wrapper = new StackPane(controls);
        wrapper.setAlignment(Pos.BOTTOM_LEFT);
        wrapper.setMouseTransparent(true);
        return wrapper;
    }

    private StackPane buildQuizOverlay() {
        quizOverlay = new StackPane();
        quizOverlay.setVisible(false);
        quizOverlay.setStyle("-fx-background-color: radial-gradient(center 50% 0%, radius 90%, rgba(35,47,122,0.96), rgba(3,6,22,0.98));");
        quizOverlay.setPadding(new Insets(20));

        HBox shell = new HBox(16);
        shell.setMaxWidth(1120);
        shell.setAlignment(Pos.CENTER);

        VBox main = new VBox(14);
        main.setPadding(new Insets(18));
        main.setMinWidth(720);
        main.setStyle("-fx-background-color: rgba(8,13,45,0.92); -fx-border-color: #f0bd3f; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;");

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Label title = label("Who Wants to Be a Company Millionaire?", 19, true, Color.rgb(247, 243, 232));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button returnButton = quizButton("Return to museum");
        returnButton.setOnAction(e -> quizOverlay.setVisible(false));
        top.getChildren().addAll(title, spacer, returnButton);

        questionLabel = label("Question", 20, true, Color.WHITE);
        questionLabel.setWrapText(true);
        questionLabel.setAlignment(Pos.CENTER);
        questionLabel.setMinHeight(122);
        questionLabel.setMaxWidth(Double.MAX_VALUE);
        questionLabel.setStyle("-fx-background-color: linear-gradient(#111a46, #17266b); -fx-border-color: #f0bd3f; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8; -fx-padding: 20;");

        answersGrid = new GridPane();
        answersGrid.setHgap(12);
        answersGrid.setVgap(12);

        quizMessage = label("", 14, true, Color.WHITE);
        quizMessage.setMinHeight(28);

        main.getChildren().addAll(top, questionLabel, answersGrid, quizMessage);

        VBox ladder = new VBox(8);
        ladder.setMinWidth(260);
        ladder.setPadding(new Insets(12));
        ladder.setStyle("-fx-background-color: rgba(8,13,45,0.92); -fx-border-color: #f0bd3f; -fx-border-width: 2; -fx-background-radius: 8; -fx-border-radius: 8;");
        Label ladderTitle = label("Prize ladder", 16, true, Color.WHITE);
        ladderTitle.setAlignment(Pos.CENTER);
        ladderTitle.setMaxWidth(Double.MAX_VALUE);
        prizeList = new VBox(6);
        ladder.getChildren().addAll(ladderTitle, prizeList);

        shell.getChildren().addAll(main, ladder);
        quizOverlay.getChildren().add(shell);
        return quizOverlay;
    }

    private void bindInput(Scene scene, SubScene subScene) {
        scene.setOnKeyPressed(e -> {
            if (!isTypingKey(e.getCode())) {
                pressed.add(e.getCode());
            }
            if ((e.getCode() == KeyCode.W || e.getCode() == KeyCode.A || e.getCode() == KeyCode.S || e.getCode() == KeyCode.D || e.getCode() == KeyCode.E) && (musicEngine == null || !musicEngine.isEnabled())) {
                startRandomMusic();
            }
            if (e.getCode() == KeyCode.E) {
                interact();
            }
            if (e.getCode() == KeyCode.Q && carryingPainting != null) {
                carryingPainting = null;
            }
            if (e.getCode() == KeyCode.F11) {
                toggleFullscreen();
            }
        });
        scene.setOnKeyReleased(e -> pressed.remove(e.getCode()));

        subScene.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            draggingMouse = true;
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
            subScene.requestFocus();
            if (musicEngine == null || !musicEngine.isEnabled()) {
                startRandomMusic();
            }
        });
        subScene.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> draggingMouse = false);
        subScene.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!draggingMouse) {
                return;
            }
            double dx = e.getSceneX() - lastMouseX;
            double dy = e.getSceneY() - lastMouseY;
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
            yaw += dx * 0.18;
            pitch = clamp(pitch - dy * 0.14, -42, 42);
            updateCamera();
        });
    }

    private void startLoop() {
        AnimationTimer timer = new AnimationTimer() {
            private long last;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    return;
                }
                double delta = Math.min((now - last) / 1_000_000_000.0, 0.05);
                last = now;
                movePlayer(delta);
                updateInteractions();
            }
        };
        timer.start();
    }

    private void movePlayer(double delta) {
        double speed = pressed.contains(KeyCode.SHIFT) ? 6.0 : 3.8;
        double rad = Math.toRadians(yaw);
        double forwardX = Math.sin(rad);
        double forwardZ = -Math.cos(rad);
        double rightX = Math.cos(rad);
        double rightZ = Math.sin(rad);
        double mx = 0;
        double mz = 0;

        if (pressed.contains(KeyCode.W)) {
            mx += forwardX;
            mz += forwardZ;
        }
        if (pressed.contains(KeyCode.S)) {
            mx -= forwardX;
            mz -= forwardZ;
        }
        if (pressed.contains(KeyCode.D)) {
            mx += rightX;
            mz += rightZ;
        }
        if (pressed.contains(KeyCode.A)) {
            mx -= rightX;
            mz -= rightZ;
        }

        double length = Math.sqrt(mx * mx + mz * mz);
        if (length > 0) {
            playerX += (mx / length) * speed * delta;
            playerZ += (mz / length) * speed * delta;
            playerX = clamp(playerX, -10.5, 10.5);
            playerZ = clamp(playerZ, -10.5, 10.5);
            updateCamera();
        }
    }

    private void updateCamera() {
        camera.setTranslateX(playerX);
        camera.setTranslateY(PLAYER_Y);
        camera.setTranslateZ(playerZ);
        yawRotate.setAngle(yaw);
        pitchRotate.setAngle(pitch);
    }

    private void updateInteractions() {
        Painting nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Painting painting : paintings) {
            if (painting.delivered || painting == carryingPainting) {
                continue;
            }
            double distance = distance2D(playerX, playerZ, painting.group.getTranslateX(), painting.group.getTranslateZ());
            if (distance < nearestDistance) {
                nearest = painting;
                nearestDistance = distance;
            }
        }
        nearestPainting = nearestDistance < 2.6 ? nearest : null;
        nearTable = playerX > -2.7 && playerX < 2.7 && playerZ > -1.8 && playerZ < 1.8;

        if (nearestPainting != null) {
            showCompany(nearestPainting.company);
        } else {
            companyCard.setVisible(false);
            companyCard.setManaged(false);
        }

        if (carryingPainting != null) {
            double rad = Math.toRadians(yaw);
            carryingPainting.group.setTranslateX(playerX + Math.sin(rad) * 1.25);
            carryingPainting.group.setTranslateY(PLAYER_Y + 0.2);
            carryingPainting.group.setTranslateZ(playerZ - Math.cos(rad) * 1.25);
            carryingPainting.group.getTransforms().setAll(new Rotate(yaw, Rotate.Y_AXIS), new Rotate(-pitch * 0.25, Rotate.X_AXIS));
        }

        updateHud();
    }

    private void interact() {
        if (quizOverlay.isVisible()) {
            return;
        }
        if (carryingPainting != null && nearTable) {
            deliverCarried();
            return;
        }
        if (carryingPainting == null && nearestPainting != null) {
            carryingPainting = nearestPainting;
            carryingPainting.picked = true;
            return;
        }
        if (carryingPainting == null && delivered.size() == paintings.size()) {
            startQuiz();
        }
    }

    private void deliverCarried() {
        Painting painting = carryingPainting;
        int slot = delivered.size();
        double x = -1.65 + (slot % 3) * 1.65;
        double z = -0.7 + (slot / 3) * 0.7;
        painting.group.setTranslateX(x);
        painting.group.setTranslateY(-1.15 - slot * 0.012);
        painting.group.setTranslateZ(z);
        painting.group.setScaleX(0.55);
        painting.group.setScaleY(0.55);
        painting.group.setScaleZ(0.55);
        painting.group.getTransforms().setAll(new Rotate(90, Rotate.X_AXIS));
        painting.delivered = true;
        delivered.add(painting.index);
        carryingPainting = null;
    }

    private void updateHud() {
        long picked = paintings.stream().filter(p -> p.picked).count();
        pickedLabel.setText(picked + "/" + paintings.size());
        deliveredLabel.setText(delivered.size() + "/" + paintings.size());
        quizLabel.setText(quizFinished ? "Won" : delivered.size() == paintings.size() ? "Ready" : "Locked");

        if (carryingPainting != null && nearTable) {
            promptLabel.setText("Press E to place " + carryingPainting.company.name + " on the table.");
        } else if (carryingPainting != null) {
            promptLabel.setText("Carrying " + carryingPainting.company.name + ". Bring it to the table.");
        } else if (nearestPainting != null) {
            promptLabel.setText("Press E to pick up " + nearestPainting.company.name + ".");
        } else if (delivered.size() == paintings.size()) {
            promptLabel.setText("Press E to start the millionaire quiz.");
        } else {
            promptLabel.setText("Move with WASD, hold mouse button and drag to look, approach a picture to inspect it.");
        }
    }

    private void showCompany(Company company) {
        companyCard.getChildren().clear();
        Label title = label(company.name, 20, true, Color.rgb(240, 189, 63));
        Label description = label(company.description, 13, false, Color.rgb(199, 193, 175));
        description.setWrapText(true);
        GridPane details = new GridPane();
        details.setHgap(10);
        details.setVgap(7);
        addDetail(details, 0, "Website", company.website);
        addDetail(details, 1, "Revenue", company.revenue);
        addDetail(details, 2, "Employees", company.employees);
        addDetail(details, 3, "Headquarters", company.headquarters);
        addDetail(details, 4, "Net income", company.netIncome);
        addDetail(details, 5, "Share price", company.sharePrice);
        companyCard.getChildren().addAll(title, description, details);
        companyCard.setVisible(true);
        companyCard.setManaged(true);
    }

    private void startQuiz() {
        quizQuestions = makeQuizQuestions();
        quizIndex = 0;
        quizLocked = false;
        quizOverlay.setVisible(true);
        showQuestion();
    }

    private List<Question> makeQuizQuestions() {
        List<Question> questions = new ArrayList<>();
        String[] fields = {"headquarters", "revenue", "employees", "netIncome", "sharePrice"};
        String[] labels = {"headquarters", "revenue", "employees", "net income", "share price"};
        for (Company company : companies) {
            for (int i = 0; i < fields.length; i++) {
                String answer = company.field(fields[i]);
                List<String> distractors = new ArrayList<>();
                for (Company other : companies) {
                    String value = other.field(fields[i]);
                    if (!other.equals(company) && !value.equals(answer) && !distractors.contains(value)) {
                        distractors.add(value);
                    }
                }
                if (distractors.size() >= 3) {
                    Collections.shuffle(distractors, random);
                    List<String> options = new ArrayList<>();
                    options.add(answer);
                    options.addAll(distractors.subList(0, 3));
                    Collections.shuffle(options, random);
                    questions.add(new Question("What is the " + labels[i] + " of " + company.name + "?", answer, options));
                }
            }
        }
        Collections.shuffle(questions, random);
        return questions.subList(0, Math.min(15, questions.size()));
    }

    private void showQuestion() {
        drawPrizeList();
        answersGrid.getChildren().clear();
        if (quizIndex >= quizQuestions.size()) {
            quizFinished = true;
            questionLabel.setText("You collected every picture and answered every question correctly.");
            quizMessage.setText("Final prize: 1,000,000 EUR");
            return;
        }

        Question question = quizQuestions.get(quizIndex);
        questionLabel.setText(question.text);
        quizMessage.setText("Question " + (quizIndex + 1) + " of " + quizQuestions.size());
        quizLocked = false;

        for (int i = 0; i < question.options.size(); i++) {
            String option = question.options.get(i);
            Button button = quizButton((char) ('A' + i) + ": " + option);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setMinHeight(50);
            int col = i % 2;
            int row = i / 2;
            answersGrid.add(button, col, row);
            GridPane.setHgrow(button, Priority.ALWAYS);
            button.setOnAction(e -> chooseAnswer(button, option));
        }
    }

    private void chooseAnswer(Button selectedButton, String option) {
        if (quizLocked) {
            return;
        }
        quizLocked = true;
        Question question = quizQuestions.get(quizIndex);
        boolean correct = option.equals(question.correct);

        for (Node node : answersGrid.getChildren()) {
            if (node instanceof Button button) {
                button.setDisable(true);
                if (button.getText().endsWith(question.correct)) {
                    button.setStyle(answerStyle("#31c66c", "#168840"));
                }
            }
        }

        if (!correct) {
            selectedButton.setStyle(answerStyle("#ec6464", "#bd2e2e"));
            String safePrize = quizIndex >= 10 ? PRIZES[9] : quizIndex >= 5 ? PRIZES[4] : "0 EUR";
            quizMessage.setText("Wrong. Correct answer: " + question.correct + ". Safe prize: " + safePrize);
            return;
        }

        quizMessage.setText("Correct. Current prize: " + PRIZES[quizIndex]);
        quizIndex++;
        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(CompanyMuseumGame.this::showQuestion);
            }
        }, 1150);
    }

    private void drawPrizeList() {
        prizeList.getChildren().clear();
        for (int i = PRIZES.length - 1; i >= 0; i--) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7, 8, 7, 8));
            row.setStyle(i == quizIndex
                    ? "-fx-background-color: linear-gradient(#efb633, #c77d12); -fx-background-radius: 6;"
                    : "-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(240,189,63,0.42); -fx-background-radius: 6; -fx-border-radius: 6;");
            Label level = label(String.valueOf(i + 1), 13, i == quizIndex, i == quizIndex ? Color.rgb(17, 17, 17) : Color.WHITE);
            Label prize = label(PRIZES[i], 13, true, i == quizIndex ? Color.rgb(17, 17, 17) : Color.WHITE);
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            row.getChildren().addAll(level, spacer, prize);
            prizeList.getChildren().add(row);
        }
    }

    private void toggleFullscreen() {
        stage.setFullScreen(!stage.isFullScreen());
        fullscreenButton.setText(stage.isFullScreen() ? "Exit fullscreen" : "Fullscreen");
    }

    private void toggleMusic() {
        if (musicEngine != null && musicEngine.isEnabled()) {
            musicEngine.stop();
            musicButton.setText("Music on");
            return;
        }
        startRandomMusic();
    }

    private void nextTrack() {
        if (musicEngine == null) {
            musicEngine = new MusicEngine();
        }
        int next = musicEngine.nextTrackIndex();
        musicEngine.play(next);
        musicButton.setText("Music off: " + musicEngine.trackName());
    }

    private void startRandomMusic() {
        if (musicEngine == null) {
            musicEngine = new MusicEngine();
        }
        musicEngine.play(random.nextInt(MusicEngine.TRACKS.length));
        musicButton.setText("Music off: " + musicEngine.trackName());
    }

    private WritableImage makePaintingImage(Company company, int index) {
        Canvas canvas = new Canvas(1024, 640);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Color[] palette = {
                Color.rgb(32, 58, 95), Color.rgb(107, 45, 53), Color.rgb(49, 81, 58),
                Color.rgb(91, 70, 49), Color.rgb(67, 47, 99), Color.rgb(93, 107, 45)
        };

        gc.setFill(palette[index % palette.length]);
        gc.fillRect(0, 0, 1024, 640);
        gc.setFill(Color.rgb(255, 255, 255, 0.08));
        for (int i = 0; i < 9; i++) {
            gc.fillOval(50 + i * 110, 20 + ((i * 61) % 420), 140, 140);
        }
        gc.setFill(Color.rgb(241, 196, 91));
        gc.fillRect(52, 52, 920, 536);
        gc.setFill(Color.rgb(23, 26, 36));
        gc.fillRect(82, 82, 860, 476);

        gc.setFill(Color.rgb(248, 242, 223));
        gc.setFont(Font.font("Arial", FontWeight.BOLD, 62));
        drawWrappedCentered(gc, company.name, 512, 210, 790, 72);
        gc.setFill(Color.rgb(215, 208, 189));
        gc.setFont(Font.font("Arial", 34));
        drawWrappedCentered(gc, company.headquarters, 512, 410, 760, 44);

        WritableImage image = new WritableImage(1024, 640);
        canvas.snapshot(null, image);
        return image;
    }

    private void drawWrappedCentered(GraphicsContext gc, String text, double x, double y, double maxWidth, double lineHeight) {
        String[] words = text.split(" ");
        String line = "";
        for (String word : words) {
            String test = line.isEmpty() ? word : line + " " + word;
            if (test.length() * 32 > maxWidth && !line.isEmpty()) {
                gc.fillText(line, x - (line.length() * 16), y);
                line = word;
                y += lineHeight;
            } else {
                line = test;
            }
        }
        gc.fillText(line, x - (line.length() * 16), y);
    }

    private Label label(String text, int size, boolean bold, Color color) {
        Label label = new Label(text);
        label.setTextFill(color);
        label.setFont(Font.font("Arial", bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        return label;
    }

    private Label meterValue() {
        return label("0/0", 16, true, Color.WHITE);
    }

    private VBox meter(String title, Label value) {
        VBox box = new VBox(3);
        box.setMinWidth(104);
        box.setPadding(new Insets(8));
        box.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-border-color: rgba(255,255,255,0.16); -fx-background-radius: 6; -fx-border-radius: 6;");
        Label titleLabel = label(title.toUpperCase(Locale.ROOT), 10, false, Color.rgb(199, 193, 175));
        box.getChildren().addAll(titleLabel, value);
        return box;
    }

    private Label controlKey(String text) {
        Label label = label(text, 13, true, Color.WHITE);
        label.setMinHeight(34);
        label.setMinWidth(38);
        label.setAlignment(Pos.CENTER);
        label.setPadding(new Insets(6, 10, 6, 10));
        label.setStyle("-fx-background-color: rgba(14,18,31,0.78); -fx-border-color: rgba(255,255,255,0.2); -fx-background-radius: 6; -fx-border-radius: 6;");
        return label;
    }

    private Button smallButton(String text) {
        Button button = new Button(text);
        button.setMinHeight(36);
        button.setStyle("-fx-background-color: rgba(255,255,255,0.08); -fx-border-color: rgba(240,189,63,0.52); -fx-background-radius: 6; -fx-border-radius: 6; -fx-text-fill: #f7f3e8; -fx-font-weight: bold;");
        return button;
    }

    private Button quizButton(String text) {
        Button button = new Button(text);
        button.setWrapText(true);
        button.setStyle(answerStyle("#24398b", "#1b286f"));
        return button;
    }

    private String answerStyle(String top, String bottom) {
        return "-fx-background-color: linear-gradient(" + top + ", " + bottom + "); -fx-border-color: #f0bd3f; -fx-border-width: 2; -fx-background-radius: 28; -fx-border-radius: 28; -fx-text-fill: #f7f3e8; -fx-font-weight: bold; -fx-padding: 10 16;";
    }

    private String panelStyle() {
        return "-fx-background-color: rgba(14,18,31,0.84); -fx-border-color: rgba(240,189,63,0.52); -fx-background-radius: 8; -fx-border-radius: 8;";
    }

    private void addDetail(GridPane grid, int row, String key, String value) {
        Label k = label(key, 13, false, Color.rgb(199, 193, 175));
        Label v = label(value, 13, true, Color.WHITE);
        v.setWrapText(true);
        grid.add(k, 0, row);
        grid.add(v, 1, row);
    }

    private Box box(double w, double h, double d, double x, double y, double z, PhongMaterial material) {
        Box box = new Box(w, h, d);
        box.setTranslateX(x);
        box.setTranslateY(y);
        box.setTranslateZ(z);
        box.setMaterial(material);
        return box;
    }

    private PhongMaterial material(Color color) {
        PhongMaterial material = new PhongMaterial(color);
        material.setSpecularColor(Color.rgb(40, 40, 40));
        return material;
    }

    private boolean isTypingKey(KeyCode code) {
        return code == KeyCode.TAB || code == KeyCode.ENTER || code == KeyCode.ESCAPE;
    }

    private double distance2D(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Company(String name, String description, String website, String revenue, String employees,
                           String headquarters, String netIncome, String sharePrice) {
        String field(String name) {
            return switch (name) {
                case "headquarters" -> headquarters;
                case "revenue" -> revenue;
                case "employees" -> employees;
                case "netIncome" -> netIncome;
                case "sharePrice" -> sharePrice;
                default -> "";
            };
        }
    }

    private static class Painting {
        final int index;
        final Company company;
        final Group group;
        boolean picked;
        boolean delivered;

        Painting(int index, Company company, Group group) {
            this.index = index;
            this.company = company;
            this.group = group;
        }
    }

    private record Question(String text, String correct, List<String> options) {
    }

    private static class MusicEngine {
        static final Track[] TRACKS = {
                new Track("Gallery Pulse", 92, new String[]{"C2", "G2", "A#1", "F2"}, new String[]{"G3", "C4", "D#4", "G4", "F4", "D#4", "C4", "G3"}),
                new Track("Velvet Exhibit", 78, new String[]{"A1", "E2", "G2", "D2"}, new String[]{"E3", "A3", "C4", "E4", "D4", "C4", "A3", "G3"}),
                new Track("Night Curator", 104, new String[]{"D2", "A1", "C2", "G1"}, new String[]{"F3", "A3", "D4", "E4", "D4", "A3", "C4", "A3"}),
                new Track("Golden Hall", 86, new String[]{"F2", "C2", "D2", "A#1"}, new String[]{"A3", "C4", "F4", "G4", "F4", "D4", "C4", "A3"}),
                new Track("Quiz Tension", 96, new String[]{"E2", "B1", "D2", "A1"}, new String[]{"G3", "B3", "E4", "F4", "E4", "B3", "D4", "B3"})
        };

        private volatile boolean enabled;
        private Thread thread;
        private int trackIndex = -1;

        boolean isEnabled() {
            return enabled;
        }

        String trackName() {
            return trackIndex >= 0 ? TRACKS[trackIndex].name : "";
        }

        int nextTrackIndex() {
            return trackIndex < 0 ? 0 : (trackIndex + 1) % TRACKS.length;
        }

        void play(int index) {
            stop();
            trackIndex = index;
            enabled = true;
            Track track = TRACKS[index];
            thread = new Thread(() -> runTrack(track), "CompanyMuseumMusic");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            enabled = false;
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }

        private void runTrack(Track track) {
            float sampleRate = 44100f;
            AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
            try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                line.open(format, 4096);
                line.start();
                double beat = 60.0 / track.bpm;
                int step = 0;
                while (enabled && !Thread.currentThread().isInterrupted()) {
                    String bass = step % 4 == 0 ? track.bass[(step / 4) % track.bass.length] : null;
                    String lead = step % 2 == 0 ? track.lead[(step / 2) % track.lead.length] : null;
                    byte[] data = synthStep(sampleRate, beat, bass, lead);
                    line.write(data, 0, data.length);
                    step = (step + 1) % 16;
                }
                line.drain();
            } catch (Exception ignored) {
                enabled = false;
            }
        }

        private byte[] synthStep(float sampleRate, double seconds, String bass, String lead) {
            int samples = (int) (sampleRate * seconds);
            byte[] data = new byte[samples * 2];
            double bassFreq = bass == null ? 0 : frequency(bass);
            double leadFreq = lead == null ? 0 : frequency(lead);
            for (int i = 0; i < samples; i++) {
                double t = i / sampleRate;
                double env = Math.min(1.0, i / (sampleRate * 0.025)) * Math.min(1.0, (samples - i) / (sampleRate * 0.06));
                double sample = 0;
                if (bassFreq > 0) {
                    sample += Math.sin(2 * Math.PI * bassFreq * t) * 0.18;
                }
                if (leadFreq > 0) {
                    sample += Math.sin(2 * Math.PI * leadFreq * t) * 0.10;
                    sample += Math.sin(2 * Math.PI * leadFreq * 2 * t) * 0.025;
                }
                short value = (short) (sample * env * Short.MAX_VALUE * 0.38);
                data[i * 2] = (byte) (value & 0xff);
                data[i * 2 + 1] = (byte) ((value >> 8) & 0xff);
            }
            return data;
        }

        private double frequency(String note) {
            Map<String, Integer> semitones = Map.ofEntries(
                    Map.entry("C", -9), Map.entry("C#", -8), Map.entry("D", -7), Map.entry("D#", -6),
                    Map.entry("E", -5), Map.entry("F", -4), Map.entry("F#", -3), Map.entry("G", -2),
                    Map.entry("G#", -1), Map.entry("A", 0), Map.entry("A#", 1), Map.entry("B", 2)
            );
            String name = note.substring(0, note.length() - 1);
            int octave = Integer.parseInt(note.substring(note.length() - 1));
            int distance = semitones.getOrDefault(name, 0) + (octave - 4) * 12;
            return 440.0 * Math.pow(2, distance / 12.0);
        }

        private record Track(String name, int bpm, String[] bass, String[] lead) {
        }
    }
}
