/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.basics;

import com.melloware.jintellitype.JIntellitype;
import java.util.*;
import org.sikuli.basics.Debug;
import org.sikuli.basics.HotkeyEvent;
import org.sikuli.basics.HotkeyListener;
import org.sikuli.basics.HotkeyManager;

public class WindowsHotkeyManager extends HotkeyManager {

  class HotkeyData {

    int key, modifiers;
    HotkeyListener listener;

    public HotkeyData(int key_, int mod_, HotkeyListener l_) {
      key = key_;
      modifiers = mod_;
      listener = l_;
    }
  };

  class JIntellitypeHandler implements
          com.melloware.jintellitype.HotkeyListener {

    @Override
    public void onHotKey(int id) {
      Debug.log(4, "Hotkey pressed");
      HotkeyData data = _idCallbackMap.get(id);
      HotkeyEvent e = new HotkeyEvent(data.key, data.modifiers);
      data.listener.invokeHotkeyPressed(e);
    }
  };
  private Map<Integer, HotkeyData> _idCallbackMap = new HashMap<Integer, HotkeyData>();
  private int _gHotkeyId = 1;

  @Override
  public boolean _addHotkey(int keyCode, int modifiers, HotkeyListener listener) {
    JIntellitype itype = JIntellitype.getInstance();

    if (_gHotkeyId == 1) {
      itype.addHotKeyListener(new JIntellitypeHandler());
    }

    _removeHotkey(keyCode, modifiers);
    int id = _gHotkeyId++;
    HotkeyData data = new HotkeyData(keyCode, modifiers, listener);
    _idCallbackMap.put(id, data);

    itype.registerSwingHotKey(id, modifiers, keyCode);
    return true;
  }

  @Override
  public boolean addHotkey(int htype, HotkeyListener listener) {
    JIntellitype itype = JIntellitype.getInstance();

    if (_gHotkeyId == 1) {
      itype.addHotKeyListener(new JIntellitypeHandler());
    }

    int keyCode = -1;
    int modifiers = -1;
    if (htype == 1) {
      keyCode = (int) '2';
      modifiers = JIntellitype.MOD_ALT + JIntellitype.MOD_SHIFT;
    } else if (htype == 2) {
      keyCode = (int) 'c';
      modifiers = JIntellitype.MOD_ALT + JIntellitype.MOD_SHIFT;
    } else {
      return false;
    }

    int id = _gHotkeyId++;
    HotkeyData data = new HotkeyData(keyCode, modifiers, listener);
    _idCallbackMap.put(id, data);

    itype.registerHotKey(id, modifiers, keyCode);
    return true;
  }

  @Override
  public boolean _removeHotkey(int keyCode, int modifiers) {
    for (Map.Entry<Integer, HotkeyData> entry : _idCallbackMap.entrySet()) {
      HotkeyData data = entry.getValue();
      if (data.key == keyCode && data.modifiers == modifiers) {
        JIntellitype itype = JIntellitype.getInstance();
        int id = entry.getKey();
        itype.unregisterHotKey(id);
        _idCallbackMap.remove(id);
        return true;
      }
    }
    return false;
  }

  @Override
  public void cleanUp() {
    JIntellitype itype = JIntellitype.getInstance();
    for (Map.Entry<Integer, HotkeyData> entry : _idCallbackMap.entrySet()) {
      int id = entry.getKey();
      itype.unregisterHotKey(id);
    }
    _gHotkeyId = 1;
    _idCallbackMap.clear();
    itype.cleanUp();
  }
}
