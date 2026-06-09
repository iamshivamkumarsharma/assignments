package PaymentGatewaySystem;

class CardPayment extends Payment {

    public CardPayment(double amount) {
        super(amount);
    }

    @Override
    void makePayment() {
        System.out.println("Payment made through Card");
    }
}