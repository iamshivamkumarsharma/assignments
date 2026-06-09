package Payment;

public class Main {

    public static void main(String[] args) {

        Payment p1 = new UpiPayment(500);
        Payment p2 = new CardPayment(1200);
        Payment p3 = new WalletPayment(300);

        p1.processPayment();
        p2.processPayment();
        p3.processPayment();
    }
}