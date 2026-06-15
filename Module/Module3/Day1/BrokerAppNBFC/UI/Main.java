package UI;

import dao.Broker;
import dao.InvestmentService;
import entity.GrowwBroker;
import entity.NorthernPay;
import entity.SangramPay;
import entity.ZerodhaBroker;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        System.out.println("Choose Investment Platform:");
        System.out.println("1. FOR Zerodha");
        System.out.println("2. for  Groww");
        System.out.println("3. for NorthernPay");
        System.out.println("4. for SangramPay");

        int choice = sc.nextInt();

        Broker broker = null;

        switch (choice) {

            case 1:
                broker = new ZerodhaBroker();
                break;

            case 2:
                broker = new GrowwBroker();
                break;

            case 3:
                broker = new NorthernPay();
                break;

            case 4:
                broker = new SangramPay();
                break;

            default:
                System.out.println("Invalid Choice");
                System.exit(0);
        }

        InvestmentService service = new InvestmentService(broker);

        service.invest("TCS", 10);
        service.invest("HONDA", 15);
        service.invest("BMW", 11);
        service.invest("Vedanta", 12);

        sc.close();
    }
}