package com.spe.calculator;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        String portEnv = System.getenv("PORT");
        int port = portEnv == null || portEnv.isBlank() ? 8080 : Integer.parseInt(portEnv.trim());

        CalculatorServer server = new CalculatorServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
        System.out.println("Calculator listening on port " + server.getPort());
    }
}
