import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Thread {
    public static void main(String[] args) {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
        exec.scheduleAtFixedRate(new Runnable() {
            int i = 0;

            @Override
            public void run() {
                String timeStamp = new SimpleDateFormat("HH-mm-ss").format(Calendar.getInstance().getTime());
                System.out.println(String.format("Iteration: %d - time: %s", i++, timeStamp));
            }
        }, 0, 5, TimeUnit.SECONDS);
    }
}
