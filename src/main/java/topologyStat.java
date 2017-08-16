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

    public static void main(String[] args) {
        schedulerResults = deserializeMap("results.ser");
        if (schedulerResults == null) {
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            exec.scheduleAtFixedRate(new MyTask(), 0, 8, TimeUnit.SECONDS);
//        schedulerResults.setCompleted(true);
//        serializeMap(schedulerResults,"results.ser");
        } else if (schedulerResults.isCompleted()) {
            drawCharts();
            //TODO: clear current serialized data
        } else {
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            exec.scheduleAtFixedRate(new MyTask(), 0, 8, TimeUnit.SECONDS);
        }
    }

    public static void drawCharts() {
        Map<String, ArrayList<Integer>> points = new HashMap<String, ArrayList<Integer>>();
        for (Map.Entry<String, HashMap<String, HashMap<Integer, Integer>>> metric :
                schedulerResults.getResults().entrySet()) {

            // Create Chart
            CategoryChart chart = new CategoryChartBuilder().width(800).height(600).theme(Styler.ChartTheme.GGPlot2).title(metric.getKey()).xAxisTitle("Time").yAxisTitle("Tuples").build();
            // Customize Chart
            chart.getStyler().setDefaultSeriesRenderStyle(CategorySeries.CategorySeriesRenderStyle.Line);
            chart.getStyler().setXAxisLabelRotation(270);
            chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
            chart.getStyler().setAvailableSpaceFill(0);
            chart.getStyler().setOverlapped(true);

            boolean sampledXAxis = false;
            System.out.println(String.format("\nMetric: %s", metric.getKey()));
            for (Map.Entry<String, HashMap<Integer, Integer>> scheduler :
                    schedulerResults.getSchedulersOfMetric(metric.getKey()).entrySet()) {

                List<Integer> x = new ArrayList<Integer>();
                List<Integer> y = new ArrayList<Integer>();
                System.out.println(String.format("\tScheduler: %s\n\t\tValues:", scheduler.getKey()));

                for (Map.Entry<Integer, Integer> value :
                        schedulerResults.getSchedulerMetricValues(metric.getKey(), scheduler.getKey()).entrySet()) {
                    x.add(value.getKey());
                    y.add(value.getValue());
                    System.out.println(String.format("\t\t\tIteration: %d, Value: %d", value.getKey(), value.getValue()));
                }
                if (!sampledXAxis) {
                    chart.addSeries(scheduler.getKey(), x, y);
                    sampledXAxis = true;
                } else {
                    sampledXAxis = false;
                    chart.addSeries(scheduler.getKey(), x, y);
                    new SwingWrapper<CategoryChart>(chart).displayChart();
                    try {
                        BitmapEncoder.saveBitmap(chart, "./" + metric.getKey(), BitmapEncoder.BitmapFormat.PNG);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    static class MyTask implements Runnable {
        JSONObject topologiesObject = getJsonObject("api/v1/topology/summary");
        JSONArray topologiesArray = topologiesObject.getJSONArray("topologies");

        @Override
        public void run() {
            for (int i = 0; i < topologiesArray.length(); i++) {
                JSONObject jsonObject = topologiesArray.getJSONObject(i);
                String topoId = jsonObject.getString("id");

                JSONObject theTopologyObject = getJsonObject(String.format("api/v1/topology/%s", topoId));

                int uptimeSecond = theTopologyObject.getInt("uptimeSeconds");
                JSONObject configurationObject = theTopologyObject.getJSONObject("configuration");

                Date timeStamp = Calendar.getInstance().getTime();
                String schedulerName = configurationObject.getString("storm.scheduler");
                String[] elements = schedulerName.split(Pattern.quote("."));
                schedulerName = elements[elements.length - 1];
                int emitted = getTopologyStatMetrics(theTopologyObject, "emitted");
                int transferred = getTopologyStatMetrics(theTopologyObject, "transferred");
                iteration++;
                fillSchedulerResult("emitted", schedulerName, timeStamp, iteration, emitted);
                fillSchedulerResult("transferred", schedulerName, timeStamp, iteration, transferred);

                System.out.println(String.format("Iteration: %d - metric: %d time: %s", iteration, emitted, timeStamp));
                if (iteration >= 10) {
                    System.out.println("Saving Stats...");
                    serializeMap(schedulerResults, "results.ser");
                    if (schedulerResults.isCompleted())
                        drawCharts();
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
                metric = stat.getInt(metricName);
            }
        }
        return metric;
    }

    public static void fillSchedulerResult(String metricName, String schedulerName, Date timeStamp, int iteration, int metric) {
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
        schedulerResults.getSchedulerMetricValues(metricName, schedulerName).put(iteration, metric);
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