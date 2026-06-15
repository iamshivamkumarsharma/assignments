package entity;

import dao.Broker;

public class InvestmentService {

    private Broker broker;                     //Investment Service does not care
    //which broker is used

    public InvestmentService(
            Broker broker) {                          //Any object implementing Broker
                                                       //  can buy stocks

        this.broker = broker;                 //Constructor Injection
    }

    public void invest(
            String stock,
            int quantity) {

        broker.buyStocks(
                stock,
                quantity
        );
    }
}