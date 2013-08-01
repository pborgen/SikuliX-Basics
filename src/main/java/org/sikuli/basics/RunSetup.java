package org.sikuli.basics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class RunSetup {

  private static boolean runningfromJar = true;
  private static String workDir;
  private static String uhome;
  private static String logfile;
  private static String version = "1.0.1";
  private static String downloadBaseDir = "https://dl.dropboxusercontent.com/u/42895525/SikuliX-" + version + "/";
  private static String downloadIDE = "sikuli-ide-" + version + ".jar";
  private static String downloadMacApp = "sikuli-macapp-" + version + ".jar";
  private static String downloadScript = "sikuli-script-" + version + ".jar";
  private static String downloadJava = "sikuli-java-" + version + ".jar";
  private static String downloadTess = "sikuli-tessdata-" + version + ".jar";
  private static String localJava = "sikuli-java.jar";
  private static String localScript = "sikuli-script.jar";
  private static String localIDE = "sikuli-ide.jar";
  private static String localMacApp = "sikuli-macapp.jar";
  private static String localMacAppIDE = "SikuliX-IDE.app/Contents/sikuli-ide.jar";
  private static String folderMacApp = "SikuliX-IDE.app";
  private static String localSetup = "sikuli-setup.jar";
  private static String localTess = "sikuli-tessdata.jar";
  private static String localLogfile = "SikuliX-" + version + "-SetupLog.txt";
  private static boolean sikuliUsed = false;
  private static SetUpSelect winSU;
  private static JFrame winSetup;
  private static boolean getIDE, getScript, getJava, getTess;
  private static String localJar;
  private static boolean test = false;
  private static List<String> options = new ArrayList<String>();
  //<editor-fold defaultstate="collapsed" desc="new logging concept">
  private static String me = "RunSetup";
  private static String mem = "...";
  private static int lvl = 2;
  private static String msg;
  private static boolean forAllSystems = false;
  private static long start;

  private static void log(int level, String message, Object... args) {
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + mem + ": " + message, args);
  }

  private static void log0(int level, String message, Object... args) {
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + message, args);
  }

  private static void log1(int level, String message, Object... args) {
    String sout;
    String prefix = level < 0 ? "error" : "debug";
    if (args.length != 0) {
      sout = String.format("[" + prefix + "] " + message, args);
    } else {
      sout = "[" + prefix + "] " + message;
    }
    System.out.println(sout);
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + message, args);
  }
