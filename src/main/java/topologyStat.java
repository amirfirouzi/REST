import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
        if (schedulerResults.getResults().size() == MaxEvaluationCount) {
            drawCharts();
            //TODO: clear current serialized data
        } else {
//        schedulerResults.setCompleted(true);
//        serializeMap(schedulerResults,"results.ser");
            ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();
            exec.scheduleAtFixedRate(new MyTask(), 0, 8, TimeUnit.SECONDS);
        }
    }

    public static void drawCharts() {

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
                JSONArray topologyStatsArray = theTopologyObject.getJSONArray("topologyStats");
                JSONObject configurationObject = theTopologyObject.getJSONObject("configuration");

                Date timeStamp = Calendar.getInstance().getTime();
                String schedulerName = configurationObject.getString("storm.scheduler");
                String[] elements = schedulerName.split(Pattern.quote("."));
                schedulerName = elements[elements.length - 1];
                String metricName = "emitted";
                JSONObject stat = null;
                int emitted = 0;
                for (int j = 0; j < topologyStatsArray.length(); j++) {
                    stat = topologyStatsArray.getJSONObject(j);
                    if (stat.getString("windowPretty").equals("All time")) {
                        emitted = stat.getInt("emitted");
                    }
                }
//                Date timeStamp = new SimpleDateFormat("HH-mm-ss").format(Calendar.getInstance().getTime());
                if (schedulerResults == null)
                    schedulerResults = new SchedulerResult(false, timeStamp);
                if (!schedulerResults.getResults().containsKey(schedulerName)) {
                    schedulerResults.addScheduler(schedulerName);
                    if (schedulerResults.getResults().size() == MaxEvaluationCount)
                        schedulerResults.setCompleted(true);
                }

                if (!schedulerResults.getSchedulerMetrics(schedulerName).containsKey(metricName))
                    schedulerResults.addSchedulerMetric(schedulerName, metricName);

                schedulerResults.getSchedulerMetricValues(schedulerName, metricName).put(iteration++, emitted);

                System.out.println(String.format("Iteration: %d - metric: %d time: %s", iteration, emitted, timeStamp));

                if (iteration >= 5) {
                    System.out.println("Saving Stats...");
                    serializeMap(schedulerResults, "results.ser");
                }

            }
        }
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
            ioe.printStackTrace();
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