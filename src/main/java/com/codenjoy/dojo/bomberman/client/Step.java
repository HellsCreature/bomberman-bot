package com.codenjoy.dojo.bomberman.client;

import com.codenjoy.dojo.services.Point;

import java.util.ArrayList;

public class Step {

    Point previousPoint;
    int expectedCost;
    ArrayList<Point> newPoints;

    public Step (Point previousPoint,
                 int expectedCost,
                 ArrayList<Point> newPoints) {
        this.previousPoint = previousPoint;
        this.expectedCost = expectedCost;
        this.newPoints = new ArrayList<>(newPoints);
    }

    public String toString() {
        return String.format("[%s,%s,[%s]]", this.previousPoint.toString(), this.expectedCost, this.newPoints.toString());
    }

}
