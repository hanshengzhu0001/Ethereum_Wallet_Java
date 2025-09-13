package com.ethereum.wallet.ui.controller;

import com.ethereum.wallet.dto.TransferRequest;
import com.ethereum.wallet.dto.WalletResponse;
import com.ethereum.wallet.service.WalletService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.regex.Pattern;

@Component
public class SendEthController {
    
    @FXML private Label fromWalletLabel;
    @FXML private Label balanceLabel;
    @FXML private TextField toAddressField;
    @FXML private TextField amountField;
    @FXML private TextField gasPriceField;
    @FXML private TextField gasLimitField;
    @FXML private PasswordField passwordField;
    @FXML private Button sendButton;
    @FXML private Button cancelButton;
    @FXML private Button maxButton;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    @FXML private CheckBox customGasCheckBox;
    
    private final WalletService walletService;
    private MainController mainController;
    private WalletResponse wallet;
    
    // Ethereum address pattern
    private static final Pattern ETH_ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");
    
    @Autowired
    public SendEthController(WalletService walletService) {
        this.walletService = walletService;
    }
    
    @FXML
    private void initialize() {
        sendButton.setOnAction(e -> sendEth());
        cancelButton.setOnAction(e -> closeDialog());
        maxButton.setOnAction(e -> setMaxAmount());
        
        // Setup custom gas fields
        customGasCheckBox.setOnAction(e -> toggleCustomGas());
        gasPriceField.setVisible(false);
        gasLimitField.setVisible(false);
        
        // Enable send button only when required fields are filled
        sendButton.disableProperty().bind(
            toAddressField.textProperty().isEmpty()
            .or(amountField.textProperty().isEmpty())
            .or(passwordField.textProperty().isEmpty())
        );
        
        // Add input validation
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                amountField.setText(oldVal);
            }
        });
        
        gasPriceField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*(\\.\\d*)?")) {
                gasPriceField.setText(oldVal);
            }
        });
        
        gasLimitField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                gasLimitField.setText(oldVal);
            }
        });
        
        loadingIndicator.setVisible(false);
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    public void setWallet(WalletResponse wallet) {
        this.wallet = wallet;
        fromWalletLabel.setText(wallet.getName() + " (" + 
            wallet.getAddress().substring(0, 6) + "..." + 
            wallet.getAddress().substring(wallet.getAddress().length() - 4) + ")");
        balanceLabel.setText(String.format("%.6f ETH", wallet.getBalance()));
    }
    
    private void toggleCustomGas() {
        boolean customGas = customGasCheckBox.isSelected();
        gasPriceField.setVisible(customGas);
        gasLimitField.setVisible(customGas);
        
        if (customGas) {
            gasPriceField.setPromptText("Gas price in Gwei (e.g., 20)");
            gasLimitField.setPromptText("Gas limit (e.g., 21000)");
        }
    }
    
    private void setMaxAmount() {
        if (wallet != null) {
            // Reserve some ETH for gas fees (approximately 0.001 ETH)
            BigDecimal maxAmount = wallet.getBalance().subtract(new BigDecimal("0.001"));
            if (maxAmount.compareTo(BigDecimal.ZERO) > 0) {
                amountField.setText(maxAmount.toPlainString());
            } else {
                amountField.setText("0");
            }
        }
    }
    
    private void sendEth() {
        String toAddress = toAddressField.getText().trim();
        String amountStr = amountField.getText().trim();
        String password = passwordField.getText();
        String gasPriceStr = gasPriceField.getText().trim();
        String gasLimitStr = gasLimitField.getText().trim();
        
        // Validate input
        if (!ETH_ADDRESS_PATTERN.matcher(toAddress).matches()) {
            showError("Invalid recipient address format");
            return;
        }
        
        if (toAddress.equalsIgnoreCase(wallet.getAddress())) {
            showError("Cannot send to the same wallet");
            return;
        }
        
        BigDecimal amount;
        try {
            amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Amount must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid amount format");
            return;
        }
        
        if (amount.compareTo(wallet.getBalance()) > 0) {
            showError("Insufficient balance");
            return;
        }
        
        // Disable UI during transfer
        setUIEnabled(false);
        loadingIndicator.setVisible(true);
        statusLabel.setText("Sending transaction...");
        
        TransferRequest request = new TransferRequest(wallet.getId(), toAddress, amount, password);
        
        // Set custom gas parameters if specified
        if (customGasCheckBox.isSelected()) {
            if (!gasPriceStr.isEmpty()) {
                try {
                    request.setGasPrice(new BigDecimal(gasPriceStr));
                } catch (NumberFormatException e) {
                    showError("Invalid gas price format");
                    setUIEnabled(true);
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("");
                    return;
                }
            }
            
            if (!gasLimitStr.isEmpty()) {
                try {
                    request.setGasLimit(Long.parseLong(gasLimitStr));
                } catch (NumberFormatException e) {
                    showError("Invalid gas limit format");
                    setUIEnabled(true);
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("");
                    return;
                }
            }
        }
        
        Task<String> task = new Task<String>() {
            @Override
            protected String call() throws Exception {
                return walletService.transferEth(request);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    String txHash = getValue();
                    showSuccess("Transaction sent successfully!\n\nTransaction Hash: " + txHash + 
                              "\n\nThe transaction is now pending confirmation on the network.");
                    
                    if (mainController != null) {
                        mainController.onTransactionSent();
                    }
                    
                    closeDialog();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setUIEnabled(true);
                    loadingIndicator.setVisible(false);
                    statusLabel.setText("Transaction failed");
                    showError("Failed to send transaction: " + getException().getMessage());
                });
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void setUIEnabled(boolean enabled) {
        toAddressField.setDisable(!enabled);
        amountField.setDisable(!enabled);
        gasPriceField.setDisable(!enabled);
        gasLimitField.setDisable(!enabled);
        passwordField.setDisable(!enabled);
        customGasCheckBox.setDisable(!enabled);
        sendButton.setDisable(!enabled);
        maxButton.setDisable(!enabled);
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
