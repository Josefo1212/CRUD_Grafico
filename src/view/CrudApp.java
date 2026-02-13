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
import javafx.scene.control.RadioButton;
import javafx.scene.control.Separator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
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

    private static final String BG = "-fx-background-color: #eef1f6;";
    private static final String CARD = "-fx-background-color: #ffffff; -fx-border-color: #d6dbe6; -fx-border-radius: 14; -fx-background-radius: 14; -fx-padding: 14; -fx-effect: dropshadow(gaussian, rgba(31,122,236,0.08), 12, 0.2, 0, 2);";
    private static final String TITLE = "-fx-font-weight: 700; -fx-text-fill: #1c2b4a; -fx-font-size: 13px;";
    private static final String LABEL = "-fx-text-fill: #1c2b4a;";
    private static final String BTN_PRIMARY = "-fx-background-color: #1f7aec; -fx-text-fill: white; -fx-background-radius: 10; -fx-padding: 6 14;";
    private static final String BTN_GHOST = "-fx-background-color: #ffffff; -fx-text-fill: #1c2b4a; -fx-background-radius: 10; -fx-border-color: #d6dbe6; -fx-border-radius: 10;";
    private static final String FIELD = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d6dbe6;";
    private static final String TABLE_STYLE = "-fx-background-color: #ffffff; -fx-border-color: #d6dbe6; -fx-border-radius: 12; -fx-background-radius: 12;";

    @Override
    public void start(Stage stage) {
        var statusLabel = new Label("Sin conexión");
        statusLabel.setWrapText(true);
        statusLabel.setMaxWidth(420);
        statusLabel.setMinHeight(Region.USE_PREF_SIZE);
        statusLabel.setStyle("-fx-text-fill: #1c2b4a; -fx-font-size: 12px;");

        var connectButton = new Button("Conectar");
        connectButton.setStyle(BTN_PRIMARY);
        var connection = buildConnectionForm(statusLabel, connectButton);

        var tablesButton = new MenuButton("Tablas");
        tablesButton.setStyle(BTN_GHOST);
        var tableLabel = new Label("Tabla: -");
        tableLabel.setStyle("-fx-text-fill: #1c2b4a; -fx-font-weight: 600;");

        var readButton = new Button("Leer");
        var createButton = new Button("Insertar");
        var updateButton = new Button("Actualizar");
        var deleteButton = new Button("Eliminar");
        var runButton = new Button("Hacer CRUD");
        var actionLabel = new Label("Accion: " + Action.READ.label);
        actionLabel.setStyle("-fx-text-fill: #1c2b4a; -fx-font-weight: 600;");

        var readModeGroup = new ToggleGroup();
        var readAllRadio = new RadioButton("Leer todo");
        readAllRadio.setToggleGroup(readModeGroup);
        var readByPkRadio = new RadioButton("Leer por PK");
        readByPkRadio.setToggleGroup(readModeGroup);
        readAllRadio.setSelected(true);

        var actionBar = new HBox(8, readButton, createButton, updateButton, deleteButton, new Separator(), runButton, actionLabel);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        runButton.setStyle(BTN_PRIMARY);
        readButton.setStyle(BTN_GHOST);
        createButton.setStyle(BTN_GHOST);
        updateButton.setStyle(BTN_GHOST);
        deleteButton.setStyle(BTN_GHOST);

        var fieldsBox = new VBox(8);
        fieldsBox.setPadding(new Insets(6, 0, 6, 0));

        var table = new TableView<Map<String, Object>>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setStyle(TABLE_STYLE);

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
                stateRef.value = UiState.empty();
                fieldsBox.getChildren().clear();
                table.getItems().clear();
                table.getColumns().clear();
                tableLabel.setText("Tabla: -");
                if (ok) {
                    for (var tableName : controller.listTables()) {
                        var item = new MenuItem(tableName);
                        item.setOnAction(_ -> stateRef.value = loadTable(controller, stateRef.value, tableName, table, fieldsBox, statusLabel, tableLabel, readAllRadio, readByPkRadio));
                        tablesButton.getItems().add(item);
                    }
                }
            } catch (Exception ex) {
                statusLabel.setText("Error: " + ex.getMessage());
            }
        });

        readButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.READ, controller, table, statusLabel, readAllRadio, readByPkRadio));
        createButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.INSERT, controller, table, statusLabel, readAllRadio, readByPkRadio));
        updateButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.UPDATE, controller, table, statusLabel, readAllRadio, readByPkRadio));
        deleteButton.setOnAction(_ -> stateRef.value = setAction(stateRef.value, fieldsBox, actionLabel, Action.DELETE, controller, table, statusLabel, readAllRadio, readByPkRadio));

        runButton.setOnAction(_ -> {
            var state = stateRef.value;
            if (state.table() == null || state.table().isBlank()) {
                statusLabel.setText("Seleccione una tabla");
                return;
            }
            switch (state.action()) {
                case READ -> {
                    if (readAllRadio.isSelected()) {
                        runReadAll(controller, state, table, statusLabel);
                    } else {
                        runReadByPk(controller, state, table, statusLabel);
                    }
                }
                case INSERT -> runInsert(controller, state, table, statusLabel);
                case UPDATE -> runUpdate(controller, state, table, statusLabel);
                case DELETE -> runDelete(controller, state, table, statusLabel);
            }
        });

        var connectionTitle = new Label("Conexion");
        connectionTitle.setStyle(TITLE);
        var connectionBox = new VBox(8, connectionTitle, connection.form());
        connectionBox.setStyle(CARD);

        var tableTitle = new Label("Tabla");
        tableTitle.setStyle(TITLE);
        var tablesBar = new HBox(8, tablesButton, tableLabel);
        tablesBar.setAlignment(Pos.CENTER_LEFT);
        var tableBox = new VBox(8, tableTitle, tablesBar);
        tableBox.setStyle(CARD);

        var topRow = new HBox(12, connectionBox, tableBox);
        topRow.setAlignment(Pos.TOP_LEFT);
        HBox.setHgrow(connectionBox, Priority.ALWAYS);
        HBox.setHgrow(tableBox, Priority.NEVER);

        var actionTitle = new Label("Acciones");
        actionTitle.setStyle(TITLE);
        var actionBox = new VBox(8, actionTitle, actionBar);
        actionBox.setStyle(CARD);

        var fieldsTitle = new Label("Campos");
        fieldsTitle.setStyle(TITLE);
        var fieldsSection = new VBox(8, fieldsTitle, fieldsBox);
        fieldsSection.setStyle(CARD);
        fieldsSection.setMinWidth(320);

        var dataTitle = new Label("Datos");
        dataTitle.setStyle(TITLE);
        var tableSection = new VBox(8, dataTitle, table);
        tableSection.setStyle(CARD);

        var bottomRow = new HBox(12, fieldsSection, tableSection);
        HBox.setHgrow(tableSection, Priority.ALWAYS);

        var root = new VBox(12, topRow, actionBox, bottomRow);
        root.setPadding(new Insets(14));
        root.setStyle(BG);

        VBox.setVgrow(bottomRow, Priority.ALWAYS);
        VBox.setVgrow(tableSection, Priority.ALWAYS);
        table.setMinHeight(320);
        table.setPrefHeight(Region.USE_COMPUTED_SIZE);

        var scene = new Scene(root, 1020, 680);
        stage.setTitle("CRUD Grafico");
        stage.setScene(scene);
        stage.show();
    }

    private static ConnectionFields buildConnectionForm(Label statusLabel, Button connectButton) {
        var hostField = new TextField();
        hostField.setPromptText("localhost");
        hostField.setText(Database.DB_HOST);
        hostField.setStyle(FIELD);

        var portField = new TextField();
        portField.setPromptText("5432");
        portField.setText(Database.DB_PORT);
        portField.setStyle(FIELD);

        var nameField = new TextField();
        nameField.setPromptText("nombre_db");
        nameField.setText(Database.DB_NAME);
        nameField.setStyle(FIELD);

        var userField = new TextField();
        userField.setPromptText("usuario");
        userField.setText(Database.DB_USER);
        userField.setStyle(FIELD);

        var passField = new PasswordField();
        passField.setPromptText("contrasena");
        passField.setText(Database.DB_PASSWORD);
        passField.setStyle(FIELD);

        var form = new GridPane();
        form.setHgap(10);
        form.setVgap(8);
        form.addRow(0, new Label("Host"), hostField, new Label("Puerto"), portField);
        form.addRow(1, new Label("Base"), nameField, new Label("Usuario"), userField);
        form.addRow(2, new Label("Clave"), passField, connectButton, statusLabel);

        for (var node : form.getChildren()) {
            if (node instanceof Label label) label.setStyle(LABEL);
        }

        return new ConnectionFields(hostField, portField, nameField, userField, passField, form);
    }

    private static UiState loadTable(CrudController controller, UiState state, String tableName,
                                  TableView<Map<String, Object>> table, VBox fieldsBox,
                                  Label statusLabel, Label tableLabel,
                                  RadioButton readAllRadio, RadioButton readByPkRadio) {
        if (tableName == null || tableName.isBlank()) return state;
        fieldsBox.getChildren().clear();
        table.getItems().clear();
        table.getColumns().clear();
        try {
            var columns = controller.listColumnsInfo(tableName);
            var pk = controller.getPrimaryKey(tableName);
            var pkType = controller.getPrimaryKeyType(tableName, pk);
            var updated = state.withLoadedTable(tableName, columns, pk, pkType, isPkAuto(pk, columns));
            table.getColumns().clear();
            var colCount = Math.max(1, updated.columns().size());
            for (var col : updated.columns()) {
                var c = new TableColumn<Map<String, Object>, Object>(col.name());
                c.setCellValueFactory(data -> new javafx.beans.property.SimpleObjectProperty<>(data.getValue().get(col.name())));
                c.setMinWidth(120);
                c.prefWidthProperty().bind(table.widthProperty().subtract(18).divide(colCount));
                table.getColumns().add(c);
            }
            tableLabel.setText("Tabla: " + tableName);
            statusLabel.setText("Tabla seleccionada: " + tableName);
            buildFields(updated, fieldsBox, controller, table, statusLabel, readAllRadio, readByPkRadio);
            return updated;
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
            return state;
        }
    }

    private static void buildFields(UiState state, VBox fieldsBox, CrudController controller,
                                    TableView<Map<String, Object>> table, Label statusLabel,
                                    RadioButton readAllRadio, RadioButton readByPkRadio) {
        fieldsBox.getChildren().clear();
        state.fields().clear();

        switch (state.action()) {
            case READ -> {
                var modeRow = new HBox(12, readAllRadio, readByPkRadio);
                modeRow.setAlignment(Pos.CENTER_LEFT);
                modeRow.setStyle("-fx-padding: 4 0 4 0;");
                fieldsBox.getChildren().add(modeRow);
                if (hasPk(state)) {
                    addField(state, fieldsBox, state.pk());
                    var pkField = state.fields().get(state.pk());
                    if (pkField != null) {
                        pkField.disableProperty().bind(readAllRadio.selectedProperty());
                    }
                } else {
                    fieldsBox.getChildren().add(new Label("Sin campos para leer"));
                }
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

    private static UiState setAction(UiState state, VBox fieldsBox, Label actionLabel, Action action,
                                     CrudController controller, TableView<Map<String, Object>> table,
                                     Label statusLabel, RadioButton readAllRadio, RadioButton readByPkRadio) {
        var updated = state.withAction(action);
        actionLabel.setText("Accion: " + action.label);
        buildFields(updated, fieldsBox, controller, table, statusLabel, readAllRadio, readByPkRadio);
        return updated;
    }

    private static void addField(UiState state, VBox fieldsBox, String name) {
        var field = new TextField();
        field.setPromptText(name);
        field.setPrefWidth(240);
        field.setStyle(FIELD);
        var label = new Label(name);
        label.setMinWidth(140);
        label.setStyle(LABEL);
        var row = new HBox(8, label, field);
        row.setAlignment(Pos.CENTER_LEFT);
        state.fields().put(name, field);
        fieldsBox.getChildren().add(row);
    }

    private static void runReadAll(CrudController controller, UiState state,
                                   TableView<Map<String, Object>> table, Label statusLabel) {
        try {
            table.getItems().setAll(controller.readTable(state.table()));
            statusLabel.setText("Datos cargados: " + table.getItems().size());
        } catch (SQLException ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private static void runReadByPk(CrudController controller, UiState state,
                                    TableView<Map<String, Object>> table, Label statusLabel) {
        try {
            var pkValueText = getPkValue(state);
            if (pkValueText == null) {
                statusLabel.setText("Ingrese PK para leer");
                return;
            }
            var pkType = (state.pkType() == null || state.pkType().isBlank())
                    ? getColumnType(state, state.pk())
                    : state.pkType();
            var pkValue = parseValue(pkValueText, pkType, statusLabel, "PK");
            if (pkValue == null) return;
            table.getItems().setAll(controller.readRowByPk(state.table(), state.pk(), pkValue));
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
            if (valueText == null || valueText.isBlank()) {
                map.put(key, null);
                continue;
            }
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
