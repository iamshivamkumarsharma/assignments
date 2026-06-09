package Payment;

class UpiPayment extends Payment {

    public UpiPayment(double amount) {
        super(amount);
    }

    @Override
    public void processPayment() {
        System.out.println("UPI Payment Successful: rs" + amount);
    }
}