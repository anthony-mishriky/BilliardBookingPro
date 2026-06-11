package view;

import controller.AnalyticsController;
import controller.DashboardController;
import database.DatabaseManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    private DashboardController dashController;
    private AnalyticsController analyticsController;
    private Scene mainScene;
    private TabPane tabPane;

    @Override
    public void start(Stage primaryStage) {
        DatabaseManager.initializeDatabase();
        DatabaseManager.performAutoMaintenance();

        dashController = new DashboardController();
        analyticsController = new AnalyticsController();

        tabPane = new TabPane();
        
        Tab dashTab = new Tab("Dashboard", dashController.getRoot());
        dashTab.setClosable(false);
        
        Tab analyticsTab = new Tab("Analytics", analyticsController.getRoot());
        analyticsTab.setClosable(false);
        
        Tab settingsTab = new Tab("Settings");
        settingsTab.setClosable(false);
        settingsTab.setContent(buildSettingsView());
        
        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (newTab == settingsTab) {
                if (!promptForPin()) {
                    tabPane.getSelectionModel().select(oldTab);
                }
            }
        });

        tabPane.getTabs().addAll(dashTab, analyticsTab, settingsTab);

        mainScene = new Scene(tabPane, 1050, 750);
        mainScene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        
        applyTheme();

        primaryStage.setTitle("Billiard Booking Pro v1.5");
        primaryStage.setScene(mainScene);
        primaryStage.setOnCloseRequest(e -> dashController.shutdown());
        primaryStage.show();
    }

    private boolean promptForPin() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Security Clearance");
        dialog.setHeaderText("Settings is PIN Protected");
        dialog.setContentText("Enter PIN:");
        
        String currentPin = "0000";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db");
             PreparedStatement pstmt = conn.prepareStatement("SELECT pin FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) currentPin = rs.getString("pin");
        } catch (Exception e) {}

        String finalPin = currentPin;
        return dialog.showAndWait().map(input -> input.equals(finalPin)).orElse(false);
    }

    private StackPane buildSettingsView() {
        VBox box = new VBox(15);
        box.setPadding(new Insets(20));

        TextField txtPin = new TextField();
        TextField txtCurrency = new TextField();
        TextField txtStdCount = new TextField();
        TextField txtVipCount = new TextField();
        TextField txtStdRate = new TextField();
        TextField txtVipRate = new TextField();
        TextField txtVipMin = new TextField();
        
        CheckBox chkVip = new CheckBox("Enforce VIP Minimum Charge");
        CheckBox chkClient = new CheckBox("Prompt for Client Name");
        CheckBox chkTheme = new CheckBox("Enable Dark Mode");
        
        chkVip.getStyleClass().add("label");
        chkClient.getStyleClass().add("label");
        chkTheme.getStyleClass().add("label");

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db");
             PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                txtPin.setText(rs.getString("pin"));
                txtCurrency.setText(rs.getString("currency_string"));
                txtStdCount.setText(String.valueOf(rs.getInt("std_count")));
                txtVipCount.setText(String.valueOf(rs.getInt("vip_count")));
                txtStdRate.setText(String.valueOf(rs.getDouble("std_rate")));
                txtVipRate.setText(String.valueOf(rs.getDouble("vip_rate")));
                txtVipMin.setText(String.valueOf(rs.getDouble("vip_min")));
                
                chkVip.setSelected(rs.getInt("vip_min_toggle") == 1);
                chkClient.setSelected(rs.getInt("client_name_toggle") == 1);
                chkTheme.setSelected(rs.getInt("dark_mode_toggle") == 1);
            }
        } catch (Exception e) {}

        Button btnSave = new Button("Save Settings");
        btnSave.getStyleClass().add("action-btn");
        btnSave.setOnAction(e -> {
            String sql = "UPDATE settings SET pin=?, currency_string=?, vip_min_toggle=?, client_name_toggle=?, dark_mode_toggle=?, std_count=?, vip_count=?, std_rate=?, vip_rate=?, vip_min=? WHERE id=1";
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db");
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, txtPin.getText());
                pstmt.setString(2, txtCurrency.getText());
                pstmt.setInt(3, chkVip.isSelected() ? 1 : 0);
                pstmt.setInt(4, chkClient.isSelected() ? 1 : 0);
                pstmt.setInt(5, chkTheme.isSelected() ? 1 : 0);
                
                pstmt.setInt(6, Integer.parseInt(txtStdCount.getText()));
                pstmt.setInt(7, Integer.parseInt(txtVipCount.getText()));
                pstmt.setDouble(8, Double.parseDouble(txtStdRate.getText()));
                pstmt.setDouble(9, Double.parseDouble(txtVipRate.getText()));
                pstmt.setDouble(10, Double.parseDouble(txtVipMin.getText()));
                
                pstmt.executeUpdate();
                applyTheme();
                
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Settings Saved! Restart app to fully apply Dashboard layout changes.");
                alert.showAndWait();
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Please enter valid numbers for counts and rates.");
                alert.showAndWait();
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(15);
        
        Label[] labels = {
            new Label("Admin PIN:"), new Label("Currency:"), 
            new Label("Standard Tables Count:"), new Label("VIP Tables Count:"),
            new Label("Standard Hourly Rate:"), new Label("VIP Hourly Rate:"), new Label("VIP Minimum Charge:")
        };
        for (Label l : labels) l.getStyleClass().add("label");
        
        grid.add(labels[0], 0, 0); grid.add(txtPin, 1, 0);
        grid.add(labels[1], 0, 1); grid.add(txtCurrency, 1, 1);
        grid.add(labels[2], 0, 2); grid.add(txtStdCount, 1, 2);
        grid.add(labels[3], 0, 3); grid.add(txtVipCount, 1, 3);
        grid.add(labels[4], 0, 4); grid.add(txtStdRate, 1, 4);
        grid.add(labels[5], 0, 5); grid.add(txtVipRate, 1, 5);
        grid.add(labels[6], 0, 6); grid.add(txtVipMin, 1, 6);
        
        box.getChildren().addAll(grid, chkVip, chkClient, chkTheme, btnSave);

        Label watermark = new Label("@anthony_mishriky");
        watermark.getStyleClass().add("watermark");
        
        StackPane stackRoot = new StackPane(box, watermark);
        StackPane.setAlignment(watermark, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(watermark, new Insets(0, 40, 35, 0));

        return stackRoot;
    }

    private void applyTheme() {
        boolean isDark = true;
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "/billiard_data_v14.db");
             PreparedStatement pstmt = conn.prepareStatement("SELECT dark_mode_toggle FROM settings WHERE id = 1");
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) isDark = (rs.getInt("dark_mode_toggle") == 1);
        } catch (Exception e) {}

        if (isDark) {
            if (!mainScene.getRoot().getStyleClass().contains("dark-theme")) {
                mainScene.getRoot().getStyleClass().add("dark-theme");
            }
        } else {
            mainScene.getRoot().getStyleClass().remove("dark-theme");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}