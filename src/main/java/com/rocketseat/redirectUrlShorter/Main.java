package com.rocketseat.redirectUrlShorter;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class Main implements RequestHandler<Map<String, Object>, Map<String , Object>> {

    private final S3Client s3Client = S3Client.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        String pathParameter = (String) input.get("rawPath");
        String shortUrlCode = pathParameter.replace("/", "");

        if (shortUrlCode == null || shortUrlCode.isEmpty()) {
            throw new IllegalArgumentException("Invalid input: path parameter");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("url-shortner-math")
                .key(shortUrlCode + ".json")
                .build();

        InputStream s3ObjectStream;
        try {
            s3ObjectStream = s3Client.getObject(getObjectRequest);
        } catch (Exception exception) {
            throw new RuntimeException("Error fetching URL data from S3: " + exception.getMessage(), exception);
        }

        UrlData urlData;
        try {
            urlData = objectMapper.readValue(s3ObjectStream, UrlData.class);
        } catch(Exception exception) {
            throw new RuntimeException("Error deserializing URL data: " + exception.getMessage(), exception);
        }

        long currentTimeInSeconds = System.currentTimeMillis() / 1000;

        Map<String, Object> response = new HashMap<>();

        // Cenario onde a URL expirou
        if (urlData.getExpirationTime() < currentTimeInSeconds ) {

            response.put("statusCode", 410);
            response.put("body", "This URL has expired");
            return response;
        }
        // Cenario onde a URL ainda e valida
        response.put("statusCode", 302);
        Map<String, String> headers = new HashMap<>();
        headers.put("Location", urlData.getOriginalUrl());
        response.put("headers", headers);

        return response;
    }
}