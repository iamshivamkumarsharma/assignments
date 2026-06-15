package dao;

public class InvestmentService {

    private Broker broker;                     //Investment Service does not care
    //which broker is used

    public InvestmentService(
            Broker broker) {                          //Any object implementing Broker
                                                       //  can buy stocks

        this.broker = broker;                 //Constructor Injection
    }

    public void invest(String stock, int quantity) {

        broker.buyStocks(stock, quantity);
    }
}

/*
*Instead of directly depending on a specific class like ZerodhaBroker,
* we depend on an interface (Broker). The interface acts as a contract, allowing different
* implementations to be plugged in without changing the service class.
*
*
*
* */