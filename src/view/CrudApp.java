package view;

import config.Database;
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
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;

public class CrudApp extends Application {
    @Override
    public void start(Stage stage) {
        var hostField = new TextField();
        hostField.setPromptText("localhost");
        hostField.setText(Database.DB_HOST);

        var portField = new TextField();
        portField.setPromptText("5432");
        portField.setText(Database.DB_PORT);

        var nameField = new TextField();
        nameField.setPromptText("nombre_db");
        nameField.setText(Database.DB_NAME);

        var userField = new TextField();
        userField.setPromptText("usuario");
        userField.setText(Database.DB_USER);

        var passField = new PasswordField();
        passField.setPromptText("contrasena");
        passField.setText(Database.DB_PASSWORD);

        var connectButton = new Button("Conectar");
        var statusLabel = new Label("Sin conexion");

        var form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Host"), hostField, new Label("Puerto"), portField);
        form.addRow(1, new Label("Base"), nameField, new Label("Usuario"), userField);
        form.addRow(2, new Label("Clave"), passField, connectButton, statusLabel);

        var tablesButton = new MenuButton("Tablas");
        var tableLabel = new Label("Tabla: -");

        var readButton = new Button("Leer");
        var createButton = new Button("Insertar");
        var updateButton = new Button("Actualizar");
        var deleteButton = new Button("Eliminar");
        var runButton = new Button("Hacer CRUD");
        var actionLabel = new Label("Accion: Leer");

        var actionBar = new HBox(8, readButton, createButton, updateButton, deleteButton, runButton, actionLabel);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        var fieldsBox = new VBox(6);

        var table = new TableView<Map<String, Object>>();

        var controller = new CrudController();
        var state = new UiState();

        connectButton.setOnAction(_ -> {
            try {
                boolean ok = controller.connect(hostField.getText().isBlank() ? "localhost" : hostField.getText(),
                        portField.getText().isBlank() ? "5432" : portField.getText(),
                        nameField.getText(), userField.getText(), passField.getText());
                statusLabel.setText(ok ? "Conexion exitosa" : "Conexion fallida");
                tablesButton.getItems().clear();
                if (ok) {
                    for (var tableName : controller.listTables()) {
                        var item = new MenuItem(tableName);
                        item.setOnAction(__ -> loadTable(controller, state, tableName, table, fieldsBox, statusLabel, tableLabel));
                        tablesButton.getItems().add(item);
                    }
                }
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        readButton.setOnAction(_ -> {
            state.action = "Leer";
            actionLabel.setText("Accion: Leer");
            buildFields(state, fieldsBox);
        });

        createButton.setOnAction(_ -> {
            state.action = "Insertar";
            actionLabel.setText("Accion: Insertar");
            buildFields(state, fieldsBox);
        });

        updateButton.setOnAction(_ -> {
            state.action = "Actualizar";
            actionLabel.setText("Accion: Actualizar");
            buildFields(state, fieldsBox);
        });

        deleteButton.setOnAction(_ -> {
            state.action = "Eliminar";
            actionLabel.setText("Accion: Eliminar");
            buildFields(state, fieldsBox);
        });

        runButton.setOnAction(_ -> {
            if (state.table == null || state.table.isBlank()) {
                statusLabel.setText("Seleccione una tabla");
                return;
            }
            if ("Leer".equals(state.action)) {
                runRead(controller, state.table, table, statusLabel);
                return;
            }
            if ("Insertar".equals(state.action)) {
                runInsert(controller, state, table, statusLabel);
                return;
            }
            if ("Actualizar".equals(state.action)) {
                runUpdate(controller, state, table, statusLabel);
                return;
            }
            if ("Eliminar".equals(state.action)) {
                runDelete(controller, state, table, statusLabel);
            }
        });

        var tablesBar = new HBox(8, tablesButton, tableLabel);
        tablesBar.setAlignment(Pos.CENTER_LEFT);

        var root = new VBox(12, form, tablesBar, actionBar, fieldsBox, table);
        root.setPadding(new Insets(12));

        var scene = new Scene(root, 900, 560);
        stage.setTitle("CRUD Grafico");
        stage.setScene(scene);
        stage.show();
    }

    private static void loadTable(CrudController controller, UiState state, String tableName,
                                  TableView<Map<String, Object>> table, VBox fieldsBox,
                                  Label statusLabel, Label tableLabel) {
        if (tableName == null || tableName.isBlank()) return;
        try {
            state.table = tableName;
            state.columns = controller.listColumnsInfo(tableName);
            state.pk = controller.getPrimaryKey(tableName);
            table.getColumns().clear();
            for (var col : state.columns) {
                var c = new TableColumn<Map<String, Object>, Object>(col.name());
                c.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().get(col.name())));
                table.getColumns().add(c);
            }
            table.getItems().setAll(controller.readTable(tableName));
            tableLabel.setText("Tabla: " + tableName);
            statusLabel.setText("Tabla cargada: " + tableName);
            buildFields(state, fieldsBox);
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void buildFields(UiState state, VBox fieldsBox) {
        fieldsBox.getChildren().clear();
        state.fields.clear();

        if ("Leer".equals(state.action)) {
            fieldsBox.getChildren().add(new Label("Sin campos para leer"));
            return;
        }

        if ("Eliminar".equals(state.action)) {
            if (state.pk != null && !state.pk.isBlank()) {
                addField(state, fieldsBox, state.pk);
            } else {
                fieldsBox.getChildren().add(new Label("No se encontro PK"));
            }
            return;
        }

        if ("Actualizar".equals(state.action)) {
            if (state.pk != null && !state.pk.isBlank()) {
                addField(state, fieldsBox, state.pk);
            }
        }

        for (var col : state.columns) {
            if (state.pk != null && !state.pk.isBlank() && col.name().equals(state.pk)) continue;
            if ("Insertar".equals(state.action) && col.auto()) continue;
            if ("Actualizar".equals(state.action) && col.auto()) continue;
            addField(state, fieldsBox, col.name());
        }
    }

    private static void addField(UiState state, VBox fieldsBox, String name) {
        var field = new TextField();
        field.setPromptText(name);
        state.fields.put(name, field);
        fieldsBox.getChildren().add(new HBox(8, new Label(name), field));
    }

    private static void runRead(CrudController controller, String tableName,
                                TableView<Map<String, Object>> table, Label statusLabel) {
        try {
            table.getItems().setAll(controller.readTable(tableName));
            statusLabel.setText("Datos cargados: " + table.getItems().size());
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void runInsert(CrudController controller, UiState state,
                                  TableView<Map<String, Object>> table, Label statusLabel) {
        var values = collectValues(state, false);
        if (values.isEmpty()) {
            statusLabel.setText("Ingrese valores para insertar");
            return;
        }
        try {
            controller.insertRow(state.table, values);
            table.getItems().setAll(controller.readTable(state.table));
            statusLabel.setText("Insertado");
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void runUpdate(CrudController controller, UiState state,
                                  TableView<Map<String, Object>> table, Label statusLabel) {
        var pkValue = getPkValue(state);
        if (pkValue == null) {
            statusLabel.setText("Ingrese valor de PK");
            return;
        }
        var values = collectValues(state, false);
        if (values.isEmpty()) {
            statusLabel.setText("Ingrese valores para actualizar");
            return;
        }
        try {
            if (controller.updateRow(state.table, state.pk, pkValue, values)) {
                table.getItems().setAll(controller.readTable(state.table));
                statusLabel.setText("Actualizado");
            } else {
                statusLabel.setText("No actualizado");
            }
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void runDelete(CrudController controller, UiState state,
                                  TableView<Map<String, Object>> table, Label statusLabel) {
        var pkValue = getPkValue(state);
        if (pkValue == null) {
            statusLabel.setText("Ingrese valor de PK");
            return;
        }
        try {
            if (controller.deleteRow(state.table, state.pk, pkValue)) {
                table.getItems().setAll(controller.readTable(state.table));
                statusLabel.setText("Eliminado");
            } else {
                statusLabel.setText("No eliminado");
            }
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static String getPkValue(UiState state) {
        if (state.pk == null || state.pk.isBlank()) return null;
        var field = state.fields.get(state.pk);
        if (field == null) return null;
        var value = field.getText();
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private static Map<String, Object> collectValues(UiState state, boolean includePk) {
        var map = new LinkedHashMap<String, Object>();
        for (var entry : state.fields.entrySet()) {
            var key = entry.getKey();
            if (!includePk && key.equals(state.pk)) continue;
            var value = entry.getValue().getText();
            if (value == null || value.isBlank()) continue;
            map.put(key, value);
        }
        return map;
    }

    private static class UiState {
        String action = "Leer";
        String table;
        String pk;
        List<CrudController.ColumnInfo> columns = new ArrayList<>();
        Map<String, TextField> fields = new LinkedHashMap<>();
    }
}
