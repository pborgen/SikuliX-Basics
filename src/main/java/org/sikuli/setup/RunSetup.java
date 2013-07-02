package org.sikuli.setup;

import org.sikuli.script.Debug;
import org.sikuli.script.FileManager;
import org.sikuli.script.IResourceLoader;

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
    log(lvl, args[0]);
    IResourceLoader loader = FileManager.getNativeLoader("basic", args);
//    loader.install(args);
    String[] cmd = new String[] {args[0]};
    loader.doSomethingSpecial("runcmd", cmd);
    log(lvl, "result from runcmd" + cmd[0]);
  }
}
