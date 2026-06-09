import java.util.List;

public interface TaskDAO {

    void addTask(Task task);

    List<Task> getAllTasks();

    Task getTaskById(int id);

    void markCompleted(int id);

    void deleteTask(int id);
}

