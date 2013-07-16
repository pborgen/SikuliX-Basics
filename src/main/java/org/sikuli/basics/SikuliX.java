/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added RaiMan 2013
 */
package org.sikuli.basics;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.ServiceLoader;

/**
 *
 * used as anchor for the preferences store and for global supporting features
 */
public class SikuliX {

  private static IScriptRunner runner;
  private static final String ScriptSikuliXCL = "org.sikuli.script.SikuliX";
  private static final String ScriptKeyCL = "org.sikuli.script.Key";
  private static Class ScriptCl, KeyCl;
  private static Method endWhat, toJavaKeyCode;

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
    cleanup(0);
    System.exit(1);
  }

  public static void cleanup(int n) {
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
}
