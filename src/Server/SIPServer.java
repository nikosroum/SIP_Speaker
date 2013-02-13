package Server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 *
 * @author Roumpoutsos Nikolaos - Sapountzis Ioannis
 */
public class SIPServer extends Thread {

    //private static int MAX_CLIENTS = 100;
    //public static SIPServer[] sipservers = new SIPServer[MAX_CLIENTS];//how many simioultanesly can handle - na dw me arraylist
    public static ArrayList<SIPServer> sipservers;
    public static String newmessage;
    public static int client_counter = 0;
    public static DatagramSocket UDPServerSocket;  //The main socket to listen on
    public static Parameters parameters;
    public static String myAddress;
    public String mytag;    //used for unique identification
    //For sending and receiving via UDP
    private DatagramPacket receivePacket;
    private DatagramPacket SendPacket;
    byte[] receiveData;
    byte[] sendData;
    //Obtained from SIP Header parsing
    private String ViaAddress;
    private String tag;
    private String branch;
    private String CSeq;
    //The combination of From-To-Call-ID is the session dialog
    private String Call_ID;           //Call-ID contains a globally unique identifier for this call
    private int senderPort;          //the sender is using that port
    private String senderUsername;  //Username from whom the call is received
    private String senderRtpPort;  //Port on which to transmit RTP
    private String senderAddress;  //IPv4 address of the sender as a string
    private String receiverUser;  //The user to which the caller wants to speak
    public boolean busy = false;
    private String session_id;
    
    public SIPServer(Parameters params) {
        this.parameters = params;
        this.receiveData = new byte[1024];
        this.sendData = new byte[1024];
        newmessage="";
        mytag = UUID.randomUUID().toString().replace("-", "");

    }

    public static void main(String[] args) throws UnknownHostException, SocketException, IOException, InterruptedException {
        sipservers = new ArrayList();
        // Register a shutdown thread
      
        Signal.handle(new Signal("INT"), new SignalHandler () {
    public void handle(Signal sig) {
        System.out.print("newmessage======= "+newmessage);
      if(!newmessage.isEmpty()){
        System.out.println("I am recording the new message...");
      WavHandler.RecordMessage(newmessage,parameters.message_wav);}
      
      System.exit(0);
    }
  });
        if (args.length == 1 && args[0].equals("--help")) {
            System.out.println("java SIPSpeaker [-c config_file_name] [-user sip_uri] [-http http_bind_address]");
            return;
        }
        if ((args.length % 2) == 1 || args.length > 7) {
            System.out.println("java SIPSpeaker [-c config_file_name] [-user sip_uri] [-http http_bind_address]");
            return;
        }

        if (args.length > 1) {
            parameters = new Parameters(args);
            if (parameters.getSip_user().equals("ERROR")) {
                System.out.println("user format should be: user@host[:port]");
                return;
            }
        } else {
            parameters = new Parameters();
        }

        parameters.ReadConfigFile();
        parameters.saveProperties();
        parameters.PrintParams();

        //Initiate and start the WebServer
        WebServer myWebServer = new WebServer(parameters.getHTTP_port(), parameters.getHTTP_DEFAULT_INTERFACE(), parameters);
        myWebServer.start();
        //Prepare SIPServer

        SIPServer mySIPServer = new SIPServer(parameters);
        //create the UDP listening Socket
        System.out.println("Sip Server listening to :" + parameters.getSIP_DEFAULT_INTERFACE() + "- on port :" + parameters.getSIP_port());
        UDPServerSocket = new DatagramSocket(parameters.getSIP_port(), parameters.getSIP_DEFAULT_INTERFACE());
        mySIPServer.Action();

    }

