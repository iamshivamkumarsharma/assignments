package PaymentGatewaySystem;

class UpiPayment extends Payment {

    public UpiPayment(double amount) {
        super(amount);
    }

    @Override
    void makePayment() {
        System.out.println("Payment made through UPI");
    }
}
