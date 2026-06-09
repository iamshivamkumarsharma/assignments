package transactionFeeCalculator;

class UPITransaction extends Transaction {

    public UPITransaction(double amount) {
        super(amount);
    }

    @Override
    public double calculateFee() {
        return 0; // No fee
    }
}
