package com.hcmute.studymate.model;

import java.util.ArrayList;
import java.util.List;

public class ExamPrepSection {
    private String heading;
    private List<String> bullets = new ArrayList<>();

    public ExamPrepSection() {
    }

    public ExamPrepSection(String heading, List<String> bullets) {
        this.heading = heading;
        this.bullets = bullets == null ? new ArrayList<>() : bullets;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public List<String> getBullets() {
        return bullets;
    }

    public void setBullets(List<String> bullets) {
        this.bullets = bullets == null ? new ArrayList<>() : bullets;
    }
}
