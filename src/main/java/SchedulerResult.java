import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class SchedulerResult implements Serializable {
    private boolean completed;
    private Date startTime;
    private HashMap<String, HashMap<String, HashMap<Integer, Integer>>> results = new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();

    public SchedulerResult(boolean completed, Date startTime) {
        this.completed = completed;
        this.startTime = startTime;
        results = new HashMap<String, HashMap<String, HashMap<Integer, Integer>>>();
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public HashMap<String, HashMap<String, HashMap<Integer, Integer>>> getResults() {
        return results;
    }

    public HashMap<String, HashMap<Integer, Integer>> getSchedulerMetrics(String schedulerName) {
        return results.get(schedulerName);
    }

    public HashMap<Integer, Integer> getSchedulerMetricValues(String schedulerName, String metric) {
        return results.get(schedulerName).get(metric);
    }

    public void addScheduler(String schedulerName) {
        this.results.put(schedulerName, new HashMap<String, HashMap<Integer, Integer>>());
    }

    public void addSchedulerMetric(String schedulerName, String metricName) {
        this.results.get(schedulerName).put(metricName, new HashMap<Integer, Integer>());
    }

}
