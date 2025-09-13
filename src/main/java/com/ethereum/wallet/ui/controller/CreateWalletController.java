package com.ethereum.wallet.ui.controller;

import com.ethereum.wallet.dto.WalletCreateRequest;
import com.ethereum.wallet.dto.WalletResponse;
import com.ethereum.wallet.service.WalletService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateWalletController {
    
    @FXML private TextField walletNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button createButton;
    @FXML private Button cancelButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    
    private final WalletService walletService;
    private MainController mainController;
    
    @Autowired
    public CreateWalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @FXML
    private void initialize() {
        createButton.setOnAction(e -> createWallet());
        cancelButton.setOnAction(e -> closeDialog());
        
        // Enable create button only when all fields are filled
        createButton.disableProperty().bind(
            walletNameField.textProperty().isEmpty()
            .or(passwordField.textProperty().isEmpty())
            .or(confirmPasswordField.textProperty().isEmpty())
        );
        
        loadingIndicator.setVisible(false);
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    private void createWallet() {
        String walletName = walletNameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        
        // Validate input
        if (walletName.isEmpty()) {
            showError("Wallet name is required");
            return;
        }
        
        if (password.length() < 8) {
            showError("Password must be at least 8 characters long");
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match");
            return;
        }
        
        // Disable UI during creation
        setUIEnabled(false);
        loadingIndicator.setVisible(true);
        statusLabel.setText("Creating wallet...");
        
        WalletCreateRequest request = new WalletCreateRequest(walletName, password);
        
        Task<WalletResponse> task = new Task<WalletResponse>() {
            @Override
            protected WalletResponse call() throws Exception {
                return walletService.createWallet(request);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    WalletResponse wallet = getValue();
                    showSuccess("Wallet created successfully!\n\nAddress: " + wallet.getAddress());
                    
                    if (mainController != null) {
                        mainController.onWalletCreated();
                    }
                    
                    closeDialog();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setUIEnabled(true);
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("Failed to create wallet");
                    showError("Failed to create wallet: " + getException().getMessage());
                });
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void setUIEnabled(boolean enabled) {
        walletNameField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        confirmPasswordField.setDisable(!enabled);
        createButton.setDisable(!enabled);
    }
    
    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
