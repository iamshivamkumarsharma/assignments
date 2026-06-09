package PaymentGatewaySystem;

public class Main {

    public static void main(String[] args) {

        Payment p1 = new UpiPayment(500);
        Payment p2 = new CardPayment(1000);
        Payment p3 = new WalletPayment(300);

        p1.showAmount();
        p1.makePayment();

        p2.showAmount();
        p2.makePayment();

        p3.showAmount();
        p3.makePayment();
    }
}
