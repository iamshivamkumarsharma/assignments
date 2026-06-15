package entity;


import dao.Broker;

public class GrowwBroker
        implements Broker {



    @Override
    public void buyStocks(String stockName, int quantity) {
        System.out.println(" shares of "+ stockName+" purchased by growww");
    }
}