package com.example.correctorexamenesmobile.data.api.model;

import org.opencv.core.Point;

public class Circulos {
    private Point centro;
    private int radio;

    public Circulos(Point centro, int radio) {
        this.centro = centro;
        this.radio = radio;
    }

    public Point getCentro() {
        return centro;
    }

    public void setCentro(Point centro) {
        this.centro = centro;
    }

    public int getRadio() {
        return radio;
    }

    public void setRadio(int radio) {
        this.radio = radio;
    }

    @Override
    public String toString() {
        return "Circulos{" +
                "centro=" + centro +
                ", radio=" + radio +
                '}';
    }
}
