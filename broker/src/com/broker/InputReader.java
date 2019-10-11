package com.broker;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Scanner;
import java.util.concurrent.Callable;

public class InputReader implements Callable {

    private BufferedReader reader;
    private boolean done;

    public InputReader(){
        reader = new BufferedReader(new InputStreamReader(System.in));
    }

    @Override
    public Object call() throws Exception {
        String input;
        do {
            try {
                while (!reader.ready()) {
                    Thread.sleep(2);
                }

                input = reader.readLine();
            } catch (InterruptedException e) {
                return null;
            }
        } while ("".equals(input));
        return input;
    }
}
