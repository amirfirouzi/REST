import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.IOException;
import java.util.*;

public class DateChart {
    public static void main(String[] args) {

        // Create Chart
        XYChart chart = new XYChartBuilder().width(1024).height(800).title("Millisecond Scale").build();

        // Customize Chart
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideS);
        chart.getStyler().setLegendLayout(Styler.LegendLayout.Horizontal);

        // Series
        Random random = new Random();

        // generate data
//        List<Integer> xData1 = new ArrayList<Integer>();
//        List<Integer> yData1 = new ArrayList<Integer>();
//        List<Integer> xData2 = new ArrayList<Integer>();
//        List<Integer> yData2 = new ArrayList<Integer>();

        Map<Integer,Integer> xData1 = new HashMap<Integer, Integer>();
        Map<Integer,Integer> yData1 = new HashMap<Integer, Integer>();
        Map<Integer,Integer> xData2 = new HashMap<Integer, Integer>();
        Map<Integer,Integer> yData2 = new HashMap<Integer, Integer>();


        int y1 = 20;
        int y2 = 50;
        for (int i = 0; i <= 30; i++) {

            xData1.put(i, i * 10);
            xData2.put(i, i * 10);
            yData1.put(i, (int) (y1 +  Math.random() * i));
            yData2.put(i, (int) (y2 +  Math.random() * i));
        }
        List<Integer> xData1List = new ArrayList<Integer>(xData1.values());
        List<Integer> xData2List = new ArrayList<Integer>(xData2.values());
        List<Integer> yData1List = new ArrayList<Integer>(yData1.values());
        List<Integer> yData2List = new ArrayList<Integer>(yData2.values());

        XYSeries series = chart.addSeries("D1", xData1List, yData1List);
        series.setMarker(SeriesMarkers.NONE);
        chart.addSeries("D2", xData2List, yData2List).setMarker(SeriesMarkers.NONE).setYAxisGroup(1);
        new SwingWrapper<XYChart>(chart).displayChart();
        try {
            BitmapEncoder.saveBitmap(chart, "./Sample_Chart", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
