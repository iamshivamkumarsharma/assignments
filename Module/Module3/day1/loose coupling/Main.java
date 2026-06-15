package UI;

import dao.Broker;
import entity.InvestmentService;
import entity.SangramPay;
import entity.ZerodhaBroker;

public class Main {

    public static void main(String[] args) {

        Broker broker =
                new SangramPay();

        InvestmentService service =
                new InvestmentService(
                        broker
                );

        service.invest(
                "TCS",
                10
        );
        service.invest("HONDA",15);
    }
}