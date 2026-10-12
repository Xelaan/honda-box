package com.crewbeat.hondabox;

/** Only actions found in the stock Honda UI. No arbitrary command/argument input. */
public enum AudioCommand {
    RADIO("AM/FM", 0, 0), CD("CD", 1, 0), USB("USB", 3, 0),
    PREVIOUS("Предыдущий", -1, 3), NEXT("Следующий", -1, 4);
    public final String label;
    public final int mode, key;
    AudioCommand(String label, int mode, int key) { this.label = label; this.mode = mode; this.key = key; }
    public String title(){return I18n.stored(label);}
    public boolean selectsSource() { return mode >= 0; }
}
