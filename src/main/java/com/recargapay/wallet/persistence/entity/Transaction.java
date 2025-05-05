package com.recargapay.wallet.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_uuid", columnNames = "uuid"),
        @UniqueConstraint(name = "uk_external_tx_id", columnNames = {"external_tx_id", "destination_account_id", "source_account_id","wallet_id"})
})
@Getter
@Setter
public class Transaction extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED")
    protected Long id;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, unique = true, length = 36)
    private UUID uuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_wallet"))
    private Wallet wallet;

    @Column(name = "source_account_id", nullable = false, length = 100)
    private String sourceAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_account_type", nullable = false, length = 20)
    private AccountType sourceAccountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_account_type", nullable = false, length = 20)
    private AccountType destinationAccountType;

    @Column(name = "destination_account_id", nullable = false, length = 100)
    private String destinationAccountId;

    @Column(name = "extra_info", length = 300)
    private String extraInfo;
    @Column(name = "external_tx_id", nullable = false, length = 150)
    private String externalTxId;

}
