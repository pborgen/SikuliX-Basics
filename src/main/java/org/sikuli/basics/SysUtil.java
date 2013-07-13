/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sikuli.basics;

import java.lang.reflect.Constructor;
import org.sikuli.basics.OSUtil;

/**
 *
 * @author rhocke
 */
public class SysUtil {
  static OSUtil osUtil = null;

  static String getOSUtilClass() {
    String pkg = "org.sikuli.system.";
    switch (Settings.getOS()) {
      case Settings.ISMAC:
        return pkg + "MacUtil";
      case Settings.ISWINDOWS:
        return pkg + "WinUtil";
      case Settings.ISLINUX:
        return pkg + "LinuxUtil";
      default:
        Debug.error("BasicsUtil: Fatal error 300: your OS is not supported");
        SikuliX.terminate(300);
        return null;
    }
  }

  public static OSUtil getOSUtil() {
    if (osUtil == null) {
      try {
        Class c = Class.forName(SysUtil.getOSUtilClass());
        Constructor constr = c.getConstructor();
        osUtil = (OSUtil) constr.newInstance();
      } catch (Exception e) {
        Debug.error("Can't create OS Util: " + e.getMessage());
      }
    }
    return osUtil;
  }
  
}
