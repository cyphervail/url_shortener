package com.example.url_shortener.service.helper;

public class Encode {
    private static final String ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final Integer BASE=ALPHABET.length();

    public static String encode(Long num){
        StringBuilder sb=new StringBuilder();

        while(num>0){
            sb.append(ALPHABET.charAt((int) (num%BASE)));
            num/=BASE;
        }
        return sb.reverse().toString();
    }
}
