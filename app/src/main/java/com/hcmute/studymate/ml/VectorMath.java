package com.hcmute.studymate.ml;

public final class VectorMath {
    private VectorMath() {
    }

    public static float[] l2Normalize(float[] values) {
        if (values == null || values.length == 0) {
            return new float[0];
        }
        double sumSquares = 0;
        for (float value : values) {
            sumSquares += (double) value * value;
        }
        double norm = Math.sqrt(sumSquares);
        if (norm < 1e-12) {
            return values.clone();
        }
        float[] normalized = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            normalized[i] = (float) (values[i] / norm);
        }
        return normalized;
    }

    public static double cosineSimilarity(float[] left, float[] right) {
        if (left == null || right == null || left.length == 0 || left.length != right.length) {
            return 0;
        }
        double dot = 0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
        }
        return dot;
    }

    public static float[] toFloatArray(java.util.List<Double> values) {
        if (values == null || values.isEmpty()) {
            return new float[0];
        }
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            result[i] = value == null ? 0f : value.floatValue();
        }
        return result;
    }

    public static java.util.List<Double> toDoubleList(float[] values) {
        java.util.List<Double> result = new java.util.ArrayList<>();
        if (values == null) {
            return result;
        }
        for (float value : values) {
            result.add((double) value);
        }
        return result;
    }
}
