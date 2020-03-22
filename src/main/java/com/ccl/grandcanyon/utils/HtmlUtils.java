package com.ccl.grandcanyon.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.stream.Collectors;

public final class HtmlUtils {
    private HtmlUtils(){}

    public static String ReadHtmlFile(ClassLoader classLoader, String filename) throws RuntimeException{
        try {
            URL resource = classLoader.getResource(filename);
            if (resource == null) {
                throw new FileNotFoundException("File '" + filename + "' not found.");
            }
            File file = new File(resource.getFile());
            BufferedReader br = new BufferedReader(new FileReader(file));
            return br.lines().collect(Collectors.joining());
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to load Html file: " + e.getLocalizedMessage());
        }
    }
}
