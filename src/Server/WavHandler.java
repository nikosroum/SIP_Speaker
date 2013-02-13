package Server;

import java.io.File;
import javax.media.*;
import javax.media.format.AudioFormat;
import javax.media.protocol.ContentDescriptor;
import javax.media.protocol.DataSource;
import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

import com.sun.speech.freetts.audio.AudioPlayer;
import com.sun.speech.freetts.audio.SingleFileAudioPlayer;

 
import javax.sound.sampled.AudioFileFormat;


/**
 *
 * @author Roumpoutsos Nikolaos - Sapountzis Ioannis
 */
public class WavHandler extends Thread {

    
    static void CreateDefaultWavMessage(String wavname){
        String txt="This is a default message";
        RecordMessage(txt,wavname);
    }
    static void RecordMessage(String message,String wavname) {
        String voiceName = "kevin16";
        String text = "";
        text = message;

        System.out.println(text);

        try {
            VoiceManager voiceManager = VoiceManager.getInstance();
            System.out.println("Voices------"+voiceManager.toString());
            Voice helloVoice = voiceManager.getVoice(voiceName);
            //String wavname=SIPServer.parameters.message_wav;
            if (helloVoice==null){
                System.err.println("Cannot find a voice named "+ voiceName + "\nPlease specify a different voice.");
                System.err.println("Use kevin16 as default instead");
                helloVoice = voiceManager.getVoice("kevin16");//try again with the default voice
                if (helloVoice==null){
                System.err.println("The specified voice is not defined");
                System.exit(1);
                }
            }
            String newfilename = wavname.substring(0,wavname.indexOf(".wav"));
            
            helloVoice.allocate();
            
            //create a audioplayer to dump the output file
            SingleFileAudioPlayer fileplayer = new SingleFileAudioPlayer(newfilename, AudioFileFormat.Type.WAVE);
            //attach the audioplayer 
            helloVoice.setAudioPlayer((AudioPlayer) fileplayer);
            
            fileplayer.begin(10000); //Starts the output of a set of data.
         
            helloVoice.speak(text);//text
            fileplayer.drain(); // Waits for all queued audio to be played
            fileplayer.close();
            helloVoice.deallocate();
            
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void SendWavFile(String wavFileName, String senderIpAddress, String RtpPort) throws Exception {

        MediaLocator locator = new MediaLocator("rtp://" + senderIpAddress + ":" + RtpPort + "/audio");
        File mediaFile = new File(wavFileName);
        DataSource source = Manager.createDataSource(new MediaLocator(mediaFile.toURL()));
        System.out.println("Sending from data source: '" + mediaFile.getAbsolutePath() + "'");

        /*The processor is responsible for reading the file from a file and converting it to
         * an RTP stream.
         * The processor used to read the media from a local file, and produce an
         * output stream which will be handed to the data sink object for broadcast.
         */
        Processor mediaProcessor = null;
        Format[] FORMATS = new Format[]{new AudioFormat(AudioFormat.GSM_RTP,8000,8,1)};
        ContentDescriptor CONTENT_DESCRIPTOR = new ContentDescriptor(ContentDescriptor.RAW_RTP);
        mediaProcessor = Manager.createRealizedProcessor(new ProcessorModel(source, FORMATS, CONTENT_DESCRIPTOR));
        DataSink dataSink = null;
        /* Create the data sink.  The data sink is used to do the actual work 
        of broadcasting the RTP data over a network.
         */
        
        dataSink = Manager.createDataSink(mediaProcessor.getDataOutput(), locator);
        // start transmitting the file over the network.
        // start the processor
        mediaProcessor.start();

        // open and start the data sink
        dataSink.open();
        dataSink.start();
        double duration = mediaProcessor.getDuration().getSeconds();
        System.out.println("Duration of wav file: " + duration + "\r\n");
        
        //wait until the file is transmitted
        Thread.sleep(500 + 1000 * (int) duration);

        dataSink.stop();
        dataSink.close();
        mediaProcessor.stop();
        mediaProcessor.close();

    }
}