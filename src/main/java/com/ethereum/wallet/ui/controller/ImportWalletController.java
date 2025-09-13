package com.ethereum.wallet.ui.controller;

import com.ethereum.wallet.dto.WalletImportRequest;
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
public class ImportWalletController {
    
    @FXML private TextField walletNameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextArea privateKeyOrMnemonicArea;
    @FXML private PasswordField mnemonicPasswordField;
    @FXML private Label mnemonicPasswordLabel;
    @FXML private Button importButton;
    @FXML private Button cancelButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private RadioButton privateKeyRadio;
    @FXML private RadioButton mnemonicRadio;
    @FXML private ToggleGroup importTypeGroup;
    
    private final WalletService walletService;
    private MainController mainController;
    
    @Autowired
    public ImportWalletController(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @FXML
    private void initialize() {
        importButton.setOnAction(e -> importWallet());
        cancelButton.setOnAction(e -> closeDialog());
        
        // Setup toggle group
        importTypeGroup = new ToggleGroup();
        privateKeyRadio.setToggleGroup(importTypeGroup);
        mnemonicRadio.setToggleGroup(importTypeGroup);
        privateKeyRadio.setSelected(true);
        
        // Show/hide mnemonic password field based on selection
        importTypeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            boolean isMnemonic = newVal == mnemonicRadio;
            mnemonicPasswordField.setVisible(isMnemonic);
            mnemonicPasswordLabel.setVisible(isMnemonic);
            
            if (isMnemonic) {
                privateKeyOrMnemonicArea.setPromptText("Enter 12 or 24 word mnemonic phrase...");
            } else {
                privateKeyOrMnemonicArea.setPromptText("Enter private key (with or without 0x prefix)...");
            }
        });
        
        // Enable import button only when required fields are filled
        importButton.disableProperty().bind(
            walletNameField.textProperty().isEmpty()
            .or(passwordField.textProperty().isEmpty())
            .or(confirmPasswordField.textProperty().isEmpty())
            .or(privateKeyOrMnemonicArea.textProperty().isEmpty())
        );
        
        loadingIndicator.setVisible(false);
        mnemonicPasswordField.setVisible(false);
        mnemonicPasswordLabel.setVisible(false);
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    private void importWallet() {
        String walletName = walletNameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String privateKeyOrMnemonic = privateKeyOrMnemonicArea.getText().trim();
        String mnemonicPassword = mnemonicPasswordField.getText();
        
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
        
        if (privateKeyOrMnemonic.isEmpty()) {
            showError("Private key or mnemonic phrase is required");
            return;
        }
        
        // Disable UI during import
        setUIEnabled(false);
        loadingIndicator.setVisible(true);
        statusLabel.setText("Importing wallet...");
        
        WalletImportRequest request = new WalletImportRequest(walletName, password, privateKeyOrMnemonic);
        if (mnemonicRadio.isSelected() && !mnemonicPassword.isEmpty()) {
            request.setMnemonicPassword(mnemonicPassword);
        }
        
        Task<WalletResponse> task = new Task<WalletResponse>() {
            @Override
            protected WalletResponse call() throws Exception {
                return walletService.importWallet(request);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    WalletResponse wallet = getValue();
                    showSuccess("Wallet imported successfully!\n\nAddress: " + wallet.getAddress() + 
                              "\nBalance: " + wallet.getBalance() + " ETH");
                    
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
                    statusLabel.setText("Failed to import wallet");
                    showError("Failed to import wallet: " + getException().getMessage());
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
        privateKeyOrMnemonicArea.setDisable(!enabled);
        mnemonicPasswordField.setDisable(!enabled);
        privateKeyRadio.setDisable(!enabled);
        mnemonicRadio.setDisable(!enabled);
        importButton.setDisable(!enabled);
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
