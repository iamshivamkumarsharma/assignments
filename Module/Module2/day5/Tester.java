package ProductJDBC;

import dao.ProductDAO;
import dao.ProductDAOImpl;
import model.Product;

public class Tester {

    public static void main(String[] args) {

        ProductDAO dao =
                new ProductDAOImpl();

        Product p =
                new Product(
                        101,
                        "Laptop",
                        "Dell",
                        65000,
                        10
                );

        dao.save(p);
    }
}