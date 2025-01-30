import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.scene.shape.Line;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;

import java.io.File;

public class Main extends Application {

    private TextArea logArea;
    private File selectedFolderInput;
    private File selectedFileInput;
    private File selectedOutputDir;
    private Button selectFileInputButton;
    private Button selectFolderInputButton;
    private Button selectOutputButton;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        Prevodnik prevodnik = new Prevodnik();

        // Výběr složek
        selectFolderInputButton = new Button("Vyber SLOŽKU pro převod - víš, co je složka, ne?");
        selectFileInputButton = new Button("Vyber PDF pro převod - jakože jen soubor");
        selectOutputButton = new Button("Vybrat výstupní složku, kua!");
        Button startButton = new Button("Převeď, kemo!!!");
        Button vymazatCestyButton = new Button("Vymaž fšechny cesty, bobe!!! (A VESMÍR IMPLODUJE!!!)");
        startButton.setDisable(true); // Deaktivované, dokud nejsou vybrány oba vstupy

        // tabulka se seznamy posledních cest
        GridPane tabulkaCest = new GridPane();
        tabulkaCest.setHgap(20);
        tabulkaCest.setVgap(10);
        tabulkaCest.setMaxHeight(300);
        Line prepazka = new Line();
        prepazka.setStartX(10);
        prepazka.setEndX(400);

        prepazka.endXProperty().bind(Bindings.createDoubleBinding(
                tabulkaCest::getWidth,
                tabulkaCest.widthProperty()
        ));

        // vstupní sloupec
        Label nadpisCestyVstupu = new Label("Poslední vstupní složky");
        tabulkaCest.add(nadpisCestyVstupu, 0, 0);
        ListView<String> seznamPoslednichVstupu = new ListView<>();
        seznamPoslednichVstupu.getItems().setAll(prevodnik.nactiSeznamCest(prevodnik.getVstupniCestySeznamCsv()));
        tabulkaCest.add(seznamPoslednichVstupu, 0, 1);
        Button zvolVstupniCestu = new Button("VOLÍM SI TEBE, VSTUPE!!!");
        zvolVstupniCestu.setDisable(true);
        tabulkaCest.add(zvolVstupniCestu, 0, 2);

