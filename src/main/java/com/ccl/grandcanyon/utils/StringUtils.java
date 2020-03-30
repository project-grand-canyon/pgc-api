package com.ccl.grandcanyon.utils;

import java.util.List;

public final class StringUtils {
    private StringUtils(){}

    public static String createCommaSeparatedList(List<String> input) {
        if(input.isEmpty()){
            return "";
        }
        if(input.size() == 1){
            return input.get(0);
        }
        if(input.size() == 2){
            return input.get(0) + " and " + input.get(1);
        }
        String firstPart = String.join(", ", input.subList(0, input.size() - 1));
        String lastPart = ", and " + input.get(input.size() - 1);
        return firstPart + lastPart;
    }
}
