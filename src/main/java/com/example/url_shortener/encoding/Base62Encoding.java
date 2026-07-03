package com.example.url_shortener.encoding;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoding {

    private static final String ALPHANUMERICAL="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE=62;
    public String encode(long number){
        if(number==0)
            return String.valueOf(ALPHANUMERICAL.charAt(0));
        StringBuilder sb=new StringBuilder();
        while(number>0){
            int remainder=(int)(number%BASE);
            sb.append(ALPHANUMERICAL.charAt(remainder));
            number=number/BASE;
        }
        return sb.reverse().toString();
    }
}