//</editor-fold>

  public static void main(String[] args) {
    mem = "main";

    options.addAll(Arrays.asList(args));
    if (args.length > 0 && "test".equals(args[0])) {
      test = true;
      options.remove(0);
    }

    uhome = System.getProperty("user.home");
    workDir = FileManager.getJarParentFolder();
    if (workDir.startsWith("N")) {
      runningfromJar = false;
    }
    workDir = workDir.substring(1);

    if (runningfromJar) {
      logfile = (new File(workDir, localLogfile)).getAbsolutePath();
    } else {
      workDir = (new File(uhome, "SikuliX")).getAbsolutePath();
      (new File(workDir)).mkdirs();
      logfile = (new File(uhome, "SikuliX/SikuliXSetupLog.txt")).getAbsolutePath();
      popInfo("\n... not running from sikuli-setup.jar - using as download folder\n" + workDir);
    }

    if (!Debug.setLogFile(logfile)) {
      popError(workDir + "\n... folder we are running in must be user writeable! \n"
              + "pls. correct the problem and start again.");
      System.exit(0);
    }
    Settings.LogTime = true;
    Debug.setDebugLevel(3);

    if (args.length > 0) {
      log(lvl, "... starting with " + args[0]);
    } else {
      log(lvl, "... starting with no args given");
    }

    //<editor-fold defaultstate="collapsed" desc="option reset">
    if (options.size() > 0 && options.get(0).equals("reset")) {
      log1(3, "requested to reset: " + workDir);
      FileManager.deleteFileOrFolder(workDir, new FileManager.fileFilter() {
        @Override
        public boolean accept(File entry) {
          if (entry.getName().equals("runSetup")) {
            return false;
          } else if (entry.getName().equals(localSetup)) {
            return false;
          } else if (entry.getName().equals(localLogfile)) {
            return false;
          } else if (test && entry.getName().equals(localIDE)) {
            return false;
          }
          return true;
        }
      });
      log1(3, "completed!");
      System.exit(0);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="option update">
    if (options.size() > 0 && options.get(0).equals("update")) {
      log1(3, "requested to check update");
      // check for updates and optionally download and build
      log1(3, "completed!");
      System.exit(0);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="option makeJar">
    if (options.size() > 0 && options.get(0).equals("makeJar")) {
      options.remove(0);
      String todo, jarName, folder;
      while (options.size() > 0) {
        todo = options.get(0);
        options.remove(0);
        //***
        // pack a jar from a folder
        //***
        if (todo.equals("packJar")) {
          if (options.size() < 2) {
            log1(-1, "packJar: invalid options!");
            System.exit(1);
          }
          jarName = FileManager.slashify(options.get(0), false);
          options.remove(0);
          folder = options.get(0);
          options.remove(0);
          log1(3, "requested to pack %s from %s", jarName, folder);
          FileManager.packJar(folder, jarName, null);
          log1(3, "completed!");
          continue;
          //***
          // unpack a jar to a folder
          //***
        } else if (todo.equals("unpackJar")) {
          if (options.size() < 2) {
            log1(-1, "unpackJar: invalid options!");
            System.exit(1);
          }
          jarName = options.get(0);
          options.remove(0);
          folder = options.get(0);
          options.remove(0);
          log1(3, "requested to unpack %s to %s", jarName, folder);
          // action
          log1(3, "completed!");
          continue;
          //***
          // build a jar by combining other jars (optionally filtered) and/or folders
          //***
        } else if (todo.equals("buildJar")) {
          // build jar arg0
          if (options.size() < 2) {
            log1(-1, "unpackJar: invalid options!");
            System.exit(1);
          }
          jarName = options.get(0);
          options.remove(0);
          folder = options.get(0);
          options.remove(0);
          log1(3, "requested to unpack %s to %s", jarName, folder);
          // action
          log1(3, "completed!");
          continue;
        } else {
          log1(-1, "makejar: invalid option: " + todo);
          System.exit(1);
        }
      }
      System.exit(0);
    }
    //</editor-fold>

//***
// starting normal setup
//***
    if ((new File(workDir, "libs").exists())
            || new File(workDir, localIDE).exists()
            || new File(workDir, localScript).exists()
            || new File(workDir, localJava).exists()) {
      popError(workDir + "\n... folder we are running in must not be a current Sikuli folder! \n"
              + "pls. correct the problem and start again.");
      if (!test) {
        System.exit(0);
      }
    }

    log0(lvl, "user home: %s", uhome);

    winSetup = new JFrame("SikuliX-Setup");
    Container winCP = winSetup.getContentPane();
    winCP.setLayout(new BorderLayout());
    winSU = new SetUpSelect();
    winCP.add(winSU, BorderLayout.CENTER);
    winSetup.pack();
    winSetup.setAlwaysOnTop(true);
    winSetup.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    winSetup.setVisible(true);

    // running system
    Settings.getOS();
    msg = Settings.osName + " " + Settings.getOSVersion();
    winSU.suSystem.setText(msg);
    log0(lvl, "RunningSystem: " + msg);

    // folder running in
    winSU.suFolder.setText(workDir);
    log0(lvl, "parent of jar/classes: %s", workDir);

    // running Java
    String osarch = System.getProperty("os.arch");
    msg = "Java " + Settings.JavaVersion + " (" + osarch + ") " + Settings.JREVersion;
    winSU.suJava.setText(msg);
    log0(lvl, "RunningJava: " + msg);

    // Sikuli used before
    msg = checkSikuli();
    winSU.suRC3.setText(msg);
    log0(lvl, msg);

    if (sikuliUsed) {
    }

    getIDE = false;
    getScript = false;
    getJava = false;
    getTess = false;

    winSU.addPropertyChangeListener("background", new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent pce) {
        winSetup.setVisible(false);
      }
    });

    while (true) {
      if (winSU.getBackground() == Color.YELLOW) {
        break;
      }
      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {
      }
    }

    if (winSU.option1.isSelected()) {
      getIDE = true;
    }
    if (winSU.option2.isSelected() && !getIDE) {
      getScript = true;
    }
    if (winSU.option3.isSelected()) {
      getJava = true;
    }
    if (winSU.option4.isSelected() && !getIDE && !getScript) {
      getJava = true;
    }
    if (winSU.option5.isSelected()) {
      getTess = true;
    }
    if (winSU.option6.isSelected()) {
      forAllSystems = true;
    }
    
    if (getIDE || getScript || getJava) {
      msg = "The following file(s) will be downloaded to\n"
              + workDir + "\n";
      if (getIDE) {
        msg += "\n--- Package 1 ---\n" + downloadIDE;
        if (Settings.isMac()) {
          msg += "\n" + downloadMacApp;
        }
      }
      if (getScript) {
        msg += "\n--- Package 2 ---\n" + downloadScript;
      }
      if (getJava) {
        msg += "\n--- Package 3 ---\n" + downloadJava;
      }
      if (getTess) {
        msg += "\n--- Additions ---\n" + downloadTess;
      }
      if (!popAsk(msg)) System.exit(1);

      // downloading
      localJar = null;
      String targetJar;
      boolean downloadOK = true;
      boolean dlOK = true;
      if (getIDE) {
        localJar = new File(workDir, localIDE).getAbsolutePath();
        if (!test) {
          dlOK = download(downloadBaseDir, workDir, downloadIDE, localJar);
        }
        downloadOK &= dlOK;
        if (Settings.isMac()) {
          targetJar = new File(workDir, localMacApp).getAbsolutePath();
          if (!test) {
            dlOK = download(downloadBaseDir, workDir, downloadMacApp, targetJar);
          }
          if (dlOK) {
            FileManager.deleteFileOrFolder((new File(workDir, folderMacApp)).getAbsolutePath());
            FileManager.unpackJar(targetJar, workDir, false);
          }
          downloadOK &= dlOK;
        }
      } else if (getScript) {
        localJar = new File(workDir, localScript).getAbsolutePath();
        if (!test) {
          downloadOK = download(downloadBaseDir, workDir, downloadScript, localJar);
        }
        downloadOK &= dlOK;
      }
      if (getJava) {
        targetJar = new File(workDir, localJava).getAbsolutePath();
        if (!test) {
          downloadOK = download(downloadBaseDir, workDir, downloadJava, targetJar);
        }
        downloadOK &= dlOK;
      }
      if (getTess) {
        targetJar = new File(workDir, localTess).getAbsolutePath();
        if (!test) {
          downloadOK = download(downloadBaseDir, workDir, downloadTess, targetJar);
        }
        downloadOK &= dlOK;
      }
      log0(lvl, "Download ended");
      if (!test && !downloadOK) {
        terminate("download not completed successfully");
      }
    } else {
      popError("Nothing selected! Sikuli not useable!\nYou might try again ;-)");
      System.exit(0);
    }

    // add the native stuff to the jars
    JFrame splash = showSplash("Now adding native stuff to selected jars.", "pls. wait - may take some seconds ...");

    // ide or script
    String[] jarsList = new String[3];
    String localTemp = "sikuli-temp.jar";
    String[] localJars = new String[2];
    String localTestJar = null;
    if (getIDE) {
      localJars[0] = localIDE;
      localTestJar = (new File(workDir, localIDE)).getAbsolutePath();
    } else if (getScript) {
      localJars[0] = localScript;
      localTestJar = (new File(workDir, localScript)).getAbsolutePath();
    } else {
      localJars[0] = null;
    }
    if (getJava) {
      localJars[1] = localJava;
    }
    boolean success = true;
    FileManager.JarFileFilter libsFilter = new FileManager.JarFileFilter() {
      @Override
      public boolean accept(ZipEntry entry) {
        if (forAllSystems) {
            return true;
        } else if (Settings.isWindows()) {
          if (entry.getName().startsWith("META-INF/libs/mac")
                  || entry.getName().startsWith("META-INF/libs/linux")) {
            return false;
          }
        } else if (Settings.isMac()) {
          if (entry.getName().startsWith("META-INF/libs/windows")
                  || entry.getName().startsWith("META-INF/libs/linux")) {
            return false;
          }
        } else if (Settings.isLinux()) {
          if (entry.getName().startsWith("META-INF/libs/windows")
                  || entry.getName().startsWith("META-INF/libs/mac")) {
            return false;
          }
        }
        return true;
      }
    };
    String targetJar;
    for (String path : localJars) {
      if (path == null) {
        continue;
      }
      log0(lvl, "adding native stuff to " + path);
      localJar = (new File(workDir, path)).getAbsolutePath();
      jarsList[0] = localJar;
      jarsList[1] = (new File(workDir, localSetup)).getAbsolutePath();
      if (!getTess) {
        jarsList[2] = null;
      } else {
        jarsList[2] = (new File(workDir, localTess)).getAbsolutePath();
      }
      targetJar = (new File(workDir, localTemp)).getAbsolutePath();
      success &= FileManager.buildJar(targetJar, jarsList, null, null, libsFilter);
      success &= (new File(localJar)).delete();
      success &= (new File(workDir, localTemp)).renameTo(new File(localJar));
    }
    if (Settings.isMac() && getIDE) {
      closeSplash(splash);
      splash = showSplash("Now preparing Mac app SikuliX-IDE.app.", "pls. wait - may take some seconds ...");
      forAllSystems = false;
      targetJar = (new File(workDir, localMacAppIDE)).getAbsolutePath();
      jarsList = new String[] {(new File(workDir, localIDE)).getAbsolutePath()};
      success &= FileManager.buildJar(targetJar, jarsList, null, null, libsFilter);
    }
    closeSplash(splash);
    if (!success) {
      popError("Bad things happened trying to add native stuff to selected jars --- terminating!");
      System.exit(1);
    }


    // create libsDir and system path entry (windows)
    splash = showSplash("Now I will try to set up the environment!", "pls. wait - may take some seconds ...");
    File folderLibs = new File(workDir, "libs");
    if (folderLibs.exists()) {
      FileManager.deleteFileOrFolder(folderLibs.getAbsolutePath());
    }
    folderLibs.mkdirs();
    IResourceLoader loader = FileManager.getNativeLoader("basic", args);
    loader.check(Settings.SIKULI_LIB);
    if (loader.doSomethingSpecial("checkLibsDir", null)) {
      closeSplash(splash);
      splash = showSplash(" ", "Environment seems to be ready!");
      closeSplash(splash);
    } else {
      closeSplash(splash);
      popError("Something serious happened! Sikuli not useable!\n"
              + "Check the error log at " + logfile);
      System.exit(0);
    }

    if (getJava) {
      splash = showSplash("Trying to run functional test(s)", "Java-API: org.sikuli.script.SikuliX.testSetup()");
      if (!SikuliX.addToClasspath(localJar)) {
        closeSplash(splash);
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        System.exit(0);
      }
      try {
        log0(lvl, "trying to run org.sikuli.script.SikuliX.testSetup()");
        Class sysclass = URLClassLoader.class;
        Class SikuliCL = sysclass.forName("org.sikuli.script.SikuliX");
        log0(lvl, "class found: " + SikuliCL.toString());
        Method method = SikuliCL.getDeclaredMethod("testSetup", new Class[0]);
        log0(lvl, "getMethod: " + method.toString());
        method.setAccessible(true);
        closeSplash(splash);
        log0(lvl, "invoke: " + method.toString());
        Object ret = method.invoke(null, new Object[0]);
        if (!(Boolean) ret) {
          throw new Exception("testSetup returned false");
        }
      } catch (Exception ex) {
        closeSplash(splash);
        log0(-1, ex.getMessage());
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        System.exit(0);
      }
    }
    if (getIDE || getScript) {
      splash = showSplash("Trying to run functional test(s)", "running testSetup.sikuli using SikuliScript");
      if (!SikuliX.addToClasspath(localTestJar)) {
        closeSplash(splash);
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        System.exit(0);
      }
      String testSetupSuccess = "Setup: Sikuli seems to work! Have fun!";
      log0(lvl, "trying to run testSetup.sikuli using SikuliScript");
      try {
        String testargs[] = new String[]{"-testSetup", "jython", "popup(\"" + testSetupSuccess + "\")"};
        closeSplash(splash);
        SikuliScript.main(testargs);
        if (null == testargs[0]) {
          throw new Exception("testSetup ran with problems");
        }
      } catch (Exception ex) {
        closeSplash(splash);
        log0(-1, ex.getMessage());
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        System.exit(0);
      }
    }
    splash = showSplash("Setup seems to have ended successfully!", "Detailed information see: " + logfile);
    start += 2000;
    closeSplash(splash);
    log0(lvl, "... seems to have ended successfully ;-)");
    System.exit(0);
  }

  public static void popError(String msg) {
    JOptionPane.showMessageDialog(null, msg, "SikuliX-Setup: having problems ...", JOptionPane.ERROR_MESSAGE);
  }

  public static void popInfo(String msg) {
    JOptionPane.showMessageDialog(null, msg, "SikuliX-Setup: info ...", JOptionPane.PLAIN_MESSAGE);
  }

  public static boolean popAsk(String msg) {
    int ret = JOptionPane.showConfirmDialog(null, msg, "SikuliX-Setup: ... want to proceed? ", JOptionPane.YES_NO_OPTION);
    if (ret == JOptionPane.CLOSED_OPTION || ret == JOptionPane.NO_OPTION) {
      return false;
    }
    return true;
  }

  public static JFrame showSplash(String title, String msg) {
    start = (new Date()).getTime();
    return new MultiFrame(new String[]{"splash", "# " + title, "#... " + msg});
  }
  
  public static void closeSplash(JFrame splash) {
    long elapsed = (new Date()).getTime() - start;
    if (elapsed < 3000) {
      try {
        Thread.sleep(3000 - elapsed);
      } catch (InterruptedException ex) {}
    }
    splash.dispose();
  }
  
  private static String checkSikuli() {
    String msg1 = "... it seems SikuliX-1.0rc3 or SikuliX-1.x.x has been used before on this system";
    String msg0 = "... it seems that Sikuli is used the first time on this system";
    String msg = msg0;
    File props;
    if (Settings.isMac()) {
      props = new File(uhome, "Library/Preferences/org.sikuli.ide.plist");
      if (props.exists()) {
        msg = msg1;
        sikuliUsed = true;
      }
    }
    return msg;
  }

  private static boolean download(String sDir, String tDir, String item, String jar) {
    JFrame progress = new MultiFrame("download");
    String fname = FileManager.downloadURL(sDir + item, tDir, progress);
    progress.dispose();
    if (null == fname) {
      log(-1, "Fatal error 001: not able to download: %s", item);
      return false;
    }
    if (!(new File(tDir, item)).renameTo(new File(jar))) {
      log0(-1, "rename to %s did not work", jar);
      return false;
    }
    return true;
  }

  private static void terminate(String msg) {
    log0(-1, msg);
    log0(-1, "... terminated abnormally :-(");
    popError("Something serious happened! Sikuli not useable!\n"
            + "Check the error log at " + logfile);
    System.exit(0);
  }
}
/*
 IResourceLoader loader = FileManager.getNativeLoader("basic", args);
 //    loader.install(args);
 String[] cmd = new String[] {args[0]};
 loader.doSomethingSpecial("runcmd", cmd);
 log(lvl, "result from runcmd" + cmd[0]);
 */

/*
 Preferences pref = Preferences.userNodeForPackage(SikuliX.class);
 try {
 //pref.exportNode(new FileOutputStream("/Users/rhocke/SikuliXPrefs.xml"));
 //pref.importPreferences(new FileInputStream("/Users/rhocke/SikuliXPrefs.xml"));
 } catch (Exception ex) {
 log(-1, "problem with /Users/rhocke/SikuliXPrefs.xml\n" + ex.getMessage());
 }
 log(-2, pref.get("USER_NAME", "N/A"));
 */
