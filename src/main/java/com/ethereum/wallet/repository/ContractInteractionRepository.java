package com.ethereum.wallet.repository;

import com.ethereum.wallet.entity.ContractInteraction;
import com.ethereum.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractInteractionRepository extends JpaRepository<ContractInteraction, Long> {
    
    List<ContractInteraction> findByWalletOrderByCreatedAtDesc(Wallet wallet);
    
    List<ContractInteraction> findByContractAddressOrderByCreatedAtDesc(String contractAddress);
    
    List<ContractInteraction> findByStatus(ContractInteraction.InteractionStatus status);
    
    Optional<ContractInteraction> findByTransactionHash(String transactionHash);
    
    @Query("SELECT ci FROM ContractInteraction ci WHERE ci.wallet = :wallet AND ci.contractAddress = :contractAddress ORDER BY ci.createdAt DESC")
    List<ContractInteraction> findByWalletAndContractAddress(@Param("wallet") Wallet wallet, 
                                                            @Param("contractAddress") String contractAddress);
    
    @Query("SELECT ci FROM ContractInteraction ci WHERE ci.wallet = :wallet AND ci.functionName = :functionName ORDER BY ci.createdAt DESC")
    List<ContractInteraction> findByWalletAndFunctionName(@Param("wallet") Wallet wallet, 
                                                         @Param("functionName") String functionName);
    
    @Query("SELECT COUNT(ci) FROM ContractInteraction ci WHERE ci.wallet = :wallet AND ci.status = 'SUCCESS'")
    long countSuccessfulInteractionsByWallet(@Param("wallet") Wallet wallet);
}
