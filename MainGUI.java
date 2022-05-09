import java.io.UnsupportedEncodingException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.text.TextAlignment;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.input.KeyCode;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.collections.ObservableList;
import java.lang.Math;
import javafx.scene.input.KeyEvent;
import javafx.event.EventHandler;

public class MainGUI extends Application {
    // MVC objects
    BoardDriver board;
    Controller control;

    // GUI objects
    final int numChannels = 4; // much, but not all, of the GUI is dynamically created

    Label[] lblChannelTitles;
    TextField[] txtChannelAlias;
    TextField[] txtVset;
    TextField[] txtVoutRead;
    TextField[] txtIout;
    TextField[] txtPower;
    TextField[] txtVlimit;
    TextField[] txtIlimit;
    Button[] btnChannelEnable;
    Boolean[] channelEnabled;

    Button btnHeartbeat;
    Boolean heartbeatLit = false;

    final int MAX_CHART_VALUES = 10;
    XYChart.Series[] seriesV;
    Boolean[] seriesVInitialized;

    int[] guiChannelColumns;
    final int CH1_COL       = 0;
    final int CH2_COL       = 4;
    final int CH3_COL       = 8;
    final int CH4_COL       = 12;

    final int CHANNEL_ALIAS_ROW = 0;
    final int TITLE_ROW     = CHANNEL_ALIAS_ROW + 1;
    final int VOUT_SET_ROW  = TITLE_ROW + 1;
    final int VOUT_READ_ROW = VOUT_SET_ROW + 1;
    final int IOUT_READ_ROW      = VOUT_READ_ROW + 1;
    final int POWER_ROW     = IOUT_READ_ROW + 1;
    final int VLIMIT_ROW    = POWER_ROW + 1;
    final int ILIMIT_ROW    = VLIMIT_ROW + 1;
    final int EN_BUTTON_ROW = ILIMIT_ROW + 1;
    final int PLOT_ROW  = EN_BUTTON_ROW + 1;
    final int PROGRESS_ROW  = PLOT_ROW + 1;

    public MainGUI() throws UnsupportedEncodingException, InterruptedException {
        board = new BoardDriver();
        control = new Controller(board, this);
    }

