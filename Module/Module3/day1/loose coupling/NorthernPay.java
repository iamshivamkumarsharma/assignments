package entity;

import dao.Broker;

public class NorthernPay implements Broker {
    @Override
    public void buyStocks(String stockName, int quantity) {
        System.out.println("share of " + stockName + "purchasd by northern pay");
    }
}
