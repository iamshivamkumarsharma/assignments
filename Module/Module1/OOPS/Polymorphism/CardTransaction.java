package transactionFeeCalculator;

class CardTransaction extends Transaction {

    public CardTransaction(double amount) {
        super(amount);
    }

    @Override
    public double calculateFee() {
        return amount * 0.02; // 2% suppose
    }
}
