package com.example.blottermanagementsystem.data.api;

public interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);
}
