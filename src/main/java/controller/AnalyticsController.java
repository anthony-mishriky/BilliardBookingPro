package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class AnalyticsController {
    private StackPane root;
    private TableView<ArchiveRecord> tableView;
    private ObservableList<ArchiveRecord> tableData;
    private Label lblTotalRev;
    private Label lblTotalHours;
    private String currency = "LE";

    public AnalyticsController() {
        tableData = FXCollections.observableArrayList();
        loadCurrency();
        buildUI();
        loadAnalytics(null, null, "");
    }

    private void loadCurrency() {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT currency_string FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) currency = rs.getString("currency_string");
        } catch (Exception e) {}
    }

    @SuppressWarnings("unchecked")
    private void buildUI() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(20));

        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(15));
        topBar.getStyleClass().add("bordered-panel");
        BorderPane.setMargin(topBar, new Insets(0, 0, 15, 0));
        
        DatePicker dpStart = new DatePicker();
        dpStart.setPromptText("Select Date");
        DatePicker dpEnd = new DatePicker();
        dpEnd.setPromptText("Select Date");
        TextField txtName = new TextField();
        txtName.setPromptText("Search Client Name");
        
        Button btnFilter = new Button("Apply Filters");
        btnFilter.getStyleClass().add("action-btn");
        btnFilter.setOnAction(e -> {
            loadAnalytics(dpStart.getValue(), dpEnd.getValue(), txtName.getText());
        });

        Label lblFrom = new Label("Filter Range From:");
        lblFrom.getStyleClass().add("label");
        Label lblTo = new Label("To:");
        lblTo.getStyleClass().add("label");

        topBar.getChildren().addAll(lblFrom, dpStart, lblTo, dpEnd, txtName, btnFilter);
        mainLayout.setTop(topBar);

        tableView = new TableView<>();
        tableView.setItems(tableData);
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<ArchiveRecord, String> colGame = new TableColumn<>("Game #");
        colGame.setCellValueFactory(new PropertyValueFactory<>("gameNum"));
        
        TableColumn<ArchiveRecord, String> colDate = new TableColumn<>("Date");
        colDate.setCellValueFactory(new PropertyValueFactory<>("date"));

        TableColumn<ArchiveRecord, String> colTable = new TableColumn<>("Table");
        colTable.setCellValueFactory(new PropertyValueFactory<>("tableId"));

        TableColumn<ArchiveRecord, String> colClient = new TableColumn<>("Client");
        colClient.setCellValueFactory(new PropertyValueFactory<>("client"));

        TableColumn<ArchiveRecord, String> colTime = new TableColumn<>("Time (Mins)");
        colTime.setCellValueFactory(new PropertyValueFactory<>("time"));

        TableColumn<ArchiveRecord, String> colCost = new TableColumn<>("Cost");
        colCost.setCellValueFactory(new PropertyValueFactory<>("cost"));

        tableView.getColumns().addAll(colGame, colDate, colTable, colClient, colTime, colCost);
        mainLayout.setCenter(tableView);

        VBox bottomStats = new VBox(5);
        bottomStats.setPadding(new Insets(15));
        bottomStats.getStyleClass().add("bordered-panel");
        BorderPane.setMargin(bottomStats, new Insets(15, 0, 0, 0));
        
        lblTotalRev = new Label("Total Revenue: 0.00 " + currency);
        lblTotalRev.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2ecc71;");
        lblTotalHours = new Label("Total Hours: 0.00");
        lblTotalHours.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #3498db;");
        
        bottomStats.getChildren().addAll(lblTotalHours, lblTotalRev);
        mainLayout.setBottom(bottomStats);

        Label watermark = new Label("@anthony_mishriky");
        watermark.getStyleClass().add("watermark");

        root = new StackPane(mainLayout, watermark);
        StackPane.setAlignment(watermark, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(watermark, new Insets(0, 40, 35, 0));
    }

    private void loadAnalytics(LocalDate start, LocalDate end, String nameFilter) {
        tableData.clear();
        double totalRev = 0;
        double totalMins = 0;

        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        StringBuilder sql = new StringBuilder("SELECT * FROM historical_archives WHERE 1=1");
        
        if (nameFilter != null && !nameFilter.trim().isEmpty()) {
            sql.append(" AND client_name LIKE ?");
        }
        if (start != null) {
            sql.append(" AND start_timestamp >= ?");
        }
        if (end != null) {
            sql.append(" AND start_timestamp < ?");
        }
        
        sql.append(" ORDER BY start_timestamp ASC");

        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            if (nameFilter != null && !nameFilter.trim().isEmpty()) {
                pstmt.setString(paramIndex++, "%" + nameFilter.trim() + "%");
            }
            if (start != null) {
                pstmt.setString(paramIndex++, start.atStartOfDay().toString());
            }
            if (end != null) {
                pstmt.setString(paramIndex++, end.plusDays(1).atStartOfDay().toString());
            }

            ResultSet rs = pstmt.executeQuery();
            
            int gameCounter = 1;
            String currentDateTracker = "";
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

            while (rs.next()) {
                LocalDateTime startDt = LocalDateTime.parse(rs.getString("start_timestamp"));
                String rawDate = startDt.toLocalDate().toString();
                
                if (!rawDate.equals(currentDateTracker)) {
                    currentDateTracker = rawDate;
                    gameCounter = 1; 
                }

                String displayDate = startDt.format(formatter);
                String tId = rs.getString("table_id");
                String client = rs.getString("client_name");
                double mins = rs.getDouble("duration_minutes");
                double charge = rs.getDouble("final_charge");
                
                totalMins += mins;
                totalRev += charge;

                tableData.add(new ArchiveRecord(
                    String.valueOf(gameCounter), 
                    displayDate, 
                    tId, 
                    client, 
                    String.format("%.0f", mins), 
                    String.format("%.2f %s", charge, currency)
                ));
                
                gameCounter++;
            }
        } catch (Exception e) {}

        lblTotalHours.setText(String.format("Total Hours: %.2f", totalMins / 60.0));
        lblTotalRev.setText(String.format("Total Revenue: %.2f %s", totalRev, currency));
    }

    public StackPane getRoot() { return root; }

    public static class ArchiveRecord {
        private final String gameNum, date, tableId, client, time, cost;

        public ArchiveRecord(String gameNum, String date, String tableId, String client, String time, String cost) {
            this.gameNum = gameNum;
            this.date = date;
            this.tableId = tableId;
            this.client = client;
            this.time = time;
            this.cost = cost;
        }

        public String getGameNum() { return gameNum; }
        public String getDate() { return date; }
        public String getTableId() { return tableId; }
        public String getClient() { return client; }
        public String getTime() { return time; }
        public String getCost() { return cost; }
    }
}