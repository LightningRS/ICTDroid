package org.square16.ictdroid;

import org.square16.ictdroid.utils.ArgParser;

public class Main {
    public static void main(String[] args) {
        if (!ArgParser.parse(args)) {
            return;
        }
        TestController controller = new TestController();
        controller.start();
    }
}
