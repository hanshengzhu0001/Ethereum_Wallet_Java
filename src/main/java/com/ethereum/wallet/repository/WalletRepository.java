package com.ethereum.wallet.repository;

import com.ethereum.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    
    Optional<Wallet> findByAddress(String address);
    
    List<Wallet> findByIsActiveTrue();
    
    @Query("SELECT w FROM Wallet w WHERE w.name LIKE %:name% AND w.isActive = true")
    List<Wallet> findByNameContainingAndIsActiveTrue(@Param("name") String name);
    
    boolean existsByAddress(String address);
    
    @Query("SELECT COUNT(w) FROM Wallet w WHERE w.isActive = true")
    long countActiveWallets();
}
