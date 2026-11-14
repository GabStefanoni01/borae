package com.borae.identity;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;
import io.imagekit.models.files.FileDeleteParams;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ImageKitRestaurantObjectStorage implements RestaurantObjectStorage {

    private final String privateKey;
    private final String urlEndpoint;
    private ImageKitClient client;

    public ImageKitRestaurantObjectStorage(
            @Value("${app.storage.imagekit.private-key:}") String privateKey,
            @Value("${app.storage.imagekit.url-endpoint:}") String urlEndpoint
    ) {
        this.privateKey = privateKey == null ? "" : privateKey.trim();
        this.urlEndpoint = urlEndpoint == null ? "" : urlEndpoint.trim().replaceAll("/+$", "");
    }

    @Override
    public StoredObject upload(String folder, String fileName, byte[] bytes, String contentType) {
        FileUploadParams params = FileUploadParams.builder()
                .file(bytes)
                .fileName(fileName)
                .folder(folder)
                .build();

        FileUploadResponse response = client().files().upload(params);
        return new StoredObject(response.fileId(), response.url());
    }

    @Override
    public void delete(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            throw new IllegalArgumentException("O identificador do arquivo no ImageKit é obrigatório.");
        }
        client().files().delete(FileDeleteParams.builder().fileId(fileId).build());
    }

    private synchronized ImageKitClient client() {
        if (privateKey.isBlank() || urlEndpoint.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "O armazenamento de imagens ainda não está configurado.");
        }
        if (client == null) {
            client = ImageKitOkHttpClient.builder()
                    .privateKey(privateKey)
                    .build();
        }
        return client;
    }
}