    private void Action() throws IOException, InterruptedException {
        while (true) {

            System.out.println("Clients connecteds:" + client_counter);
            receivePacket = new DatagramPacket(receiveData, receiveData.length);

            UDPServerSocket.receive(receivePacket);
            senderPort = receivePacket.getPort();
            String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
            System.out.println(received);
            switch (RequestType(parseUDP(received)[0])) {//first line contains the request type of the header
                case 0://INVITE
                    String RcvSocketAddr = receivePacket.getSocketAddress().toString();
                    System.out.println("INVITE received from: " + RcvSocketAddr);
                    //Check if call is for me
                    System.out.println("In request:" + receiverUser + "\n In parameters:" + parameters.getSip_user() + "|");
                    if (receiverUser.equals(parameters.getSip_user())) {
                        if (checkForDialogSession()) {//other dialog session exists with same From-To-Call_ID
                            System.out.println("Session with client already started");
                            break;
                        }
                        //send trying
                        System.out.println("Trying");
                        TryingMessage(UDPServerSocket.getLocalPort(), parameters.getSip_user());
                        Thread.sleep(100);//wait a little for ringing in softphone
                        //send ringing
                        System.out.println("Ringing");
                        RingingMessage(UDPServerSocket.getLocalPort(), parameters.getSip_user());
                        Thread.sleep(100);//wait a little for ringing in softphone
                        //send OK
                        System.out.println("OK");
                        OKMessage(UDPServerSocket.getLocalPort(), parameters.getSip_user());
                    } else {
                        NotFoundMessage(UDPServerSocket.getLocalPort(), parameters.getSip_user());
                    }
                    break;
                case 1://OK
                    System.out.println("200 OK received!");
                    break;
                case -1://Cancel received
                    System.out.println("CANCEL received!");
                    break;
                case 2://BYE
                    System.out.println("BYE received!");
                    removeClient(this);
                    OKafterBYEMessage(UDPServerSocket.getLocalPort(), parameters.getSip_user());
                    break;
                case 3://ACK
                    System.out.println("ACK received!");
                    if ((receiverUser.equals(parameters.sip_user))) {
                        SIPServer newsipserver = new SIPServer(parameters);
                        newsipserver = new SIPServer(parameters);
                        newsipserver.receiverUser = receiverUser;
                        newsipserver.tag = tag;

                        newsipserver.Call_ID = Call_ID;

                        newsipserver.senderAddress = senderAddress;
                        newsipserver.senderUsername = senderUsername;
                        newsipserver.senderPort = senderPort;

                        newsipserver.senderRtpPort = senderRtpPort;

                        newsipserver.branch = branch;

                        sipservers.add(newsipserver);

                        sipservers.get(client_counter).start();

                        client_counter++;


                    }
                    System.out.print("Clients connected now: ");
                    System.out.println(client_counter);

                    break;




            }

        }


    }

