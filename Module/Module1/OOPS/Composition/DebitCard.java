package AccountDebitCard;

class DebitCard {
    private String cardNumber;

    public DebitCard(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public void displayCard() {
        System.out.println("Debit Card Number: " + cardNumber);
    }
}
