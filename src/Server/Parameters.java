/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author roumdic
 */
public class Parameters {

    public Properties properties;

    public Properties getProperties() {
        return properties;
        
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }
    public static String default_wav;
    public static String message_text;
    public static String message_wav;
    public static String sip_user;
    String default_filename = "sipspeaker.cfg";
    private static  InetAddress HTTP_DEFAULT_INTERFACE;
    private static InetAddress SIP_DEFAULT_INTERFACE;
    String config_file_name;
    boolean Command_defined_httpaddress;
    boolean Command_defined_httpport;
    boolean Command_defined_sipport;
    boolean Command_defined_sipuser;
    boolean Command_defined_sipinterface;
    int HTTP_port;
    int SIP_port;

    public void setCommand_defined_httpaddress(boolean Command_defined_httpaddress) {
        this.Command_defined_httpaddress = Command_defined_httpaddress;
    }

    public void setCommand_defined_httpport(boolean Command_defined_httpport) {
        this.Command_defined_httpport = Command_defined_httpport;
    }

    public void setCommand_defined_sipinterface(boolean Command_defined_sipinterface) {
        this.Command_defined_sipinterface = Command_defined_sipinterface;
    }

    public void setCommand_defined_sipport(boolean Command_defined_sipport) {
        this.Command_defined_sipport = Command_defined_sipport;
    }

    public void setCommand_defined_sipuser(boolean Command_defined_sipuser) {
        this.Command_defined_sipuser = Command_defined_sipuser;
    }
    
    
    public static String getSip_user() {
        return sip_user;
    }

    public static void setSip_user(String sip_user) {
        Parameters.sip_user = sip_user;
    }
    

    public  InetAddress getHTTP_DEFAULT_INTERFACE() {
        return HTTP_DEFAULT_INTERFACE;
    }

    public void setHTTP_DEFAULT_INTERFACE(InetAddress HTTP_DEFAULT_INTERFACE) {
        Parameters.HTTP_DEFAULT_INTERFACE = HTTP_DEFAULT_INTERFACE;
    }

    public static InetAddress getSIP_DEFAULT_INTERFACE() {
        return SIP_DEFAULT_INTERFACE;
    }

    public static void setSIP_DEFAULT_INTERFACE(InetAddress SIP_DEFAULT_INTERFACE) {
        Parameters.SIP_DEFAULT_INTERFACE = SIP_DEFAULT_INTERFACE;
    }

    public static String getMessage_text() {
        return message_text;
    }

    public static void setMessage_text(String message_text) {
        Parameters.message_text = message_text;
    }

    public static String getMessage_wav() {
        return message_wav;
    }

    public static void setMessage_wav(String message_wav) {
        Parameters.message_wav = message_wav;
    }

    public void PrintParams() {
        System.out.println(config_file_name);
        System.out.println(HTTP_DEFAULT_INTERFACE);
        System.out.println(HTTP_port);
        System.out.println(SIP_DEFAULT_INTERFACE);
        System.out.println(sip_user);
        System.out.println(SIP_port);
        System.out.println(message_text);
        System.out.println(message_wav);

    }

    public void ReadConfigFile() {
        String filepath = default_filename;

        try {
            if (!config_file_name.isEmpty()) {//if config_file_name specified
                if (config_file_name.matches("^[0-9a-zA-Z\\.]+$")) { //it is an absolute path 
                    filepath = new File("").getAbsolutePath() + "/" + config_file_name;
                } else {
                    filepath = config_file_name;
                }
            }
            FileInputStream in = new FileInputStream(filepath);
            System.out.println("Using configuration file " + filepath);
            
            properties.load(in);
            
            in.close();
        } catch (FileNotFoundException ex) {
            System.out.println("Config file not found: " + filepath + "; using default settings");
        } catch (IOException ex) {
            System.out.println("Error while reading from file: " + filepath + "; using default settings");
        }
    }

