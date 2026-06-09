package transactionFeeCalculator;

public class Main {

    public static void main(String[] args) {

        Transaction[] transactions = {
                new UPITransaction(1000),
                new CardTransaction(1000),
                new WalletTransaction(1000)
        };

        for (Transaction t : transactions) {
            t.display();
            System.out.println("----------------");
        }
    }
}