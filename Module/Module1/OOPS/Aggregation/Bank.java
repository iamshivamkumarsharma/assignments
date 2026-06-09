package BankCustomerAgg;

class Bank {
    private String bankName;
    private Customer customer;

    public Bank(String bankName, Customer customer) {
        this.bankName = bankName;
        this.customer = customer;
    }

    public void displayBankDetails() {
        System.out.println("Bank: " + bankName);
        customer.display();
    }
}