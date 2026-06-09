package Payment;

class WalletPayment extends Payment {

    public WalletPayment(double amount) {
        super(amount);
    }

    @Override
    public void processPayment() {
        System.out.println("Wallet Payment Successful:" + amount);
    }
}
