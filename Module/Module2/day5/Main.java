package ProductJDBC;

import dao.ProductDAO;
import dao.ProductDAOImpl;
import model.Product;

import java.util.List;

public class Main{

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







        Product p1 =
                dao.findById(101);

        System.out.println(
                p1.getName()
        );

        List<Product> products =
                dao.findAll();

        for(Product p2 : products) {

            System.out.println(
                    p2.getId() + " "
                            + p2.getName()
            );
        }
    }
}