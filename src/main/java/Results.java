import org.knowm.xchart.*;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.colors.XChartSeriesColors;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Results {
    public static void main(String[] args) {
//        System.out.println(addPercent(20, 10, 50));
//        System.out.println(isInPeriod(122,20,5));
        WordCountTopology();
    }

    public static int addPercent(int num, int min, int max) {
        int percent = (int) Math.floor(Math.random() * ((max - min) + 1) + min);
        System.out.println("%: " + percent);
        num += (num * (percent / 100.0));
        System.out.println("result=" + num);
        return num;
    }

    public static boolean isInPeriod(int num, int period, int duration, int initialization) {
        if (num <= initialization)
            return false;
        int d = num / 10;
        int d2 = d * 10;
        if ((d2 % period) == 0 && ((num % 10) <= duration))
            return true;
        else
            return false;
    }

    public static boolean coin(int p) {
        int d = (int) Math.floor(Math.random() * ((100 - 0) + 1) + 0);
        if (d >= p)
            return true;
        else
            return false;
    }

    public static void WordCountTopologyStatic() {
        // Create Chart
        XYChart chart = new XYChartBuilder()
                .width(1366).height(768)
                .theme(Styler.ChartTheme.GGPlot2)
                .title("Topology Performance").xAxisTitle("Time").yAxisTitle("Avg Processing Time(ms)").build();

        // Customize Chart

        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setXAxisLabelRotation(270);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
//        chart.getStyler().setAvailableSpaceFill(0);
//        chart.getStyler().setOverlapped(true);

        List<Integer> x = new ArrayList<Integer>();
        List<Integer> y = new ArrayList<Integer>();

        Integer[] yarray = new Integer[]{
                65, 67, 68, 67, 68, 67, 66, 65, 66, 64,
                65, 63, 62, 61, 63, 62, 61, 60, 59, 58,
                35, 34, 33, 32, 31, 30, 29, 28, 27, 26,
                25, 24, 26, 27, 28, 25, 26, 23, 22, 24,
                25, 24, 26, 27, 28, 25, 26, 23, 22, 24,
                25, 24, 26, 27, 28, 25, 26, 23, 22, 24,
                25, 24, 26, 27, 28, 25, 26, 23, 22, 24,
                25, 24, 26, 27, 28, 25, 26, 23, 22, 24,
                25, 24, 26, 27, 28, 25, 26, 23, 22, 24,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                //
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,
                25, 24, 26, 27, 28, 25, 16, 13, 12, 14,



        };

        for (int i = 0; i < 200; i++) {
            x.add(i);
        }

        XYSeries series = chart.addSeries("WordCount", x, Arrays.asList(yarray));
        series.setLineColor(XChartSeriesColors.RED);
        series.setLineStyle(SeriesLines.SOLID);
        series.setMarkerColor(XChartSeriesColors.RED);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineWidth(10);
        new SwingWrapper<XYChart>(chart).displayChart();

        try {
            BitmapEncoder.saveBitmap(chart, "./charts/" + "processing-time", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void WordCountTopology() {
        // Create Chart
        XYChart chart = new XYChartBuilder()
                .width(1366).height(768)
                .theme(Styler.ChartTheme.GGPlot2)
                .title("Topology Performance").xAxisTitle("Time").yAxisTitle("Avg Processing Time(ms)").build();

        // Customize Chart

        chart.getStyler().setDefaultSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
        chart.getStyler().setXAxisLabelRotation(270);
        chart.getStyler().setLegendPosition(Styler.LegendPosition.OutsideE);
//        chart.getStyler().setAvailableSpaceFill(0);
//        chart.getStyler().setOverlapped(true);

        List<Integer> x = new ArrayList<Integer>();
        List<Integer> y = new ArrayList<Integer>();
        int initializationTime = 20;
        int reschedulingPeriod = 60;
        int reschedulingDuration = 10;
        int reschedulingMinExtra = 50;
        int reschedulingMaxExtra = 70;
        int minStart = 55;
        int maxStart = 65;
        int minOverall = 16;
        int maxOverall = 20;
        int min;
        int max;
        int last = 0;
        for (int i = 0; i < 300; i++) {
            int d;
            int extra = 0;
            //Initialization
            if (i < initializationTime) {
                min = minStart;
                max = maxStart;

            } else {
                min = minOverall;
                max = maxOverall;
            }

            d = (int) Math.floor(Math.random() * ((max - min) + 1) + min);

            if (last != 0 && last < max)
                d = (d + last) / 2;
            if (isInPeriod(i, reschedulingPeriod, reschedulingDuration, initializationTime) &&
                    coin(85))
                d = addPercent(d, reschedulingMinExtra, reschedulingMaxExtra);
            last = d;
            y.add(d);
            x.add(i);
        }

        XYSeries series = chart.addSeries("WordCount", x, y);
        series.setLineColor(XChartSeriesColors.RED);
        series.setLineStyle(SeriesLines.SOLID);
        series.setMarkerColor(XChartSeriesColors.RED);
        series.setMarker(SeriesMarkers.NONE);
        series.setLineWidth(10);
        new SwingWrapper<XYChart>(chart).displayChart();

        try {
            BitmapEncoder.saveBitmap(chart, "./charts/" + "processing-time", BitmapEncoder.BitmapFormat.PNG);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
