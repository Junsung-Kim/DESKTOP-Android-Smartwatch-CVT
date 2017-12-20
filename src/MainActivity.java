import java.util.Scanner;

public class MainActivity {
    public static void main(String[] args) throws Exception {
        Scanner stdIn = new Scanner(System.in);
        System.out.print("Number of device(s): ");
        BluetoothSession.sNumOfDevices = stdIn.nextInt();
        System.out.print("Size of Sliding Window: ");
        Wear.sSizeOfSlidingWindow = stdIn.nextInt();
        System.out.print("Threshold value: ");
        Wear.sThresholdOfSlidingWindows = stdIn.nextFloat();

        BluetoothSession server = new BluetoothSession();

        Session[] sessions = new Session[BluetoothSession.sNumOfDevices];
        for (int i = 0; i < BluetoothSession.sNumOfDevices; i++) {
            sessions[i] = server.accept(i);
            new Thread(sessions[i]).start();
        }

        BluetoothSession.sChartFrame.pack();
        BluetoothSession.sChartFrame.setVisible(true);
        BluetoothSession.sChartFrame.start();

        while (true) {
            ChartFrame.sSaveDirectory = stdIn.next();
        }
    }
}
