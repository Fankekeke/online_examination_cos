package com.onlineexamsystem.backend.common.model;

import java.util.List;

public class PageData<T> {

    private List<T> items;
    private long total;

    public PageData() {
    }

    public PageData(List<T> items, long total) {
        this.items = items;
        this.total = total;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
