package co.unacademy;

//Task interface
public interface Task<T> {
    T onExecuteTask();
    void onTaskComplete(T result);
}
