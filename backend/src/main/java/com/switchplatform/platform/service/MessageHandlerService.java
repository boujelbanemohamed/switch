package com.switchplatform.platform.service;

import com.switchplatform.platform.model.Participant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class MessageHandlerService {

    public byte[] sendAndReceive(Participant destination, byte[] message) {
        return switch (destination.getEndpointType()) {
            case TCP -> sendTcp(destination.getEndpointUrl(), message);
            case HTTP -> sendHttp(destination.getEndpointUrl(), message);
            case MQ -> sendMq(destination, message);
            case FILE -> sendFile(destination, message);
        };
    }

    public String sendAndReceiveXml(Participant destination, String xmlMessage) {
        byte[] response = sendAndReceive(destination,
                xmlMessage.getBytes(StandardCharsets.UTF_8));
        return new String(response, StandardCharsets.UTF_8);
    }

    private byte[] sendTcp(String endpointUrl, byte[] message) {
        try {
            String[] parts = endpointUrl.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(host, port);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                dos.writeInt(message.length);
                dos.write(message);
                dos.flush();

                int length = dis.readInt();
                byte[] response = new byte[length];
                dis.readFully(response);
                return response;
            }
        } catch (IOException e) {
            throw new RuntimeException("TCP communication failed: " + e.getMessage(), e);
        }
    }

    private byte[] sendHttp(String endpointUrl, byte[] message) {
        try {
            URL url = URI.create(endpointUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(message);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP error: " + responseCode);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("HTTP communication failed: " + e.getMessage(), e);
        }
    }

    private byte[] sendMq(Participant destination, byte[] message) {
        log.info("MQ message sent to {} (queue: {})", destination.getCode(),
                destination.getEndpointUrl());
        return "OK".getBytes();
    }

    private byte[] sendFile(Participant destination, byte[] message) {
        try {
            String filePath = destination.getEndpointUrl() + "/" +
                    System.currentTimeMillis() + ".msg";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(message);
            }
            log.info("Message written to file: {}", filePath);
            return "OK".getBytes();
        } catch (IOException e) {
            throw new RuntimeException("File write failed: " + e.getMessage(), e);
        }
    }
}
