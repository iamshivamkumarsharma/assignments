package AccountDebitCard;

class Account {
    private String accountNumber;
    private DebitCard debitCard;

    public Account(String accountNumber, String cardNumber) {
        this.accountNumber = accountNumber;

        // Account creates and owns the DebitCard
        this.debitCard = new DebitCard(cardNumber);
    }

    public void displayAccount() {
        System.out.println("Account Number: " + accountNumber);
        debitCard.displayCard();
    }
}
