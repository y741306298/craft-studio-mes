package com.piliofpala.craftstudio.pangolin.domain.gatherplatform.product.entity;

public class ProductSpec {
    private String name;
    private String code;
    private String cover;
    private String brand;


    public ProductSpec(String name, String code, String cover, String brand) {
        this.name = name;
        this.code = code;
        this.cover = cover;
        this.brand = brand;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
