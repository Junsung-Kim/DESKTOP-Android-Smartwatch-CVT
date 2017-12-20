class Wear {
    private float[] mSensorData = new float[3];
    SlidingWindow slidingWindows;
    boolean recordingFlag = false;
    int recordingIndex = 0;
    boolean pendingFlag = false;
    int pendingIndex = 0;

    // Threshold
    static int sSizeOfSlidingWindow = 0;
    static float sThresholdOfSlidingWindows = 0.0f;

    Wear() {
        slidingWindows = new SlidingWindow(sSizeOfSlidingWindow, sThresholdOfSlidingWindows);
    }

    float getSensorData(int index) {
        return mSensorData[index];
    }

    void setSensorData(float[] mSensorData) {
        this.mSensorData = mSensorData;
    }

    void initSensorData() {
        for(int i = 0 ; i < 3; i++)
            this.mSensorData[i] = 0.0f;
    }
}
