package com.inkcore.infrastructure.out.objectstorage;

import com.inkcore.domain.objectstorage.exception.ObjectStorageUnavailableException;
import com.inkcore.infrastructure.config.ObjectStorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.CopyObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class S3ObjectStorageOperationRetryTest {

    @TempDir
    Path tempDir;

    @Test
    void downloadObject_retriesTimeoutThenSucceeds() throws Exception {
        S3Client s3 = mock(S3Client.class);
        doThrow(SdkClientException.create("timeout"))
                .doReturn(objectStream("pdf"))
                .when(s3).getObject(any(GetObjectRequest.class));

        Path dest = tempDir.resolve("art.pdf");
        adapter(s3).downloadObject("tmp/k/original.pdf", dest, 1000);

        assertEquals("pdf", Files.readString(dest));
        verify(s3, times(2)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void downloadObject_retriesBrokenStreamThenSucceeds() throws Exception {
        S3Client s3 = mock(S3Client.class);
        doReturn(failingStream())
                .doReturn(objectStream("ok"))
                .when(s3).getObject(any(GetObjectRequest.class));

        Path dest = tempDir.resolve("art.bin");
        adapter(s3).downloadObject("tmp/k/original.bin", dest, 1000);

        assertEquals("ok", Files.readString(dest));
        verify(s3, times(2)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void downloadObject_afterRetriesBecomesUnavailable() {
        S3Client s3 = mock(S3Client.class);
        doThrow(SdkClientException.create("timeout")).when(s3).getObject(any(GetObjectRequest.class));

        Path dest = tempDir.resolve("art.bin");
        assertThrows(ObjectStorageUnavailableException.class,
                () -> adapter(s3).downloadObject("tmp/k/original.bin", dest, 1000));
        verify(s3, times(3)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void downloadObject_doesNotRetryMissingKey() {
        S3Client s3 = mock(S3Client.class);
        doThrow(NoSuchKeyException.builder().message("missing").build())
                .when(s3).getObject(any(GetObjectRequest.class));

        Path dest = tempDir.resolve("art.bin");
        assertThrows(IllegalArgumentException.class,
                () -> adapter(s3).downloadObject("tmp/k/original.bin", dest, 1000));
        verify(s3, times(1)).getObject(any(GetObjectRequest.class));
    }

    @Test
    void copyObject_retriesServiceUnavailableThenSucceeds() {
        S3Client s3 = mock(S3Client.class);
        doThrow(S3Exception.builder().statusCode(503).message("slow").build())
                .doReturn(CopyObjectResponse.builder().build())
                .when(s3).copyObject(any(CopyObjectRequest.class));

        adapter(s3).copyObject("tmp/a", "company/a");
        verify(s3, times(2)).copyObject(any(CopyObjectRequest.class));
    }

    private static S3ObjectStorageAdapter adapter(S3Client s3) {
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setBucket("inkcore");
        properties.setRegion("us-east-1");
        properties.setAccessKey("test");
        properties.setSecretKey("test");
        properties.setPathStyleAccess(true);
        properties.setEndpoint("http://localhost:9100");
        properties.setPublicEndpoint("http://localhost:9100");
        properties.setOperationRetryAttempts(3);
        properties.setOperationRetryBackoffMs(0);
        S3Presigner presigner = S3Presigner.builder()
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .endpointOverride(URI.create(properties.getPublicEndpoint()))
                .build();
        return new S3ObjectStorageAdapter(properties, s3, presigner, true);
    }

    private static ResponseInputStream<GetObjectResponse> objectStream(String body) {
        byte[] bytes = body.getBytes();
        return new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength((long) bytes.length).build(),
                AbortableInputStream.create(new ByteArrayInputStream(bytes))
        );
    }

    private static ResponseInputStream<GetObjectResponse> failingStream() {
        InputStream boom = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset");
            }
        };
        return new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(10L).build(),
                AbortableInputStream.create(boom)
        );
    }
}
