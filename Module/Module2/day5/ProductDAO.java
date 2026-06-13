package dao;

import model.Product;
import java.util.List;

public interface ProductDAO {

    void save(Product product);

    Product findById(int id);

    List<Product> findAll();

    void delete(int id);

    void update(Product product);
}