package Payment;

class CardPayment extends Payment {

    public CardPayment(double amount) {
        super(amount);
    }

    @Override
    public void processPayment() {
        System.out.println("Card Payment Successful: rupees" + amount);
    }
}