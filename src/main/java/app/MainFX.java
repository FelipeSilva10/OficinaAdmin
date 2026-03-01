package app;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainFX extends Application {

    @Override
    public void start(Stage primaryStage) {
        // 1. A Mágica do AtlantaFX: Aplica o Tema Escuro e Moderno!
        Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

        // 2. Carrega a nossa nova Tela de Escolas
        EscolasView escolasView = new EscolasView();

        // 3. Configura a Janela (Stage)
        Scene scene = new Scene(escolasView.getView(), 900, 600);
        primaryStage.setTitle("Oficina Code - Backoffice Administrativo");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}