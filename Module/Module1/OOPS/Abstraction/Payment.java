package PaymentGatewaySystem;

abstract class Payment {

    protected double amount;

    public Payment(double amount) {
        this.amount = amount;
    }

    // Abstract method
    abstract void makePayment();

    // Concrete method
    void showAmount() {
        System.out.println("Amount: " + amount);
    }
}