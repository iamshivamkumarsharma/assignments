package InterestCalculator;


import java.util.InputMismatchException;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        try {

            System.out.print("Enter Principal Amount ");
            double principal = sc.nextDouble();

            System.out.print("Enter Interest Rate ");
            double rate = sc.nextDouble();

            System.out.print("Enter Time (how amany year) ");
            int years = sc.nextInt();

            validate(principal, rate, years);

            InterestCalculator obj =
                    new InterestCalculator(
                            principal,
                            rate,
                            years);

            System.out.println("Simple Interest = "
                    + obj.calculateSimpleInterest());

            System.out.println("Total Amount = "
                    + obj.totalAmount());

        }

        catch (InvalidInputException e) {
            System.out.println("Custom Exception : "
                    + e.getMessage());
        }

        catch (InputMismatchException e) {
            System.out.println(
                    "Please enter valid numeric values.");
        }


    }

    public static void validate(
            double principal,
            double rate,
            int years)
            throws InvalidInputException {

        if (principal <= 0) {
            throw new InvalidInputException(
                    "Principal amount must be greater than 0");
        }

        if (rate <= 0) {
            throw new InvalidInputException(
                    "Interest rate must be greater than 0");
        }

        if (years <= 0) {
            throw new InvalidInputException(
                    "Years must be greater than 0");
        }
    }
}