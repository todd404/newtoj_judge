package com.toddwu.toj_judge.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

public class MyUtils {
    static public void downloadFile(String source, String path) throws IOException {
        URL url = new URL(source);
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        FileOutputStream fos = new FileOutputStream(path);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    }

    static public String readFileToString(String filePath) throws IOException {
        return Files.readString(Path.of(filePath));
    }
}
