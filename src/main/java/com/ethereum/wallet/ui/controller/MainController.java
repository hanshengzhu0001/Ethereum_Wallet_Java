package com.ethereum.wallet.ui.controller;

import com.ethereum.wallet.dto.WalletResponse;
import com.ethereum.wallet.entity.Transaction;
import com.ethereum.wallet.service.TransactionService;
import com.ethereum.wallet.service.WalletService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@Component
public class MainController implements Initializable {
    
    @FXML private ListView<WalletResponse> walletListView;
    @FXML private Label selectedWalletName;
    @FXML private Label selectedWalletAddress;
    @FXML private Label selectedWalletBalance;
    @FXML private Button createWalletButton;
    @FXML private Button importWalletButton;
    @FXML private Button sendEthButton;
    @FXML private Button refreshButton;
    @FXML private TableView<Transaction> transactionTable;
    @FXML private TableColumn<Transaction, String> txHashColumn;
    @FXML private TableColumn<Transaction, String> fromAddressColumn;
    @FXML private TableColumn<Transaction, String> toAddressColumn;
    @FXML private TableColumn<Transaction, BigDecimal> amountColumn;
    @FXML private TableColumn<Transaction, Transaction.TransactionStatus> statusColumn;
    @FXML private TableColumn<Transaction, LocalDateTime> dateColumn;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Label statusLabel;
    
    private final WalletService walletService;
    private final TransactionService transactionService;
    
    private ObservableList<WalletResponse> walletList = FXCollections.observableArrayList();
    private ObservableList<Transaction> transactionList = FXCollections.observableArrayList();
    
