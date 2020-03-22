package com.ccl.grandcanyon.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.stream.Collectors;

public class FileReader {

    public static FileReader getInstance() {
        return new FileReader();
    }

    public String read(String resource) throws FileNotFoundException {
        URL url = getClass().getClassLoader().getResource(resource);
        if (url == null) {
            throw new FileNotFoundException("File '" + resource + "' not found.");
        }
        File file = new File(url.getFile());
        BufferedReader br = new BufferedReader(new java.io.FileReader(file));
        return br.lines().collect(Collectors.joining());
    }
}
