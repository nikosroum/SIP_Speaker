/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author roumdic
 */
public class WebServer extends Thread{

    Parameters params;
    ServerSocket myServerSocket;
    int port;
    InetAddress http_address;
    boolean listen;

    public WebServer(int port, InetAddress http_address, Parameters params) {
        this.port = port;
        this.http_address = http_address;
        this.params = params;
        listen = true;
    }

    @Override
    public void run() {
        System.out.println("Starting WebServer...");

        try {

            try {

                System.out.println("Trying to bind to localhost on port " + Integer.toString(port) + "...");
                //make a ServerSocket and bind it to given port,
                myServerSocket = new ServerSocket(port);
            } catch (Exception e) { //catch any errors and print errors
                System.out.println("\nFatal Error:" + e.getMessage());
                return;
            }
            System.out.println("WebServer is up and listening...");

            while (listen) {
                Socket mySocket = myServerSocket.accept();

                WebHandler myHandler = new WebHandler(mySocket, params);
                myHandler.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void shutdownWebServer() {
        listen = false;
    }
}
