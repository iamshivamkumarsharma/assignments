//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static void main(String[] args) {

        TaskDAO taskDAO = new TaskDAOImpl();

        taskDAO.addTask(
                new Task(1, "Learn Java"));

        taskDAO.addTask(
                new Task(2, "Learn Collections"));

        taskDAO.addTask(
                new Task(3, "Learn DAO"));

        System.out.println("All Tasks:");

        for(Task task : taskDAO.getAllTasks()) {
            System.out.println(task);
        }

        taskDAO.markCompleted(2);

        System.out.println("\nAfter Completion:");

        for(Task task : taskDAO.getAllTasks()) {
            System.out.println(task);
        }

        taskDAO.deleteTask(1);

        System.out.println("\nAfter Deletion:");

        for(Task task : taskDAO.getAllTasks()) {
            System.out.println(task);
        }
    }
}