package entity;


import dao.Broker;

public class ZerodhaBroker     implements Broker {




    @Override
    public void buyStocks(String stockName, int quantity) {

        System.out.println(
                quantity +
                        " shares of " +
                        stockName +
                        " purchased through Zerodha"
        );

    }
}