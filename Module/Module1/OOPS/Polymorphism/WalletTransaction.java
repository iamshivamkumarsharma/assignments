package transactionFeeCalculator;

class WalletTransaction extends Transaction {

    public WalletTransaction(double amount) {
        super(amount);
    }

    @Override
    public double calculateFee() {
        return amount * 0.01; // 1%
    }
}