    public void initialize() {

        txtVset = new TextField[numChannels];
        for (int i=0 ; i<numChannels ; i++) {
            int channel = i;
            txtVset[i] = new TextField("0.0");
            txtVset[i].setOnKeyPressed(event -> {
                if(event.getCode().equals(KeyCode.ENTER)){
                    double v;
                    try {
                        v = Double.parseDouble(txtVset[channel].getText());
                    } catch (Exception e) {
                        return;
                    }
                    try {
                        control.setV(channel+1, v);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        guiChannelColumns = new int[numChannels];
        for (int i=0 ; i<numChannels ; i++) {
            guiChannelColumns[i] = 4*i;
        }

        channelEnabled = new Boolean[numChannels];
        for (int i=0 ; i<numChannels ; i++) {
            channelEnabled[i] = false;
        }

        seriesV = new XYChart.Series[numChannels];
        for (int i=0 ; i<numChannels ; i++) {
            seriesV[i] = new XYChart.Series();
            seriesV[i].setName("CH" + (i+1));
            // an empty series can't be added to a chart, so add a negative time stamp value which can be removed later
            seriesV[i].getData().add(new XYChart.Data(-1.0,0.0));
        }

        txtChannelAlias = new TextField[numChannels];
        for (int i=0 ; i<numChannels ; i++) {
            int channel = i;
            txtChannelAlias[i] = new TextField("CH"+(i+1));
            txtChannelAlias[i].setOnKeyPressed(event -> {
                if(event.getCode().equals(KeyCode.ENTER)){
                    String str = txtChannelAlias[channel].getText();
                    seriesV[channel].setName(str);
                }
            });
        }
        
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        initialize();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        Scene scene = new Scene(grid, 900, 700);
        primaryStage.setScene(scene);
    
        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        // Dynamically allocated and placed GUI objects

        lblChannelTitles = new Label[numChannels];
        txtVoutRead = new TextField[numChannels];
        txtIout = new TextField[numChannels];
        txtPower = new TextField[numChannels];
        txtVlimit = new TextField[numChannels];
        txtIlimit = new TextField[numChannels];
        btnChannelEnable = new Button[numChannels];
        Label lblPowerCalc[] = new Label[numChannels];
        Label lblVlimit[] = new Label[numChannels];
        Label lblIlimit[] = new Label[numChannels];
        Label lblVoutSets[] = new Label[numChannels];
        Label lblVoutReads[] = new Label[numChannels];
        Label lblIoutReads[] = new Label[numChannels];
        Label lblVsetUnits[] = new Label[numChannels];
        Label lblVreadUnits[] = new Label[numChannels];
        Label lblIoutUnits[] = new Label[numChannels];
        Label lblPowerUnits[] = new Label[numChannels];
        Label lblVoutLimitUnits[] = new Label[numChannels];
        Label lblIoutLimitUnits[] = new Label[numChannels];

        scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            public void handle(KeyEvent ke) {
                if (ke.getCode() == KeyCode.F1) {
                    toggleChannel(1);
                    //System.out.println("Key Pressed: " + ke.getCode());
                    ke.consume(); // <-- stops passing the event to next node
                }
                else if (ke.getCode() == KeyCode.F2) {
                    toggleChannel(2);
                    ke.consume(); // <-- stops passing the event to next node
                }
                else if (ke.getCode() == KeyCode.F3) {
                    toggleChannel(3);
                    ke.consume(); // <-- stops passing the event to next node
                }
                else if (ke.getCode() == KeyCode.F4) {
                    toggleChannel(4);
                    ke.consume(); // <-- stops passing the event to next node
                }

            }
        });

        for (int i = 0 ; i < numChannels ; i++) {
            txtChannelAlias[i].setAlignment(Pos.CENTER);
            txtChannelAlias[i].setEditable(true);
            txtChannelAlias[i].setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
            GridPane.setHalignment(txtChannelAlias[i], HPos.CENTER);
            grid.add(txtChannelAlias[i], guiChannelColumns[i], CHANNEL_ALIAS_ROW,3,1);

            lblChannelTitles[i] = new Label("(Channel " + (i+1) + " )");
            lblChannelTitles[i].setFont(Font.font("Tahoma", FontWeight.NORMAL, 15));
            lblChannelTitles[i].setStyle("-fx-background-color: #cccccc; ");
            GridPane.setHalignment(lblChannelTitles[i], HPos.CENTER);
            grid.add(lblChannelTitles[i], guiChannelColumns[i], TITLE_ROW, 3, 1);

            txtVset[i].setAlignment(Pos.CENTER);
            txtVset[i].setPrefWidth(60);
            txtVset[i].setEditable(true);
            grid.add(txtVset[i], guiChannelColumns[i]+1, VOUT_SET_ROW);

            txtVoutRead[i] = new TextField("0.0");
            txtVoutRead[i].setPrefWidth(60);
            txtVoutRead[i].setAlignment(Pos.CENTER);
            txtVoutRead[i]. setStyle("-fx-background-color: #cccccc; ");
            grid.add(txtVoutRead[i], guiChannelColumns[i]+1, VOUT_READ_ROW);

            txtIout[i] = new TextField("0.0");
            txtIout[i].setAlignment(Pos.CENTER);
            txtIout[i].setPrefWidth(60);
            txtIout[i].setEditable(false);
            txtIout[i]. setStyle("-fx-background-color: #cccccc; ");
            grid.add(txtIout[i], guiChannelColumns[i]+1, IOUT_READ_ROW);
    
            txtPower[i] = new TextField("0.0");
            txtPower[i].setAlignment(Pos.CENTER);
            txtPower[i].setPrefWidth(60);
            txtPower[0].setEditable(false);
            txtIout[i]. setStyle("-fx-background-color: #cccccc; ");
            grid.add(txtPower[i], guiChannelColumns[i]+1, POWER_ROW);

            txtVlimit[i] = new TextField("5.0");
            txtVlimit[i].setAlignment(Pos.CENTER);
            txtVlimit[i].setPrefWidth(60);
            grid.add(txtVlimit[i], guiChannelColumns[i]+1, VLIMIT_ROW);

            lblVoutSets[i] = new Label("Vout set");
            GridPane.setHalignment(lblVoutSets[i], HPos.RIGHT);
            lblVoutSets[i].setTextAlignment(TextAlignment.RIGHT);
            grid.add(lblVoutSets[i], guiChannelColumns[i], VOUT_SET_ROW);
            
            lblVoutReads[i] = new Label("Vout");
            GridPane.setHalignment(lblVoutReads[i], HPos.RIGHT);
            lblVoutReads[i].setTextAlignment(TextAlignment.RIGHT);
            grid.add(lblVoutReads[i], guiChannelColumns[i], VOUT_READ_ROW);
    
            lblIoutReads[i] = new Label("Iout");
            GridPane.setHalignment(lblIoutReads[i], HPos.RIGHT);
            lblIoutReads[i].setTextAlignment(TextAlignment.RIGHT);
            grid.add(lblIoutReads[i], guiChannelColumns[i], IOUT_READ_ROW);
    
            txtIlimit[i] = new TextField("100");
            txtIlimit[i].setAlignment(Pos.CENTER);
            txtIlimit[i].setPrefWidth(60);
            txtIout[i].setStyle("-fx-background-color: #cccccc; ");
            grid.add(txtIlimit[i], guiChannelColumns[i]+1, ILIMIT_ROW);

            lblPowerCalc[i] = new Label("Power");
            GridPane.setHalignment(lblPowerCalc[i], HPos.RIGHT);
            lblPowerCalc[i].setTextAlignment(TextAlignment.RIGHT);
            grid.add(lblPowerCalc[i], guiChannelColumns[i], POWER_ROW);

            lblVlimit[i] = new Label("Vlimit");
            GridPane.setHalignment(lblVlimit[i], HPos.RIGHT);
            lblVlimit[i].setTextAlignment(TextAlignment.RIGHT);
            grid.add(lblVlimit[i], guiChannelColumns[i], VLIMIT_ROW);

            lblIlimit[i] = new Label("Vlimit");
            GridPane.setHalignment(lblIlimit[i], HPos.RIGHT);
            lblIlimit[i].setTextAlignment(TextAlignment.RIGHT);
            grid.add(lblIlimit[i], guiChannelColumns[i], ILIMIT_ROW);

            lblVsetUnits[i] = new Label("V");
            lblVsetUnits[i].setTextAlignment(TextAlignment.LEFT);
            grid.add(lblVsetUnits[i], guiChannelColumns[i]+2, VOUT_SET_ROW);

            lblVreadUnits[i] = new Label("V");
            lblVreadUnits[i].setTextAlignment(TextAlignment.LEFT);
            grid.add(lblVreadUnits[i], guiChannelColumns[i]+2, VOUT_READ_ROW);

            lblIoutUnits[i] = new Label("mA");
            lblIoutUnits[i].setTextAlignment(TextAlignment.LEFT);
            grid.add(lblIoutUnits[i], guiChannelColumns[i]+2, IOUT_READ_ROW);

            lblPowerUnits[i] = new Label("mW");
            lblPowerUnits[i].setTextAlignment(TextAlignment.LEFT);
            grid.add(lblPowerUnits[i], guiChannelColumns[i]+2, POWER_ROW);

            lblVoutLimitUnits[i] = new Label("V");
            lblVoutLimitUnits[i].setTextAlignment(TextAlignment.LEFT);
            grid.add(lblVoutLimitUnits[i], guiChannelColumns[i]+2, VLIMIT_ROW);

            lblIoutLimitUnits[i] = new Label("mA");
            lblIoutLimitUnits[i].setTextAlignment(TextAlignment.LEFT);
            grid.add(lblIoutLimitUnits[i], guiChannelColumns[i]+2, ILIMIT_ROW);

            btnChannelEnable[i] = new Button("OFF");
            btnChannelEnable[i].setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            GridPane.setFillWidth(btnChannelEnable[i], true);
            btnChannelEnable[i].setStyle("-fx-background-color: #ff9999; ");
            int channel = i;
            btnChannelEnable[i].setOnAction(value ->  {
                toggleChannel(channel+1);
            });
            grid.add(btnChannelEnable[i], guiChannelColumns[i], EN_BUTTON_ROW, 3, 1);
    
       }


        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        // Channel 1

        Label lblPadCh1 = new Label("");
        lblPadCh1.setPrefWidth(50);
        grid.add(lblPadCh1,CH1_COL+3,1);


        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        // Channel 2
        
        Label lblPadCh2 = new Label("");
        lblPadCh2.setPrefWidth(50);
        grid.add(lblPadCh2,CH2_COL+3,1);

        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        // Channel 3
        
        Label lblPadCh3 = new Label("");
        lblPadCh3.setPrefWidth(50);
        grid.add(lblPadCh3,CH3_COL+3,1);

        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////////////////
        // Channel 4
        
        btnHeartbeat = new Button();
        btnHeartbeat.setStyle("-fx-background-color: #ff7777; -fx-border-color: #555555;");
        heartbeatLit = false;
        grid.add(btnHeartbeat, 0, PROGRESS_ROW);

        ////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////
        //  Set up Line Chart

        final NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Time (s)");
        xAxis.setAutoRanging(true);
        xAxis.setForceZeroInRange(false);

        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Voltage (V)");
        yAxis.setAutoRanging(true);
        yAxis.setForceZeroInRange(false);
        
        final LineChart<Number,Number> lineChart = 
                new LineChart<Number,Number>(xAxis,yAxis);
                
        lineChart.setTitle("Voltage Monitor");
        
        for (int i=0 ; i<numChannels ; i++) {
            lineChart.getData().add(seriesV[i]);
        }
        
        grid.add(lineChart,0,PLOT_ROW,15,1);

        primaryStage.show();
    }

    public void toggleHeartBeat() {
        if (heartbeatLit) {
            btnHeartbeat.setStyle("-fx-background-color: #eeeeee; -fx-border-color: #555555;");
            heartbeatLit = false;
        }
        else {
            btnHeartbeat.setStyle("-fx-background-color: #ff7777; -fx-border-color: #555555;");
            heartbeatLit = true;
        }
    }

    public void setVout(int channel, double vout, long timeStamp) {
        System.out.println("GUI::setVout(1) time=" + System.currentTimeMillis());
        timeStamp = timeStamp / 100;
        txtVoutRead[channel-1].setText(String.format("%.3f", vout));
        XYChart.Series<Number,Number> series = seriesV[channel-1];
        ObservableList<XYChart.Data<Number,Number>> data = seriesV[channel-1].getData();
        System.out.println("GUI::setVout(2) time=" + System.currentTimeMillis());
        seriesV[channel-1].getData().add(new XYChart.Data(timeStamp, vout));

        int size = data.size();
        System.out.println("GUI::setVout(3) time=" + System.currentTimeMillis() + ", data.size()=" + size);
        System.out.println("GUI::setVout(4) time=" + System.currentTimeMillis() + ", data.size()=" + size);

        // an empty series can't be added to 
        // if (size < 1) {
        //     System.out.println("ERROR: Empty data series, this shouldn't happen");
        // }

        System.out.println("GUI::setVout(5) time=" + System.currentTimeMillis() + ", get(0) = " + (double) data.get(0).getXValue());
        if ( (double) data.get(0).getXValue() < 0.0) {
            System.out.println("First time stamp is negative time, removing the datapoint");
            if (size < 2) {
                System.out.println("Changed my mind, the series has only 1 data point, leaving it be");
            }
            else {
                data.remove(0);
            }
        }
        System.out.println("GUI::setVout(6) time=" + System.currentTimeMillis());
    }

    public void setIout(int channel, double vout, long timeStamp) {
        txtIout[channel-1].setText(String.format("%3.0f", vout));
    }

    public void setPower(int channel, double vout, long timeStamp) {
        txtPower[channel-1].setText(String.format("%3.0f", vout));
    }

    public static void main(String[] args) {
        Application.launch(args);
    }

    private void toggleChannel(int channel) {
        if (control.getChannelEnabled(channel) == false) {
            if (control.enableChannel(channel)) {
                channelEnabled[channel-1] = true;
                btnChannelEnable[channel-1].setText("ON");
                btnChannelEnable[channel-1].setStyle("-fx-background-color: #99ff99; ");
            }
        }
        else {
            if (control.disableChannel(channel)) {
                channelEnabled[channel-1] = false;
                btnChannelEnable[channel-1].setText("OFF");
                btnChannelEnable[channel-1].setStyle("-fx-background-color: #ff9999; ");
            }
        }
    };
}
