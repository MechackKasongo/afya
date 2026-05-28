package com.afya.platform.clinical.storage;

import java.io.InputStream;

public interface ObjectStorageService {

    void put(String objectKey, InputStream data, long size, String contentType);

    InputStream get(String objectKey);

    boolean exists(String objectKey);
}
