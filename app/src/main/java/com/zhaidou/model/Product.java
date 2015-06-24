package com.zhaidou.model;

import java.util.List;

/**
 * Created by wangclark on 15/6/12.
 */
public class Product {
    private int id;
    private String title;
    private int price;
    private String url;
    private int bean_like_count;
    private List<Category> categories;

    private String image;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getBean_like_count() {
        return bean_like_count;
    }

    public void setBean_like_count(int bean_like_count) {
        this.bean_like_count = bean_like_count;
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    public Product(int id, String title, int price, String url, int bean_like_count, List<Category> categories, String image) {
        this.id = id;
        this.title = title;
        this.price = price;
        this.url = url;
        this.bean_like_count = bean_like_count;
        this.categories = categories;
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Product() {
    }
}