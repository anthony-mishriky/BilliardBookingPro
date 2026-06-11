package controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import model.BookingSession;

public class DashboardController {
    private StackPane root;
    private ListView<String> historyView;
    private ObservableList<String> historyData;
    private Map<String, BookingSession> activeSessions;
    private Map<String, Button> tableButtons;
    private ScheduledExecutorService timerService;
    private LinkedList<String> recentClients;
    private String selectedTableId = "";

    private int stdCount = 10;
    private int vipCount = 5;
    private double stdRate = 0.0;
    private double vipRate = 0.0;
    private double vipMin = 0.0;
    private boolean useVipMin = false;
    private String currency = "LE";

    public DashboardController() {
        activeSessions = new HashMap<>();
        tableButtons = new HashMap<>();
        recentClients = new LinkedList<>();
        historyData = FXCollections.observableArrayList();
        loadSettings();
        buildUI();
        startLiveTimers();
    }

    private void loadSettings() {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                currency = rs.getString("currency_string");
                stdCount = rs.getInt("std_count");
                vipCount = rs.getInt("vip_count");
            }
        } catch (Exception e) {}
    }

    private void fetchLiveBillingRates() {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT std_rate, vip_rate, vip_min, vip_min_toggle FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                stdRate = rs.getDouble("std_rate");
                vipRate = rs.getDouble("vip_rate");
                vipMin = rs.getDouble("vip_min");
                useVipMin = rs.getInt("vip_min_toggle") == 1;
            }
        } catch (Exception e) {}
    }

    private void buildUI() {
        BorderPane mainLayout = new BorderPane();
        mainLayout.setPadding(new Insets(15));

        VBox centerPanel = new VBox(20);
        centerPanel.getStyleClass().add("bordered-panel");
        
        Label lblStd = new Label("STANDARD TABLES");
        lblStd.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblStd.getStyleClass().add("label");
        GridPane stdGrid = new GridPane();
        stdGrid.setHgap(10); stdGrid.setVgap(10);
        drawGridSection(stdGrid, "STD-", stdCount, false);

        Label lblVip = new Label("VIP TABLES");
        lblVip.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblVip.getStyleClass().add("label");
        GridPane vipGrid = new GridPane();
        vipGrid.setHgap(10); vipGrid.setVgap(10);
        drawGridSection(vipGrid, "VIP-", vipCount, true);

        centerPanel.getChildren().addAll(lblStd, stdGrid, lblVip, vipGrid);
        
        ScrollPane scroll = new ScrollPane(centerPanel);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-control-inner-background: transparent;");
        mainLayout.setCenter(scroll);

        VBox rightPanel = new VBox(15);
        rightPanel.setPrefWidth(280);
        rightPanel.getStyleClass().add("bordered-panel");
        BorderPane.setMargin(rightPanel, new Insets(0, 0, 0, 15));
        
        Label lblControls = new Label("CONTROLS");
        lblControls.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblControls.getStyleClass().add("label");
        
        Button btnStart = new Button("Start Session");
        Button btnCheckout = new Button("Checkout");
        btnStart.setMaxWidth(Double.MAX_VALUE);
        btnCheckout.setMaxWidth(Double.MAX_VALUE);
        btnStart.getStyleClass().add("action-btn");
        btnCheckout.getStyleClass().add("action-btn");

        Label lblData = new Label("SESSION DATA");
        lblData.setFont(Font.font("System", FontWeight.BOLD, 16));
        lblData.getStyleClass().add("label");
        VBox.setMargin(lblData, new Insets(15, 0, 0, 0)); 
        
        Button btnHistory = new Button("Load Daily History");
        Button btnClear = new Button("Clear Data");
        btnHistory.setMaxWidth(Double.MAX_VALUE);
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnHistory.getStyleClass().add("action-btn");
        btnClear.getStyleClass().add("action-btn");

        historyView = new ListView<>(historyData);
        historyView.setPrefHeight(350); 
        
        rightPanel.getChildren().addAll(
            lblControls, btnStart, btnCheckout, 
            lblData, btnHistory, btnClear, historyView
        );
        mainLayout.setRight(rightPanel);

        Label watermark = new Label("@anthony_mishriky");
        watermark.getStyleClass().add("watermark");
        
        root = new StackPane(mainLayout, watermark);
        StackPane.setAlignment(watermark, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(watermark, new Insets(0, 40, 35, 0));

        btnStart.setOnAction(e -> {
            if (!selectedTableId.isEmpty() && !activeSessions.containsKey(selectedTableId)) {
                startSession(selectedTableId);
            }
        });

        btnCheckout.setOnAction(e -> {
            if (!selectedTableId.isEmpty() && activeSessions.containsKey(selectedTableId)) {
                checkoutSession(selectedTableId);
            }
        });

        btnHistory.setOnAction(e -> loadTodayHistory());
        btnClear.setOnAction(e -> historyData.clear());
    }

    private void drawGridSection(GridPane grid, String prefix, int count, boolean isVip) {
        int col = 0;
        int row = 0;
        for (int i = 1; i <= count; i++) {
            String tId = prefix + i;
            Button btn = new Button(tId + "\nAvailable");
            btn.setPrefSize(110, 90);
            btn.getStyleClass().add("table-btn");
            btn.getStyleClass().add(isVip ? "table-vip" : "table-std");
            
            btn.setOnAction(e -> {
                selectedTableId = tId;
                updateButtonStyles();
            });
            
            tableButtons.put(tId, btn);
            grid.add(btn, col, row);
            col++;
            if (col == 6) { col = 0; row++; }
        }
    }

    private void updateButtonStyles() {
        for (Map.Entry<String, Button> entry : tableButtons.entrySet()) {
            String tId = entry.getKey();
            Button btn = entry.getValue();
            
            btn.getStyleClass().removeAll("table-std", "table-vip", "table-occupied", "table-selected");
            
            if (activeSessions.containsKey(tId)) {
                btn.getStyleClass().add("table-occupied");
            } else if (tId.startsWith("VIP")) {
                btn.getStyleClass().add("table-vip");
            } else {
                btn.getStyleClass().add("table-std");
            }
            
            if (tId.equals(selectedTableId)) {
                btn.getStyleClass().add("table-selected");
            }
        }
    }

    private void startSession(String tableId) {
        boolean isVip = tableId.startsWith("VIP");
        String clientName = "No Name";
        
        boolean shouldPrompt = false;
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement("SELECT client_name_toggle FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) shouldPrompt = rs.getInt("client_name_toggle") == 1;
        } catch (Exception e) {}

        if (shouldPrompt) {
            String modalResult = showClientModal();
            if (modalResult == null) return; 
            clientName = modalResult;
        }

        BookingSession session = new BookingSession(tableId, clientName, isVip);
        activeSessions.put(tableId, session);
        updateButtonStyles();
        saveActiveShift(session);
    }

    private void checkoutSession(String tableId) {
        fetchLiveBillingRates();
        BookingSession session = activeSessions.remove(tableId);
        session.setEndTime(LocalDateTime.now());
        
        double minutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
        session.setDurationMinutes(minutes);
        
        double rate = session.isVip() ? vipRate : stdRate;
        double cost = (rate / 60.0) * minutes;
        
        if (session.isVip() && useVipMin && cost < vipMin) {
            cost = vipMin;
        }
        
        session.setFinalCharge(Math.round(cost * 100.0) / 100.0);
        
        saveHistoricalArchive(session);
        deleteActiveShift(tableId);
        
        Button btn = tableButtons.get(tableId);
        btn.setText(tableId + "\nAvailable");
        updateButtonStyles();
        
        String record = String.format("%s - %s: %.2f %s", session.getTableId(), session.getClientName(), session.getFinalCharge(), currency);
        historyData.add(record);
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Checkout Complete");
        alert.setHeaderText("Table: " + tableId);
        alert.setContentText(String.format("Time: %.0f mins\nTotal Cost: %.2f %s", minutes, session.getFinalCharge(), currency));
        alert.showAndWait();
    }

    private String showClientModal() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Client Name");
        
        VBox box = new VBox(15);
        box.setPadding(new Insets(15));
        
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList(recentClients));
        combo.setEditable(true);
        combo.setPromptText("Enter or select name");
        
        Button btnStart = new Button("Confirm Name");
        Button btnSkip = new Button("Skip (No Name)");
        
        btnStart.getStyleClass().add("action-btn");
        btnSkip.getStyleClass().add("action-btn");
        
        final String[] result = new String[1];
        
        btnStart.setOnAction(e -> {
            String name = combo.getEditor().getText();
            if (name != null && !name.trim().isEmpty()) {
                result[0] = name.trim();
                if (!recentClients.contains(result[0])) {
                    recentClients.addFirst(result[0]);
                    if (recentClients.size() > 5) recentClients.removeLast();
                }
                modal.close();
            }
        });
        
        btnSkip.setOnAction(e -> {
            result[0] = "No Name";
            modal.close();
        });
        
        HBox buttonBox = new HBox(10, btnStart, btnSkip);
        
        box.getChildren().addAll(new Label("Enter Client Name:"), combo, buttonBox);
        Scene modalScene = new Scene(box, 320, 150);
        modal.setScene(modalScene);
        modal.showAndWait();
        
        return result[0];
    }

    private void loadTodayHistory() {
        historyData.clear();
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        String today = LocalDate.now().toString();
        String sql = "SELECT * FROM historical_archives WHERE start_timestamp >= ?";
        
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, today);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String record = String.format("%s - %s: %.2f %s", 
                        rs.getString("table_id"), rs.getString("client_name"), rs.getDouble("final_charge"), currency);
                historyData.add(record);
            }
        } catch (Exception e) {}
    }

    private void startLiveTimers() {
        timerService = Executors.newSingleThreadScheduledExecutor();
        timerService.scheduleAtFixedRate(() -> {
            Platform.runLater(() -> {
                LocalDateTime now = LocalDateTime.now();
                for (Map.Entry<String, BookingSession> entry : activeSessions.entrySet()) {
                    String tId = entry.getKey();
                    BookingSession session = entry.getValue();
                    Duration d = Duration.between(session.getStartTime(), now);
                    String timeStr = String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
                    
                    Button btn = tableButtons.get(tId);
                    if (btn != null) {
                        btn.setText(tId + "\n" + session.getClientName() + "\n" + timeStr);
                    }
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    private void saveActiveShift(BookingSession s) {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        String sql = "INSERT INTO active_shift (table_id, client_name, start_timestamp) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getTableId());
            pstmt.setString(2, s.getClientName());
            pstmt.setString(3, s.getStartTime().toString());
            pstmt.executeUpdate();
        } catch (Exception e) {}
    }

    private void deleteActiveShift(String tableId) {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        String sql = "DELETE FROM active_shift WHERE table_id = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableId);
            pstmt.executeUpdate();
        } catch (Exception e) {}
    }

    private void saveHistoricalArchive(BookingSession s) {
        String url = "jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db";
        String sql = "INSERT INTO historical_archives (table_id, client_name, start_timestamp, end_timestamp, duration_minutes, final_charge) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, s.getTableId());
            pstmt.setString(2, s.getClientName());
            pstmt.setString(3, s.getStartTime().toString());
            pstmt.setString(4, s.getEndTime().toString());
            pstmt.setDouble(5, s.getDurationMinutes());
            pstmt.setDouble(6, s.getFinalCharge());
            pstmt.executeUpdate();
        } catch (Exception e) {}
    }

    public StackPane getRoot() { return root; }
    
    public void shutdown() {
        if (timerService != null) timerService.shutdown();
    }
}