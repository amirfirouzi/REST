import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;


public class NetClientGet {

    // http://localhost:8080/RESTfulExample/json/product/get
    public static void main(String[] args) {
        String output = "";
        String host = "stormmaster";
        String port = "8080";

        try {

            URL url = new URL(String.format("http://%s:%s/api/v1/supervisor/summary", host, port));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            System.out.println("Output from Server .... \n");
            output = br.readLine();
//      while ((output = br.readLine()) != null) {
//        System.out.println(output);
//      }

            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        JSONObject obj = new JSONObject(output);
        JSONArray array = obj.getJSONArray("topologies");

        HashMap<String, HashMap<String, Integer>> CPU = new HashMap<String, HashMap<String, Integer>>();
        HashMap<String, HashMap<String, Integer>> RAM = new HashMap<String, HashMap<String, Integer>>();

        for (int i = 0; i < array.length(); i++) {
            JSONObject jsonObject = array.getJSONObject(i);
            String supervisor = jsonObject.getString("host");
            HashMap<String, Integer> cpu = new HashMap<String, Integer>();
            cpu.put("totalCpu", jsonObject.getInt("totalCpu"));
            cpu.put("usedCpu", jsonObject.getInt("usedCpu"));
            CPU.put(supervisor, cpu);

            HashMap<String, Integer> ram = new HashMap<String, Integer>();
            ram.put("totalMem", jsonObject.getInt("totalMem"));
            ram.put("usedMem", jsonObject.getInt("usedMem"));
            RAM.put(supervisor, ram);
        }

        System.out.println();

    }

}