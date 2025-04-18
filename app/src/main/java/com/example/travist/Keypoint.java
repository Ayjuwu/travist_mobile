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
    public int is_altered;
    public int cityId;
    private String cityName;
    private String tags;


    public Keypoint(int id, String name, float price, String startDate, String endDate, String cover,
                    float gpsX, float gpsY, int is_altered, int cityId) {
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}