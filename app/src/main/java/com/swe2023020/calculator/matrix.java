package com.swe2023020.calculator;

/**
 * Matrix operations: determinant, inverse, transpose, addition,
 * subtraction, multiplication — supports 2×2 up to 4×4.
 */
public class matrix {

    /** Transpose of any matrix */
    public static double[][] transpose(double[][] m) {
        int rows = m.length, cols = m[0].length;
        double[][] t = new double[cols][rows];
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                t[j][i] = m[i][j];
        return t;
    }

    /** Element-wise addition (same dimensions required) */
    public static double[][] add(double[][] a, double[][] b) throws Exception {
        checkSameDims(a, b);
        int r = a.length, c = a[0].length;
        double[][] res = new double[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                res[i][j] = a[i][j] + b[i][j];
        return res;
    }

    /** Element-wise subtraction */
    public static double[][] subtract(double[][] a, double[][] b) throws Exception {
        checkSameDims(a, b);
        int r = a.length, c = a[0].length;
        double[][] res = new double[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                res[i][j] = a[i][j] - b[i][j];
        return res;
    }

    /** Matrix multiplication (a cols must equal b rows) */
    public static double[][] multiply(double[][] a, double[][] b) throws Exception {
        if (a[0].length != b.length)
            throw new Exception("Incompatible dimensions for multiplication");
        int r = a.length, k = b.length, c = b[0].length;
        double[][] res = new double[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                for (int p = 0; p < k; p++)
                    res[i][j] += a[i][p] * b[p][j];
        return res;
    }

    /** Determinant (square matrices 2×2 – 4×4) */
    public static double determinant(double[][] m) throws Exception {
        int n = m.length;
        if (m[0].length != n) throw new Exception("Determinant requires a square matrix");
        if (n > 4) throw new Exception("Determinant supported for matrices up to 4×4");
        if (n == 2) return det2(m);
        if (n == 3) return det3(m);
        return det4(m);
    }

    /** Inverse (square matrices 2×2 – 4×4) */
    public static double[][] inverse(double[][] m) throws Exception {
        int n = m.length;
        if (m[0].length != n) throw new Exception("Inverse requires a square matrix");
        if (n > 4) throw new Exception("Inverse supported for matrices up to 4×4");
        double det = determinant(m);
        if (Math.abs(det) < 1e-12) throw new Exception("Matrix is singular (no inverse)");
        // Gauss-Jordan elimination
        double[][] aug = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) aug[i][j] = m[i][j];
            aug[i][i + n] = 1;
        }
        for (int col = 0; col < n; col++) {
            // Partial pivot
            int maxRow = col;
            for (int row = col + 1; row < n; row++)
                if (Math.abs(aug[row][col]) > Math.abs(aug[maxRow][col])) maxRow = row;
            double[] tmp = aug[col]; aug[col] = aug[maxRow]; aug[maxRow] = tmp;
            double pivot = aug[col][col];
            for (int j = 0; j < 2 * n; j++) aug[col][j] /= pivot;
            for (int row = 0; row < n; row++) {
                if (row == col) continue;
                double factor = aug[row][col];
                for (int j = 0; j < 2 * n; j++) aug[row][j] -= factor * aug[col][j];
            }
        }
        double[][] inv = new double[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                inv[i][j] = aug[i][j + n];
        return inv;
    }

    /** Scalar multiplication */
    public static double[][] scalarMultiply(double[][] m, double s) {
        int r = m.length, c = m[0].length;
        double[][] res = new double[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                res[i][j] = m[i][j] * s;
        return res;
    }

    /** Trace (sum of diagonal elements) */
    public static double trace(double[][] m) throws Exception {
        if (m.length != m[0].length) throw new Exception("Trace requires a square matrix");
        double t = 0;
        for (int i = 0; i < m.length; i++) t += m[i][i];
        return t;
    }

    // ── Private determinant helpers
    private static double det2(double[][] m) {
        return m[0][0] * m[1][1] - m[0][1] * m[1][0];
    }

    private static double det3(double[][] m) {
        return m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1])
                - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
    }

    private static double det4(double[][] m) {
        // Cofactor expansion along first row
        double det = 0;
        for (int col = 0; col < 4; col++) {
            double[][] minor = minor(m, 0, col);
            det += (col % 2 == 0 ? 1 : -1) * m[0][col] * det3(minor);
        }
        return det;
    }

    private static double[][] minor(double[][] m, int skipRow, int skipCol) {
        int n = m.length;
        double[][] sub = new double[n - 1][n - 1];
        int ri = 0;
        for (int i = 0; i < n; i++) {
            if (i == skipRow) continue;
            int ci = 0;
            for (int j = 0; j < n; j++) {
                if (j == skipCol) continue;
                sub[ri][ci++] = m[i][j];
            }
            ri++;
        }
        return sub;
    }

    private static void checkSameDims(double[][] a, double[][] b) throws Exception {
        if (a.length != b.length || a[0].length != b[0].length)
            throw new Exception("Matrix dimensions must match");
    }

    /** Format a matrix for display */
    public static String matrixToString(double[][] m) {
        StringBuilder sb = new StringBuilder();
        for (double[] row : m) {
            sb.append("[ ");
            for (int j = 0; j < row.length; j++) {
                sb.append(formatNum(row[j]));
                if (j < row.length - 1) sb.append("  ");
            }
            sb.append(" ]\n");
        }
        return sb.toString().trim();
    }

    private static String formatNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.valueOf((long) v);
        return String.format("%.4g", v);
    }
}
