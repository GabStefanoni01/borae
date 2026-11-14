package com.borae.identity;

public interface RestaurantObjectStorage {

    StoredObject upload(String folder, String fileName, byte[] bytes, String contentType);

    void delete(String fileId);

    record StoredObject(String fileId, String publicUrl) {}
}
