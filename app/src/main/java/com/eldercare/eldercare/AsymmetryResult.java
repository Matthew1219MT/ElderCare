package com.eldercare.eldercare;

public class AsymmetryResult {
    private double asymmetryScore;
    private boolean asymmetryDetected;
    private String riskLevel;

    public AsymmetryResult(double asymmetryScore, boolean asymmetryDetected, String riskLevel) {
        this.asymmetryScore = asymmetryScore;
        this.asymmetryDetected = asymmetryDetected;
        this.riskLevel = riskLevel;
    }

    public double getAsymmetryScore() {
        return asymmetryScore;
    }

    public boolean isAsymmetryDetected() {
        return asymmetryDetected;
    }

    public String getRiskLevel() {
        return riskLevel;
    }
}