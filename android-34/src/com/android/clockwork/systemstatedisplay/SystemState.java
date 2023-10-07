package com.android.clockwork.systemstatedisplay;

/** A class that contains a system state information. */
class SystemState {

  static final SystemState INVALID_STATE = new SystemState("", "");

  private final String mTitle;
  private final String mValue;

  SystemState(String title, String value) {
    mTitle = title;
    mValue = value;
  }

  /** Returns the title of the state. */
  String getTitle() {
    return mTitle;
  }

  /** Returns the value of the state. */
  String getValue() {
    return mValue;
  }
}
