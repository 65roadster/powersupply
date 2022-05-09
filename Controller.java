import java.io.UnsupportedEncodingException;
import java.util.Scanner;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import javafx.util.Duration;


// MVC Architecture
//
// Controller handles
// - periodic polling of the hardware
// - inputs from the GUI which require commands sent to the hardware

public class Controller {
    BoardDriver board;
    MainGUI gui;

    long time1, time2;
    long timeZero; // reference point for 0:00:00

    final int numChannels = 4;
    ChannelInfo[] channels;

    SerialPort activePort;
    SerialPort[] ports = SerialPort.getCommPorts();

    class ChannelInfo {
        double vSet = 3.3;
        double Vmax = 4.096;
        double Vmin = 0;
        double vRead = 0;
        double iRead = 0;
        double power = 0;
        double vLimit = 5;
        double iLimit = 0.1;
        boolean enabled = false;
    }

    public Controller(BoardDriver b, MainGUI g) throws UnsupportedEncodingException, InterruptedException {
        gui = g;

        channels = new ChannelInfo[numChannels];
        for (int i=0 ; i<numChannels ; i++) {
            channels[i] = new ChannelInfo();
        }

        System.out.println("Starting...");
        
        board = b;
        BoardDriver brd = board;
        
        if (!brd.findBoard()) {
            System.out.println("Didn't find board, exiting");
        }
        if (!brd.cmdPing()) {
            System.out.println("Failed ping a");
        }
        System.out.println("Initiatlizing board...");
        System.out.print("  Setting up DAC... ");
        brd.DACSWClear();
        brd.DACSWReset();
        brd.DACConfigSet();
        brd.DACRefSet();
        System.out.println("Done");
        System.out.print("  Setting up ADC... ");
        System.out.println("Done");
        System.out.print("  Setting up linear module 1... ");
        System.out.println("Done");
        System.out.print("  Setting up linear module 2... ");
        System.out.println("Done");
        System.out.print("  Setting up linear module 3... ");
        System.out.println("Done");
        System.out.print("  Setting up linear module 4... ");
        System.out.println("Done");
        // brd.setDAC(1, 0b00000000, 0b00000000);
        // brd.setDAC(1, 0b11111111, 0b11111111);
        // brd.setDAC(1, 0b01111111, 0b11111111);
        // brd.setDAC(1, 0b00000000, 0b00000000);

        // brd.setDAC(2, 0b00000000, 0b00000000);
        // brd.setDAC(2, 0b11111111, 0b11111111);
        // brd.setDAC(2, 0b01111111, 0b11111111);
        //brd.setDAC(2, 0b00000000, 0b00000000);

        // brd.setDAC(3, 0b00000000, 0b00000000);
        //brd.setDAC(3, 0b11111111, 0b11111111);
        // brd.setDAC(3, 0b01111111, 0b11111111);
        //brd.setDAC(3, 0b00000000, 0b00000000);

        // brd.setDAC(4, 0b00000000, 0b00000000);
        // brd.setDAC(4, 0b11111111, 0b11111111);
        // brd.setDAC(4, 0b01111111, 0b11111111);
        // brd.setDAC(4, 0b00000000, 0b00000000);

        timeZero = System.currentTimeMillis();
        startTimer();
    }

    public void showAllPort() {
		int i = 0;
		for(SerialPort port : ports) {
			System.out.print(i + ". " + port.getDescriptivePortName() + ", description: ");
			System.out.println(port.getPortDescription());
			i++;
		}	
	}

	public void setPort(int portIndex) {
		activePort = ports[portIndex];
		
		if (activePort.openPort()) {
			System.out.println(activePort.getPortDescription() + " port opened.");
        }
		
		activePort.addDataListener(new SerialPortDataListener() {
			
			@Override
			public void serialEvent(SerialPortEvent event) {
				int size = event.getSerialPort().bytesAvailable();
				byte[] buffer = new byte[size];
				event.getSerialPort().readBytes(buffer, size);
				for(byte b:buffer)
					System.out.print((char)b);
            }

			@Override
			public int getListeningEvents() { 
				return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;  
            }
        });
    }
	
	public void start() {
		showAllPort();
		Scanner reader = new Scanner(System.in);
		System.out.print("Port? ");
		int p = reader.nextInt();
		setPort(p);
		reader.close();
    }
	
    public Boolean setV(int channel, double voltage) throws InterruptedException {
        
        // if (!channels[channel].enabled){
        //     return false;
        // }

        if (voltage > channels[channel].Vmax) {
            voltage = channels[channel].Vmax;
        }
        else if (voltage < channels[channel].Vmin) {
            voltage = channels[channel].Vmin;
        }

        long vInt = (long) (voltage*1000.0);

        int vIntMSB = (int) vInt/16;
        int vIntLSB = (int) vInt - vIntMSB*16;

        // System.out.println("Decimal="+vInt+", msb="+vIntMSB+", lsb=" + vIntLSB);
        board.setDAC(channel, vIntMSB, vIntLSB);
        channels[channel-1].vSet = voltage;

        return true;
    }

    public double readI(int channel) {
        double iout = 1000*Math.random();
        return iout;
    }

    public double readV(int channel) {
        double vout = Math.random()/10;
        return channels[channel-1].vSet + vout;
    }

    public Boolean enableChannel(int channel) {
        channels[channel-1].enabled = true;
        return true;
    }

    public Boolean disableChannel(int channel) {
        channels[channel-1].enabled = false;
        return true;
    }

    public Boolean getChannelEnabled(int channel) {
        return channels[channel-1].enabled;
    }

    private void startTimer() {
        ScheduledService<Boolean> scheduledService = new ScheduledService<Boolean>() {
            @Override
            protected Task<Boolean> createTask() {
                Task<Boolean> task = new Task<Boolean>() {
                    @Override
                    protected Boolean call() throws Exception {
                        time2 = System.currentTimeMillis();
                        if (time2-time1 > 500) {
                            gui.toggleHeartBeat();    
                            time1 = time2;
                        }
                       
                        for (int chan = 1 ; chan <= numChannels ; chan++) {
                            System.out.println("controller(1) (chan " + chan + "), time = " + System.currentTimeMillis());
                            if (channels[chan-1].enabled) {
                                System.out.println("controller(2) modify (chan " + chan + "), time = " + System.currentTimeMillis());
                                double vout = readV(chan);
                                double iout = readI(chan);
                                gui.setVout(chan, vout, time2-timeZero);
                                gui.setIout(chan, iout, time2-timeZero);
                                gui.setPower(chan, vout*iout, time2-timeZero);
                                System.out.println("controller(3) modify (chan " + chan + "), time = " + System.currentTimeMillis());
                            }
                            System.out.println("\n");
                        }
                        return true;
                    }
                };
                return task;
            }
        };
        scheduledService.setPeriod(Duration.seconds(1));
        time1 = System.currentTimeMillis();
        scheduledService.start();
    }
}