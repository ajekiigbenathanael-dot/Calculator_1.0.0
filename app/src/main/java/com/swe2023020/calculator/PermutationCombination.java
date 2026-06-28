package com.swe2023020.calculator;

/**
 * Permutation and combination for the scientific calculator.
 * Uses double throughout to match CalculatorEngine and avoid long overflow.
 */
public class PermutationCombination {

    private PermutationCombination() {}

    public static double factorial(int n) throws Exception {
        if (n < 0)   throw new Exception("Factorial undefined for negative numbers");
        if (n > 170) throw new Exception("Factorial overflow (max input: 170)");
        double result = 1;
        for (int i = 2; i <= n; i++) result *= i;
        return result;
    }

    public static double nPr(int n, int r) throws Exception {
        if (n < 0 || r < 0) throw new Exception("n and r must be non-negative");
        if (r > n)           throw new Exception("r cannot exceed n  →  nPr(" + n + "," + r + ")");
        return factorial(n) / factorial(n - r);
    }

    public static double nCr(int n, int r) throws Exception {
        if (n < 0 || r < 0) throw new Exception("n and r must be non-negative");
        if (r > n)           throw new Exception("r cannot exceed n  →  nCr(" + n + "," + r + ")");
        return factorial(n) / (factorial(r) * factorial(n - r));
    }
}