class SensorDataSet {
    float[] data;

    SensorDataSet(float[] data) {
        this.data = new float[3];
        for(int i = 0; i < 3; i++)
            this.data[i] = round(data[i]);
    }

    private float round(float target) {
        String s = String.format("%.2f", target);
        return Float.parseFloat(s);
    }
}
