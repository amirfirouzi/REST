import org.json.JSONArray;
import org.json.JSONObject;
import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


public class topologyStat {
    static ScheduledFuture<?> t;
    static String host = "stormmaster";
    static String port = "8080";
    static int iteration = 0;
    static int MaxEvaluationCount = 2;
    static SchedulerResult schedulerResults;
    static int evaluationPeriod = 50;
    static int samplingGap = 10;

    public static void main(String[] args) {
        schedulerResults = deserializeMap("results.ser");
//        serializeMap(schedulerResults,"results.ser");
        if (schedulerResults == null) {
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            t = exec.scheduleAtFixedRate(new MetricTask(), 0, 1, TimeUnit.SECONDS);
//        schedulerResults.setCompleted(true);
//        serializeMap(schedulerResults,"results.ser");
        } else if (schedulerResults.isCompleted()) {
            drawCharts();
            //TODO: clear current serialized data
        } else {
            ScheduledExecutorService metricExec = Executors.newSingleThreadScheduledExecutor();
            metricExec.scheduleAtFixedRate(new MetricTask(), 0, 1, TimeUnit.SECONDS);
        }
    }

    public static void drawCharts() {
        Map<String, ArrayList<Integer>> points = new HashMap<String, ArrayList<Integer>>();
        for (Map.Entry<String, HashMap<String, HashMap<Integer, Number>>> metric :
                schedulerResults.getResults().entrySet()) {

            // Create Chart
            CategoryChart chart = new CategoryChartBuilder()
                    .width(1366).height(768)
                    .theme(Styler.ChartTheme.GGPlot2)
                    .title(metric.getKey()).xAxisTitle("Time").yAxisTitle("Tuples").build();
            // Customize Chart
            chart.getStyler().setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
            chart.getStyler().setXAxisLabelRotation(270);
            chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
            chart.getStyler().setAvailableSpaceFill(0);
            chart.getStyler().setOverlapped(true);

            boolean sampledXAxis = false;
            System.out.println(String.format("\nMetric: %s", metric.getKey()));
            //TODO: make it a for loop to effect SamplingGap variable
            for (Map.Entry<String, HashMap<Integer, Number>> scheduler :
                    schedulerResults.getSchedulersOfMetric(metric.getKey()).entrySet()) {

                List<Integer> x = new ArrayList<Integer>();
                List<Number> y = new ArrayList<Number>();
                System.out.println(String.format("\tScheduler: %s\n\t\tValues:", scheduler.getKey()));

                for (Map.Entry<Integer, Number> value :
                        schedulerResults.getSchedulerMetricValues(metric.getKey(), scheduler.getKey()).entrySet()) {
                    x.add(value.getKey());
                    y.add(value.getValue());
                    System.out.println(String.format("\t\t\tUptime: %d, Value: %f", value.getKey(), value.getValue()));
                }
                if (!sampledXAxis) {
                    chart.addSeries(scheduler.getKey(), x, y);
                    sampledXAxis = true;
                } else {
                    sampledXAxis = false;
                    chart.addSeries(scheduler.getKey(), x, y);
                    new SwingWrapper<CategoryChart>(chart).displayChart();
                    try {
                        BitmapEncoder.saveBitmap(chart, "./charts/" + metric.getKey(), BitmapEncoder.BitmapFormat.PNG);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class MetricTask implements Runnable {
        JSONObject topologiesObject = getJsonObject("api/v1/topology/summary");
        JSONArray topologiesArray = topologiesObject.getJSONArray("topologies");

        @Override
        public void run() {
            for (int i = 0; i < topologiesArray.length(); i++) {
                JSONObject jsonObject = topologiesArray.getJSONObject(i);
                String topoId = jsonObject.getString("id");
                String topoStatus = jsonObject.getString("status");

                if (!topoStatus.toLowerCase().equals("active"))
                    continue;
                JSONObject theTopologyObject = getJsonObject(String.format("api/v1/topology/%s", topoId));

                int uptimeSecond = theTopologyObject.getInt("uptimeSeconds");
                JSONObject configurationObject = theTopologyObject.getJSONObject("configuration");

                Date timeStamp = Calendar.getInstance().getTime();
                String schedulerName = configurationObject.getString("storm.scheduler");
                String[] elements = schedulerName.split(Pattern.quote("."));
                schedulerName = elements[elements.length - 1];
                int emitted = getTopologyStatMetrics(theTopologyObject, "emitted");
                if (emitted != -1) {
                    iteration++;
                    int transferred = getTopologyStatMetrics(theTopologyObject, "transferred");
                    double processLatency = getBoltMetrics(theTopologyObject, "processLatency");
                    double executeLatency = getBoltMetrics(theTopologyObject, "executeLatency");
                    int executed = getBoltMetricsInt(theTopologyObject, "executed");
                    double capacity = getBoltMetrics(theTopologyObject, "capacity");

                    fillSchedulerResult("emitted", schedulerName, timeStamp, iteration, emitted, uptimeSecond);
                    fillSchedulerResult("transferred", schedulerName, timeStamp, iteration, transferred, uptimeSecond);
                    fillSchedulerResult("processLatency", schedulerName, timeStamp, iteration, processLatency, uptimeSecond);
                    fillSchedulerResult("executeLatency", schedulerName, timeStamp, iteration, executeLatency, uptimeSecond);
                    fillSchedulerResult("executed", schedulerName, timeStamp, iteration, executed, uptimeSecond);
                    fillSchedulerResult("capacity", schedulerName, timeStamp, iteration, capacity, uptimeSecond);

                    System.out.println(String.format("********************************\nIteration: %d at upTime: %s\n********************************", iteration, uptimeSecond));
                    if (iteration >= evaluationPeriod) {
                        System.out.println("Saving Stats...");
                        serializeMap(schedulerResults, "results.ser");
                        if (schedulerResults.isCompleted())
                            drawCharts();
                        t.cancel(false);
                    }
                } else {
                    System.out.println("metrics are not Available Yet! Waiting..., Uptime: " + uptimeSecond);
                }
            }
        }
    }

    public static int getTopologyStatMetrics(JSONObject theTopologyObject, String metricName) {
        JSONArray topologyStatsArray = theTopologyObject.getJSONArray("topologyStats");
        int metric = 0;
        JSONObject stat;
        for (int j = 0; j < topologyStatsArray.length(); j++) {
            stat = topologyStatsArray.getJSONObject(j);
            if (stat.getString("windowPretty").equals("All time")) {
                if (!stat.isNull(metricName))
                    metric = stat.getInt(metricName);
                else
                    return -1;
            }
        }
        System.out.println(String.format("%s: %d\n", metricName, metric));
        return metric;
    }

    public static double getBoltMetrics(JSONObject theTopologyObject, String metricName) {
        System.out.println(metricName + ":");
        JSONArray boltsArray = theTopologyObject.getJSONArray("bolts");
        double metric = 0;
        JSONObject stat;
        for (int j = 0; j < boltsArray.length(); j++) {
            stat = boltsArray.getJSONObject(j);
            if (!stat.isNull(metricName)) {
                metric += stat.getDouble(metricName);
                System.out.println(String.format("\tbolt:%s - %s:%1.3f", stat.getString("boltId"),
                        metricName, stat.getDouble(metricName)));
            } else
                return -1;
        }
        System.out.println(String.format("Average: %f\n", metric / boltsArray.length()));
        return metric / boltsArray.length();
    }

    public static int getBoltMetricsInt(JSONObject theTopologyObject, String metricName) {
        System.out.println(metricName + ":");
        JSONArray boltsArray = theTopologyObject.getJSONArray("bolts");
        int metric = 0;
        JSONObject stat;
        for (int j = 0; j < boltsArray.length(); j++) {
            stat = boltsArray.getJSONObject(j);
            if (!stat.isNull(metricName)) {
                metric += stat.getInt(metricName);
                System.out.println(String.format("\tbolt:%s - %s:%d", stat.getString("boltId"),
                        metricName, stat.getInt(metricName)));
            } else
                return -1;
        }
        System.out.println(String.format("Average: %d\n", metric / boltsArray.length()));
        return metric;
    }

    public static void fillSchedulerResult(String metricName, String schedulerName, Date timeStamp,
                                           int iteration, double metric, int uptimeSecond) {
        if (metric == -1)
            return;
        //Date timeStamp = new SimpleDateFormat("HH-mm-ss").format(Calendar.getInstance().getTime());
        if (schedulerResults == null)
            schedulerResults = new SchedulerResult(false, timeStamp);

        if (schedulerResults.getSchedulersOfMetric(metricName) != null) {
            if (!schedulerResults.getSchedulersOfMetric(metricName).containsKey(schedulerName)) {
                schedulerResults.addSchedulerForMetric(metricName, schedulerName);
                if (schedulerResults.getSchedulersOfMetric(metricName).size() == MaxEvaluationCount)
                    schedulerResults.setCompleted(true);
            }
        } else {
            schedulerResults.addMetric(metricName);
            schedulerResults.addSchedulerForMetric(metricName, schedulerName);
        }
        schedulerResults.getSchedulerMetricValues(metricName, schedulerName).put(uptimeSecond, metric);
    }

    public static SchedulerResult deserializeMap(String filename) {
        SchedulerResult result = null;
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            result = (SchedulerResult) ois.readObject();
            ois.close();
            fis.close();
            System.out.println("Deserialized HashMap..");
            return result;
        } catch (IOException ioe) {
            System.out.println("File not Found..");
            return null;
        } catch (ClassNotFoundException c) {
            System.out.println("Class not found");
            c.printStackTrace();
            return null;
        }
    }

    public static void serializeMap(SchedulerResult results, String filename) {
        try {
            FileOutputStream fos =
                    new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(results);
            oos.close();
            fos.close();
            System.out.printf("Serialized HashMap data is saved in " + filename);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
//        t.cancel(false);
    }

    public static JSONObject getJsonObject(String requestedURL) {
        String output = "";
        try {
            URL url = new URL(String.format("http://%s:%s/%s", host, port, requestedURL));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));
//            System.out.println("Output from Server .... \n");
            output = br.readLine();
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new JSONObject(output);
    }

}