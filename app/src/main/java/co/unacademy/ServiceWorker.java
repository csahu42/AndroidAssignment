package co.unacademy;

import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceWorker<T> {
    private static final String TAG = ServiceWorker.class.getSimpleName();
    private String serviceWorker;
    private static int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    private static final int KEEP_ALIVE_TIME = 1;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private final ExecutorService mExecutorService;
    private final BlockingQueue<Runnable> mTaskQueue;
    private List<Future> mRunningTaskList;
    private volatile Status mStatus = Status.PENDING;

    private final AtomicBoolean mCancelled = new AtomicBoolean();
    private final AtomicBoolean mTaskInvoked = new AtomicBoolean();
    ServiceWorker(String serviceWorker) {
        this.serviceWorker = serviceWorker;
        mTaskQueue = new LinkedBlockingQueue<>();
        mRunningTaskList = new ArrayList<>();
        Log.e(TAG, "Available cores: " + NUMBER_OF_CORES);
        //setting corePoolSize and maximum Pool Size to 1 for the only one task at a time
        mExecutorService = new ThreadPoolExecutor(1, 1, KEEP_ALIVE_TIME,  KEEP_ALIVE_TIME_UNIT, mTaskQueue, new BackgroundThreadFactory());
    }

    void addTask(final Task<T> task) {
        WorkerRunnable<T> mWorker = new WorkerRunnable<T>() {
            public T call() {
                 mTaskInvoked.set(true);
                T result = null;
                try {
                    mStatus = Status.RUNNING;
                    result = task.onExecuteTask();
                } catch (Throwable tr) {
                     mCancelled.set(true);
                    throw tr;
                } finally {
                    mStatus = Status.FINISHED;
                    task.onTaskComplete(result);
                }
                return result;
            }
        };

        Future future = mExecutorService.submit(mWorker);
        mRunningTaskList.add(future);
    }

    public void cancelAllTasks() {
        synchronized (this) {
            mTaskQueue.clear();
            for (Future task : mRunningTaskList) {
                if (!task.isDone()) {
                    task.cancel(true);
                }
            }
            mRunningTaskList.clear();
        }

    }

    /* A ThreadFactory implementation which create new threads for the thread pool.
       The threads created is set to background priority, so it does not compete with the UI thread.
     */
    private static class BackgroundThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable);
            int sTag = 1;
            thread.setName("TaskThread" + sTag);
            thread.setPriority(Process.THREAD_PRIORITY_BACKGROUND);

            // A exception handler is created to log the exception from threads
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    Log.e(TAG, thread.getName() + " encountered an error: " + ex.getMessage());
                }
            });
            return thread;
        }
    }


    private abstract static class WorkerRunnable<T> implements Callable<T> {
    }

    /**
     * Indicates the current status of the task. Each status will be set only once
     * during the lifetime of a task.
     */
    public enum Status {
        PENDING,
        RUNNING,
        FINISHED,
    }
}
