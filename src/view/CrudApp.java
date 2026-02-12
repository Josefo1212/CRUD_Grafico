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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import java.util.UUID;

public class CrudApp extends Application {
    private enum Action {
        READ("Leer"),
        INSERT("Insertar"),
        UPDATE("Actualizar"),
        DELETE("Eliminar");

        final String label;
        Action(String label) {
            this.label = label;
        }
    }

    private static final class UiStateRef {
        UiState value = UiState.empty();
    }

    private record ConnectionFields(TextField host, TextField port, TextField name, TextField user,
                                    PasswordField pass, GridPane form) {}

    @Override
    public void start(Stage stage) {
        var statusLabel = new Label("Sin conexión");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(320);
        statusLabel.setMinHeight(Region.USE_PREF_SIZE);

        var connectButton = new Button("Conectar");
        var connection = buildConnectionForm(statusLabel, connectButton);

        var tablesButton = new MenuButton("Tablas");
        var tableLabel = new Label("Tabla: -");

        var readButton = new Button("Leer");
        var createButton = new Button("Insertar");
        var updateButton = new Button("Actualizar");
        var deleteButton = new Button("Eliminar");
        var runButton = new Button("Hacer CRUD");
        var actionLabel = new Label("Accion: " + Action.READ.label);

        var actionBar = new HBox(8, readButton, createButton, updateButton, deleteButton, runButton, actionLabel);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        var fieldsBox = new VBox(6);

        var table = new TableView<Map<String, Object>>();

        var controller = new CrudController();
        var stateRef = new UiStateRef();

        connectButton.setOnAction(_ -> {
            try {
                var host = connection.host().getText().isBlank() ? "localhost" : connection.host().getText();
                var port = connection.port().getText().isBlank() ? "5432" : connection.port().getText();
                boolean ok = controller.connect(host, port,
                        connection.name().getText(), connection.user().getText(), connection.pass().getText());
                statusLabel.setText(ok ? "Conexion exitosa" : "Conexion fallida");
                tablesButton.getItems().clear();
                if (ok) {
                    for (var tableName : controller.listTables()) {
                        var item = new MenuItem(tableName);
                        item.setOnAction(_ -> stateRef.value = loadTable(controller, stateRef.value, tableName, table, fieldsBox, statusLabel, tableLabel));
                        tablesButton.getItems().add(item);
                    }
                }
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        readButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.READ));
        createButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.INSERT));
        updateButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.UPDATE));
        deleteButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.DELETE));

        runButton.setOnAction(_ -> {
            var state = stateRef.value;
            if (state.table() == null || state.table().isBlank()) {
                statusLabel.setText("Seleccione una tabla");
                return;
            }
            switch (state.action()) {
                case READ -> runRead(controller, state.table(), table, statusLabel);
                case INSERT -> runInsert(controller, state, table, statusLabel);
                case UPDATE -> runUpdate(controller, state, table, statusLabel);
                case DELETE -> runDelete(controller, state, table, statusLabel);
            }
        });

        var tablesBar = new HBox(8, tablesButton, tableLabel);
        tablesBar.setAlignment(Pos.CENTER_LEFT);

        var root = new VBox(12, connection.form(), tablesBar, actionBar, fieldsBox, table);
        root.setPadding(new Insets(12));

        var scene = new Scene(root, 900, 560);
        stage.setTitle("CRUD Grafico");
        stage.setScene(scene);
        stage.show();
    }

    private static ConnectionFields buildConnectionForm(Label statusLabel, Button connectButton) {
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

        var form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Host"), hostField, new Label("Puerto"), portField);
        form.addRow(1, new Label("Base"), nameField, new Label("Usuario"), userField);
        form.addRow(2, new Label("Clave"), passField, connectButton, statusLabel);

        return new ConnectionFields(hostField, portField, nameField, userField, passField, form);
    }

    private static UiState loadTable(CrudController controller, UiState state, String tableName,
                                  TableView<Map<String, Object>> table, VBox fieldsBox,
                                  Label statusLabel, Label tableLabel) {
        if (tableName == null || tableName.isBlank()) return state;
        try {
            var columns = controller.listColumnsInfo(tableName);
            var pk = controller.getPrimaryKey(tableName);
            var pkType = controller.getPrimaryKeyType(tableName, pk);
            var updated = state.withLoadedTable(tableName, columns, pk, pkType, isPkAuto(pk, columns));
            table.getColumns().clear();
            for (var col : updated.columns()) {
                var c = new TableColumn<Map<String, Object>, Object>(col.name());
                c.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().get(col.name())));
                table.getColumns().add(c);
            }
            table.getItems().setAll(controller.readTable(tableName));
            tableLabel.setText("Tabla: " + tableName);
            statusLabel.setText("Tabla cargada: " + tableName);
            buildFields(updated, fieldsBox);
            return updated;
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            return state;
        }
    }

    private static void buildFields(UiState state, VBox fieldsBox) {
        fieldsBox.getChildren().clear();
        state.fields().clear();

        switch (state.action()) {
            case READ -> {
                fieldsBox.getChildren().add(new Label("Sin campos para leer"));
                return;
            }
            case DELETE -> {
                if (hasPk(state)) {
                    addField(state, fieldsBox, state.pk());
                } else {
                    fieldsBox.getChildren().add(new Label("No se encontro PK"));
                }
                return;
            }
            case UPDATE -> {
                if (hasPk(state)) {
                    addField(state, fieldsBox, state.pk());
                }
            }
            case INSERT -> {
            }
        }

        var includePkInInsert = state.action() == Action.INSERT && !state.pkAuto();
        for (var col : state.columns()) {
            var isPk = hasPk(state) && col.name().equals(state.pk());
            if (isPk && includePkInInsert) {
                addField(state, fieldsBox, col.name());
                continue;
            }
            if (isPk || col.auto()) continue;
            addField(state, fieldsBox, col.name());
        }
    }

    private static UiState setAction(UiState state, VBox fieldsBox, Label actionLabel, Action action) {
        var updated = state.withAction(action);
        actionLabel.setText("Accion: " + action.label);
        buildFields(updated, fieldsBox);
        return updated;
    }

    private static void addField(UiState state, VBox fieldsBox, String name) {
        var field = new TextField();
        field.setPromptText(name);
        state.fields().put(name, field);
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
        var values = collectValues(state, !state.pkAuto, statusLabel);
        if (values == null || values.isEmpty()) {
            if (values == null) return;
            statusLabel.setText("Ingrese valores para insertar");
            return;
        }
        try {
            controller.insertRow(state.table, values);
            refreshTable(controller, state, table, statusLabel, "Insertado");
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void runUpdate(CrudController controller, UiState state,
                                  TableView<Map<String, Object>> table, Label statusLabel) {
        var pkValue = parsePkOrWarn(state, statusLabel);
        if (pkValue == null) return;
        var values = collectValues(state, false, statusLabel);
        if (values == null || values.isEmpty()) {
            if (values == null) return;
            statusLabel.setText("Ingrese valores para actualizar");
            return;
        }
        try {
            if (controller.updateRow(state.table, state.pk, pkValue, values)) {
                refreshTable(controller, state, table, statusLabel, "Actualizado");
            } else {
                statusLabel.setText("No actualizado");
            }
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void runDelete(CrudController controller, UiState state,
                                  TableView<Map<String, Object>> table, Label statusLabel) {
        var pkValue = parsePkOrWarn(state, statusLabel);
        if (pkValue == null) return;
        try {
            if (controller.deleteRow(state.table, state.pk, pkValue)) {
                refreshTable(controller, state, table, statusLabel, "Eliminado");
            } else {
                statusLabel.setText("No eliminado");
            }
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void refreshTable(CrudController controller, UiState state,
                                     TableView<Map<String, Object>> table, Label statusLabel,
                                     String successMessage) throws SQLException {
        table.getItems().setAll(controller.readTable(state.table));
        statusLabel.setText(successMessage);
    }

    private static Object parsePkOrWarn(UiState state, Label statusLabel) {
        var pkValueText = getPkValue(state);
        if (pkValueText == null) {
            statusLabel.setText("Ingrese valor de PK");
            return null;
        }
        var pkType = (state.pkType() == null || state.pkType().isBlank())
                ? getColumnType(state, state.pk())
                : state.pkType();
        return parseValue(pkValueText, pkType, statusLabel, "PK");
    }

    private static String getPkValue(UiState state) {
        if (!hasPk(state)) return null;
        var field = state.fields().get(state.pk());
        if (field == null) return null;
        var value = field.getText();
        if (value == null || value.isBlank()) return null;
        return value;
    }

    private static Map<String, Object> collectValues(UiState state, boolean includePk, Label statusLabel) {
        var map = new LinkedHashMap<String, Object>();
        for (var entry : state.fields().entrySet()) {
            var key = entry.getKey();
            if (!includePk && key.equals(state.pk())) continue;
            var valueText = entry.getValue().getText();
            if (valueText == null || valueText.isBlank()) continue;
            var typeName = getColumnType(state, key);
            var value = parseValue(valueText, typeName, statusLabel, key);
            if (value == null) return null;
            map.put(key, value);
        }
        return map;
    }

    private static String getColumnType(UiState state, String columnName) {
        for (var col : state.columns()) {
            if (col.name().equals(columnName)) return col.typeName();
        }
        return null;
    }

    private static Object parseValue(String value, String typeName, Label statusLabel, String columnName) {
        if (typeName == null || typeName.isBlank()) return value;
        var t = typeName.toUpperCase();
        try {
            if (t.contains("INT") || t.contains("SERIAL")) return Long.parseLong(value);
            if (t.contains("UUID")) return UUID.fromString(value);
            if (t.contains("BOOL")) return Boolean.parseBoolean(value);
        } catch (Exception ex) {
            statusLabel.setText("Valor invalido para " + columnName + " (" + typeName + ")");
            return null;
        }
        return value;
    }

    private static boolean hasPk(UiState state) {
        return state.pk() != null && !state.pk().isBlank();
    }

    private static boolean isPkAuto(String pk, List<CrudController.ColumnInfo> columns) {
        if (pk == null || pk.isBlank()) return false;
        for (var col : columns) {
            if (pk.equals(col.name())) return col.auto();
        }
        return false;
    }

    private record UiState(Action action, String table, String pk, String pkType, boolean pkAuto,
                           List<CrudController.ColumnInfo> columns, Map<String, TextField> fields) {
        static UiState empty() {
            return new UiState(Action.READ, null, null, null, false, new ArrayList<>(), new LinkedHashMap<>());
        }

        UiState withAction(Action action) {
            return new UiState(action, table, pk, pkType, pkAuto, columns, fields);
        }

        UiState withLoadedTable(String table, List<CrudController.ColumnInfo> columns, String pk,
                                String pkType, boolean pkAuto) {
            return new UiState(action, table, pk, pkType, pkAuto, columns, fields);
        }
    }
}