    public void saveProperties() throws UnknownHostException {

        boolean current_defined = false;
        boolean current_exists = false;
        default_wav = "default.wav";//even if default is not defined , default_wav is hard coded
        if (properties.containsKey("default_message")) {
            if (properties.getProperty("default_message").matches("^/.*")) {
                default_wav = properties.getProperty("default_message");
            } else {
                default_wav = new File("").getAbsolutePath() + "/" + properties.getProperty("default_message");
            }
            File defaultmessage = new File(default_wav);
            if (!defaultmessage.exists()) {
                default_wav = "default.wav";//give default value to wavfile
                WavHandler.CreateDefaultWavMessage(default_wav);
                
            }


        }
        //message_recived = seconds date
        //Get the message_wav field
        if (properties.containsKey("message_wav")) {
            current_defined = true;
            if (properties.getProperty("message_wav").matches("^/.*")) {
                message_wav = properties.getProperty(message_wav);
            } else {
                message_wav = new File("").getAbsolutePath() + "/" + properties.getProperty("message_wav");
            }
            File currentmessage = new File(message_wav);
            if (currentmessage.exists()) {
                current_exists = true;
            }
        }
        if (!current_defined || !current_exists) {//current message doesnot exists or is not defined so default wav will be used
            message_wav = default_wav;
        }
        //Get the message text
        if (properties.containsKey("message_text")) {
            message_text = properties.getProperty("message_text");
        }

        if (properties.containsKey("sip_interface") && !Command_defined_sipinterface) {
            if (properties.getProperty("sip_interface") != null) {
                SIP_DEFAULT_INTERFACE = InetAddress.getByName(properties.getProperty("sip_interface"));
            }
        }

        if (properties.containsKey("sip_port") && !Command_defined_sipport) {
            SIP_port = Integer.parseInt(properties.getProperty("sip_port"));
        }
        if (properties.containsKey("sip_user") && !Command_defined_sipuser) {
            if (properties.getProperty("sip_user") != null) {
                sip_user = properties.getProperty("sip_user");
            }
        }
        if (properties.containsKey("http_interface") && !Command_defined_httpaddress) {
            HTTP_DEFAULT_INTERFACE = InetAddress.getByName(properties.getProperty("http_interface"));

        }
        if (properties.containsKey("http_port") && !Command_defined_httpport) {
            HTTP_port = Integer.parseInt(properties.getProperty("http_port"));
        }





    }

    public Parameters() {
        try {
            //default parameters
            sip_user = "sipspeaker";
            SIP_DEFAULT_INTERFACE = InetAddress.getByName("0.0.0.0");
            SIP_port = 5060;

            HTTP_port = 80;
            HTTP_DEFAULT_INTERFACE = InetAddress.getByName("127.0.0.1");

            config_file_name = default_filename;
            message_wav = "default.wav";
            Command_defined_httpaddress=false;
            Command_defined_httpport=false;
            Command_defined_sipinterface=false;
            Command_defined_sipport=false;
            Command_defined_sipuser=false;
            properties=new Properties();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Parameters.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
 public Parameters(String[] args) throws UnknownHostException {
        boolean config = false;
        boolean sip = false;
        boolean http = false;
        properties=new Properties();
        sip_user = "sipspeaker";
            SIP_DEFAULT_INTERFACE = InetAddress.getByName("0.0.0.0");
            SIP_port = 5060;

            HTTP_port = 80;
            HTTP_DEFAULT_INTERFACE = InetAddress.getByName("127.0.0.1");

            config_file_name = default_filename;
            message_wav = "default.wav";
            Command_defined_httpaddress=false;
            Command_defined_httpport=false;
            Command_defined_sipinterface=false;
            Command_defined_sipport=false;
            Command_defined_sipuser=false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-c") && !config) {//to avoid wrong user arguments
                config_file_name=args[i + 1];
                config = true;
                
            }
            if (args[i].equals("-user") && !sip) {
//na petaei exception gia lathos 
                String sip_uri = args[i+1];
                if(!sip_uri.matches("^.*@.*"))
                    sip_user="ERROR";
                else
                    sip_user=(sip_uri.split("@")[0]);
                Command_defined_sipuser=true;
                if (sip_uri.matches("^.*:.*$"))//sip_uri is in format someone@something:port
                {
                    SIP_DEFAULT_INTERFACE=(InetAddress.getByName(sip_uri.split("@")[1].split(":")[0]));
                    SIP_port=(Integer.parseInt(sip_uri.split(":")[1]));
                    Command_defined_sipport=true;
                    Command_defined_sipinterface=true;
                }else{//no port was specified
                    SIP_DEFAULT_INTERFACE=(InetAddress.getByName(sip_uri.split("@")[1]));
                    Command_defined_sipinterface=true;
                }
                    
                sip = true;
            }
            
            if (args[i].equals("-http") && !http) {
                if(args[i+1].matches(".*:.*")){
                String http_address = args[i + 1].split(":")[0];
                int http_port = Integer.parseInt(args[i + 1].split(":")[1]);
                    setHTTP_DEFAULT_INTERFACE(InetAddress.getByName(http_address));
                     HTTP_port=http_port;
                    Command_defined_httpport=true;
                }else
                    setHTTP_DEFAULT_INTERFACE(InetAddress.getByName(args[i+1]));
                Command_defined_httpaddress=true;
                http = true;
            }
        }
    }
    public int getHTTP_port() {
        return HTTP_port;
    }

    public void setHTTP_port(int HTTP_port) {
        this.HTTP_port = HTTP_port;
    }

    public int getSIP_port() {
        return SIP_port;
    }

    public void setSIP_port(int SIP_port) {
        this.SIP_port = SIP_port;
    }

    public String getConfig_file_name() {
        return config_file_name;
    }

    public void setConfig_file_name(String config_file_name) {
        this.config_file_name = config_file_name;
    }
}
