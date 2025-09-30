package ru.prodvigaeff.control.modules.commentquality;

public class AIAnalysisResult
{
    private int detailScore;
    private int realismScore;
    private int concreteScore;
    private double totalScore;
    private String verdict;
    private String reason;

    public AIAnalysisResult(int detailScore, int realismScore, int concreteScore, 
                            double totalScore, String verdict, String reason)
    {
        this.detailScore = detailScore;
        this.realismScore = realismScore;
        this.concreteScore = concreteScore;
        this.totalScore = totalScore;
        this.verdict = verdict;
        this.reason = reason;
    }

    public int getDetailScore() { return detailScore; }
    public int getRealismScore() { return realismScore; }
    public int getConcreteScore() { return concreteScore; }
    public double getTotalScore() { return totalScore; }
    public String getVerdict() { return verdict; }
    public String getReason() { return reason; }
}