    @Autowired
    public MainController(WalletService walletService, TransactionService transactionService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
    }
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupUI();
        loadWallets();
    }
    
    private void setupUI() {
        // Setup wallet list
        walletListView.setItems(walletList);
        walletListView.setCellFactory(listView -> new WalletListCell());
        walletListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> onWalletSelected(newValue)
        );
        
        // Setup transaction table
        transactionTable.setItems(transactionList);
        txHashColumn.setCellValueFactory(new PropertyValueFactory<>("transactionHash"));
        fromAddressColumn.setCellValueFactory(new PropertyValueFactory<>("fromAddress"));
        toAddressColumn.setCellValueFactory(new PropertyValueFactory<>("toAddress"));
        amountColumn.setCellValueFactory(new PropertyValueFactory<>("amount"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        
        // Format columns
        txHashColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.length() > 10 ? item.substring(0, 10) + "..." : item);
                    setTooltip(new Tooltip(item));
                }
            }
        });
        
        fromAddressColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.length() > 10 ? item.substring(0, 6) + "..." + item.substring(item.length() - 4) : item);
                    setTooltip(new Tooltip(item));
                }
            }
        });
        
        toAddressColumn.setCellFactory(column -> new TableCell<Transaction, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.length() > 10 ? item.substring(0, 6) + "..." + item.substring(item.length() - 4) : item);
                    setTooltip(new Tooltip(item));
                }
            }
        });
        
        amountColumn.setCellFactory(column -> new TableCell<Transaction, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.6f ETH", item));
                }
            }
        });
        
        statusColumn.setCellFactory(column -> new TableCell<Transaction, Transaction.TransactionStatus>() {
            @Override
            protected void updateItem(Transaction.TransactionStatus item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toString());
                    switch (item) {
                        case CONFIRMED -> setStyle("-fx-text-fill: green;");
                        case PENDING -> setStyle("-fx-text-fill: orange;");
                        case FAILED -> setStyle("-fx-text-fill: red;");
                        case CANCELLED -> setStyle("-fx-text-fill: gray;");
                    }
                }
            }
        });
        
        // Setup buttons
        createWalletButton.setOnAction(e -> showCreateWalletDialog());
        importWalletButton.setOnAction(e -> showImportWalletDialog());
        sendEthButton.setOnAction(e -> showSendEthDialog());
        refreshButton.setOnAction(e -> refreshData());
        
        // Initially disable send button
        sendEthButton.setDisable(true);
    }
    
    private void loadWallets() {
        setLoading(true);
        updateStatus("Loading wallets...");
        
        Task<List<WalletResponse>> task = new Task<List<WalletResponse>>() {
            @Override
            protected List<WalletResponse> call() throws Exception {
                return walletService.getAllWallets();
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    walletList.clear();
                    walletList.addAll(getValue());
                    setLoading(false);
                    updateStatus("Wallets loaded successfully");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    setLoading(false);
                    updateStatus("Failed to load wallets: " + getException().getMessage());
                    showError("Failed to load wallets", getException().getMessage());
                });
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void onWalletSelected(WalletResponse wallet) {
        if (wallet != null) {
            selectedWalletName.setText(wallet.getName());
            selectedWalletAddress.setText(wallet.getAddress());
            selectedWalletBalance.setText(String.format("%.6f ETH", wallet.getBalance()));
            sendEthButton.setDisable(false);
            loadTransactions(wallet.getId());
        } else {
            selectedWalletName.setText("No wallet selected");
            selectedWalletAddress.setText("");
            selectedWalletBalance.setText("");
            sendEthButton.setDisable(true);
            transactionList.clear();
        }
    }
    
    private void loadTransactions(Long walletId) {
        updateStatus("Loading transactions...");
        
        Task<List<Transaction>> task = new Task<List<Transaction>>() {
            @Override
            protected List<Transaction> call() throws Exception {
                return transactionService.getWalletTransactions(walletId);
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    transactionList.clear();
                    transactionList.addAll(getValue());
                    updateStatus("Transactions loaded");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    updateStatus("Failed to load transactions");
                });
            }
        };
        
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    
    private void showCreateWalletDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/create-wallet.fxml"));
            Parent root = loader.load();
            
            CreateWalletController controller = loader.getController();
            controller.setMainController(this);
            
            Stage stage = new Stage();
            stage.setTitle("Create New Wallet");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Failed to open create wallet dialog: " + e.getMessage());
        }
    }
    
    private void showImportWalletDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/import-wallet.fxml"));
            Parent root = loader.load();
            
            ImportWalletController controller = loader.getController();
            controller.setMainController(this);
            
            Stage stage = new Stage();
            stage.setTitle("Import Wallet");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Failed to open import wallet dialog: " + e.getMessage());
        }
    }
    
    private void showSendEthDialog() {
        WalletResponse selectedWallet = walletListView.getSelectionModel().getSelectedItem();
        if (selectedWallet == null) {
            showError("No Wallet Selected", "Please select a wallet first.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/send-eth.fxml"));
            Parent root = loader.load();
            
            SendEthController controller = loader.getController();
            controller.setMainController(this);
            controller.setWallet(selectedWallet);
            
            Stage stage = new Stage();
            stage.setTitle("Send ETH");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();
        } catch (IOException e) {
            showError("Error", "Failed to open send ETH dialog: " + e.getMessage());
        }
    }
    
    private void refreshData() {
        loadWallets();
        WalletResponse selectedWallet = walletListView.getSelectionModel().getSelectedItem();
        if (selectedWallet != null) {
            // Refresh selected wallet balance
            Task<WalletResponse> task = new Task<WalletResponse>() {
                @Override
                protected WalletResponse call() throws Exception {
                    return walletService.getWallet(selectedWallet.getId());
                }
                
                @Override
                protected void succeeded() {
                    Platform.runLater(() -> {
                        WalletResponse updatedWallet = getValue();
                        selectedWalletBalance.setText(String.format("%.6f ETH", updatedWallet.getBalance()));
                        loadTransactions(updatedWallet.getId());
                    });
                }
            };
            
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        }
    }
    
    public void onWalletCreated() {
        loadWallets();
    }
    
    public void onTransactionSent() {
        refreshData();
    }
    
    private void setLoading(boolean loading) {
        loadingIndicator.setVisible(loading);
    }
    
    private void updateStatus(String message) {
        statusLabel.setText(message);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Custom cell for wallet list
    private static class WalletListCell extends ListCell<WalletResponse> {
        @Override
        protected void updateItem(WalletResponse wallet, boolean empty) {
            super.updateItem(wallet, empty);
            if (empty || wallet == null) {
                setText(null);
            } else {
                setText(String.format("%s\n%s\nBalance: %.6f ETH", 
                    wallet.getName(), 
                    wallet.getAddress().substring(0, 6) + "..." + wallet.getAddress().substring(wallet.getAddress().length() - 4),
                    wallet.getBalance()));
            }
        }
    }
}
