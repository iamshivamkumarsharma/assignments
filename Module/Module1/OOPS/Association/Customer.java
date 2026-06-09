package CustomerAndBankAccount;

class Customer {
    private String name;
    private BankAccount account;

    public Customer(String name, BankAccount account) {
        this.name = name;
        this.account = account;
    }

    public void displayCustomerDetails() {
        System.out.println("Customer Name: " + name);
        account.displayAccount();
    }
}