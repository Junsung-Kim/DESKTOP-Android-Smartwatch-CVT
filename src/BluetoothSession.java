import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import javax.microedition.io.StreamConnectionNotifier;
import java.io.IOException;
import java.util.Date;

/**
 * Created by Junsung on 2016. 12. 2..
 */
public class BluetoothSession {
    static int sNumOfDevices = 1;
    private static final String SERVER_UUID = "0000110100001000800000805F9B34FB";
    private StreamConnectionNotifier mNotifier = null;
    static Wear[] sWears;
    static ChartFrame sChartFrame;

    BluetoothSession() throws IOException {
        sWears = new Wear[sNumOfDevices];
        sChartFrame = new ChartFrame("Data Incoming", sNumOfDevices);

        mNotifier = (StreamConnectionNotifier) Connector.open(
                "btspp://localhost:" + SERVER_UUID,
                Connector.READ_WRITE, true
        );

        ServiceRecord mRecord = LocalDevice.getLocalDevice().getRecord(mNotifier);
        LocalDevice.getLocalDevice().updateRecord(mRecord);
    }

    Session accept(int index) throws IOException {
        log("Waiting connection...");
        StreamConnection mConnection = mNotifier.acceptAndOpen();
        log("Connected.. watch index: " + String.valueOf(index));
        return new Session(mConnection, index);
    }

    static void log(String msg) {
        System.out.println("[" + (new Date()) + "] " + msg);
    }

}