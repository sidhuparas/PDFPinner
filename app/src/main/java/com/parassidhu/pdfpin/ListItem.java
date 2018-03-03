package com.parassidhu.pdfpin;

public class ListItem {

    private String path;
    private String name;

    public ListItem(String path, String name) {
        this.path = path;
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setName(String name) {
        this.name = name;
    }
}
