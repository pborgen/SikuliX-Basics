/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added RaiMan 2013
 */
package org.sikuli.basics;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Date;
import java.util.Iterator;
import java.util.ServiceLoader;
import javax.swing.JFrame;

/**
 *
 * used as anchor for the preferences store and for global supporting features
 */
public class SikuliX {

  //<editor-fold defaultstate="collapsed" desc="new logging concept">
  private static String me = "SikuliX";
  private static String mem = "...";
  private static int lvl = 2;
  private static String msg;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + mem + ": " + message, args);
  }

  private static void log0(int level, String message, Object... args) {
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + message, args);
  }
  //</editor-fold>

  private static IScriptRunner runner;
  private static final String ScriptSikuliXCL = "org.sikuli.script.SikuliX";
  private static final String ScriptKeyCL = "org.sikuli.script.Key";
  private static Class ScriptCl, KeyCl;
  private static Method endWhat, toJavaKeyCode;
  private static boolean runningSetup = false;

  public static void setRunningSetup(boolean _runningSetup) {
    runningSetup = _runningSetup;
  }

  private static JFrame splash = null;
  private static long start;
 
  public static void displaySplash(String [] args) {
    if (args == null) {
      if (splash != null) {
        splash.dispose();
      }
      if (start > 0) {
        Debug.log(3, "Sikuli-Script startup: " + ((new Date()).getTime() - start));
        start = 0;
      }
      return;
    }
    start = (new Date()).getTime();
    String[] splashArgs = new String[ ] { 
      "splash", "#", "#" + Settings.SikuliVersionScript, "", "#", "#... starting - pls. wait ..." };
    for (String e : args) {
      splashArgs[3] += e + " ";
    }
    splashArgs[3] = splashArgs[3].trim();
    splash = new MultiFrame(splashArgs);
  }
  
  public static void displaySplashFirstTime(String [] args) {
    if (args == null) {
      if (splash != null) splash.dispose();
      if (start > 0) {
        Debug.log(3, "Sikuli-IDE environment setup: " + ((new Date()).getTime() - start));
        start = 0;
     }
      return;
    }
    start = (new Date()).getTime();
    String[] splashArgs = new String[ ] { 
      "splash", "#", "#SikuliX-IDE-1.0.1", "#... setting up environement - pls. wait ..." };
    splash = new MultiFrame(splashArgs);
  }
  
  private static void callScriptEndMethod(String m, int n) {
    try {
      ScriptCl = Class.forName(ScriptSikuliXCL);
      endWhat = ScriptCl.getMethod(m, new Class[]{int.class});
      endWhat.invoke(ScriptCl, new Object[]{n});
    } catch (Exception ex) {
      Debug.error("BasicsFinalCleanUp: Fatal Error 999: could not be run!");
      System.exit(999);
    }
  }

  public static int[] callKeyToJavaKeyCodeMethod(char key) {
    try {
      KeyCl = Class.forName(ScriptKeyCL);
      toJavaKeyCode = KeyCl.getMethod("toJavaKeyCode", new Class[]{Character.class});
      return (int[]) toJavaKeyCode.invoke(KeyCl, new Object[]{key});
    } catch (Exception ex) {
      return null;
    }
  }

  public static void endNormal(int n) {
    callScriptEndMethod("endNormal", n);
  }

  public static void endWarning(int n) {
    callScriptEndMethod("endWarning", n);
  }

  public static void endError(int n) {
    callScriptEndMethod("endError", n);
  }

  public static void terminate(int n) {
    Debug.error("Terminating SikuliX after a fatal error"
            + (n == 0 ? "" : "(%d)")
            + "! Sorry, but it makes no sense to continue!\n"
            + "If you do not have any idea about the error cause or solution, run again\n"
            + "with a Debug level of 3. You might paste the output to the Q&A board.", n);
    if (runningSetup) {
      RunSetup.popError("Something serious happened! Sikuli not useable!\n" +
              "Check the error log at " + Debug.logfile);
      System.exit(0);
    }
    cleanUp(0);
    System.exit(1);
  }

  public static void cleanUp(int n) {
    callScriptEndMethod("cleanUp", n);
  }

  public static boolean removeHotkey(char key, int modifiers) {
    return HotkeyManager.getInstance().removeHotkey(key, modifiers);
  }

  public static boolean removeHotkey(String key, int modifiers) {
    return HotkeyManager.getInstance().removeHotkey(key, modifiers);
  }

  public static boolean addHotkey(char key, int modifiers, HotkeyListener listener) {
    return HotkeyManager.getInstance().addHotkey(key, modifiers, listener);
  }

  public static boolean addHotkey(String key, int modifiers, HotkeyListener listener) {
    return HotkeyManager.getInstance().addHotkey(key, modifiers, listener);
  }

  /**
   * Finds a ScriptRunner implementation to execute the script.
   *
   * @param name Name of the ScriptRunner, might be null (then type is used)
   * @param ending fileending of script to run
   * @return first ScriptRunner with matching name or file ending, null if none found
   */
  public static IScriptRunner getScriptRunner(String name, String ending, String[] args) {
    runner = null;
    ServiceLoader<IScriptRunner> loader = ServiceLoader.load(IScriptRunner.class);
    Iterator<IScriptRunner> scriptRunnerIterator = loader.iterator();
    while (scriptRunnerIterator.hasNext()) {
      IScriptRunner currentRunner = scriptRunnerIterator.next();
      if ((name != null && currentRunner.getName().toLowerCase().equals(name.toLowerCase())) || (ending != null && currentRunner.hasFileEnding(ending) != null)) {
        runner = currentRunner;
        runner.init(args);
        break;
      }
    }
    if (runner == null) {
      if (name != null) {
        Debug.error("Fatal error 121: Could not load script runner with name: %s", name);
        SikuliX.terminate(121);
      } else if (ending != null) {
        Debug.error("Fatal error 120: Could not load script runner for ending: %s", ending);
        SikuliX.terminate(120);
      } else {
        Debug.error("Fatal error 122: While loading script runner with name=%s and ending= %s", name, ending);
        SikuliX.terminate(122);
      }
    }
    return runner;
  }

  public static IScriptRunner setRunner(IScriptRunner _runner) {
    runner = _runner;
    return runner;
  }

  public static IScriptRunner getRunner() {
    return runner;
  }

  protected static boolean addToClasspath(String jar) {
    Method method;
    URLClassLoader sysLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    URL[] urls = sysLoader.getURLs();
    log0(lvl, "before adding to classpath: " + jar);
    for (int i = 0; i < urls.length; i++) {
      log0(lvl, "%d: %s", i, urls[i]);
    }
    Class sysclass = URLClassLoader.class;
    try {
      jar = FileManager.slashify(new File(jar).getAbsolutePath(), false);
      if (Settings.isWindows()) {
        jar = "/" + jar;
      }
      URL u = (new URI("file", jar, null)).toURL();
      method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
      method.setAccessible(true);
      method.invoke(sysLoader, new Object[]{u});
    } catch (Exception ex) {
      log0(-1, ex.getMessage());
      return false;
    }
    urls = sysLoader.getURLs();
    log0(lvl, "after adding to classpath");
    for (int i = 0; i < urls.length; i++) {
      log0(lvl, "%d: %s", i, urls[i]);
    }
    return true;
  }
}
