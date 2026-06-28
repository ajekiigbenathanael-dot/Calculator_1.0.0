package com.swe2023020.calculator;

import java.util.*;

/**
 * Statistical computations: mean, median, mode, variance,
 * standard deviation, range, sum, product, geometric mean,
 * harmonic mean, and percentile.
 */
public class stats {

    /** Parse a comma-separated list of numbers, e.g. "1,2,3,4,5" */
    public static double[] parseDataset(String input) throws Exception {
        String[] parts = input.trim().split("[,\\s]+");
        double[] data = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                data[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException e) {
                throw new Exception("Invalid number: " + parts[i]);
            }
        }
        return data;
    }

    public static double mean(double[] data) {
        return sum(data) / data.length;
    }

    public static double sum(double[] data) {
        double s = 0;
        for (double d : data) s += d;
        return s;
    }

    public static double product(double[] data) {
        double p = 1;
        for (double d : data) p *= d;
        return p;
    }

    public static double median(double[] data) {
        double[] sorted = Arrays.copyOf(data, data.length);
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 1) return sorted[n / 2];
        return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
    }

    /** Returns all modes (the most frequently occurring values) */
    public static double[] mode(double[] data) {
        Map<Double, Integer> freq = new LinkedHashMap<>();
        for (double d : data) freq.put(d, freq.getOrDefault(d, 0) + 1);
        int maxFreq = Collections.max(freq.values());
        List<Double> modes = new ArrayList<>();
        for (Map.Entry<Double, Integer> e : freq.entrySet())
            if (e.getValue() == maxFreq) modes.add(e.getKey());
        double[] result = new double[modes.size()];
        for (int i = 0; i < modes.size(); i++) result[i] = modes.get(i);
        return result;
    }

    /** Population variance */
    public static double variance(double[] data) {
        double m = mean(data);
        double s = 0;
        for (double d : data) s += (d - m) * (d - m);
        return s / data.length;
    }

    /** Sample variance */
    public static double sampleVariance(double[] data) {
        if (data.length < 2) throw new ArithmeticException("Need at least 2 values for sample variance");
        double m = mean(data);
        double s = 0;
        for (double d : data) s += (d - m) * (d - m);
        return s / (data.length - 1);
    }

    /** Population standard deviation */
    public static double stdDev(double[] data) {
        return Math.sqrt(variance(data));
    }

    /** Sample standard deviation */
    public static double sampleStdDev(double[] data) {
        return Math.sqrt(sampleVariance(data));
    }

    public static double range(double[] data) {
        double min = data[0], max = data[0];
        for (double d : data) { if (d < min) min = d; if (d > max) max = d; }
        return max - min;
    }

    public static double min(double[] data) {
        double min = data[0];
        for (double d : data) if (d < min) min = d;
        return min;
    }

    public static double max(double[] data) {
        double max = data[0];
        for (double d : data) if (d > max) max = d;
        return max;
    }

    public static double geometricMean(double[] data) throws Exception {
        for (double d : data) if (d <= 0) throw new Exception("Geometric mean requires positive numbers");
        double logSum = 0;
        for (double d : data) logSum += Math.log(d);
        return Math.exp(logSum / data.length);
    }

    public static double harmonicMean(double[] data) throws Exception {
        double recipSum = 0;
        for (double d : data) {
            if (d == 0) throw new Exception("Harmonic mean: zero value not allowed");
            recipSum += 1.0 / d;
        }
        return data.length / recipSum;
    }

    /** Percentile (0–100) using linear interpolation */
    public static double percentile(double[] data, double p) {
        double[] sorted = Arrays.copyOf(data, data.length);
        Arrays.sort(sorted);
        double index = (p / 100.0) * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sorted[lower];
        return sorted[lower] + (index - lower) * (sorted[upper] - sorted[lower]);
    }

    /** Build a human-readable summary of all statistics */
    public static String fullSummary(double[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("n = ").append(data.length).append("\n");
        sb.append("Sum      = ").append(fmt(sum(data))).append("\n");
        sb.append("Mean     = ").append(fmt(mean(data))).append("\n");
        sb.append("Median   = ").append(fmt(median(data))).append("\n");

        double[] m = mode(data);
        sb.append("Mode     = ");
        for (int i = 0; i < m.length; i++) { sb.append(fmt(m[i])); if (i < m.length - 1) sb.append(", "); }
        sb.append("\n");

        sb.append("Min      = ").append(fmt(min(data))).append("\n");
        sb.append("Max      = ").append(fmt(max(data))).append("\n");
        sb.append("Range    = ").append(fmt(range(data))).append("\n");
        sb.append("Variance = ").append(fmt(variance(data))).append("\n");
        sb.append("Std Dev  = ").append(fmt(stdDev(data))).append("\n");
        sb.append("Q1       = ").append(fmt(percentile(data, 25))).append("\n");
        sb.append("Q3       = ").append(fmt(percentile(data, 75)));
        return sb.toString();
    }

    private static String fmt(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e15)
            return String.valueOf((long) v);
        return String.format("%.6g", v);
    }
}
