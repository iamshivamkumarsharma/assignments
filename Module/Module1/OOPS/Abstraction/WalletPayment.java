package PaymentGatewaySystem;

class WalletPayment extends Payment {

    public WalletPayment(double amount) {
        super(amount);
    }

    @Override
    void makePayment() {
        System.out.println("Payment made through Wallet");
    }
}