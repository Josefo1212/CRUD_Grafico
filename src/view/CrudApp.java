package view;

import controller.CrudController;
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
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import model.Cliente;

import java.sql.SQLException;

public class CrudApp extends Application {
    @Override
    public void start(Stage stage) {
        var hostField = new TextField();
        hostField.setPromptText("localhost");

        var portField = new TextField();
        portField.setPromptText("5432");

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

        var clienteNombreField = new TextField();
        clienteNombreField.setPromptText("Nombre cliente");
        var clienteCorreoField = new TextField();
        clienteCorreoField.setPromptText("Correo cliente");

        var createButton = new Button("Crear");
        var readButton = new Button("Leer");
        var updateButton = new Button("Actualizar");
        var deleteButton = new Button("Eliminar");

        var inputBar = new HBox(8, clienteNombreField, clienteCorreoField);
        inputBar.setAlignment(Pos.CENTER_LEFT);

        var crudBar = new HBox(8, createButton, readButton, updateButton, deleteButton);
        crudBar.setAlignment(Pos.CENTER_LEFT);

        var table = new TableView<Cliente>();

        var idCol = new TableColumn<Cliente, Integer>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        var nombreCol = new TableColumn<Cliente, String>("Nombre");
        nombreCol.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        var correoCol = new TableColumn<Cliente, String>("Correo");
        correoCol.setCellValueFactory(new PropertyValueFactory<>("correo"));
        table.getColumns().addAll(idCol, nombreCol, correoCol);

        var controller = new CrudController();

        connectButton.setOnAction(ev -> {
            try {
                boolean ok = controller.connect(hostField.getText().isBlank() ? "localhost" : hostField.getText(),
                        portField.getText().isBlank() ? "5432" : portField.getText(),
                        nameField.getText(), userField.getText(), passField.getText());
                statusLabel.setText(ok ? "Conectado" : "Error al conectar");
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        readButton.setOnAction(ev -> {
            try {
                table.getItems().setAll(controller.readAllClientes());
                statusLabel.setText("Datos cargados: " + table.getItems().size());
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        createButton.setOnAction(ev -> {
            try {
                var nombre = clienteNombreField.getText();
                var correo = clienteCorreoField.getText();
                if (nombre == null || nombre.isBlank()) {
                    statusLabel.setText("Nombre requerido");
                    return;
                }
                var c = controller.createCliente(nombre, correo);
                if (c != null) {
                    table.getItems().add(c);
                    statusLabel.setText("Cliente creado: " + c.getId());
                    clienteNombreField.clear();
                    clienteCorreoField.clear();
                }
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        deleteButton.setOnAction(ev -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                statusLabel.setText("Seleccione un registro para eliminar");
                return;
            }
            try {
                if (controller.deleteCliente(sel.getId())) {
                    table.getItems().remove(sel);
                    statusLabel.setText("Eliminado: " + sel.getId());
                }
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        updateButton.setOnAction(ev -> {
            var sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                statusLabel.setText("Seleccione un registro para actualizar");
                return;
            }
            var nuevoNombre = clienteNombreField.getText();
            var nuevoCorreo = clienteCorreoField.getText();
            if ((nuevoNombre == null || nuevoNombre.isBlank()) && (nuevoCorreo == null || nuevoCorreo.isBlank())) {
                statusLabel.setText("Ingrese nuevos datos en los campos");
                return;
            }
            if (nuevoNombre != null && !nuevoNombre.isBlank()) sel.setNombre(nuevoNombre);
            if (nuevoCorreo != null && !nuevoCorreo.isBlank()) sel.setCorreo(nuevoCorreo);
            try {
                if (controller.updateCliente(sel)) {
                    table.refresh();
                    statusLabel.setText("Actualizado: " + sel.getId());
                    clienteNombreField.clear();
                    clienteCorreoField.clear();
                }
            } catch (SQLException ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        var root = new VBox(12, form, inputBar, crudBar, table);
        root.setPadding(new Insets(12));

        var scene = new Scene(root, 900, 520);
        stage.setTitle("CRUD Grafico");
        stage.setScene(scene);
        stage.show();
    }
}