        seznamPoslednichVstupu.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        seznamPoslednichVstupu.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> zvolVstupniCestu.setDisable(false));

        zvolVstupniCestu.setOnAction(event -> {
            selectedFolderInput = new File(seznamPoslednichVstupu.getSelectionModel().getSelectedItem());
            selectFileInputButton.setDisable(true);
            enableConvertButtonIfReady(startButton);
        });

        // výstupní sloupec
        Label nadpisCestVystupu = new Label("Poslední výstupní složky");
        tabulkaCest.add(nadpisCestVystupu, 1, 0);
        ListView<String> seznamPoslednichVystupu = new ListView<>();
        seznamPoslednichVystupu.getItems().setAll(prevodnik.nactiSeznamCest(prevodnik.getVystupniCestySeznamCsv()));
        tabulkaCest.add(seznamPoslednichVystupu, 1, 1);
        Button zvolVystupniCestu = new Button("VOLÍM SI TEBE, VÝSTUPE!!!");
        zvolVystupniCestu.setDisable(true);
        tabulkaCest.add(zvolVystupniCestu, 1, 2);

        seznamPoslednichVystupu.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        seznamPoslednichVystupu.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> zvolVystupniCestu.setDisable(false));

        zvolVystupniCestu.setOnAction(event -> {
            selectedOutputDir = new File(seznamPoslednichVystupu.getSelectionModel().getSelectedItem());
            selectOutputButton.setDisable(true);
            enableConvertButtonIfReady(startButton);
        });

        logArea = new TextArea();
        logArea.setEditable(false);

        ProgressBar progresPdfSouboru = new ProgressBar(0);
        progresPdfSouboru.setMinWidth(300);
        progresPdfSouboru.setMinHeight(10);

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getChildren().addAll(
                tabulkaCest,
                prepazka,
                selectFolderInputButton,
                selectFileInputButton,
                selectOutputButton,
                startButton,
                progresPdfSouboru,
                logArea,
                vymazatCestyButton
        );

        // Akce pro smazání vstupních cest
        vymazatCestyButton.setOnAction(event -> {
            vymazVsechnyCesty();
            enableConvertButtonIfReady(startButton);
        });

        // Akce pro výběr složky
        selectFolderInputButton.setOnAction(event -> ukazDialogProVyberVstupniSlozky(primaryStage, startButton));

        // Akce pro výběr PDF souboru
        selectFileInputButton.setOnAction(event -> ukazDialogProVyberSouboru(primaryStage, startButton));

        // Akce pro výběr výstupní složky
        selectOutputButton.setOnAction(event -> ukazDialogProVyberVystupniSlozky(primaryStage, startButton));

        // Akce pro převod PDF
        startButton.setOnAction(event -> {
            if (selectedFileInput != null && selectedOutputDir != null) {
                Task<Void> vlaknoPrevadeniSouboru = new Task<>() {
                    @Override
                    protected Void call() {
                        prevodnik.prevadejPdfSouborDoJpg(selectedFileInput, selectedOutputDir, logArea);
                        updateProgress(prevodnik.getAktualniCisloStrankyDokumentu(), prevodnik.getPocetStranDokumentu());
                        return null;
                    }
                };
                progresPdfSouboru.progressProperty().bind(vlaknoPrevadeniSouboru.progressProperty());

                Thread prevadeniSouboru = new Thread(vlaknoPrevadeniSouboru);
                prevadeniSouboru.setDaemon(true);
                prevadeniSouboru.start();
            } else if (selectedFolderInput != null && selectedOutputDir != null) {
                Task<Void> vlaknoPrevadeniSlozky = new Task<>() {
                    @Override
                    protected Void call() {
                        prevodnik.prevadejSlozkuPdfDoJpg(selectedFolderInput, selectedOutputDir, logArea);
                        updateProgress(prevodnik.getAktualniCisloSouboruDokumentu(), prevodnik.getPocetSouboruVeSlozce());
                        return null;
                    }
                };
                progresPdfSouboru.progressProperty().bind(vlaknoPrevadeniSlozky.progressProperty());

                Thread prevadeniSlozky = new Thread(vlaknoPrevadeniSlozky);
                prevadeniSlozky.setDaemon(true);
                prevadeniSlozky.start();
            } else {
                logArea.appendText("Vyberte PDF soubor nebo vstupní SLOŽKU a výstupní složku před převodem!\n");
            }
        });

        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

    private void ukazDialogProVyberVstupniSlozky(Stage primaryStage, Button startButton) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Vyberte složku s PDF soubory");
        selectedFolderInput = directoryChooser.showDialog(primaryStage);
        if (selectedFolderInput != null && selectedFolderInput.isDirectory()) {
            logArea.appendText("Vybraná složka: " + selectedFolderInput.getAbsolutePath() + "\n");
            selectFileInputButton.setDisable(true);
            enableConvertButtonIfReady(startButton);
        } else {
            logArea.appendText("Nebyla vybrána žádná platná složka.\n");
        }
    }

    private void ukazDialogProVyberVystupniSlozky(Stage primaryStage, Button startButton) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Vyberte výstupní složku");
        File dir = directoryChooser.showDialog(primaryStage);
        if (dir != null) {
            selectedOutputDir = dir;
            logArea.appendText("Výstupní složka: " + selectedOutputDir.getAbsolutePath() + "\n");
            enableConvertButtonIfReady(startButton);
        }
    }

    private void ukazDialogProVyberSouboru(Stage primaryStage, Button startButton) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Vyberte PDF soubor");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = fileChooser.showOpenDialog(primaryStage);
        if (file != null) {
            selectedFileInput = file;
            selectFolderInputButton.setDisable(true);
            logArea.appendText("Vybraný soubor: " + selectedFileInput.getAbsolutePath() + "\n");
            enableConvertButtonIfReady(startButton);
        }
    }

    private void enableConvertButtonIfReady(Button convertButton) {
        convertButton.setDisable((selectedFileInput == null && selectedFolderInput == null) || selectedOutputDir == null);
    }

    private void vymazVsechnyCesty() {
        selectedFileInput = null;
        selectedFolderInput = null;
        selectedOutputDir = null;
        selectFolderInputButton.setDisable(false);
        selectFileInputButton.setDisable(false);
        selectOutputButton.setDisable(false);
        logArea.appendText("Všechny vstupní cesty byly vymazány");
    }
}
