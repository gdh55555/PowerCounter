package com.lab.powercounter.phone;

/**
 * Created by Admin on 2016/7/5.
 */
public interface PhoneConstants {
    /* Return the name of the model represented by these constants
     */
    public String modelName();

    /* Gives the maximum power that this phone can drain from its battery.
     */
    public double maxPower();

    /* Gives the coefficient to multiply the LCD display's brightness by to
     * calculate power usage.
     */
    public double lcdBrightness();

    /* Gives the power usage for the lcd display that is incurred by the display
     * just being turned on.
     */
    public double lcdBacklight();

    /* Gives the base power usage for the OLED display for just being on.
     */
    public double oledBasePower();

    /* Gives the power coefficient for rgb channels (in that order) for a signle
     * pixel.
     */
    public double[] oledChannelPower();

    /* Gives the modulation coefficient for the per pixel power calculation.
     */
    public double oledModulation();

    /* Gives the coefficients at different cpu frequencies for the amount of
     * power/cpu utilization the processor is using.
     */
    public double[] cpuPowerRatios();

    /* Gives the frequency for each of the power ratios listed in
     * cpuPowerRatios().
     */
    public double[] cpuFreqs();

    /* Gives the usage for the audio output being used.  The model doesn't
     * currently take into account volume.
     */
    public double audioPower();

    /* Gives the power consumption for each of the GPS states.  These states are
     * {OFF, SLEEP, ON} in that order.  See GPS.java.
     */
    public double[] gpsStatePower();

    /* Gives the time in seconds that the GPS sleeps for after the session
     * has ended.
     */
    public double gpsSleepTime();

    /* Gives the power consumption of wifi in the low power state.
     */
    public double wifiLowPower();

    /* Gives the base power consumption while the wifi is in high power mode.
     */
    public double wifiHighPower();

    /* Gives the packet rate needed to transition from the low power state
     * to the high power state.
     */
    public double wifiLowHighTransition();

    /* Gives the packet rate needed to transition from the high power state
     * to the low power state.
     */
    public double wifiHighLowTransition();

    /* Gives the power/uplinkrate for different link speeds for wifi in high
     * power mode.
     */
    public double[] wifiLinkRatios();

    /* Gives the link speed associated with each link power ratio.  Elements
     * should be in increasing order.  Should have the same number of elements
     * as wifiLinkRatios().
     */
    public double[] wifiLinkSpeeds();

    /* Gives the name of the 3G interface for this phone.
     */
    public String cellularInterface();

    /* Gives the power consumed while the 3G interface is in the idle state.
     */
    public double cellularIdlePower(String oper);

    /* Gives the power consumed while the 3G interface is in the FACH state.
     */
    public double cellularFachPower(String oper);

    /* Gives the power consumed while the 3G interface is in the DCH state.
     */
    public double cellularDchPower(String oper);

    /* Gives the number of bytes in the uplink queue.
     */
    public int cellularUplinkQueue(String oper);

    /* Gives the number of bytes in the downlink queue.
     */
    public int cellularDownlinkQueue(String oper);

    /* Gives the time in seconds that the 3G interface stays idle in the DCH state
     * before transitioning to the FACH state.
     */
    public int cellularDchFachDelay(String oper);

    /* Gives the time in seconds that the 3G interface stays idle in the FACH
     * state before transitioning to the IDLE state.
     */
    public int cellularFachIdleDelay(String oper);

    /* Gives the power consumed by each of the sensors.  Should have the same size
     * as Sensors.MAX_SENSORS.
     */
    public double[] sensorPower();

    /* Gives the maximum power in mW that the named component can generate.
     */
    public double getMaxPower(String componentName);
}
