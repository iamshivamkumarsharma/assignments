import java.util.ArrayList;
import java.util.List;

public class TaskDAOImpl implements TaskDAO {

    private List<Task> tasks = new ArrayList<>();

    @Override
    public void addTask(Task task) {
        tasks.add(task);
    }

    @Override
    public List<Task> getAllTasks() {
        return tasks;
    }

    @Override
    public Task getTaskById(int id) {

        for(Task task : tasks) {
            if(task.getId() == id) {
                return task;
            }
        }

        return null;
    }

    @Override
    public void markCompleted(int id) {

        Task task = getTaskById(id);

        if(task != null) {
            task.setCompleted(true);
        }
    }

    @Override
    public void deleteTask(int id) {

        tasks.removeIf(task ->
                task.getId() == id);
    }
}
