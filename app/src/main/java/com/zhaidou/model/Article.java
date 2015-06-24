package com.zhaidou.model;

/**
 * Created by wangclark on 15/6/12.
 */
public class Article {
    private int id;
    private String title;
    private String img_url;
    private String is_new;
    private int reviews;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getIs_new() {
        return is_new;
    }

    public void setIs_new(String is_new) {
        this.is_new = is_new;
    }

    public int getReviews() {
        return reviews;
    }

    public void setReviews(int reviews) {
        this.reviews = reviews;
    }

    public Article(int id, String title, String img_url, String is_new, int reviews) {
        this.id = id;
        this.title = title;
        this.img_url = img_url;
        this.is_new = is_new;
        this.reviews = reviews;
    }

    public Article() {
    }
}