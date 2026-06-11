package model;

public class LoungeConfig {
    private int standardCount;
    private int vipCount;
    private double standardRate;
    private double vipRate;
    private double vipMinCharge;

    public LoungeConfig(int standardCount, int vipCount, double standardRate, double vipRate, double vipMinCharge) {
        this.standardCount = standardCount;
        this.vipCount = vipCount;
        this.standardRate = standardRate;
        this.vipRate = vipRate;
        this.vipMinCharge = vipMinCharge;
    }

    public int getStandardCount() { return standardCount; }
    public int getVipCount() { return vipCount; }
    public double getStandardRate() { return standardRate; }
    public double getVipRate() { return vipRate; }
    public double getVipMinCharge() { return vipMinCharge; }
}