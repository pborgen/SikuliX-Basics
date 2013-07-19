package org.sikuli.basics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import static javax.swing.WindowConstants.DISPOSE_ON_CLOSE;

public class RunSetup {

  private static boolean runningfromJar = true;
  private static String workDir;
  private static String uhome;
  private static String logfile;
  private static String downloadBaseDir = "https://dl.dropboxusercontent.com/u/42895525/SikuliX-1.0.1/";
  private static String downloadIDE = "sikuli-ide-1.0.1.jar";
  private static String downloadScript = "sikuli-script-1.0.1.jar";
  private static String downloadJava = "sikuli-java-1.0.1.jar";
  private static String localJava = "sikuli-java.jar";
  private static String localScript = "sikuli-script.jar";
  private static String localIDE = "sikuli-ide.jar";
  private static boolean sikuliUsed = false;
  private static SetUpSelect winSU;
  private static JFrame winSetup;
  private static boolean getIDE, getScript, getJava;
  private static String localJar;
  private static boolean test = false;
  //<editor-fold defaultstate="collapsed" desc="new logging concept">
  private static String me = "RunSetup";
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

  public static void main(String[] args) {
    mem = "main";

    if (args.length > 0 && "test".equals(args[0])) {
      test = true;
    }

    uhome = System.getProperty("user.home");
    workDir = FileManager.getJarParentFolder();
    if (workDir.startsWith("N")) {
      runningfromJar = false;
    }
    workDir = workDir.substring(1);

    if (runningfromJar) {
      logfile = (new File(workDir, "SikuliX-1.0.1-SetupLog.txt")).getAbsolutePath();
    } else {
      workDir = (new File(uhome, "SikuliX")).getAbsolutePath();
      (new File(workDir)).mkdirs();
      logfile = (new File(uhome, "SikuliX/SikuliXSetupLog.txt")).getAbsolutePath();
      popInfo("\n... not running from sikuli-setup.jar - using as download folder\n" + workDir);
    }

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
    if (getIDE || getScript || getJava) {
      msg = "The following file(s) will be downloaded to\n"
              + workDir + "\n";
      if (getIDE) {
        msg += "\n" + downloadIDE;
      }
      if (getScript) {
        msg += "\n" + downloadScript;
      }
      if (getJava) {
        msg += "\n" + downloadJava;
      }
      popInfo(msg);

      // downloading
      localJar = null;
      boolean downloadOK = false;
      if (getIDE) {
        localJar = new File(workDir, localIDE).getAbsolutePath();
        if (!test) downloadOK = download(downloadBaseDir, workDir, downloadIDE, localJar);
      } else if (getScript) {
        localJar = new File(workDir, localScript).getAbsolutePath();
        if (!test) downloadOK = download(downloadBaseDir, workDir, downloadScript, localJar);
      }
      if (getJava) {
        if (localJar == null) {
          localJar = new File(workDir, localJava).getAbsolutePath();
        }
        if (!test) downloadOK = download(downloadBaseDir, workDir, downloadJava,
                new File(workDir, localJava).getAbsolutePath());
      }
      log0(lvl, "Download ended");
      if (!test && !downloadOK) {
        terminate("download not completed successfully");
      }
    } else {
      popError("Nothing selected! Sikuli not useable!\nYou might try again ;-)");
      System.exit(0);
    }

    // create libsDir and system path entry (windows)
    popInfo("Now I will try to set up the environment!");
    IResourceLoader loader = FileManager.getNativeLoader("basic", args);
    loader.check(Settings.SIKULI_LIB);
    if (!loader.doSomethingSpecial("checkLibsDir", null)) {
      popInfo("Environment seems to be ready!");
    } else {
      popError("Something serious happened! Sikuli not useable!\n"
              + "Check the error log at " + logfile);
      System.exit(0);
    }

    popInfo("Trying to run functional test");
    if (!SikuliX.addToClasspath(localJar)) {
      popError("Something serious happened! Sikuli not useable!\n"
              + "Check the error log at " + logfile);
      System.exit(0);
    }
    if (getIDE || getScript) {
      String testSetupSuccess = "Setup: Sikuli seems to work! Have fun!";
      log0(lvl, "trying to run testSetup.sikuli using SikuliScript");
      try {
        String testargs[] = new String[]{"-testSetup", "jython", "popup(\"" + testSetupSuccess + "\")"};
        SikuliScript.main(testargs);
        if (null == testargs[0]) {
          throw new Exception("testSetup ran with problems");
        }
      } catch (Exception ex) {
        log0(-1, ex.getMessage());
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        System.exit(0);
      }
    } else {
      try {
        log0(lvl, "trying to run org.sikuli.script.SikuliX.testSetup()");
        Class sysclass = URLClassLoader.class;
        Class SikuliCL = sysclass.forName("org.sikuli.script.SikuliX");
        log0(lvl, "class found: " + SikuliCL.toString());
        Method method = SikuliCL.getDeclaredMethod("testSetup", new Class[0]);
        log0(lvl, "getMethod: " + method.toString());
        method.setAccessible(true);
        log0(lvl, "invoke: " + method.toString());
        Object ret = method.invoke(null, new Object[0]);
        if (!(Boolean) ret) {
          throw new Exception("testSetup returned false");
        }
      } catch (Exception ex) {
        log0(-1, ex.getMessage());
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        System.exit(0);
      }
    }
    log0(lvl, "... seems to have ended successfully ;-)");
    System.exit(0);
  }

  public static void popError(String msg) {
    JOptionPane.showMessageDialog(null, msg, "SikuliX-Setup: having problems ...", JOptionPane.ERROR_MESSAGE);
  }

  public static void popInfo(String msg) {
    JOptionPane.showMessageDialog(null, msg, "SikuliX-Setup: having problems ...", JOptionPane.PLAIN_MESSAGE);
  }

  private static String checkSikuli() {
    String msg1 = "... it seems SikuliX-1.0rc3 or SikuliX-1.0.0 has been used before on this system";
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
