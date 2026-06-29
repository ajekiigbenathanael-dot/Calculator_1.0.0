package com.swe2023020.calculator;

import android.widget.HorizontalScrollView;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.swe2023020.calculator.R;
import com.swe2023020.calculator.CalculatorEngine;
import com.swe2023020.calculator.matrix;
import com.swe2023020.calculator.stats;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // ── Views
    private TextView tvExpression, tvResult;
    private TextView tvMode;
    private HorizontalScrollView displayScroll;

    // ── State
    private final CalculatorEngine evaluator = new CalculatorEngine();
    private final StringBuilder inputBuffer = new StringBuilder();
    private final Deque<String> history = new ArrayDeque<>();
    private boolean resultDisplayed = false;  // true after pressing =
    private static final int MAX_HISTORY = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvExpression = findViewById(R.id.tvExpression);
        tvResult     = findViewById(R.id.tvResult);
        tvMode       = findViewById(R.id.tvMode);
        displayScroll = findViewById(R.id.displayScroll);

        tvResult.setOnLongClickListener(v -> {
            copyToClipboard(tvResult.getText().toString());
            return true;
        });

        setupButtons();
        updateDisplay();
    }

    // ── Button wiring

    private void setupButtons() {
        // Numbers
        bindDigit(R.id.btn0, "0"); bindDigit(R.id.btn1, "1"); bindDigit(R.id.btn2, "2");
        bindDigit(R.id.btn3, "3"); bindDigit(R.id.btn4, "4"); bindDigit(R.id.btn5, "5");
        bindDigit(R.id.btn6, "6"); bindDigit(R.id.btn7, "7"); bindDigit(R.id.btn8, "8");
        bindDigit(R.id.btn9, "9"); bindDigit(R.id.btnDot, ".");

        // Operators
        bindOp(R.id.btnPlus,  "+");
        bindOp(R.id.btnMinus, "-");
        bindOp(R.id.btnMul,   "×");
        bindOp(R.id.btnDiv,   "÷");
        bindOp(R.id.btnPow,   "^");
        bindOp(R.id.btnMod,   "%");

        // Parentheses
        bindOp(R.id.btnOpenParen,  "(");
        bindOp(R.id.btnCloseParen, ")");

        // Trig
        bindFunc(R.id.btnSin,  "sin(");
        bindFunc(R.id.btnCos,  "cos(");
        bindFunc(R.id.btnTan,  "tan(");

        // Hyperbolic
        bindFunc(R.id.btnSinh, "sinh(");
        bindFunc(R.id.btnCosh, "cosh(");
        bindFunc(R.id.btnTanh, "tanh(");

        // Log / root
        bindFunc(R.id.btnLn,   "ln(");
        bindFunc(R.id.btnLog,  "log(");
        bindFunc(R.id.btnSqrt, "√(");

        bindFunc(R.id.btnAbs,  "abs(");


        // Constants
        bindOp(R.id.btnPi, "π");
        bindOp(R.id.btnE,  "e");

        // Special
        bindOp(R.id.btnSignChange, "+/-");
        bindOp(R.id.btnSquare,     "^2");
        bindOp(R.id.btnReciprocal, "1÷(");

        // Combinatorics
        bindFunc(R.id.btnNPR, "nPr(");
        bindFunc(R.id.btnNCR, "nCr(");

        // Control
        bindButton(R.id.btnEquals, v -> onEquals());
        bindButton(R.id.btnAC,     v -> onAllClear());
        bindButton(R.id.btnDel,    v -> onDelete());
        bindButton(R.id.btnMode,   v -> onToggleMode());

        // Advanced dialogs
        bindButton(R.id.btnMatrix, v -> showMatrixDialog());
        bindButton(R.id.btnStat,   v -> showStatDialog());
        bindButton(R.id.btnHist,   v -> showHistory());

        // Stat shortcuts
        bindButton(R.id.btnMean,   v -> promptStat("mean"));
        bindButton(R.id.btnStdDev, v -> promptStat("stddev"));
        bindButton(R.id.btnMode2,  v -> promptStat("mode"));
    }

    // ── Input helpers

    private void bindDigit(int id, String digit) {
        bindButton(id, v -> onDigit(digit));
    }

    private void bindOp(int id, String op) {
        bindButton(id, v -> onOperator(op));
    }

    private void bindFunc(int id, String func) {
        bindButton(id, v -> onFunction(func));
    }

    private void bindButton(int id, View.OnClickListener listener) {
        View btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v -> {
                v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                animateButton(v);
                listener.onClick(v);
            });
        }
    }

    private void animateButton(View v) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.92f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.92f, 1f);
        scaleX.setDuration(120);
        scaleY.setDuration(120);
        scaleX.start();
        scaleY.start();
    }

    // ── Input logic

    private void onDigit(String digit) {
        if (resultDisplayed) {
            // If last result was shown, start fresh unless user continues with digit
            inputBuffer.setLength(0);
            resultDisplayed = false;
        }
        inputBuffer.append(digit);
        updateDisplay();
    }

    private void onOperator(String op) {
        if (op.equals("+/-")) {
            toggleSign();
            return;
        }
        if (resultDisplayed) {
            // Allow chaining: result + something
            resultDisplayed = false;
        }
        String s = inputBuffer.toString();
        // Remove trailing operator if user changes their mind
        if (!s.isEmpty() && isOperatorChar(s.charAt(s.length() - 1)) && !op.equals(")")) {
            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
        }
        inputBuffer.append(op);
        updateDisplay();
    }

    private void onFunction(String func) {
        if (resultDisplayed) {
            // Wrap the result in the function: e.g. sin(42)
            String prev = tvResult.getText().toString();
            if (!prev.isEmpty() && !prev.equals("Error")) {
                inputBuffer.setLength(0);
                inputBuffer.append(func).append(prev).append(")");
                resultDisplayed = false;
                updateDisplay();
                return;
            }
            resultDisplayed = false;
        }
        inputBuffer.append(func);
        updateDisplay();
    }

    private void onEquals() {
        String expression = autoCloseBrackets(inputBuffer.toString());
        if (expression.isEmpty()) return;
        try {
            double result = evaluator.evaluate(expression);
            String formatted = formatResult(result);
            history.addFirst(expression + " = " + formatted);
            if (history.size() > MAX_HISTORY) history.removeLast();
            tvExpression.setText(expression);
            tvResult.setText(formatted);
            inputBuffer.setLength(0);
            inputBuffer.append(formatted.contains("E") ? formatted : stripTrailingZeros(formatted));
            resultDisplayed = true;
        } catch (Exception e) {
            tvResult.setText("Error");
            shakeDisplay();
        }
    }

    private void onAllClear() {
        inputBuffer.setLength(0);
        resultDisplayed = false;
        tvExpression.setText("");
        tvResult.setText("0");
        updateDisplay();
    }

    private void onDelete() {
        if (resultDisplayed) {
            onAllClear();
            return;
        }
        if (inputBuffer.length() > 0) {
            // Remove last full token (e.g. "sin(" as a unit)
            String s = inputBuffer.toString();
            String[] funcs = {"sinh(", "cosh(", "tanh(", "asin(", "acos(", "atan(",
                    "sin(", "cos(", "tan(", "log(", "ln(", "sqrt(", "cbrt(",
                    "abs(", "fact(", "floor(", "ceil(", "nPr(", "nCr("};
            boolean deleted = false;
            for (String f : funcs) {
                if (s.endsWith(f)) {
                    inputBuffer.delete(inputBuffer.length() - f.length(), inputBuffer.length());
                    deleted = true;
                    break;
                }
            }
            if (!deleted) inputBuffer.deleteCharAt(inputBuffer.length() - 1);
            updateDisplay();
        }
    }

    private void onToggleMode() {
        if (evaluator.getAngleMode() == CalculatorEngine.AngleMode.DEGREES) {
            evaluator.setAngleMode(CalculatorEngine.AngleMode.RADIANS);
            tvMode.setText("RAD");
        } else {
            evaluator.setAngleMode(CalculatorEngine.AngleMode.DEGREES);
            tvMode.setText("DEG");
        }
    }

    private void toggleSign() {
        String s = inputBuffer.toString();
        if (s.startsWith("(-") && s.endsWith(")")) {
            inputBuffer.replace(0, inputBuffer.length(), s.substring(2, s.length() - 1));
        } else if (!s.isEmpty()) {
            inputBuffer.insert(0, "(-");
            inputBuffer.append(")");
        }
        updateDisplay();
    }

    // ── Display

    private void updateDisplay() {
        String expr = inputBuffer.toString();
        tvExpression.setText(expr.isEmpty() ? "" : expr);
        if (!resultDisplayed && !expr.isEmpty()) {
            // Live preview
            try {
                double preview = evaluator.evaluate(
                        autoCloseBrackets(expr)
                );
                tvResult.setText(formatResult(preview));
            } catch (Exception e) {
                tvResult.setText("");
            }
        } else if (expr.isEmpty()) {
            tvResult.setText("0");
        }
        displayScroll.post(() -> displayScroll.fullScroll(View.FOCUS_RIGHT));
    }

    private String formatResult(double v) {
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (Double.isNaN(v)) return "Not a number";
        // Use scientific notation for very large / very small numbers
        if (Math.abs(v) >= 1e15 || (Math.abs(v) < 1e-9 && v != 0)) {
            return String.format("%.6e", v);
        }
        // Show up to 10 decimal places, strip trailing zeros
        String s = String.format("%.10f", v);
        return stripTrailingZeros(s);
    }

    private String stripTrailingZeros(String s) {
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            s = s.replaceAll("\\.$", "");
        }
        return s;
    }

    private boolean isOperatorChar(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '×' || c == '÷';
    }

    private void shakeDisplay() {
        ObjectAnimator anim = ObjectAnimator.ofFloat(tvResult, "translationX",
                0, 18, -18, 12, -12, 6, -6, 0);
        anim.setDuration(400);
        anim.start();
    }

    // ── Copy to clipboard
    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("result", text));
        Toast.makeText(this, "Copied: " + text, Toast.LENGTH_SHORT).show();
    }

    // ── History
    private void showHistory() {
        if (history.isEmpty()) {
            Toast.makeText(this, "No history yet", Toast.LENGTH_SHORT).show();
            return;
        }
        String[] items = history.toArray(new String[0]);
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("History")
                .setItems(items, (dialog, which) -> {
                    // Tap a history item to restore the expression
                    String entry = items[which];
                    String[] parts = entry.split(" = ");
                    if (parts.length >= 1) {
                        inputBuffer.setLength(0);
                        inputBuffer.append(parts[0]);
                        resultDisplayed = false;
                        updateDisplay();
                    }
                })
                .setNegativeButton("Clear", (d, w) -> history.clear())
                .setPositiveButton("Close", null)
                .show();
    }

    // ── Statistics
    private void showStatDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter numbers separated by commas (e.g. 2,4,6,8)");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("Statistical Analysis")
                .setView(input)
                .setPositiveButton("Compute", (d, w) -> {
                    try {
                        double[] data = stats.parseDataset(input.getText().toString());
                        String summary = stats.fullSummary(data);
                        showResult("Statistics", summary);
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void promptStat(String type) {
        final EditText input = new EditText(this);
        input.setHint("Enter numbers separated by commas");
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle(type.toUpperCase())
                .setView(input)
                .setPositiveButton("Compute", (d, w) -> {
                    try {
                        double[] data = stats.parseDataset(input.getText().toString());
                        String result;
                        switch (type) {
                            case "mean":
                                result = "Mean = " + stats.mean(data); break;
                            case "stddev":
                                result = "Std Dev = " + stats.stdDev(data); break;
                            case "mode":
                                double[] modes = stats.mode(data);
                                StringBuilder sb = new StringBuilder("Mode = ");
                                for (int i = 0; i < modes.length; i++) {
                                    sb.append(modes[i]);
                                    if (i < modes.length - 1) sb.append(", ");
                                }
                                result = sb.toString(); break;
                            default: result = ""; break;
                        }
                        showResult(type.toUpperCase(), result);
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Matrix
    private void showMatrixDialog() {
        String[] sizes = {"2×2", "3×3", "4×4"};
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("Matrix Size")
                .setItems(sizes, (d, which) -> {
                    int size = which + 2;
                    showMatrixOperationDialog(size);
                })
                .show();
    }

    private void showMatrixOperationDialog(int size) {
        String[] ops = {"Determinant", "Inverse", "Transpose", "Trace",
                "A + B", "A - B", "A × B"};
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("Matrix Operation (" + size + "×" + size + ")")
                .setItems(ops, (d, which) -> {
                    if (which < 4) {
                        showSingleMatrixInput(size, ops[which]);
                    } else {
                        showDualMatrixInput(size, ops[which]);
                    }
                })
                .show();
    }

    private void showSingleMatrixInput(int size, String op) {
        LinearLayout layout = buildMatrixGrid(size, "Matrix A");
        EditText[][] fields = (EditText[][]) layout.getTag();

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle(op + " — Matrix A")
                .setView(layout)
                .setPositiveButton("Calculate", (d, w) -> {
                    try {
                        double[][] m = readMatrix(fields, size);
                        String result;
                        switch (op) {
                            case "Determinant":
                                result = "det(A) = " + matrix.determinant(m); break;
                            case "Inverse":
                                result = "A⁻¹ =\n" + matrix.matrixToString(matrix.inverse(m)); break;
                            case "Transpose":
                                result = "Aᵀ =\n" + matrix.matrixToString(matrix.transpose(m)); break;
                            case "Trace":
                                result = "tr(A) = " + matrix.trace(m); break;
                            default: result = ""; break;
                        }
                        showResult(op, result);
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDualMatrixInput(int size, String op) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 16, 32, 16);

        LinearLayout gridA = buildMatrixGrid(size, "Matrix A");
        LinearLayout gridB = buildMatrixGrid(size, "Matrix B");
        EditText[][] fieldsA = (EditText[][]) gridA.getTag();
        EditText[][] fieldsB = (EditText[][]) gridB.getTag();
        layout.addView(gridA);
        layout.addView(gridB);

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle(op)
                .setView(layout)
                .setPositiveButton("Calculate", (d, w) -> {
                    try {
                        double[][] a = readMatrix(fieldsA, size);
                        double[][] b = readMatrix(fieldsB, size);
                        double[][] res;
                        switch (op) {
                            case "A + B": res = matrix.add(a, b); break;
                            case "A - B": res = matrix.subtract(a, b); break;
                            case "A × B": res = matrix.multiply(a, b); break;
                            default: res = a; break;
                        }
                        showResult(op, "Result =\n" + matrix.matrixToString(res));
                    } catch (Exception e) {
                        showError(e.getMessage());
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /** Build a size×size grid of EditTexts, stored in layout.getTag() */
    private LinearLayout buildMatrixGrid(int size, String label) {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(0, 8, 0, 16);

        TextView lbl = new TextView(this);
        lbl.setText(label);
        lbl.setTextColor(0xFF60A5FA);
        lbl.setPadding(0, 0, 0, 8);
        container.addView(lbl);

        EditText[][] fields = new EditText[size][size];
        for (int i = 0; i < size; i++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = 0; j < size; j++) {
                EditText et = new EditText(this);
                et.setHint("0");
                et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | android.text.InputType.TYPE_NUMBER_FLAG_SIGNED);
                et.setTextColor(0xFFE2E8F0);
                et.setHintTextColor(0xFF4B5563);
                et.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF374151));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                params.setMargins(4, 4, 4, 4);
                et.setLayoutParams(params);
                fields[i][j] = et;
                row.addView(et);
            }
            container.addView(row);
        }
        container.setTag(fields);
        return container;
    }

    private double[][] readMatrix(EditText[][] fields, int size) {
        double[][] m = new double[size][size];
        for (int i = 0; i < size; i++)
            for (int j = 0; j < size; j++) {
                String txt = fields[i][j].getText().toString().trim();
                m[i][j] = txt.isEmpty() ? 0 : Double.parseDouble(txt);
            }
        return m;
    }

    // ── Dialogs
    private void showResult(String title, String message) {
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", null)
                .setNeutralButton("Copy", (d, w) -> copyToClipboard(message))
                .show();
    }

    private void showError(String msg) {
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("Error")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private String autoCloseBrackets(String expr) {
        int open = 0;

        for (char c : expr.toCharArray()) {
            if (c == '(') open++;
            if (c == ')') open--;
        }

        while (open > 0) {
            expr += ")";
            open--;
        }

        return expr;
    }
}
