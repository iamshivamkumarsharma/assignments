package transactionFeeCalculator;

class Transaction {
    protected double amount;

    public Transaction(double amount) {
        this.amount = amount;
    }

    public double calculateFee() {
        return 0;
    }

    public void display() {
        System.out.println("Amount: " + amount);
        System.out.println("Fee: " + calculateFee());
    }
}