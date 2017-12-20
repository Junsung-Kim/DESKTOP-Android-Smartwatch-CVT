import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.DynamicTimeSeriesCollection;
import org.jfree.data.time.Second;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

class ChartFrame extends ApplicationFrame {
    private static final String TITLE = "Wear index: ";
    private static final String START = "Start";
    private static final String STOP = "Stop";
    private static final float MIN_MAX = 10;
    private static final int COUNT = 2 * 60;
    private static final int FAST = 50;
    private static final int SLOW = FAST * 2;
    private static int sTimestamp = 0;
    private JTextArea timestampView;
    private Timer mTimer;
    static String sSaveDirectory = "h";

    // LED
    private static final JPanel[] ledPanels = new JPanel[BluetoothSession.sNumOfDevices];
    private static final JButton[] ledButtons = new JButton[BluetoothSession.sNumOfDevices];

    private ArrayList[] mSensorArrayLists;
    private static final int RECORDING_LENGTH = 10;
    private static int[] sInitTimestamps;
    private static final int PENDING_LENGTH = 20;

    ChartFrame(String title, int numOfWears) {
        super(title);
        // initialize wears
        for(int i = 0 ; i < numOfWears; i++) {
            BluetoothSession.sWears[i] = new Wear();
            BluetoothSession.sWears[i].initSensorData();
        }

        mSensorArrayLists = new ArrayList[numOfWears];
        sInitTimestamps = new int[numOfWears];
        for(int i = 0 ; i < numOfWears; i++)
            mSensorArrayLists[i] = new ArrayList<SensorDataSet>();

        // initialize data collections
        final DynamicTimeSeriesCollection[] dataCollections = new DynamicTimeSeriesCollection[numOfWears];
        JFreeChart[] charts = new JFreeChart[numOfWears];
        for(int i = 0 ; i < numOfWears; i++) {
            dataCollections[i] = new DynamicTimeSeriesCollection(3, COUNT, new Second());
            dataCollections[i].setTimeBase(new Second());
            for(int j = 0; j < 3; j++) {
                dataCollections[i].addSeries(getSensorData(j, i), j, "graph " + String.valueOf(j));
            }
            charts[i] = createChart(dataCollections[i], i);
        }

        // initialize LED (button)
        for(int i = 0 ; i < numOfWears; i++) {
            ledPanels[i] = new JPanel();
            ledPanels[i].setLayout(new BorderLayout(0, 0));
            ledButtons[i] = new JButton("X");
            ledButtons[i].setBackground(Color.RED);
            ledPanels[i].add(ledButtons[i]);
        }

        // initialize button
        final JButton runButton = new JButton(STOP);
        runButton.addActionListener(e -> {
            String cmd = e.getActionCommand();
            if (STOP.equals(cmd)) {
                mTimer.stop();
                runButton.setText(START);
            } else {
                mTimer.start();
                runButton.setText(STOP);
            }
        });

        // initialize comboBox
        final JComboBox<String> comboBox = new JComboBox<>();
        comboBox.addItem("Slow");
        comboBox.addItem("Fast");
        comboBox.addActionListener(e -> {
            if ("Fast".equals(comboBox.getSelectedItem())) {
                mTimer.setDelay(FAST);
            } else {
                mTimer.setDelay(SLOW);
            }
        });

        // initialize timestamp view
        timestampView = new JTextArea();
        timestampView.setText(String.valueOf(sTimestamp));

        // add charts and LED
        JPanel chartPanel = new JPanel();
        JPanel[] chartRecords = new JPanel[numOfWears];
        for(int i = 0 ; i < numOfWears; i++) {
            chartRecords[i] = new JPanel();
            chartRecords[i].add(new ChartPanel(charts[i]));
            chartRecords[i].add(ledPanels[i]);
            chartRecords[i].setLayout(new BoxLayout(chartRecords[i], BoxLayout.X_AXIS));
            chartRecords[i].setBackground(Color.WHITE);
            chartPanel.add(chartRecords[i]);
        }
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
        this.add(chartPanel, BorderLayout.CENTER);

        // add button and comboBox box
        JPanel btnPanel = new JPanel(new FlowLayout());
        btnPanel.add(timestampView);
        btnPanel.add(runButton);
        btnPanel.add(comboBox);
        this.add(btnPanel, BorderLayout.SOUTH);

        mTimer = new Timer(SLOW, new ActionListener() {
            float[][] sensorData = new float[numOfWears][3];
            @Override
            public void actionPerformed(ActionEvent e) {
                for(int i = 0 ; i < numOfWears; i++) {
                    float tmp = 0.0f;
                    for(int j = 0 ; j < 3; j++) {
                        // i: wear index, j: graph index
                        sensorData[i][j] = getValue(j, i);
                        tmp += Math.abs(sensorData[i][j]);
                    }
                    BluetoothSession.sWears[i].slidingWindows.update(tmp);

                    dataCollections[i].advanceTime();
                    dataCollections[i].appendData(sensorData[i]);

                    if (BluetoothSession.sWears[i].slidingWindows.check()
                            && !BluetoothSession.sWears[i].pendingFlag) {
                        ledButtons[i].setBackground(Color.green);
                        ledButtons[i].setText("O");

                        if (!BluetoothSession.sWears[i].recordingFlag) {
                            mSensorArrayLists[i].clear();
                            sInitTimestamps[i] = sTimestamp;
                            BluetoothSession.sWears[i].recordingFlag = true;
                            BluetoothSession.sWears[i].recordingIndex++;
                            mSensorArrayLists[i].add(new SensorDataSet(sensorData[i]));
                        }
                        else {
                            BluetoothSession.sWears[i].recordingIndex++;
                            mSensorArrayLists[i].add(new SensorDataSet(sensorData[i]));
                            if (BluetoothSession.sWears[i].recordingIndex % RECORDING_LENGTH == 0) {
                                BluetoothSession.sWears[i].recordingFlag = false;
                                saveRecord(i, sInitTimestamps[i], mSensorArrayLists[i]);

                                // enter pending status
                                BluetoothSession.sWears[i].pendingFlag = true;
                                BluetoothSession.sWears[i].pendingIndex++;
                                ledButtons[i].setBackground(Color.yellow);
                                ledButtons[i].setText("P");
                            }
                        }
                    } else if (!BluetoothSession.sWears[i].slidingWindows.check()
                            && !BluetoothSession.sWears[i].pendingFlag) {
                        ledButtons[i].setBackground(Color.RED);
                        ledButtons[i].setText("X");

                        if (BluetoothSession.sWears[i].recordingFlag) {
                            BluetoothSession.sWears[i].recordingIndex++;
                            mSensorArrayLists[i].add(new SensorDataSet(sensorData[i]));
                            if (BluetoothSession.sWears[i].recordingIndex % RECORDING_LENGTH == 0) {
                                BluetoothSession.sWears[i].recordingFlag = false;
                                saveRecord(i, sInitTimestamps[i], mSensorArrayLists[i]);

                                // enter pending status
                                BluetoothSession.sWears[i].pendingFlag = true;
                                BluetoothSession.sWears[i].pendingIndex++;
                                ledButtons[i].setBackground(Color.yellow);
                                ledButtons[i].setText("P");
                            }
                        }
                    } else if (BluetoothSession.sWears[i].pendingFlag) {
                        BluetoothSession.sWears[i].pendingIndex++;
                        if (BluetoothSession.sWears[i].pendingIndex % PENDING_LENGTH == 0)
                            BluetoothSession.sWears[i].pendingFlag = false;
                    }
                }

                sTimestamp++;
                timestampView.setText("Timestamp: " + String.valueOf(sTimestamp));
            }
        });
    }

