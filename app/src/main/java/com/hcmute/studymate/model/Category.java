package com.hcmute.studymate.model;

public class Category {
    private String id;
    private String name;
    private String colorHex;

    public Category() {
    }

    public Category(String id, String name, String colorHex) {
        this.id = id;
        this.name = name;
        this.colorHex = colorHex;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorHex() {
        return colorHex;
    }

    public void setColorHex(String colorHex) {
        this.colorHex = colorHex;
    }
}
