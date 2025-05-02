package com.recargapay.wallet.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transaction_uuid", columnNames = "uuid")
})
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

    @Column(name = "origin_account_id", nullable = false, length = 100)
    private String originAccountId;
    @Enumerated(EnumType.STRING)
    @Column(name = "origin_account_type", nullable = false, length = 20)
    private AccountType originAccountType;
    @Enumerated(EnumType.STRING)
    @Column(name = "destination_account_type", nullable = false, length = 20)
    private AccountType destinationAccountType;
    @Column(name = "destination_account_id", nullable = false, length = 100)
    private String destinationAccountId;
    @Column(name = "extra_info", length = 300)
    private String extraInfo;

}
