/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * RaiMan 2013
 */

package org.sikuli.basics;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

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
  private static boolean runningFromJar;
  private static String jarPath;
  private static String jarParentPath;
  
  static {
    CodeSource codeSrc = SikuliX.class.getProtectionDomain().getCodeSource();
    if (codeSrc != null && codeSrc.getLocation() != null) {
      URL jarURL = codeSrc.getLocation();
      jarPath = FileManager.slashify(new File(jarURL.getPath()).getAbsolutePath(), false);
      jarParentPath = (new File(jarPath)).getParent();
      if (jarPath.endsWith(".jar")) {
        runningFromJar = true;
      } else {
        jarPath += "/";
      }
    }
  }
  
  public static boolean isRunningFromJar() {
    return runningFromJar;
  }
  
  public static String getJarPath() {
    return jarPath;
  }

  public static String getJarParentPath() {
    return jarParentPath;
  }

  public static void setRunningSetup(boolean _runningSetup) {
    runningSetup = _runningSetup;
  }

  private static JFrame splash = null;
  private static long start = 0;
 
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
      if (splash != null) {
        splash.dispose();
      }
      if (start > 0) {
        Debug.log(3, "Sikuli-IDE environment setup: " + ((new Date()).getTime() - start));
        start = 0;
     }
      return;
    }
    start = (new Date()).getTime();
    String[] splashArgs = new String[] { 
      "splash", "#", "#" + Settings.SikuliVersionIDE, "", "#", "#... setting up environement - pls. wait ..." };
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

  public static int[] callKeyToJavaKeyCodeMethod(String key) {
    try {
      KeyCl = Class.forName(ScriptKeyCL);
      toJavaKeyCode = KeyCl.getMethod("toJavaKeyCode", new Class[]{String.class});
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
  
  public static String[] collectOptions(String type, String[] args) {
    List<String> resArgs = new ArrayList<String>();
    if (args != null) {
      resArgs.addAll(Arrays.asList(args));
    } 
    
    String msg = "-----------------------   You might set some options    -----------------------";
    msg += "\n\n";
    msg += "-r name       ---   Run script name: foo[.sikuli] or foo.skl (no IDE window)";
    msg += "\n";
    msg += "-u [file]        ---   Write user log messages to file (default: <WorkingFolder>/UserLog.txt )";
    msg += "\n";
    msg += "-f [file]         ---   Write Sikuli log messages to file (default: <WorkingFolder>/SikuliLog.txt)";
    msg += "\n";
    msg += "-d n             ---   Set a higher level n for Sikuli's debug messages (default: 0)";
    msg += "\n";
    msg += "-c                ---   (for IDE only) all output goes to command line window";
    msg += "\n";
    msg += "-- …more…         All space delimited entries after -- go to sys.argv";
    msg += "\n                           \"<some text>\" makes one parameter (may contain intermediate blanks)";
    msg += "\n\n";
    msg += "-------------------------------------------------------------------------";
    msg += "\n";
    msg += "-d                Special debugging option in case of mysterious errors:";
    msg += "\n";
    msg += "                    Debug level is set to 3 and all output goes to <WorkingFolder>/SikuliLog.txt";
    msg += "\n";
    msg += "                    Content might be used to ask questions or report bugs";
    msg += "\n";
    msg += "-------------------------------------------------------------------------";
    msg += "\n";
    msg += "                    Just click OK to start IDE with no options - defaults will be used";
    
    String ret = JOptionPane.showInputDialog(null, msg, "SikuliX: collect runtime options", 
                    JOptionPane.QUESTION_MESSAGE);
    
    if (ret == null) {
      return null;
    }    
    log0(0, "[" + ret + "]");
    if (!ret.isEmpty()) {
      System.setProperty("sikuli.SIKULI_COMMAND", ret);
      resArgs.addAll(Arrays.asList(ret.split(" +")));
    }
    return resArgs.toArray(new String[0]);
  }

  public static String arrayToString(String[] args) {
    String ret = "";
    for (String s : args) {
      if (s.contains(" ")) {
        s = "\"" + s + "\"";
      }
      ret += s + " ";
    }
    return ret;
  }
}
