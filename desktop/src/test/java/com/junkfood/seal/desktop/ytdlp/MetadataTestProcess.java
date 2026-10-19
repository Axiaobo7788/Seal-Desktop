package com.junkfood.seal.desktop.ytdlp;

public final class MetadataTestProcess {
    public static void main(String[] args) throws Exception {
        if (args[0].equals("never")) {
            Thread.sleep(60000);
        } else {
            if (args[0].equals("large")) {
                for (int i = 0; i < 50000; i++) {
                    System.err.println("Warning: test diagnostic without a newline boundary dependency");
                }
            } else if (args[0].equals("warning")) {
                System.err.println("Warning: harmless extraction diagnostic");
            }
            System.out.println("{\"id\":\"test\",\"title\":\"Test video\"}");
            if (args[0].equals("error")) System.exit(7);
        }
    }
}
