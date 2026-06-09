package BankCustomerAgg;

public class Main{

    public static void main(String[] args) {

        // Customer exists independently
        Customer c1 = new Customer("Rahul");

        // Bank uses Customer object
        Bank bank = new Bank("ABC Bank", c1);

        bank.displayBankDetails();
    }
}