    @Override
    public void run() {
        try {
            busy = true;
            WavHandler myWavHandler = new WavHandler();
            myWavHandler.SendWavFile(parameters.getMessage_wav(), senderAddress, senderRtpPort);
            //Thread.sleep(5000);//for Basic grade
            SendByeMessage(parameters.getSip_user());
            busy = false;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String[] parseUDP(String packet) {
        List<String> SIPRequest = new ArrayList<String>();
        int sdp_length = 0;
        StringTokenizer st = new StringTokenizer(packet, "\r\n");
        String line = null;

        while (st.hasMoreTokens()) {
            line = st.nextToken();
            SIPRequest.add(line);
            if (line.matches("INVITE sip:.*@.*")) {
                myAddress = line.substring(line.indexOf("@") + 1, line.indexOf(" SIP/2.0"));

            }
            if (line.matches("^Via: SIP/2\\.0/UDP .*")) {
                branch = line.split("branch=")[1];
                ViaAddress = line.substring(("Via: SIP/2.0/UDP ").length(), line.indexOf(";"));

            }

            if (line.matches("^To: <sip:.*@.*>$")) {

                receiverUser = line.split("@")[0].split("sip:")[1];
                System.out.println("To-------------------" + receiverUser);
            }
            if (line.startsWith("From")) {

                senderUsername = line.split("From: \"")[1].split("\"")[0];
                senderAddress = (line.split("<sip:")[1]).split(">")[0];
                System.out.println("username:" + senderUsername + "\naddress" + senderAddress);
                tag = line.split("tag=")[1];
            }

            if (line.matches("Call-ID: .*")) {
                Call_ID = line.substring(("Call-ID: ").length(), line.length());
            }

            if (line.matches("CSeq: .*")) {
                CSeq = line.split(" ")[1];

            }

            if (line.matches("^o=- .* IN IP4 .*$")) {
                session_id = (line.split("=- ")[1]).split(" IN ")[0];
            }

            if (line.matches("^m=audio .* RTP.*$")) {
                senderRtpPort = (line.split("audio ")[1]).split(" ")[0];
            }
        }
        String[] parse_result = SIPRequest.toArray(new String[SIPRequest.size()]);
        return parse_result;

    }

    private int RequestType(String line) {

        if (line.matches("INVITE sip:.*@.*")) {
            return 0;
        }

        if (line.matches("SIP/2.0 200 OK")) {
            return 1;
        }

        if (line.matches("^BYE sip:.* SIP/2.0$")) {
            return 2;
        }

        if (line.matches("^ACK sip:.* SIP/2.0$")) {
            return 3;
        }
        if (line.contains("CANCEL")) {
            return -1;
        }

        return 4;
    }

    private void SendPacket(String message) throws UnknownHostException, IOException {
        System.out.println("Sending packet...:\n");
        System.out.println(message);
        sendData = message.getBytes();
        SendPacket = new DatagramPacket(sendData, sendData.length, InetAddress.getByName(senderAddress), senderPort);
        UDPServerSocket.send(SendPacket);
    }

    private void TryingMessage(int port, String username) throws UnknownHostException, IOException {
        String message = "SIP/2.0 100 Trying\r\n"
                + "Via: SIP/2.0/UDP " + senderAddress + ";"
                + "rport=" + port + ";"
                + "received=" + senderAddress + ";"
                + "branch=" + branch + "\r\n"
                + "Content-Length: 0\r\n"
                + "Contact: <sip:" + myAddress + ":" + port + ">\r\n"
                + "Call-ID: " + Call_ID + "\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + senderUsername + "\"<sip:" + senderAddress + ">;tag=" + tag + "\r\n"
                + "To: \"" + username + "\"<sip:" + username + "@" + myAddress + ">;tag=" + mytag + "\r\n\r\n";

        SendPacket(message);

    }

    private void RingingMessage(int port, String username) throws UnknownHostException, IOException {

        String message = "SIP/2.0 180 Ringing\r\n"
                + "Via: SIP/2.0/UDP " + senderAddress + ";"
                + "rport=" + port + ";"
                + "received=" + senderAddress + ";"
                + "branch=" + branch + "\r\n"
                + "Content-Length: 0\r\n"
                + "Contact: <sip:" + myAddress + ":" + port + ">\r\n"
                + "Call-ID: " + Call_ID + "\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + senderUsername + "\"<sip:" + senderAddress + ">;tag=" + tag + "\r\n"
                + "To: \"" + username + "\"<sip:" + username + "@" + myAddress + ">;tag=" + mytag + "\r\n\r\n";

        SendPacket(message);
    }

    private void OKMessage(int port, String username) throws UnknownHostException, IOException {

        String sdp_message = createSDPMessage();

        String message = "SIP/2.0 200 OK\r\n"
                + "Via: SIP/2.0/UDP " + senderAddress + ";"
                + "rport=" + port + ";received=" + senderAddress + ";"
                + "branch=" + branch + "\r\n"
                + "Content-Length: " + sdp_message.length() + "\r\n"
                + "Contact: <sip:" + myAddress + ":" + port + ">\r\n"
                + "Call-ID: " + Call_ID + "\r\n"
                + "Content-Type: application/sdp\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + senderUsername + "\"<sip:" + senderAddress + ">;tag=" + tag + "\r\n"
                + "To: \"" + username + "\"<sip:" + username + "@" + myAddress + ">;tag=" + mytag + "\r\n\r\n"
                + sdp_message;

        SendPacket(message);
    }
private void OKafterBYEMessage(int port, String username) throws UnknownHostException, IOException {

        String message = "SIP/2.0 200 OK\r\n"
                + "Via: SIP/2.0/UDP " + senderAddress + ";\r\n"
                + "branch=" + branch + "\r\n"
                + "Call-ID: " + Call_ID + "\r\n"
                + "CSeq: "+CSeq+" BYE\r\n"
                + "From: \"" + senderUsername + "\"<sip:" + senderAddress + ">;tag=" + tag + "\r\n"
                + "To: \"" + username + "\"<sip:" + username + "@" + myAddress + ">;tag=" + mytag + "\r\n\r\n";

        SendPacket(message);
    }

    private void NotFoundMessage(int port, String username) throws UnknownHostException, IOException {
        String message = "SIP/2.0 404 Not Found\r\n"
                + "Via: SIP/2.0/UDP " + senderAddress + ";"
                + "rport=" + port + ";received=" + senderAddress + ";"
                + "branch=" + branch + "\r\n"
                + "Content-Length: 0\r\n"
                + "Contact: <sip:" + myAddress + ":" + port + ">\r\n"
                + "Call-ID: " + Call_ID + "\r\n"
                + "CSeq: 1 INVITE\r\n"
                + "From: \"" + senderUsername + "\"<sip:" + senderAddress + ">;tag=" + tag + "\r\n"
                + "To: \"" + username + "\"<sip:" + username + "@" + myAddress + ">;tag=" + mytag + "\r\n\r\n";

        SendPacket(message);

    }

    private void SendByeMessage(String sip_user) throws UnknownHostException, IOException {

        int seq;//=Integer.parseInt(CSeq);
        //seq++; //increment CSeq for the BYE transaction
        seq = 2;
        String message = "BYE sip:" + senderAddress + " SIP/2.0\r\n"
                + "Via: SIP/2.0/UDP " + myAddress + ";"
                + "rport;branch=" + branch + "\r\n"
                + "Content-Length: 0\r\n" + //nothing to send additionally
                "Call-ID: " + Call_ID + "\r\n"
                + "CSeq: " + seq + " BYE\r\n" + //It is the first message sent
                "From: \"" + sip_user + "\"<sip:" + sip_user + "@" + myAddress + ">;tag=" + mytag + "\r\n"
                + "Max-Forwards: 70\r\n"
                + "To: <sip:" + senderAddress + ">;tag=" + tag + "\r\n"
                + "User-Agent: SJphone/1.60.299a/L (SJ Labs)\r\n\r\n";

        SendPacket(message);
        System.out.print("Sending Bye to " + senderAddress + ":" + senderPort);
        removeClient(this);

    }

    private String createSDPMessage() {
        String sdp_message = "v=0\r\n"
                + "o=- " + session_id + " IN IP4 " + myAddress + "\r\n"
                + "s=SJphone\r\n"
                + "c=IN IP4 " + myAddress + "\r\n"
                + "t=0 0\r\n"
                + "m=audio " + senderRtpPort + " RTP/AVP 3 101\r\n"
                + "a=sendrecv\r\n"
                + "a=rtpmap:3 GSM/8000\r\n"
                + "a=rtpmap:101 telephone-event/8000\r\n"
                + "a=fmtp:101 0-11,16\r\n";
        return sdp_message;

    }

    private boolean checkForDialogSession() {
        if (sipservers.isEmpty()) {
            return false;
        }
        for (int i = 0; i < client_counter; i++) {
            if (sipservers.get(i).receiverUser.equals(this.receiverUser) && sipservers.get(i).Call_ID.equals(this.Call_ID) && sipservers.get(i).senderUsername.equals(this.senderUsername) && sipservers.get(i).busy) {
                return true; //there is a Call existing with same From-To-CallID
            }
        }
        return false;
    }

    private void removeClient(SIPServer a) {
        if (!sipservers.isEmpty()) {

            for (int i = 0; i < sipservers.size(); i++) {
                System.out.println(sipservers.get(i).receiverUser + (this.receiverUser) + sipservers.get(i).Call_ID + (this.Call_ID) + sipservers.get(i).senderUsername + (this.senderUsername));
                if (sipservers.get(i).receiverUser.equals(this.receiverUser) && sipservers.get(i).Call_ID.equals(this.Call_ID) && sipservers.get(i).senderUsername.equals(this.senderUsername)) {
                    if (sipservers.get(i).busy) {
                        sipservers.get(i).interrupt();
                    }

                    sipservers.remove(i);
                }
            }
            client_counter--;
        } else {
            this.interrupt();
            client_counter = 0;
        }
    }
}
