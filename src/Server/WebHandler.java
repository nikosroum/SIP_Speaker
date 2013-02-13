package Server;

import java.net.*;
import java.io.*;

import java.security.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Roumpoutsos Nikolaos - Sapountzis Ioannis
 */
public class WebHandler extends Thread {

    Socket mySocket;
    BufferedReader input;
    PrintWriter output;
    String EncodeMap = "UTF-8";//the encoded charset map
    String message;
    Parameters parameters;
    
    public String createPage(String header,String message){
    String HTML_HEAD = "<html><body><h1>"+header+"</h1><p>";
        String HTML_END = "</p></body></html>";
        if(header.equals("Current message")){
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
 
	    // Create a calendar object that will convert the date and time value in milliseconds to date. 
	     Calendar calendar = Calendar.getInstance();
	     calendar.setTimeInMillis(Long.parseLong(parameters.getProperties().getProperty("message_recived")));
	     
        HTML_END = "</p><i>Last Changed: "+ formatter.format(calendar.getTime()) +"</i></body></html>";}
        return (HTML_HEAD + message + HTML_END);
    
    }
    
    public WebHandler(Socket mySocket,Parameters params) {
        this.mySocket = mySocket;
        this.parameters=params;

        try {
            input = new BufferedReader(new InputStreamReader(mySocket.getInputStream(), EncodeMap));//ISO-8859-15
            
            output = new PrintWriter(mySocket.getOutputStream());

        } catch (Exception e) {
        }

    }

//send a file to out
    public void send_file(String filename, PrintWriter out) throws FileNotFoundException, IOException {
        FileReader file = new FileReader(filename);
        BufferedReader in = new BufferedReader(file);
        String line = "";
        while ((line = in.readLine()) != null) {
            out.println(line);
        }
        in.close();
        out.flush();
        out.close();

    }
//create an http header with a specific code

    private String http_header(int return_code) {
        String s = "HTTP/1.1 ";

        switch (return_code) {
            case 200:
                s = s + "200 OK";
                break;
            case 400:
                s = s + "400 Bad Request";
                break;
            case 404:
                s = s + "404 Not Found";
                break;
            case 500:
                s = s + "500 Internal Server Error";
                break;
            case 501:
                s = s + "501 Not Implemented";
                break;
        }

        s = s + "\r\n";
        s = s + "Connection: close\r\n"; //we can't handle persistent connections
        s = s + "Server: MyServer \r\n"; //server name

        s = s + "Content-Type: text/html\r\n"; //the only filetype our server supports

        s = s + "\r\n"; //this marks the end of the httpheader


        return s;
    }

    @Override
    public void run() {
        try {
       
                int method = 0; //1 get,2 post 0 not supported

                System.out.println("New client connected.");
                System.out.println("Socket:" + mySocket.getRemoteSocketAddress().toString());
                System.out.println("IP:" + mySocket.getInetAddress().getHostAddress());
                String submit="";
                int action;
                //This is the two types of request we can handle
                //GET /index.html HTTP/1.0
                //POST HTTP/1.0

                System.out.println("Connection, sending data.");

                // read the data sent. 

                String tmp = input.readLine(); // read the data sent. 
                while (tmp == null)//wait until client sends something
                {
                    tmp = input.readLine();
                }
                String tmp2 = new String(tmp);
                int start = 0;
                tmp.toUpperCase(); //convert it to uppercase
                if (tmp.startsWith("GET")) { //compare if is it GET
                    method = 1;
                    start = 5; //skip "GET /"
                }
                if (tmp.startsWith("POST")) { //compare if is it GET
                    method = 2;
                    start = 6; //skip "POST /"
                }
                if (method == 0) { // not supported
                    try {
                        output.print(http_header(501));// send a 501 error
                        output.close();
                        return;
                    } catch (Exception e3) { //if some error happened catch it
                        System.out.println("error:" + e3.getMessage());
                    } //and display error
                }

                if (method == 1) {
                    String webpage;
                    webpage = tmp2.substring(start, tmp2.lastIndexOf("HTTP") - 1);
                    System.out.println("Client request for: " + webpage);

                    if (webpage.equals("")) {
                        webpage = "index.html";//default webpage
                    }

                    try {
                        if (new File(webpage).isFile()) { //send the file
                            output.print(http_header(200));
                            send_file(webpage, output);
                        } else {// file not found
                            output.print(http_header(404));
                            send_file("404.html", output);
                            input.close();
                            output.close();
                            mySocket.close();
                            return;
                        }
                        input.close();
                        output.close();
                        mySocket.close();
                        return;
                    } catch (IOException ex) {
                        Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, null, ex);
                    }

                } else { //method=2 POST

                    String line;
                    int length = 0;


                    //Parse Header
                    while ((line = input.readLine()) != null && !line.isEmpty()) {
                    //first while loop to skip header
                    //  System.out.println(line);
                    if (line.contains("Content-Length:")) {// determine how many bytes we are going to receive
                        length = Integer.parseInt(line.substring(("Content-Length:").length() + 1, line.length()));
                    }
                }
                    char[] buff = new char[length];
                input.read(buff, 0, length);
                System.out.println(buff);// for debug
                StringTokenizer st2 = new StringTokenizer(String.valueOf(buff));
                    while (st2.hasMoreTokens()) { //iterate through tokens
                    line = (st2.nextToken("\r\n"));
                    if (line.startsWith("message=")) {
                       message=(line.substring(("message=").length(), line.length()));

                    }
                    if (line.startsWith("submit=")) {
                       submit=(line.substring(("submit=").length(), line.length()));
                    }
                }
                if (submit.equals("Delete")) {
                    action=2;
                    HandlePostRequest(action);
                    output.print(http_header(200));
                    output.print(createPage("Message deleted", "Your message was successfully deleted"));
                    output.flush();
                }
                
                if (submit.equals("View Current")){
                    action=0;
                    output.print(http_header(200));
                    output.print(createPage("Current message", parameters.getMessage_text()));
                    
                }
                if (submit.equals("Change")){
                    action=1;
                    HandlePostRequest(action);
                    output.print(http_header(200));
                    output.print(createPage("Message changed", "Your message was successfully changed"));
              
                    }
                

                }
                output.close();
                input.close();
                mySocket.close();
                return;
        } catch (IOException ex) {
            Logger.getLogger(WebHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
        

    }
    public void HandlePostRequest(int action) throws FileNotFoundException, IOException{
        if (action==1){//Change message
            parameters.getProperties().setProperty("message_text", message);
            System.out.println("-------------------------------------------------"+parameters.getMessage_text());
           // WavHandler.RecordMessage(message,parameters.message_wav);
            //String ts = String.valueOf(System.currentTimeMillis()); 
            SIPServer.newmessage=message;
            Date now= new Date();
            String ts = String.valueOf(now.getTime());
            parameters.getProperties().setProperty("message_recived", ts);
            parameters.getProperties().store(new FileOutputStream(parameters.getConfig_file_name()), null);
        }
        if (action==2){//Delete message
            parameters.getProperties().setProperty("message_text", "");
            parameters.getProperties().setProperty("message_wav", "");
            System.out.println("config="+parameters.getConfig_file_name());
            parameters.getProperties().store(new FileOutputStream(parameters.getConfig_file_name()), null);
        }
    }
}
