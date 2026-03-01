package app;

import javafx.application.Application;

public class Launcher {
    public static void main(String[] args) {
        // Opção mais segura e à prova de falhas:
        Application.launch(LoginFX.class, args);
    }
}