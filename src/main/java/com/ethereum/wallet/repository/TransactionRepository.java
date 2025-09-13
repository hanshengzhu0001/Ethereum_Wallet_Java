package com.ethereum.wallet.repository;

import com.ethereum.wallet.entity.Transaction;
import com.ethereum.wallet.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    Optional<Transaction> findByTransactionHash(String transactionHash);
    
    List<Transaction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    
    Page<Transaction> findByWalletOrderByCreatedAtDesc(Wallet wallet, Pageable pageable);
    
    List<Transaction> findByStatus(Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.wallet = :wallet AND t.status = :status ORDER BY t.createdAt DESC")
    List<Transaction> findByWalletAndStatus(@Param("wallet") Wallet wallet, @Param("status") Transaction.TransactionStatus status);
    
    @Query("SELECT t FROM Transaction t WHERE t.fromAddress = :address OR t.toAddress = :address ORDER BY t.createdAt DESC")
    List<Transaction> findByFromAddressOrToAddressOrderByCreatedAtDesc(@Param("address") String address);
    
    @Query("SELECT t FROM Transaction t WHERE t.wallet = :wallet AND t.createdAt BETWEEN :startDate AND :endDate ORDER BY t.createdAt DESC")
    List<Transaction> findByWalletAndCreatedAtBetween(@Param("wallet") Wallet wallet, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.wallet = :wallet AND t.status = 'CONFIRMED'")
    long countConfirmedTransactionsByWallet(@Param("wallet") Wallet wallet);
}
