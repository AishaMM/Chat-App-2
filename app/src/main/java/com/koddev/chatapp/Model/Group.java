package com.koddev.chatapp.Model;

public class Group {

    private String id;
    private String name;

    public String getImageURL() {
        return imageURL;
    }

    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }

    private String imageURL;

    public Group(String id, String name) {
    this.id = id;
        this.name = name;
    }

    public Group(){

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
}
