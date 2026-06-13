public class TestDriver {

    public static void main(String[] args) {

        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("Driver Loaded Successfully");
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