    private JFreeChart createChart(final XYDataset dataSet, int wearIndex) {
        final JFreeChart result = ChartFactory.createTimeSeriesChart(
                TITLE + String.valueOf(wearIndex),
                "hh:mm:ss", "Radian", dataSet, true, true, false);
        final XYPlot plot = result.getXYPlot();
        ValueAxis domain = plot.getDomainAxis();
        domain.setAutoRange(true);
        ValueAxis range = plot.getRangeAxis();
        range.setRange(-MIN_MAX, MIN_MAX);
        return result;
    }

    private float[] getSensorData(int sensorIndex, int wearIndex) {
        float[] a = new float[COUNT];
        for (int i = 0; i < a.length; i++) {
            a[i] = getValue(sensorIndex, wearIndex);
        }
        return a;
    }

    private float getValue(int sensorIndex, int wearIndex) {
        return BluetoothSession.sWears[wearIndex].getSensorData(sensorIndex);
    }

    void start() {
        mTimer.start();
    }

    private void saveRecord(int wearIndex, int timestamp, ArrayList<SensorDataSet> arrayList) {
        String name = "output/" + sSaveDirectory + "/" +
                String.valueOf(timestamp) + "W" + String.valueOf(wearIndex) + ".csv";
        try {
            PrintWriter writer = new PrintWriter(name);
            int size = arrayList.size();

            for (SensorDataSet anArrayList : arrayList) {
                String tmp = String.valueOf(anArrayList.data[0]) + " , "
                        + String.valueOf(anArrayList.data[1]) + " , "
                        + String.valueOf(anArrayList.data[2]);
                writer.println(tmp);
            }

            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
