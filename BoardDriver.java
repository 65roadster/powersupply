import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.lang.Thread;
import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;


public class BoardDriver {
    SerialPort activePort;
    float dacReferenceVoltage = 5.0f;

    public Boolean findBoard() throws UnsupportedEncodingException, InterruptedException {
        System.out.println("Looking for board...");
        SerialPort[] ports = SerialPort.getCommPorts();

        int i = 0;
        System.out.println("  Ports found:");
        for(SerialPort port : ports) {
            System.out.print("    " + i + ". " + port.getDescriptivePortName() + ", ");
            System.out.println(port.getPortDescription());
            i++;
        }	
        
        System.out.println("  Testing..." );
        for(SerialPort port : ports) {
            activePort = port;
            if (activePort.openPort()) {
			    System.out.println("    Port: " + activePort.getSystemPortName());
                System.out.println("      Baud Rate: " + activePort.getBaudRate());
                //activePort.setBaudRate(38400);
                //activePort.setBaudRate(9600);
                //System.out.println("      Baud Rate: " + activePort.getBaudRate());
            }

            if (cmdPing()) {
                System.out.println("  Found board on " + activePort.getSystemPortName());

                byte[] fwVersion;
                fwVersion = getFWVersion();
                String str = new String(fwVersion);
                System.out.println("  Board FW Version: " + str);

                return true;
            }
        }
        System.out.println("Did not find board");

        return false;
    }

    public Boolean cmdPing() throws InterruptedException {
        byte[] message = {'0','0','0','0','0','0'};
        activePort.writeBytes(message, 6);

        Thread.sleep(50);

        byte[] buffer = new byte[64];
        int numBytes = activePort.readBytes(buffer, 64);

        if (numBytes != 4) {
            System.out.println("failed #1, " + String.valueOf(numBytes) + ", " + new String(buffer));
            return false;
        }

        byte[] truncatedResponse = Arrays.copyOfRange(buffer, 0, 4);

        byte[] respReset = {'P', 'I', 'N', 'G'};
        if (Arrays.equals(truncatedResponse, respReset)) {
            return true;
        }
        System.out.println("failed #2");
        return false;
    }

    public byte[] getFWVersion() throws InterruptedException {
        byte[] message = {'0','2','0','0','0','0'};
        activePort.writeBytes(message, 6);

        Thread.sleep(100);

        byte[] buffer = new byte[10];
        activePort.readBytes(buffer, 10);
        //buffer[3]='\r';
        //buffer[4]='\n';

        return buffer;
    }

    public Boolean setDAC(int channel, int msb, int lsb) throws InterruptedException {
        byte[] message = {'0','3',(byte) channel, (byte) msb, (byte) lsb, 0x00};
        //byte[] message = {'0','3',0b1111, 0b1111, 0b1111, 0b1111};
        //System.out.println("bytes: " + byte5 + ","+ byte6 + ","+ byte7 + "," + byte8);
        activePort.writeBytes(message, 6);

        Thread.sleep(1);
        return false;
    }

    public void DACSWClear() throws InterruptedException {
        byte[] message = {'0','4','0','0','0','0'};
        activePort.writeBytes(message, 6);

        //Thread.sleep(1);
    }

    public void DACSWReset() throws InterruptedException {
        byte[] message = {'0','5','0','0','0','0'};
        activePort.writeBytes(message, 6);

        //Thread.sleep(1);
    }

    public void DACRefSet() throws InterruptedException {
        byte[] message = {'0','6','0','0','0','0'};
        activePort.writeBytes(message, 6);

        //Thread.sleep(1);
    }

    public void DACConfigSet() throws InterruptedException {
        byte[] message = {'0','7','0','0','0','0'};
        activePort.writeBytes(message, 6);

        //Thread.sleep(1);
    }

    public void expanderPing() {
        byte[] message = {'1','0','0','0','0','0'};
        activePort.writeBytes(message, 6);
    }
}