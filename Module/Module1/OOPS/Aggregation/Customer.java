package BankCustomerAgg;

class Customer {
    private String name;

    public Customer(String name) {
        this.name = name;
    }

    public void display() {
        System.out.println("Customer: " + name);
    }
}