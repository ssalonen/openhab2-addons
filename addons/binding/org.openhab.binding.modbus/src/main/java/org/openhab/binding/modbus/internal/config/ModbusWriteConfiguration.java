package org.openhab.binding.modbus.internal.config;

/**
 *
 * @author Sami Salonen
 *
 */
public class ModbusWriteConfiguration {
    private int start;
    private String type;
    private String trigger;
    private String transform;
    private String valueType;
    private boolean writeMultipleEvenWithSingleRegister;

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }

    public boolean isWriteMultipleEvenWithSingleRegister() {
        return writeMultipleEvenWithSingleRegister;
    }

    public void setWriteMultipleEvenWithSingleRegister(boolean writeMultipleEvenWithSingleRegister) {
        this.writeMultipleEvenWithSingleRegister = writeMultipleEvenWithSingleRegister;
    }

}
