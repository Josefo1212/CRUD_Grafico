package view;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CrudApp extends Application {
    @Override
    public void start(Stage stage) {
        var hostField = new TextField();
        hostField.setPromptText("localhost");

        var portField = new TextField();
        portField.setPromptText("5433");

        var nameField = new TextField();
        nameField.setPromptText("nombre_db");

        var userField = new TextField();
        userField.setPromptText("usuario");

        var passField = new PasswordField();
        passField.setPromptText("contrasena");

        var connectButton = new Button("Conectar");
        var statusLabel = new Label("Sin conexion");

        var form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Host"), hostField, new Label("Puerto"), portField);
        form.addRow(1, new Label("Base"), nameField, new Label("Usuario"), userField);
        form.addRow(2, new Label("Clave"), passField, connectButton, statusLabel);

        var createButton = new Button("Crear");
        var readButton = new Button("Leer");
        var updateButton = new Button("Actualizar");
        var deleteButton = new Button("Eliminar");

        var crudBar = new HBox(8, createButton, readButton, updateButton, deleteButton);
        crudBar.setAlignment(Pos.CENTER_LEFT);

        var table = new TableView<>();

        var root = new VBox(12, form, crudBar, table);
        root.setPadding(new Insets(12));

        var scene = new Scene(root, 900, 520);
        stage.setTitle("CRUD Grafico");
        stage.setScene(scene);
        stage.show();
    }
}
