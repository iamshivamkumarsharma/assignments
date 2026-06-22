package org.nbfc.assignment3.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "loan_applications")
public class LoanApplication {

    @Id
    @Column(name = "application_id")
    private String applicationId;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "lender_name")
    private String lenderName;

    @Column(name = "loan_type")
    private String loanType;

    @Column(name = "loan_amount")
    private double loanAmount;

    @Column(name = "credit_score")
    private int creditScore;

    protected LoanApplication() {
    }

    @JsonCreator
    public LoanApplication(@JsonProperty("applicationId") String applicationId,
                           @JsonProperty("customerName") String customerName,
                           @JsonProperty("lenderName") String lenderName,
                           @JsonProperty("loanType") String loanType,
                           @JsonProperty("loanAmount") double loanAmount,
                           @JsonProperty("creditScore") int creditScore) {

        this.applicationId = applicationId;
        this.customerName = customerName;
        this.lenderName = lenderName;
        this.loanType = loanType;
        this.loanAmount = loanAmount;
        this.creditScore = creditScore;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getLenderName() {
        return lenderName;
    }

    public String getLoanType() {
        return loanType;
    }

    public double getLoanAmount() {
        return loanAmount;
    }

    public int getCreditScore() {
        return creditScore;
    }
}
