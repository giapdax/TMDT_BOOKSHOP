// src/main/java/vn/fs/dto/DashboardSummary.java
package vn.fs.dto;

public class DashboardSummary {
    private long newUsers7d;     // số user đăng ký 7 ngày gần nhất
    private long salesQty7d;     // tổng SL bán ra 7 ngày gần nhất (sum quantity)
    private long orders7d;       // số đơn 7 ngày gần nhất (để gắn vào "Subscribers" trên UI sẵn có)
    private double income7d;     // doanh thu 7 ngày gần nhất

    public DashboardSummary(long newUsers7d, long salesQty7d, long orders7d, double income7d) {
        this.newUsers7d = newUsers7d;
        this.salesQty7d = salesQty7d;
        this.orders7d = orders7d;
        this.income7d = income7d;
    }
    public long getNewUsers7d() { return newUsers7d; }
    public long getSalesQty7d() { return salesQty7d; }
    public long getOrders7d() { return orders7d; }
    public double getIncome7d() { return income7d; }
}
