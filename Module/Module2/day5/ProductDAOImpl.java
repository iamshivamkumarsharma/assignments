

//ProductDAO = What operations exist
//        ProductDAOImpl = How they are implemented

package dao;

import model.Product;
import util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ProductDAOImpl
        implements ProductDAO {

    @Override
    public void save(Product p) {

        String sql =
                "INSERT INTO product VALUES(?,?,?,?,?)";

        try (
                Connection con =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, p.getId());
            ps.setString(2, p.getName());
            ps.setString(3, p.getBrand());
            ps.setDouble(4, p.getPrice());
            ps.setInt(5, p.getQuantity());

            int rows =
                    ps.executeUpdate();   // yaha dhyan do

            System.out.println(
                    rows + " row inserted"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Product findById(int id) {

        String sql =
                "SELECT * FROM product WHERE id = ?";

        try (
                Connection con =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            ResultSet rs =
                    ps.executeQuery();   //yaha dhyan do

            if (rs.next()) {

                return new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("brand"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }





    @Override
    public java.util.List<Product> findAll() {

        List<Product> products =
                new ArrayList<>();

        String sql =
                "SELECT * FROM product";

        try (
                Connection con =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql);

                ResultSet rs =
                        ps.executeQuery()
        ) {

            while (rs.next()) {

                Product p =
                        new Product(
                                rs.getInt("id"),
                                rs.getString("name"),
                                rs.getString("brand"),
                                rs.getDouble("price"),
                                rs.getInt("quantity")
                        );

                products.add(p);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return products;
    }



















    @Override
    public void delete(int id) {
        String sql =
                "DELETE FROM product WHERE id = ?";

        try (
                Connection con =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setInt(1, id);

            int rows =
                    ps.executeUpdate();

            System.out.println(
                    rows + " row deleted"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }






    }

    @Override
    public void update(Product p) {
        String sql =
                """
                UPDATE product
                SET name=?,
                    brand=?,
                    price=?,
                    quantity=?
                WHERE id=?
                """;

        try (
                Connection con =
                        DBUtil.getConnection();

                PreparedStatement ps =
                        con.prepareStatement(sql)
        ) {

            ps.setString(1, p.getName());
            ps.setString(2, p.getBrand());
            ps.setDouble(3, p.getPrice());
            ps.setInt(4, p.getQuantity());
            ps.setInt(5, p.getId());

            int rows =
                    ps.executeUpdate();

            System.out.println(
                    rows + " row updated"
            );

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}