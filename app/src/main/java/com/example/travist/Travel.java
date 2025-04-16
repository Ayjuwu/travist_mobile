package com.example.travist;

public class Travel {
    public int id;
    public String name;
    public int peopleNumber;
    public float individualPrice;
    public float totalPrice;
    public String startDate;
    public String endDate;
    public int userId;

    public Travel(int id, String name, int peopleNumber, float individualPrice, float totalPrice,
                  String startDate, String endDate, int userId) {
        this.id = id;
        this.name = name;
        this.peopleNumber = peopleNumber;
        this.individualPrice = individualPrice;
        this.totalPrice = totalPrice;
        this.startDate = startDate;
        this.endDate = endDate;
        this.userId = userId;
    }
}
