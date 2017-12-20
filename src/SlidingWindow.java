class SlidingWindow {
    private int size;
    private float[] data;
    private float threshold;

    SlidingWindow(int size, float threshold) {
        this.size = size;
        this.threshold = threshold;
        data = new float[this.size];
        init();
    }

    private void init() {
        for(int i = 0 ; i < size; i++) {
            data[i] = 0.0f;
        }
    }

    /**
     * Update new value to the end of the sliding window
     */
    void update(float newValue) {
        System.arraycopy(data, 1, data, 0, size - 1);
        data[size - 1] = newValue;
    }


    /**
     * check whether sum is over the threshold value
     * @return whether sum is over the threshold value
     */
    boolean check() {
        float sum = sum();
        return sum > threshold;
    }

    /**
     * calculate sum of data (absolute data)
     * @return sum of
     */
    private float sum() {
        float sum = 0.0f;
        for(int i = 0 ; i < size; i++)
            sum += data[i];
        return sum;
    }
}
