import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

public class SchedulerResult implements Serializable {
    private boolean completed;
    private Date startTime;
    //Map: MetricName, Map<SchedulerName, Map<Iteration, Value>>>
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

    public HashMap<String, HashMap<Integer, Integer>> getSchedulersOfMetric(String metricName) {
        return results.get(metricName);
    }

    public HashMap<Integer, Integer> getSchedulerMetricValues(String metricName, String schedulerName) {
        return results.get(metricName).get(schedulerName);
    }

    public void addMetric(String metricName) {
        this.results.put(metricName, new HashMap<String, HashMap<Integer, Integer>>());
    }

    public void addSchedulerForMetric(String metricName, String schedulerName) {
        this.results.get(metricName).put(schedulerName, new HashMap<Integer, Integer>());
    }

}
