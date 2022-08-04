package com.nowcoder.community;

import java.io.IOException;

public class WKTests {

    public static void main(String[] args) {
        String cmd = "d:/work/wkhtmltopdf/bin/wkhtmltoimage --quality 75  http://www.nowcoder.com  d:/work/data/wk-image/3.png";

        try {
            Runtime.getRuntime().exec(cmd);
            System.out.println("ok");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
