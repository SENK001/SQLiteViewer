package com.senk.sqliteviewer;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MainApp extends Application {

    private final DatabaseService dbService = new DatabaseService();

    private SplitPane mainSplitPane;
    private SplitPane rightSplitPane;
    private Node resultPanel;
    private TreeView<File> fileTree;
    private ListView<TableItem> tableListView;
    private TableView<List<Object>> dataTableView;
    private TableView<DatabaseService.ColumnInfo> schemaTableView;
    private TableView<List<Object>> resultTableView;
    private TextArea sqlTextArea;
    private Label resultStatusLabel;
    private Label statusBar;

    private int currentPage = 1;
    private int pageSize = 50;
    private long totalRows = 0;
    private String currentTableName;
    private String sortColumn;
    private String sortDirection;
    private boolean restoringSort;
    private Label pageLabel;
    private Button btnFirst, btnPrev, btnNext, btnLast;
    private ComboBox<Integer> pageSizeCombo;

    private Menu fileMenu, aboutMenu, langMenu;
    private MenuItem openFileItem, openFolderItem, exitItem, aboutItem;
    private RadioMenuItem langEnItem, langZhItem;
    private Button btnExecute, btnClear;
    private Label dataPlaceholder, schemaPlaceholder, resultPlaceholder, rowsPerPageLabel;
    private TableColumn<DatabaseService.ColumnInfo, String> schemaColName, schemaColType,
            schemaColNotNull, schemaColDefault, schemaColPK;
    private Tooltip tipFirst, tipPrev, tipNext, tipLast, tipExecute, tipClear, tipClose;
    private Tab schemaTab, allDataTab, sqlQueryTab;

    private Tab formQueryTab;
    private ComboBox<String> formTableCombo;
    private MultiSelectComboBox<String> formColumnsMultiSelect;
    private VBox formConditionsBox;
    private TextField formLimitField;
    private Button formExecuteBtn, formClearBtn, formShowSqlBtn, formAddConditionBtn;
    private Tooltip tipFormExecute, tipFormClear, tipFormShowSql;
    private Label formTableLabel, formColumnsLabel, formConditionsLabel;
    private List<String> lastColumnNames;

    private static final String APP_VERSION = loadAppVersion();

    private static String loadAppVersion() {
        try (var is = MainApp.class.getResourceAsStream(
                "/com/senk/sqliteviewer/version.properties")) {
            if (is != null) {
                var props = new java.util.Properties();
                props.load(is);
                return props.getProperty("app.version", "1.0-SNAPSHOT");
            }
        } catch (Exception ignored) {
        }
        return "1.0-SNAPSHOT";
    }

    @Override
    public void start(Stage stage) {
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar(stage));
        root.setCenter(createMainContent());
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1200, 800);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.F5) {
                onExecuteQuery();
                e.consume();
            }
        });

        I18n.localeProperty().addListener((obs, oldVal, newVal) -> refreshAllTexts());

        stage.setTitle("SQLite Viewer");
        stage.getIcons().add(new Image(
                getClass().getResourceAsStream("/com/senk/sqliteviewer/icon.png")));
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setOnCloseRequest(e -> dbService.close());
        stage.show();
    }

    private void refreshAllTexts() {
        fileMenu.setText(I18n.get("menu.file"));
        openFileItem.setText(I18n.get("menu.open"));
        openFolderItem.setText(I18n.get("menu.openFolder"));
        exitItem.setText(I18n.get("menu.exit"));
        aboutMenu.setText(I18n.get("menu.about"));
        aboutItem.setText(I18n.get("menu.about.title"));
        langMenu.setText(I18n.get("menu.language"));
        langEnItem.setText(I18n.get("menu.lang.en"));
        langZhItem.setText(I18n.get("menu.lang.zh"));

        schemaTab.setText(I18n.get("tab.schema"));
        allDataTab.setText(I18n.get("tab.data"));
        sqlQueryTab.setText(I18n.get("tab.sql"));
        formQueryTab.setText(I18n.get("tab.formQuery"));

        formExecuteBtn.setText(I18n.get("btn.execute"));
        formClearBtn.setText(I18n.get("btn.clear"));
        formShowSqlBtn.setText(I18n.get("form.showSql"));
        formTableLabel.setText(I18n.get("form.table"));
        formColumnsLabel.setText(I18n.get("form.columns"));
        formConditionsLabel.setText(I18n.get("form.conditions"));
        formColumnsMultiSelect.setPromptText(I18n.get("form.columns.all"));
        formLimitField.setPromptText("LIMIT");
        formAddConditionBtn.setText(I18n.get("form.addCondition"));
        tipFormExecute.setText(I18n.get("form.execute.tip"));
        tipFormClear.setText(I18n.get("form.clear.tip"));
        tipFormShowSql.setText(I18n.get("form.showSql.tip"));

        btnExecute.setText(I18n.get("btn.execute"));
        btnClear.setText(I18n.get("btn.clear"));
        sqlTextArea.setPromptText(I18n.get("label.sqlPrompt"));

        dataPlaceholder.setText(I18n.get("label.selectTable"));
        schemaPlaceholder.setText(I18n.get("label.selectTableSchema"));
        resultPlaceholder.setText(I18n.get("label.queryPlaceholder"));
        rowsPerPageLabel.setText(I18n.get("label.rowsPerPage"));

        schemaColName.setText(I18n.get("schema.col.name"));
        schemaColType.setText(I18n.get("schema.col.type"));
        schemaColNotNull.setText(I18n.get("schema.col.notNull"));
        schemaColDefault.setText(I18n.get("schema.col.default"));
        schemaColPK.setText(I18n.get("schema.col.pk"));

        tipFirst.setText(I18n.get("paginate.first"));
        tipPrev.setText(I18n.get("paginate.prev"));
        tipNext.setText(I18n.get("paginate.next"));
        tipLast.setText(I18n.get("paginate.last"));
        tipExecute.setText(I18n.get("btn.execute.tip"));
        tipClear.setText(I18n.get("btn.clear.tip"));
        tipClose.setText(I18n.get("close.tip"));

        if (!dbService.isConnected()) {
            statusBar.setText(I18n.get("label.noDb"));
        }
        if (currentTableName == null) {
            pageLabel.setText(I18n.get("label.noData"));
        }
    }

    private MenuBar createMenuBar(Stage stage) {
        fileMenu = new Menu(I18n.get("menu.file"));

        openFileItem = new MenuItem(I18n.get("menu.open"));
        openFileItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        openFileItem.setOnAction(e -> onOpenFile(stage));
        fileMenu.getItems().add(openFileItem);

        openFolderItem = new MenuItem(I18n.get("menu.openFolder"));
        openFolderItem.setAccelerator(new KeyCodeCombination(KeyCode.O,
                KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        openFolderItem.setOnAction(e -> onOpenFolder(stage));
        fileMenu.getItems().add(openFolderItem);

        fileMenu.getItems().add(new SeparatorMenuItem());

        exitItem = new MenuItem(I18n.get("menu.exit"));
        exitItem.setOnAction(e -> {
            dbService.close();
            Platform.exit();
        });
        fileMenu.getItems().add(exitItem);

        aboutMenu = new Menu(I18n.get("menu.about"));

        aboutItem = new MenuItem(I18n.get("menu.about.title"));
        aboutItem.setOnAction(e -> showAbout());
        aboutMenu.getItems().add(aboutItem);

        langMenu = new Menu(I18n.get("menu.language"));

        ToggleGroup langGroup = new ToggleGroup();

        langEnItem = new RadioMenuItem(I18n.get("menu.lang.en"));
        langEnItem.setToggleGroup(langGroup);
        langEnItem.setSelected(I18n.getLocale() == Locale.ENGLISH);
        langEnItem.setOnAction(e -> I18n.setLocale(Locale.ENGLISH));

        langZhItem = new RadioMenuItem(I18n.get("menu.lang.zh"));
        langZhItem.setToggleGroup(langGroup);
        langZhItem.setSelected(I18n.getLocale() == Locale.SIMPLIFIED_CHINESE);
        langZhItem.setOnAction(e -> I18n.setLocale(Locale.SIMPLIFIED_CHINESE));

        I18n.localeProperty().addListener((obs, old, newVal) -> {
            langEnItem.setSelected(newVal == Locale.ENGLISH);
            langZhItem.setSelected(newVal == Locale.SIMPLIFIED_CHINESE);
        });

        langMenu.getItems().addAll(langEnItem, langZhItem);
        aboutMenu.getItems().add(langMenu);

        return new MenuBar(fileMenu, aboutMenu);
    }

    private SplitPane createMainContent() {
        fileTree = createFileTree();
        fileTree.setVisible(false);
        fileTree.setManaged(false);
        tableListView = createTableListView();
        Node rightPanel = createRightPanel();

        mainSplitPane = new SplitPane();
        mainSplitPane.getItems().addAll(tableListView, rightPanel);
        mainSplitPane.setDividerPositions(0.22);
        return mainSplitPane;
    }

    private TreeView<File> createFileTree() {
        TreeView<File> tree = new TreeView<>();
        tree.setRoot(null);
        tree.setShowRoot(true);
        tree.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(File item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(createIcon(item.isDirectory()
                            ? FontAwesomeSolid.FOLDER : FontAwesomeSolid.DATABASE, 14));
                }
            }
        });

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null && newVal.getValue().isFile()) {
                openDatabase(newVal.getValue());
            }
        });

        return tree;
    }

    private ListView<TableItem> createTableListView() {
        ListView<TableItem> listView = new ListView<>();
        listView.setPlaceholder(new Label(I18n.get("label.noDb")));
        listView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TableItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.getName());
                    setGraphic(createIcon(item.getType() == TableItem.Type.TABLE
                            ? FontAwesomeSolid.TABLE : FontAwesomeSolid.EYE, 14));
                }
            }
        });

        listView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadTableDataView(newVal.getName());
            }
        });

        return listView;
    }

    private Node createRightPanel() {
        rightSplitPane = new SplitPane();
        rightSplitPane.setOrientation(Orientation.VERTICAL);

        TabPane dataTabPane = createDataTabPane();
        resultPanel = createResultPanel();

        rightSplitPane.getItems().addAll(dataTabPane);
        rightSplitPane.setDividerPositions(1.0);
        return rightSplitPane;
    }

    private TabPane createDataTabPane() {
        TabPane tabPane = new TabPane();

        schemaTab = new Tab(I18n.get("tab.schema"));
        schemaTab.setClosable(false);
        schemaTab.setContent(createSchemaTabContent());

        allDataTab = new Tab(I18n.get("tab.data"));
        allDataTab.setClosable(false);
        allDataTab.setContent(createAllDataTabContent());

        sqlQueryTab = new Tab(I18n.get("tab.sql"));
        sqlQueryTab.setClosable(false);
        sqlQueryTab.setContent(createSqlQueryTabContent());

        formQueryTab = new Tab(I18n.get("tab.formQuery"));
        formQueryTab.setClosable(false);
        formQueryTab.setContent(createFormQueryTabContent());

        tabPane.getTabs().addAll(schemaTab, allDataTab, sqlQueryTab, formQueryTab);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == allDataTab) {
                hideResultPanel();
            }
        });

        return tabPane;
    }

    private Node createAllDataTabContent() {
        dataTableView = new TableView<>() {
            @Override
            public void sort() {
                if (restoringSort) return;
                ObservableList<TableColumn<List<Object>, ?>> order = getSortOrder();
                if (order.isEmpty()) {
                    sortColumn = null;
                    sortDirection = null;
                } else {
                    TableColumn<List<Object>, ?> col = order.get(0);
                    sortColumn = col.getText();
                    sortDirection = col.getSortType() == TableColumn.SortType.DESCENDING ? "DESC" : "ASC";
                }
                currentPage = 1;
                loadTableData();
            }
        };
        dataTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        dataPlaceholder = new Label(I18n.get("label.selectTable"));
        dataTableView.setPlaceholder(dataPlaceholder);

        HBox paginationBar = createPaginationBar();

        BorderPane content = new BorderPane();
        content.setCenter(dataTableView);
        content.setBottom(paginationBar);
        return content;
    }

    private HBox createPaginationBar() {
        btnFirst = new Button();
        btnFirst.setGraphic(createIcon(FontAwesomeSolid.ANGLE_DOUBLE_LEFT, 12));
        btnFirst.setOnAction(e -> goToPage(1));
        btnFirst.setDisable(true);
        tipFirst = new Tooltip(I18n.get("paginate.first"));
        btnFirst.setTooltip(tipFirst);

        btnPrev = new Button();
        btnPrev.setGraphic(createIcon(FontAwesomeSolid.ANGLE_LEFT, 12));
        btnPrev.setOnAction(e -> goToPage(currentPage - 1));
        btnPrev.setDisable(true);
        tipPrev = new Tooltip(I18n.get("paginate.prev"));
        btnPrev.setTooltip(tipPrev);

        btnNext = new Button();
        btnNext.setGraphic(createIcon(FontAwesomeSolid.ANGLE_RIGHT, 12));
        btnNext.setOnAction(e -> goToPage(currentPage + 1));
        btnNext.setDisable(true);
        tipNext = new Tooltip(I18n.get("paginate.next"));
        btnNext.setTooltip(tipNext);

        btnLast = new Button();
        btnLast.setGraphic(createIcon(FontAwesomeSolid.ANGLE_DOUBLE_RIGHT, 12));
        btnLast.setOnAction(e -> {
            long totalPages = Math.max(1, (totalRows + pageSize - 1) / pageSize);
            goToPage((int) totalPages);
        });
        btnLast.setDisable(true);
        tipLast = new Tooltip(I18n.get("paginate.last"));
        btnLast.setTooltip(tipLast);

        pageLabel = new Label(I18n.get("label.noData"));
        pageLabel.setMinWidth(120);
        pageLabel.setAlignment(Pos.CENTER);

        pageSizeCombo = new ComboBox<>();
        pageSizeCombo.getItems().addAll(50, 100, 200, 500, 1000);
        pageSizeCombo.setValue(50);
        pageSizeCombo.setPrefWidth(80);
        pageSizeCombo.setEditable(false);
        pageSizeCombo.setOnAction(e -> {
            pageSize = pageSizeCombo.getValue();
            currentPage = 1;
            loadTableData();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        rowsPerPageLabel = new Label(I18n.get("label.rowsPerPage"));

        HBox bar = new HBox(6);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 8, 4, 8));
        bar.getChildren().addAll(
                btnFirst, btnPrev, pageLabel, btnNext, btnLast,
                spacer,
                rowsPerPageLabel, pageSizeCombo);
        return bar;
    }

    private Node createSqlQueryTabContent() {
        ToolBar toolBar = new ToolBar();
        btnExecute = new Button(I18n.get("btn.execute"), createIcon(FontAwesomeSolid.PLAY, 14));
        btnExecute.setOnAction(e -> onExecuteQuery());
        tipExecute = new Tooltip(I18n.get("btn.execute.tip"));
        btnExecute.setTooltip(tipExecute);

        btnClear = new Button(I18n.get("btn.clear"), createIcon(FontAwesomeSolid.TIMES, 14));
        btnClear.setOnAction(e -> sqlTextArea.clear());
        tipClear = new Tooltip(I18n.get("btn.clear.tip"));
        btnClear.setTooltip(tipClear);

        toolBar.getItems().addAll(btnExecute, btnClear);

        sqlTextArea = new TextArea();
        sqlTextArea.setPromptText(I18n.get("label.sqlPrompt"));

        BorderPane content = new BorderPane();
        content.setTop(toolBar);
        content.setCenter(sqlTextArea);
        return content;
    }

    private Node createSchemaTabContent() {
        schemaTableView = new TableView<>();
        schemaTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        schemaPlaceholder = new Label(I18n.get("label.selectTableSchema"));
        schemaTableView.setPlaceholder(schemaPlaceholder);

        TableColumn<DatabaseService.ColumnInfo, Integer> colCid = new TableColumn<>("#");
        colCid.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(cellData.getValue().getCid() + 1).asObject());
        colCid.setPrefWidth(40);
        colCid.setMinWidth(30);

        schemaColName = new TableColumn<>(I18n.get("schema.col.name"));
        schemaColName.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getName()));
        schemaColName.setPrefWidth(150);
        schemaColName.setMinWidth(60);

        schemaColType = new TableColumn<>(I18n.get("schema.col.type"));
        schemaColType.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getType()));
        schemaColType.setPrefWidth(100);
        schemaColType.setMinWidth(50);

        schemaColNotNull = new TableColumn<>(I18n.get("schema.col.notNull"));
        schemaColNotNull.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isNotNull() ? "YES" : "NO"));
        schemaColNotNull.setPrefWidth(60);
        schemaColNotNull.setMinWidth(40);

        schemaColDefault = new TableColumn<>(I18n.get("schema.col.default"));
        schemaColDefault.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDefaultValue() != null
                                ? cellData.getValue().getDefaultValue() : ""));
        schemaColDefault.setPrefWidth(120);
        schemaColDefault.setMinWidth(60);

        schemaColPK = new TableColumn<>(I18n.get("schema.col.pk"));
        schemaColPK.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isPrimaryKey() ? "YES" : ""));
        schemaColPK.setPrefWidth(60);
        schemaColPK.setMinWidth(40);

        schemaTableView.getColumns().addAll(colCid, schemaColName, schemaColType,
                schemaColNotNull, schemaColDefault, schemaColPK);

        BorderPane content = new BorderPane();
        content.setCenter(schemaTableView);
        return content;
    }

    private Node createResultPanel() {
        BorderPane panel = new BorderPane();

        resultStatusLabel = new Label("");
        resultStatusLabel.setMaxWidth(Double.MAX_VALUE);

        Button btnClose = new Button();
        btnClose.setGraphic(createIcon(FontAwesomeSolid.TIMES, 12));
        tipClose = new Tooltip(I18n.get("close.tip"));
        btnClose.setTooltip(tipClose);
        btnClose.setOnAction(e -> hideResultPanel());

        HBox header = new HBox(resultStatusLabel, btnClose);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(2, 4, 2, 4));
        header.setStyle("-fx-background-color: #f0f0f0;");
        HBox.setHgrow(resultStatusLabel, Priority.ALWAYS);

        panel.setTop(header);

        resultTableView = new TableView<>();
        resultTableView.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        resultPlaceholder = new Label(I18n.get("label.queryPlaceholder"));
        resultTableView.setPlaceholder(resultPlaceholder);
        panel.setCenter(resultTableView);

        panel.setMinHeight(100);
        return panel;
    }

    private Label createStatusBar() {
        statusBar = new Label(I18n.get("label.noDb"));
        statusBar.setPadding(new Insets(2, 8, 2, 8));
        statusBar.setMaxWidth(Double.MAX_VALUE);
        statusBar.setStyle("-fx-background-color: #e8e8e8;");
        return statusBar;
    }

    private void onOpenFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(I18n.get("filechooser.title"));
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter(I18n.get("filechooser.sqlite"),
                        "*.db", "*.sqlite", "*.sqlite3", "*.s3db"),
                new FileChooser.ExtensionFilter(I18n.get("filechooser.allFiles"), "*.*"));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            openDatabase(file);
        }
    }

    private void onOpenFolder(Stage stage) {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle(I18n.get("dirchooser.title"));
        File dir = chooser.showDialog(stage);
        if (dir != null) {
            populateFileTree(dir);
        }
    }

    private void openDatabase(File file) {
        if (!SqliteIdentifier.isSqliteFile(file)) {
            showError(I18n.get("error.invalidFile"), I18n.get("error.invalidFile.content"));
            return;
        }

        try {
            dbService.open(file);
            statusBar.setText(file.getAbsolutePath());
            loadTableList();
            clearDataDisplay();
            clearFormQuery();
        } catch (SQLException e) {
            showError(I18n.get("error.database"), I18n.get("error.databaseOpen") + "\n" + e.getMessage());
        }
    }

    private void loadTableList() {
        try {
            List<String> tables = dbService.getTables();
            List<String> views = dbService.getViews();

            ObservableList<TableItem> items = FXCollections.observableArrayList();
            for (String t : tables) {
                items.add(new TableItem(t, TableItem.Type.TABLE));
            }
            for (String v : views) {
                items.add(new TableItem(v, TableItem.Type.VIEW));
            }
            tableListView.setItems(items);

            formTableCombo.getItems().setAll(tables);
            formTableCombo.getItems().addAll(views);
        } catch (SQLException e) {
            showError(I18n.get("error.query"), I18n.get("error.listTables") + "\n" + e.getMessage());
        }
    }

    private void clearDataDisplay() {
        dataTableView.getColumns().clear();
        dataTableView.getItems().clear();
        currentTableName = null;
        totalRows = 0;
        currentPage = 1;
        pageLabel.setText(I18n.get("label.noData"));
        btnFirst.setDisable(true);
        btnPrev.setDisable(true);
        btnNext.setDisable(true);
        btnLast.setDisable(true);
    }

    private void loadTableDataView(String tableName) {
        currentTableName = tableName;
        currentPage = 1;
        sortColumn = null;
        sortDirection = null;
        dataTableView.getSortOrder().clear();
        loadTableData();
        loadSchemaData(tableName);
    }

    private void loadSchemaData(String tableName) {
        if (!dbService.isConnected()) return;
        new Thread(() -> {
            try {
                List<DatabaseService.ColumnInfo> columns = dbService.getColumnInfo(tableName);
                Platform.runLater(() -> {
                    schemaTableView.getItems().clear();
                    schemaTableView.getItems().addAll(columns);
                });
            } catch (SQLException ignored) {
            }
        }).start();
    }

    private void loadTableData() {
        if (currentTableName == null || !dbService.isConnected()) {
            return;
        }

        new Thread(() -> {
            try {
                totalRows = dbService.getTotalCount(currentTableName);
                int offset = (currentPage - 1) * pageSize;
                DatabaseService.QueryResult result = dbService.getPage(
                        currentTableName, offset, pageSize, sortColumn, sortDirection);

                Platform.runLater(() -> {
                    populateDataTable(result);
                    updatePaginationState();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> showError(I18n.get("error.query"), e.getMessage()));
            }
        }).start();
    }

    private void goToPage(int page) {
        long totalPages = Math.max(1, (totalRows + pageSize - 1) / pageSize);
        currentPage = Math.max(1, Math.min(page, (int) totalPages));
        loadTableData();
    }

    private void updatePaginationState() {
        long totalPages = Math.max(1, (totalRows + pageSize - 1) / pageSize);
        pageLabel.setText(I18n.get("label.pageFormat", currentPage, totalPages));
        btnFirst.setDisable(currentPage <= 1);
        btnPrev.setDisable(currentPage <= 1);
        btnNext.setDisable(currentPage >= totalPages);
        btnLast.setDisable(currentPage >= totalPages);
    }

    private void populateDataTable(DatabaseService.QueryResult result) {
        buildDynamicTable(dataTableView, result);
    }

    private void onExecuteQuery() {
        String sql = sqlTextArea.getText().trim();
        if (sql.isEmpty()) {
            setResultStatus(I18n.get("label.resultEmpty"), "#fff3cd");
            showResultPanel();
            return;
        }
        if (!dbService.isConnected()) {
            setResultStatus(I18n.get("label.resultNoDb"), "#fff3cd");
            showResultPanel();
            return;
        }

        new Thread(() -> {
            try {
                DatabaseService.QueryResult result = dbService.executeQuery(sql);
                Platform.runLater(() -> {
                    buildDynamicTable(resultTableView, result);
                    setResultStatus(I18n.get("label.resultOk",
                            result.getRowCount(), result.getExecutionTimeMs()), "#d4edda");
                    showResultPanel();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    setResultStatus(I18n.get("label.resultError", e.getMessage()), "#f8d7da");
                    resultTableView.getColumns().clear();
                    resultTableView.getItems().clear();
                    showResultPanel();
                });
            }
        }).start();
    }

    private void setResultStatus(String text, String color) {
        resultStatusLabel.setText(text);
        resultStatusLabel.setStyle("-fx-font-weight: bold;");
        resultStatusLabel.getParent().setStyle("-fx-background-color: " + color + ";");
    }

    private void showResultPanel() {
        if (!rightSplitPane.getItems().contains(resultPanel)) {
            rightSplitPane.getItems().add(resultPanel);
            rightSplitPane.setDividerPositions(0.72);
        }
    }

    private void hideResultPanel() {
        rightSplitPane.getItems().remove(resultPanel);
    }

    @SuppressWarnings("unchecked")
    private void buildDynamicTable(TableView<List<Object>> tableView,
                                   DatabaseService.QueryResult result) {
        restoringSort = true;

        ObservableList<TableColumn<List<Object>, ?>> oldOrder =
                FXCollections.observableArrayList(tableView.getSortOrder());
        TableColumn.SortType savedType = oldOrder.isEmpty() ? TableColumn.SortType.ASCENDING
                : oldOrder.get(0).getSortType();
        String savedCol = null;
        if (!oldOrder.isEmpty()) {
            savedCol = oldOrder.get(0).getText();
        } else {
            savedCol = sortColumn;
        }

        tableView.getSortOrder().clear();
        tableView.getColumns().clear();
        tableView.getItems().clear();

        if (!result.getColumns().isEmpty()) {
            for (int i = 0; i < result.getColumns().size(); i++) {
                final int colIndex = i;
                TableColumn<List<Object>, String> col = new TableColumn<>(result.getColumns().get(i));
                col.setCellValueFactory(cellData -> {
                    List<Object> row = cellData.getValue();
                    Object val = (colIndex < row.size()) ? row.get(colIndex) : null;
                    String text = val == null ? "" : val.toString();
                    return new SimpleStringProperty(text);
                });
                col.setCellFactory(tc -> {
                    TableCell<List<Object>, String> cell = new TableCell<>() {
                        @Override
                        protected void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);
                            if (empty || item == null) {
                                setText(null);
                                setStyle("");
                            } else {
                                TableRow<?> tableRow = getTableRow();
                                if (tableRow != null && tableRow.getItem() != null) {
                                    List<Object> rowData = (List<Object>) tableRow.getItem();
                                    if (colIndex < rowData.size() && rowData.get(colIndex) == null) {
                                        setText("(NULL)");
                                        setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
                                        return;
                                    }
                                }
                                setText(item);
                                setStyle("");
                            }
                        }
                    };
                    cell.setOnMouseClicked(e -> {
                        if (e.getClickCount() == 2 && !cell.isEmpty()) {
                            String text = cell.getText();
                            if (text != null && !text.isEmpty() && !"(NULL)".equals(text)) {
                                ClipboardContent content = new ClipboardContent();
                                content.putString(text);
                                Clipboard.getSystemClipboard().setContent(content);
                            }
                        }
                    });
                    return cell;
                });
                col.setPrefWidth(140);
                col.setMinWidth(50);
                tableView.getColumns().add(col);
            }

            tableView.getItems().addAll(result.getRows());

            if (savedCol != null && !savedCol.isEmpty()) {
                for (TableColumn<List<Object>, ?> col : tableView.getColumns()) {
                    if (savedCol.equals(col.getText())) {
                        col.setSortType(savedType);
                        tableView.getSortOrder().add(col);
                        break;
                    }
                }
            }
        }

        restoringSort = false;
    }

    private void populateFileTree(File rootDir) {
        TreeItem<File> root = createDirectoryNode(rootDir);
        root.setExpanded(true);
        fileTree.setRoot(root);
        fileTree.setVisible(true);
        fileTree.setManaged(true);
        if (!mainSplitPane.getItems().contains(fileTree)) {
            mainSplitPane.getItems().add(0, fileTree);
            mainSplitPane.setDividerPositions(0.18, 0.32);
        }
    }

    private TreeItem<File> createDirectoryNode(File dir) {
        TreeItem<File> node = new TreeItem<>(dir);
        node.expandedProperty().addListener((obs, wasExpanded, isExpanded) -> {
            if (isExpanded) {
                populateChildren(node);
            }
        });
        node.getChildren().add(new TreeItem<>());
        return node;
    }

    private void populateChildren(TreeItem<File> parent) {
        parent.getChildren().clear();
        File dir = parent.getValue();
        if (dir == null || !dir.isDirectory()) {
            return;
        }

        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }

        Arrays.sort(children, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File child : children) {
            if (child.isDirectory()) {
                parent.getChildren().add(createDirectoryNode(child));
            } else if (SqliteIdentifier.isSqliteFile(child)) {
                parent.getChildren().add(new TreeItem<>(child));
            }
        }
    }

    private Node createIcon(FontAwesomeSolid icon, int size) {
        FontIcon fontIcon = new FontIcon(icon);
        fontIcon.setIconSize(size);
        return fontIcon;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(I18n.get("menu.about.title"));
        alert.setHeaderText("SQLite Viewer");

        Label descLabel = new Label(I18n.get("menu.about.content", APP_VERSION));
        descLabel.setWrapText(true);

        Hyperlink githubLink = new Hyperlink("SENK001");
        githubLink.setOnAction(e -> {
            try {
                java.awt.Desktop.getDesktop().browse(
                        new java.net.URI(I18n.get("menu.about.github")));
            } catch (Exception ignored) {
            }
        });

        Label authorLabel = new Label(I18n.get("menu.about.author"));
        HBox authorRow = new HBox(4, authorLabel, githubLink);
        authorRow.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(12, descLabel, authorRow);
        content.setPadding(new Insets(16, 16, 16, 16));
        content.setMaxWidth(400);
        alert.getDialogPane().setContent(content);

        alert.showAndWait();
    }

    private Node createFormQueryTabContent() {
        formExecuteBtn = new Button(I18n.get("btn.execute"), createIcon(FontAwesomeSolid.PLAY, 14));
        formExecuteBtn.setOnAction(e -> onFormExecute());
        tipFormExecute = new Tooltip(I18n.get("form.execute.tip"));
        formExecuteBtn.setTooltip(tipFormExecute);

        formClearBtn = new Button(I18n.get("btn.clear"), createIcon(FontAwesomeSolid.TIMES, 14));
        formClearBtn.setOnAction(e -> clearFormQuery());
        tipFormClear = new Tooltip(I18n.get("form.clear.tip"));
        formClearBtn.setTooltip(tipFormClear);

        formShowSqlBtn = new Button(I18n.get("form.showSql"), createIcon(FontAwesomeSolid.CODE, 14));
        formShowSqlBtn.setOnAction(e -> onShowFormSql());
        tipFormShowSql = new Tooltip(I18n.get("form.showSql.tip"));
        formShowSqlBtn.setTooltip(tipFormShowSql);

        ToolBar toolBar = new ToolBar(formExecuteBtn, formClearBtn, formShowSqlBtn);

        formTableLabel = new Label(I18n.get("form.table"));
        formTableCombo = new ComboBox<>();
        formTableCombo.setPrefWidth(250);
        formTableCombo.setMaxWidth(400);
        formTableCombo.setOnAction(e -> onFormTableSelected());
        HBox tableRow = new HBox(8, formTableLabel, formTableCombo);
        tableRow.setAlignment(Pos.CENTER_LEFT);

        formColumnsLabel = new Label(I18n.get("form.columns"));
        formColumnsMultiSelect = new MultiSelectComboBox<>();
        formColumnsMultiSelect.setPrefWidth(250);
        formColumnsMultiSelect.setMaxWidth(400);
        formColumnsMultiSelect.setPopupWidth(300);
        formColumnsMultiSelect.setPromptText(I18n.get("form.columns.all"));
        HBox columnsRow = new HBox(8, formColumnsLabel, formColumnsMultiSelect);
        columnsRow.setAlignment(Pos.CENTER_LEFT);

        formConditionsLabel = new Label(I18n.get("form.conditions"));
        formConditionsBox = new VBox(4);

        addConditionRow();

        formAddConditionBtn = new Button(I18n.get("form.addCondition"));
        formAddConditionBtn.setGraphic(createIcon(FontAwesomeSolid.PLUS, 10));
        formAddConditionBtn.setOnAction(e -> addConditionRow());

        HBox conditionsHeader = new HBox(8, formConditionsLabel, formAddConditionBtn);
        conditionsHeader.setAlignment(Pos.CENTER_LEFT);

        VBox conditionsSection = new VBox(4, conditionsHeader, formConditionsBox);
        conditionsSection.setPadding(new Insets(4, 0, 4, 0));

        Label limitLabel = new Label("LIMIT:");
        formLimitField = new TextField("1000");
        formLimitField.setPrefWidth(80);
        HBox limitRow = new HBox(8, limitLabel, formLimitField);
        limitRow.setAlignment(Pos.CENTER_LEFT);

        VBox formBox = new VBox(12, tableRow, columnsRow, conditionsSection, limitRow);
        formBox.setPadding(new Insets(12));

        BorderPane content = new BorderPane();
        content.setTop(toolBar);
        content.setCenter(formBox);
        return content;
    }

    private void addConditionRow() {
        ComboBox<String> andOr = new ComboBox<>();
        andOr.getItems().addAll("AND", "OR");
        andOr.setValue("AND");
        andOr.setPrefWidth(65);

        ComboBox<String> field = new ComboBox<>();
        field.setPrefWidth(120);

        ComboBox<String> operator = new ComboBox<>();
        operator.getItems().addAll(
                "=", "!=", ">", "<", ">=", "<=",
                "LIKE", "NOT LIKE",
                "IS NULL", "IS NOT NULL",
                "IN", "NOT IN");
        operator.setValue("=");
        operator.setPrefWidth(110);
        operator.setOnAction(e -> {
            String op = operator.getValue();
            boolean nullOp = "IS NULL".equals(op) || "IS NOT NULL".equals(op);
            TextField val = (TextField) operator.getParent().getChildrenUnmodifiable().get(3);
            val.setDisable(nullOp);
            if (nullOp) {
                val.clear();
            }
        });

        TextField value = new TextField();
        value.setPrefWidth(150);

        Button removeBtn = new Button();
        removeBtn.setGraphic(createIcon(FontAwesomeSolid.MINUS, 10));
        removeBtn.setTooltip(new Tooltip(I18n.get("form.removeCondition")));
        removeBtn.setOnAction(e -> removeConditionRow((HBox) removeBtn.getParent()));

        if (formConditionsBox.getChildren().isEmpty()) {
            andOr.setDisable(true);
            removeBtn.setDisable(true);
        }

        HBox row = new HBox(4, andOr, field, operator, value, removeBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        formConditionsBox.getChildren().add(row);

        if (formConditionsBox.getChildren().size() == 2) {
            HBox first = (HBox) formConditionsBox.getChildren().get(0);
            first.getChildren().get(4).setDisable(false);
        }

        if (lastColumnNames != null && !lastColumnNames.isEmpty()) {
            field.getItems().setAll(lastColumnNames);
        }
    }

    private void removeConditionRow(HBox row) {
        formConditionsBox.getChildren().remove(row);
        HBox first = (HBox) formConditionsBox.getChildren().get(0);
        first.getChildren().get(0).setDisable(true);
        if (formConditionsBox.getChildren().size() == 1) {
            first.getChildren().get(4).setDisable(true);
        }
    }

    private void clearFormQuery() {
        formTableCombo.setValue(null);
        formColumnsMultiSelect.clearChecks();
        formColumnsMultiSelect.setItems(FXCollections.observableArrayList());
        formConditionsBox.getChildren().clear();
        addConditionRow();
        formLimitField.setText("1000");
        lastColumnNames = null;
    }

    private void onFormTableSelected() {
        String table = formTableCombo.getValue();
        if (table == null || !dbService.isConnected()) return;

        new Thread(() -> {
            try {
                List<DatabaseService.ColumnInfo> columns = dbService.getColumnInfo(table);
                List<String> colNames = columns.stream()
                        .map(DatabaseService.ColumnInfo::getName)
                        .collect(Collectors.toList());
                lastColumnNames = colNames;
                Platform.runLater(() -> {
                    formColumnsMultiSelect.clearChecks();
                    formColumnsMultiSelect.setItems(
                            FXCollections.observableArrayList(colNames));
                    formColumnsMultiSelect.setConverter(new javafx.util.StringConverter<>() {
                        @Override
                        public String toString(String s) { return s; }
                        @Override
                        public String fromString(String s) { return s; }
                    });

                    for (Node node : formConditionsBox.getChildren()) {
                        @SuppressWarnings("unchecked")
                        ComboBox<String> field = (ComboBox<String>)
                                ((HBox) node).getChildren().get(1);
                        field.getItems().setAll(colNames);
                    }
                });
            } catch (SQLException ignored) {
            }
        }).start();
    }

    private String buildFormQuery() {
        String table = formTableCombo.getValue();
        if (table == null || table.isEmpty()) return null;

        StringBuilder sql = new StringBuilder("SELECT ");

        ObservableList<String> checked = formColumnsMultiSelect.getCheckedItems();
        if (checked.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(checked.stream()
                    .map(c -> "\"" + c.replace("\"", "\"\"") + "\"")
                    .collect(Collectors.joining(", ")));
        }

        sql.append(" FROM \"").append(table.replace("\"", "\"\"")).append("\"");

        boolean hasWhere = false;
        for (Node node : formConditionsBox.getChildren()) {
            HBox row = (HBox) node;
            ComboBox<String> andOr = (ComboBox<String>) row.getChildren().get(0);
            ComboBox<String> field = (ComboBox<String>) row.getChildren().get(1);
            ComboBox<String> operator = (ComboBox<String>) row.getChildren().get(2);
            TextField value = (TextField) row.getChildren().get(3);

            String fieldName = field.getValue();
            String op = operator.getValue();
            String val = value.getText().trim();

            if (fieldName == null || op == null) continue;

            if (!hasWhere) {
                sql.append(" WHERE ");
                hasWhere = true;
            } else {
                sql.append(" ").append(andOr.getValue()).append(" ");
            }

            sql.append("\"").append(fieldName.replace("\"", "\"\"")).append("\" ");

            if ("IS NULL".equals(op)) {
                sql.append("IS NULL");
            } else if ("IS NOT NULL".equals(op)) {
                sql.append("IS NOT NULL");
            } else if ("IN".equals(op) || "NOT IN".equals(op)) {
                String[] vals = val.split("\\s*,\\s*");
                String joined = Arrays.stream(vals)
                        .filter(v -> !v.isEmpty())
                        .map(v -> "'" + v.replace("'", "''") + "'")
                        .collect(Collectors.joining(", "));
                sql.append(op).append(" (").append(joined).append(")");
            } else {
                sql.append(op).append(" '")
                        .append(val.replace("'", "''"))
                        .append("'");
            }
        }

        String limit = formLimitField.getText().trim();
        if (!limit.isEmpty()) {
            try {
                int n = Integer.parseInt(limit);
                if (n > 0) {
                    sql.append(" LIMIT ").append(n);
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return sql.toString();
    }

    private void onFormExecute() {
        if (!dbService.isConnected()) {
            setResultStatus(I18n.get("label.resultNoDb"), "#fff3cd");
            showResultPanel();
            return;
        }

        String sql = buildFormQuery();
        if (sql == null) {
            setResultStatus(I18n.get("form.noTableSelected"), "#fff3cd");
            showResultPanel();
            return;
        }

        new Thread(() -> {
            try {
                DatabaseService.QueryResult result = dbService.executeQuery(sql);
                Platform.runLater(() -> {
                    buildDynamicTable(resultTableView, result);
                    setResultStatus(I18n.get("form.resultOk",
                            result.getRowCount(), result.getExecutionTimeMs()), "#d4edda");
                    showResultPanel();
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    setResultStatus(I18n.get("label.resultError", e.getMessage()), "#f8d7da");
                    resultTableView.getColumns().clear();
                    resultTableView.getItems().clear();
                    showResultPanel();
                });
            }
        }).start();
    }

    private void onShowFormSql() {
        String sql = buildFormQuery();
        if (sql != null && !sql.isEmpty()) {
            sqlTextArea.setText(sql);
        }
    }

    public static class TableItem {

        public enum Type { TABLE, VIEW }

        private final String name;
        private final Type type;

        public TableItem(String name, Type type) {
            this.name = name;
            this.type = type;
        }

        public String getName() { return name; }

        public Type getType() { return type; }

        @Override
        public String toString() {
            return name + (type == Type.VIEW ? " [view]" : "");
        }
    }
}
