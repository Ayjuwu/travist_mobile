package com.example.travist;

public class Keypoint {
    public int id;
    public String name;
    public float price;
    public String startDate;
    public String endDate;
    public String cover;
    public float gpsX;
    public float gpsY;
    public boolean is_altered;
    public int cityId;
    private String cityName;


    public Keypoint(int id, String name, float price, String startDate, String endDate, String cover,
                    float gpsX, float gpsY, boolean is_altered, int cityId) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.startDate = startDate;
        this.endDate = endDate;
        this.cover = cover;
        this.gpsX = gpsX;
        this.gpsY = gpsY;
        this.is_altered = is_altered;
        this.cityId = cityId;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }
}
