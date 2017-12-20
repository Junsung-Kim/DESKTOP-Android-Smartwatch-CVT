import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class Session implements Runnable {
    private StreamConnection mConnection = null;
    private InputStream mBtIn = null;
    private OutputStream mBtOut = null;
    private int index;

    Session(StreamConnection channel, int index) throws IOException {
        mConnection = channel;
        mBtIn = channel.openInputStream();
        mBtOut = channel.openOutputStream();
        this.index = index;
    }

    @Override
    public void run() {
        try {
            byte[] buff = new byte[512];
            int n;
            while ((n = mBtIn.read(buff)) > 0) {
                String data = new String(buff, 0, n);
                data = data.concat(":::" + String.valueOf(index));
                float[] tmpSensorData = new float[3];
                int tmpIndex = 0;
                for(int i = 0 ; i < 4; i++) {
                    if(i < 3)
                        tmpSensorData[i] = Float.parseFloat(data.split(":::")[i]);
                    else
                        tmpIndex = Integer.parseInt(data.split(":::")[i]);
                }
                BluetoothSession.sWears[tmpIndex].setSensorData(tmpSensorData);
                mBtOut.write(data.toUpperCase().getBytes());
                mBtOut.flush();
            }

        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            close();
        }
    }

    private void close() {
        BluetoothSession.log("Session close");
        if(mBtIn != null) try {mBtIn.close();} catch (Exception ignored) {}
        if(mBtOut != null) try {mBtOut.close();} catch (Exception ignored) {}
        if(mConnection != null) try { mConnection.close();} catch (Exception ignored) {}
    }
}
