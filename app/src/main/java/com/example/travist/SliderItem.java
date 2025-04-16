package com.example.travist;

public class SliderItem {
    private int kpId;
    private String imageUrl;

    public SliderItem(int kpId, String imageUrl) {
        this.kpId = kpId;
        this.imageUrl = imageUrl;
    }

    public int getKpId() {
        return kpId;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}
