import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class MillionaerStage extends Stage {

    private List<Question> questions = new ArrayList<>();
    private int currentQuestion = 0;

    private Label questionLabel = new Label();
    private Button[] answerButtons = new Button[4];

    public MillionaerStage() {

        loadQuestions("fragen.json");
        Collections.shuffle(questions);

        questions = questions.subList(0, Math.min(15, questions.size()));

        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);

        questionLabel.setWrapText(true);
        questionLabel.setStyle("-fx-font-size:18px; -fx-font-weight:bold;");

        root.getChildren().add(questionLabel);

        for(int i=0;i<4;i++) {

            Button btn = new Button();
            btn.setPrefWidth(400);

            int index = i;

            btn.setOnAction(e -> checkAnswer(btn.getText()));

            answerButtons[i] = btn;
            root.getChildren().add(btn);
        }

        nextQuestion();

        Scene scene = new Scene(root, 600, 400);

        setTitle("Wer wird Millionär - Quiz");
        setScene(scene);
    }

    private void loadQuestions(String path) {

        try {

            String content = new String(Files.readAllBytes(Paths.get(path)));

            JSONArray arr = new JSONArray(content);

            for(int i=0;i<arr.length();i++) {

                JSONObject obj = arr.getJSONObject(i);

                String frage = obj.getString("frage");

                JSONArray answersJSON = obj.getJSONArray("antworten");

                List<String> answers = new ArrayList<>();

                for(int j=0;j<answersJSON.length();j++)
                    answers.add(answersJSON.getString(j));

                String richtig = obj.getString("richtig");

                questions.add(new Question(frage, answers, richtig));
            }

        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void nextQuestion() {

        if(currentQuestion >= questions.size()) {

            questionLabel.setText("Spiel beendet! Glückwunsch!");
            for(Button b : answerButtons) b.setDisable(true);
            return;
        }

        Question q = questions.get(currentQuestion);

        questionLabel.setText((currentQuestion+1)+". "+q.frage);

        List<String> answers = new ArrayList<>(q.antworten);
        Collections.shuffle(answers);

        for(int i=0;i<4;i++) {
            answerButtons[i].setText(answers.get(i));
        }
    }

    private void checkAnswer(String answer) {

        Question q = questions.get(currentQuestion);

        if(answer.equals(q.richtig)) {

            questionLabel.setText("Richtig! 🎉");

            currentQuestion++;

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    javafx.application.Platform.runLater(() -> nextQuestion());
                }
            },1000);

        } else {

            questionLabel.setText("Falsch! Richtige Antwort: "+q.richtig);

            for(Button b : answerButtons)
                b.setDisable(true);
        }
    }

    class Question {

        String frage;
        List<String> antworten;
        String richtig;

        Question(String f, List<String> a, String r) {
            frage = f;
            antworten = a;
            richtig = r;
        }
    }
}