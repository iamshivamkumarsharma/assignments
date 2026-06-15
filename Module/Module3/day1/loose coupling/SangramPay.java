package entity;

import dao.Broker;

public class SangramPay implements Broker {
    @Override
    public void buyStocks(String stockName, int quantity) {
        System.out.println(quantity + stockName + "purchadesd by  sangrampay");
    }
}
