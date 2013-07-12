package org.sikuli.basics;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RunSetup {

  //<editor-fold defaultstate="collapsed" desc="new logging concept">
  private static String me = "RunSetup";
  private static String mem = "...";
  private static int lvl = 2;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + mem + ": " + message, args);
  }
  //</editor-fold>

  public static void main(String[] args) {
    mem = "main";
    Debug.setDebugLevel(3);
    if (args.length > 0 ) {
      log(lvl, args[0]);
    }
    else {
      log(lvl, "no args given");
    }
    String target = "/Users/rhocke/Downloads/SikuliXDownloads/";
    String src = "https://launchpad.net/sikuli/sikuli-api/1.0.0/+download/Sikuli-1.0.0-Supplemental-LinuxVisionProxy.zip";
    FileManager.downloadURL(src, target);
      /*
       IResourceLoader loader = FileManager.getNativeLoader("basic", args);
       //    loader.install(args);
       String[] cmd = new String[] {args[0]};
       loader.doSomethingSpecial("runcmd", cmd);
       log(lvl, "result from runcmd" + cmd[0]);
       */
  /*
   * Preferences pref = Preferences.userNodeForPackage(SikuliX.class);
      try {
        //pref.exportNode(new FileOutputStream("/Users/rhocke/SikuliXPrefs.xml"));
        //pref.importPreferences(new FileInputStream("/Users/rhocke/SikuliXPrefs.xml"));
      } catch (Exception ex) {
        log(-1, "problem with /Users/rhocke/SikuliXPrefs.xml\n" + ex.getMessage());
      }
      log(-2, pref.get("USER_NAME", "N/A"));
  */
  }
}
