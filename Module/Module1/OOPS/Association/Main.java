package CustomerAndBankAccount;

public class Main {

    public static void main(String[] args) {

        BankAccount account =
                new BankAccount("ACC1001", 50000);

        Customer customer =
                new Customer("Rahul", account);

        customer.displayCustomerDetails();
    }
}
