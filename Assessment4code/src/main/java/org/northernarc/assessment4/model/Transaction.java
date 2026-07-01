package org.northernarc.assessment4.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long transactionId;

    @Positive(message = "Amount must be positive")
    @Column(nullable = false)
    private double amount;

    @NotBlank(message = "Transaction type is required")
    @Column(nullable = false)
    private String transactionType;

    @NotNull(message = "Transaction date is required")
    @Column(nullable = false)
    private LocalDate transactionDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_number")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private Account account;
}
