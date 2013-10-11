/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * RaiMan 2013
 */

package org.sikuli.basics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

public class RunSetup {

  public static String timestampBuilt;
  private static final String tsb = "##--##Mi  9 Okt 2013 12:00:59 CEST##--##"; 
  private static boolean runningfromJar = true;
  private static String workDir;
  private static String uhome;
  private static String logfile;
  private static String version = Settings.getVersionShort();
  private static String betaVersion;
  private static String updateVersion;
  private static String downloadBaseDirBase = "http://dl.dropboxusercontent.com/u/42895525/SikuliX-";
  private static String downloadBaseDir = downloadBaseDirBase + version.substring(0,3) + "/";
  private static String downloadSetup;
  private static String downloadIDE = "sikuli-ide-" + version + ".jar";
  private static String downloadMacApp = "sikuli-macapp-" + version + ".jar";
  private static String downloadScript = "sikuli-script-" + version + ".jar";
  private static String downloadJava = "sikuli-java-" + version + ".jar";
  private static String downloadTess = "sikuli-tessdata-" + version + ".jar";
  private static String downloadUpdate;
  private static String localJava = "sikuli-java.jar";
  private static boolean updateJava = false;
  private static String localScript = "sikuli-script.jar";
  private static boolean updateScript = false;
  private static String localIDE = "sikuli-ide.jar";
  private static boolean updateIDE = false;
  private static String localMacApp = "sikuli-macapp.jar";
  private static String localMacAppIDE = "SikuliX-IDE.app/Contents/sikuli-ide.jar";
  private static String folderMacApp = "SikuliX-IDE.app";
  private static String folderMacAppContent = folderMacApp + "/Contents";
  private static String localSetup = "sikuli-setup.jar";
  private static String localTess = "sikuli-tessdata.jar";
  private static boolean updateTess = false;
  private static String localLogfile = "SikuliX-" + version + "-SetupLog.txt";
  private static SetUpSelect winSU;
  private static JFrame winSetup;
  private static boolean getIDE, getScript, getJava, getTess;
  private static String localJar;
  private static boolean test = false;
  private static boolean isUpdate = false;
  private static boolean isBeta = false;
  private static boolean runningUpdate = false;
  private static List<String> options = new ArrayList<String>();
  private static JFrame splash = null;

  private static String me = "RunSetup";
  private static String mem = "...";
  private static int lvl = 2;
  private static String msg;
  private static boolean forAllSystems = false;
  private static long start;
  
  static {
    timestampBuilt = tsb.substring(6, tsb.length()-6);
    timestampBuilt = timestampBuilt.substring(
                     timestampBuilt.indexOf(" ")+1, timestampBuilt.lastIndexOf(" "));
    timestampBuilt = timestampBuilt.replaceAll(" ", "").replaceAll(":", "").toUpperCase();
  }

  //<editor-fold defaultstate="collapsed" desc="new logging concept">
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
                
    //<editor-fold defaultstate="collapsed" desc="options special">
    if (args.length > 0 && "version".equals(args[0])) {
      System.out.println(Settings.getVersionShort());
      System.exit(0);
    }
    
    if (args.length > 0 && "update".equals(args[0])) {
      runningUpdate = true;
      if (version.contains("Beta")) {
        isBeta = true;
      }
      else {
        isUpdate = true;
      }
      options.remove(0);
    }

    if (args.length > 0 && "test".equals(args[0])) {
      test = true;
      options.remove(0);
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
            log1(-1, "buildJar: invalid options!");
            System.exit(1);
          }
          jarName = options.get(0);
          options.remove(0);
          folder = options.get(0);
          options.remove(0);
          log1(3, "requested to build %s to %s", jarName, folder);
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

    //<editor-fold defaultstate="collapsed" desc="general preps">
    Settings.runningSetup = true;
    IResourceLoader loader = FileManager.getNativeLoader("basic", args);
    
    uhome = System.getProperty("user.home");
    workDir = FileManager.getJarParentFolder();
    if (workDir.startsWith("N")) {
      runningfromJar = false;
    }
    workDir = workDir.substring(1);
    
    if (runningfromJar) {
      logfile = (new File(workDir, localLogfile)).getAbsolutePath();
    } else {
      workDir = (new File(uhome, "SikuliX/ZRun")).getAbsolutePath();
      (new File(workDir)).mkdirs();
      logfile = (new File(workDir, localLogfile)).getAbsolutePath();
      popInfo("\n... not running from sikuli-setup.jar - using as download folder\n" + workDir);
    }
    
    if (!Debug.setLogFile(logfile)) {
      popError(workDir + "\n... folder we are running in must be user writeable! \n"
              + "pls. correct the problem and start again.");
      System.exit(0);
    }
    
    Settings.LogTime = true;
    Debug.setDebugLevel(3);
    log1(lvl, "SikuliX Setup Build: %s %s", Settings.getVersionShort(), RunSetup.timestampBuilt);
    
    if (args.length > 0) {
      log1(lvl, "... starting with " + SikuliX.arrayToString(args));
    } else {
      log1(lvl, "... starting with no args given");
    }
    
    File localJarSetup = new File(workDir, localSetup);
    File localJarIDE = new File(workDir, localIDE);
    File localJarScript = new File(workDir, localScript);
    File localJarJava = new File(workDir, localJava);
    File localMacFolder = new File(workDir, folderMacApp);
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="checking update/beta">
    if (!runningUpdate) {
      if (localJarIDE.exists() || localJarScript.exists()
              || localJarJava.exists() || localMacFolder.exists()) {
        int avail = -1;
        if (!popAsk("You have " + Settings.getVersion()
                + "\nClick YES if you want to run setup again\n"
                + "or NO for more options.")) {
          if (!popAsk("Click YES if you want to look for updates and/or betas\n"
                  + "or NO to leave setup now")) {
            System.exit(0);
          } else {
            splash = showSplash("Checking for update or beta versions! (you have " + version + ")",
                    "pls. wait - may take some seconds ...");
            AutoUpdater au = new AutoUpdater();
            avail = au.checkUpdate();
            closeSplash(splash);
            if (avail > 0) {
              if (avail == AutoUpdater.BETA || avail == AutoUpdater.SOMEBETA) {
                betaVersion = au.getBetaVersion();
                log1(lvl, "%s is available", betaVersion);
                if (popAsk("Version " + betaVersion + " is available\n"
                        + "You have " + Settings.getVersion()
                        + "\nClick YES, if you want to install ..."
                        + "\ncurrent stuff will be saved to BackUp."
                        + "\n... Click NO to continue ...")) {
                  isBeta = true;
                  reset(avail);
                  downloadBaseDir = downloadBaseDirBase + betaVersion + "/";
                  downloadSetup = "sikuli-update-" + betaVersion + ".jar";
                  if (!download(downloadBaseDir, workDir, downloadSetup,
                          new File(workDir, downloadSetup).getAbsolutePath())) {
                    popError("Download did not complete successfully.\n"
                            + "Check the logfile for possible error causes.\n\n"
                            + "If you think, setup's inline download from Dropbox is blocked somehow on,\n"
                            + "your system, you might download manually (see respective FAQ)\n"
                            + "For other reasons, you might simply try to run setup again.");
                    terminate("download not completed successfully");
                  }
                }
              }
              if (avail > AutoUpdater.FINAL) {
                avail -= AutoUpdater.SOMEBETA;
              }
              if (avail > 0 && avail != AutoUpdater.BETA) {
                if (popAsk(au.whatUpdate + "\n"
                        + "You have " + Settings.getVersion()
                        + "\nClick YES, if you want to install ..."
                        + "\ncurrent stuff will be saved to BackUp."
                        + "\n... Click NO to terminate.")) {
                  isUpdate = true;
                  reset(avail);
                  updateVersion = au.getVersionNumber();
                  downloadBaseDir = downloadBaseDirBase + updateVersion + "/";
                  downloadSetup = "sikuli-update-" + updateVersion + ".jar";
                  if (!download(downloadBaseDir, workDir, downloadSetup,
                          new File(workDir, downloadSetup).getAbsolutePath())) {
                    popError("Download did not complete successfully.\n"
                            + "Check the logfile for possible error causes.\n\n"
                            + "If you think, setup's inline download from Dropbox is blocked somehow on,\n"
                            + "your system, you might download manually (see respective FAQ)\n"
                            + "For other reasons, you might simply try to run setup again.");
                    terminate("download not completed successfully");
                  }
                } else {
                  avail = 0;
                }
              }
            }
            if (avail == 0) {
              popInfo("No suitable update or beta available");
            }
          }
        }
        if (avail <= 0) {
          reset(avail);
        } else {
          String cmdSetup = "runSetup";
          if (Settings.isWindows()) {
            loader.export("Commands/windows#runSetup.cmd", workDir);
            cmdSetup = "runSetup.cmd";
          } else if (Settings.isMac()) {
            loader.export("Commands/mac#runSetup", workDir);
            loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", 
              new File(workDir, "runSetup").getAbsolutePath()});
          } else if (Settings.isLinux()) {
            loader.export("Commands/linux#runSetup", workDir);
            loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", 
              new File(workDir, "runSetup").getAbsolutePath()});
          } else {
            terminate("unknown system");
          }
          if (!new File(workDir, cmdSetup).exists()) {
            String msg = "Fatal error 002: runSetup.cmd could not be exported to " + workDir;
            log0(-1, msg);
            popError(msg);
            System.exit(2);
          }
        }
      }
    }
    //</editor-fold>
        
    //<editor-fold defaultstate="collapsed" desc="option setup preps Windows">
    if (!runningUpdate && !isBeta && !isUpdate && Settings.isWindows()) {
      String syspath = System.getenv("PATH");
      for (String p : syspath.split(";")) {
        log1(lvl, "syspath: " + p);
      }
      File fLibs = new File(workDir, "libs");
      String pLibs = fLibs.getAbsolutePath().replaceAll("/", "\\");
      if (!syspath.contains(pLibs)) {
        log1(lvl, "Not on syspath: " + pLibs + " --- Extracting runSetup.cmd.");
        loader.export("Commands/windows#runSetup.cmd", workDir);
        File fCmd = new File(workDir, "runSetup.cmd");
        if (!fCmd.exists()) {
          String msg = "Fatal error 002: runSetup.cmd could not be exported to " + workDir;
          log0(-1, msg);
          popError(msg);
          System.exit(2);
        }
        loader.doSomethingSpecial("runcmd", new String[]{"start", "/i", fCmd.getAbsolutePath()});
        System.exit(0);
      }
    }
    //</editor-fold>

    log1(lvl, "user home: %s", uhome);

    popInfo("Pls. read carefully before proceeding!!");

    //<editor-fold defaultstate="collapsed" desc="option setup preps display options">
    if (!runningUpdate && !isBeta && !isUpdate) {
      winSetup = new JFrame("SikuliX-Setup");
      Border rpb = new LineBorder(Color.YELLOW, 8);
      winSetup.getRootPane().setBorder(rpb);
      Container winCP = winSetup.getContentPane();
      winCP.setLayout(new BorderLayout());
      winSU = new SetUpSelect();
      winCP.add(winSU, BorderLayout.CENTER);
      winSetup.pack();
      winSetup.setLocationRelativeTo(null);
      winSetup.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      winSetup.setVisible(true);

      //setup version basic
      winSU.suVersion.setText(Settings.getVersionShort() + "   (" + timestampBuilt + ")");

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
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="option setup: download">
    if (!runningUpdate) {
      if (!isBeta && !isUpdate) {
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
        if (winSU.option5.isSelected() && !Settings.isLinux()) {
          getTess = true;
        }
        if (winSU.option6.isSelected()) {
          forAllSystems = true;
        }
      } else {
        getIDE = updateIDE;
        getScript = updateScript;
        getJava = updateJava;
        getTess = updateTess;
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
        msg += "\n\nOnly click NO, if you want to terminate setup now!\n"
                + "Click YES even if you want to use local copies in Downloads!";
        if (!popAsk(msg)) {
          System.exit(1);
        }

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
        log1(lvl, "Download ended");
        if (!test && !downloadOK) {
          popError("Some of the downloads did not complete successfully.\n"
                  + "Check the logfile for possible error causes.\n\n"
                  + "If you think, setup's inline download from Dropbox is blocked somehow on,\n"
                  + "your system, you might download the appropriate raw packages manually and \n"
                  + "unzip them into a folder Downloads in the setup folder and run setup again.\n"
                  + "Be aware: The raw packages are not useable without being processed by setup!\n\n"
                  + "For other reasons, you might simply try to run setup again.");
          terminate("download not completed successfully");
        } else if (isBeta || isUpdate) {
          if (Settings.isWindows()) {
            popInfo("Now open a command window,\n go to the folder\n" + workDir
                    + "\n and run runSetup.cmd to finalize the update process.");
          } else {
            popInfo("Now open a terminal window,\n go to the folder\n" + workDir
                    + "\n and run runSetup to finalize the update process.");
          }
          System.exit(0);
        }
      } else {
        popError("Nothing selected! Sikuli not useable!\nYou might try again ;-)");
        System.exit(1);
      }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="option setup: add native stuff">
    if (test && !popAsk("add native stuff --- proceed?")) {
      System.exit(1);
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

    String[] jarsList = new String[] {null, null, null};
    String localTemp = "sikuli-temp.jar";
    String[] localJars = new String[3];
    String localTestJar = null;

    if (runningUpdate) {
      String message = "The following packages will be updated\n";
      if (new File(workDir, downloadIDE).exists()) {
        getIDE = true;
        localJars[0] = localIDE;
        localTestJar = (new File(workDir, localIDE)).getAbsolutePath();
        message += localIDE + "\n";
      }
      if (new File(workDir, downloadScript).exists()) {
        getScript = true;
        localJars[1] = localScript;
        localTestJar = (new File(workDir, localScript)).getAbsolutePath();
        message += localScript + "\n";
      }
      if (new File(workDir, downloadJava).exists()) {
        getJava = true;
        localJars[2] = localJava;
        message += localJava + "\n";
      }
      if (new File(workDir, downloadTess).exists()) {
        getTess = true;
        message += "\n... with OCR-Tesseract support\n\n";
      }
      popInfo(message);
    } else {
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
    }

    splash = showSplash("Now adding native stuff to selected jars.", "pls. wait - may take some seconds ...");

    String targetJar;
    for (String path : localJars) {
      if (path == null) {
        continue;
      }
      log1(lvl, "adding native stuff to " + path);
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
      log1(lvl, "preparing Mac app as SikuliX-IDE.app");
      splash = showSplash("Now preparing Mac app SikuliX-IDE.app.", "pls. wait - may take some seconds ...");
      forAllSystems = false;
      targetJar = (new File(workDir, localMacAppIDE)).getAbsolutePath();
      jarsList = new String[] {(new File(workDir, localIDE)).getAbsolutePath()};
      success &= FileManager.buildJar(targetJar, jarsList, null, null, libsFilter);
    }

    closeSplash(splash);
    if (success && (getIDE || getScript)) {
      log1(lvl, "exporting commandfiles");
      splash = showSplash("Now exporting commandfiles.", "pls. wait - may take some seconds ...");

      if (Settings.isWindows()) {
        if (getIDE) {
          loader.export("Commands/windows#runIDE.cmd", workDir);
        }
        else if (getScript) {
          loader.export("Commands/windows#runScript.cmd", workDir);
        }

      } else if (Settings.isMac()){
        if (getIDE) {
          String fmac = new File(workDir, folderMacAppContent).getAbsolutePath();
          loader.export("Commands/mac#runIDE", fmac);
          loader.export("Commands/mac#runIDE", workDir);
          loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", new File(fmac, "runIDE").getAbsolutePath()});
          loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", new File(workDir, "runIDE").getAbsolutePath()});
          FileManager.deleteFileOrFolder(new File(workDir, localIDE).getAbsolutePath());
          FileManager.deleteFileOrFolder(new File(workDir, localMacApp).getAbsolutePath());
          localTestJar = new File(fmac, localIDE).getAbsolutePath();
        }
        else if (getScript) {
          loader.export("Commands/mac#runScript", workDir);
          loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", new File(workDir, "runScript").getAbsolutePath()});
        }

      } else if (Settings.isLinux()){
        if (getIDE) {
          loader.export("Commands/linux#runIDE", workDir);
          loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", new File(workDir, "runIDE").getAbsolutePath()});
          loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", new File(workDir, localIDE).getAbsolutePath()});
        }
        else if (getScript) {
          loader.export("Commands/linux#runScript", workDir);
          loader.doSomethingSpecial("runcmd", new String[]{"chmod", "ugo+x", new File(workDir, "runScript").getAbsolutePath()});
        }
      }
      closeSplash(splash);
    }
    
    if (!success) {
      popError("Bad things happened trying to add native stuff to selected jars --- terminating!");
      terminate("Adding stuff to jars did not work");
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="option setup: environment setup and test">
    log1(lvl, "trying to set up the environment");
    splash = showSplash("Now I will try to set up the environment!", "pls. wait - may take some seconds ...");
    File folderLibs = new File(workDir, "libs");
    if (folderLibs.exists()) {
      FileManager.deleteFileOrFolder(folderLibs.getAbsolutePath());
    }
    folderLibs.mkdirs();
    loader.check(Settings.SIKULI_LIB);
    if (loader.doSomethingSpecial("checkLibsDir", null)) {
      closeSplash(splash);
      splash = showSplash(" ", "Environment seems to be ready!");
      closeSplash(splash);
    } else {
      closeSplash(splash);
      popError("Something serious happened! Sikuli not useable!\n"
              + "Check the error log at " + logfile);
      terminate("Setting up environment did not work");
    }
    
    if (getJava) {
      log1(lvl, "Trying to run functional test: JAVA-API");
      splash = showSplash("Trying to run functional test(s)", "Java-API: org.sikuli.script.SikuliX.testSetup()");
      if (!SikuliX.addToClasspath(localJarJava.getAbsolutePath())) {
        closeSplash(splash);
        log0(-1, "Java-API test: ");
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        terminate("Functional test JAVA-API did not work");
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
        terminate("Functional test JAVA-API did not work");
      }
    }
    
    if (getIDE || getScript) {
      log1(lvl, "Trying to run functional test: running Jython statements via SikuliScript");
      splash = showSplash("Trying to run functional test(s)", "running Jython statements via SikuliScript");
      if (!SikuliX.addToClasspath(localTestJar)) {
        closeSplash(splash);
        popError("Something serious happened! Sikuli not useable!\n"
                + "Check the error log at " + logfile);
        terminate("Functional test Jython did not work");
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
        terminate("Functional test Jython did not work");
      }
    }

    if (!FileManager.deleteFileOrFolder(folderLibs.getAbsolutePath())) {
      log0(-1, "Could not delete the libs folder");
    }
    
    splash = showSplash("Setup seems to have ended successfully!", "Detailed information see: " + logfile);
    start += 2000;
    closeSplash(splash);
    log1(lvl, "... SikuliX Setup seems to have ended successfully ;-)");
    //</editor-fold>

    System.exit(0);
  }
  
  private static void reset(int type) {
    log1(3, "requested to reset: " + workDir);
    String message = "";
    if (type <= 0) {
      message = "You decided to run setup again!\n";
    } else if (isBeta) {
      message = "You decided to install a beta version!\n";
    } else if (isUpdate) {
      message = "You decided to install a new version!\n";
    }
    File fBackup = new File(workDir, "BackUp");
    if (fBackup.exists()) {
      if (!popAsk(message + "A backup folder exists and will be purged!\n"
              + "Click YES if you want to proceed.\n"
              + "Click NO, to first save the current backup folder and come back. ")) {
        System.exit(0);
      }
    }
    splash = showSplash("Now creating backup and cleaning setup folder", "pls. wait - may take some seconds ...");
    String backup = fBackup.getAbsolutePath();
    FileManager.deleteFileOrFolder(backup, new FileManager.fileFilter() {
      @Override
      public boolean accept(File entry) {
        return true;
      }
    });
    try {
      FileManager.xcopyAll(workDir, backup);
    } catch (IOException ex) {
      popError("Reset: Not possible to backup:\n" + ex.getMessage());
      terminate("Reset: Not possible to backup:\n" + ex.getMessage());
    }
    FileManager.deleteFileOrFolder(workDir, new FileManager.fileFilter() {
      @Override
      public boolean accept(File entry) {
        if (entry.getName().startsWith("runSetup")) {
          return false;
        } else if (entry.getName().equals(localSetup)) {
          return false;
        } else if (workDir.equals(entry.getAbsolutePath())) {
          return false;
        } else if ("BackUp".equals(entry.getName())) {
          return false;
        } else if ("Downloads".equals(entry.getName())) {
          return false;
        } else if (entry.getName().contains("SetupLog")) {
          return false;
        } else if (entry.getName().equals(localIDE)) {
          updateIDE = true;
          return true;
        } else if (entry.getName().equals(localScript)) {
          updateScript = true;
          return true;
        } else if (entry.getName().equals(localJava)) {
          updateJava = true;
          return true;
        } else if (entry.getName().equals(localTess)) {
          updateTess = true;
          return true;
        }
        return true;
      }
    });
    closeSplash(splash);
    log1(3, "completed!");
  }
  
  public static void helpOption(int option) {
    String m;
    String om = "";
    m = "\n-------------------- Some Information on this option, that might "
        + "help to decide, wether to select it ------------------";
    switch (option) {
      case(1):
        om = "Package 1: You get the Sikuli IDE which supports all usages of Sikuli";
//              -------------------------------------------------------------
        m += "\nIt is the largest package of course ...";
        m += "\nIt is recommended for people new to Sikuli "
             + "and those who want to develop scripts with the Sikuli IDE";
        m += "\n\nFor those who know ;-) additionally you can ...";
        m += "\n- use it to run scripts from commandline";
        m += "\n- develop Java programs with Sikuli features in IDE's like Eclipse, NetBeans, ...";
        m += "\n- develop in any Java aware scripting language adding Sikuli features in IDE's like Eclipse, NetBeans, ...";
        m += "\n\nJython developement: special info:";
        m += "\n If you want to use standalone Jython in parallel, you should select Pack 3 additionally (Option 3)";
        m += "\n\nTo understand the differences, it might be helpful to read the other informations too (Pack 2 and Pack 3)";
        if (Settings.isWindows()) {
          m += "\n\nSpecial info for Windows systems:";
          m += "\nThe generated jars can be used out of the box with Java 32-Bit and Java 64-Bit as well.";
          m += "\nThe Java version is detected at runtime and the native support is switched accordingly.";
        }
        if (Settings.isMac()) {
          m += "\n\nSpecial info for Mac systems:";
          m += "\nFinally you will have a Sikuli-IDE.app in the setup working folder.";
          m += "\nTo use it, just move it into the Applications folder.";
          m += "\nIf you need to run stuff from commandline or want to use Sikuli with Java,";
          m += "\nyou will find the needed stuff in /Applications/Sikuli-IDE.app/Contents:";
          m += "\nrunIDE: the shellscript to run scripts and";
          m += "\nsikuli-ide.jar: everything you need for integration with Java developement";        
        }
        break;
      case(2):
        om = "Package 2: To allow to run Sikuli scripts from command line (no IDE)" 
                + "\n\n( ... make sure Option 1 (IDE) is not selected, if you really want this now!"
                + "\nIf you want it in addition to the IDE, run setup again after getting the IDE)";
//              -------------------------------------------------------------
        m += "\nThe primary pupose of this package: run Sikuli scripts from command line ;-)";
        m += "\nIt should be used on machines, that only run scripts and where is no need"
              +" to have the IDE or it is even not wanted to have it";
        m += "\n\nFor those who know ;-) additionally you can ...";
        m += "\n- develop Java programs with Sikuli features in IDE's like Eclipse, NetBeans, ...";
        m += "\n- develop in any Java aware scripting language adding Sikuli features in IDE's like Eclipse, NetBeans, ...";
        m += "\n\nJython developement: special info:";
        m += "\n If you want to use standalone Jython in parallel, you should select Pack 3 additionally (Option 3)";
        if (Settings.isWindows()) {
          m += "\n\nSpecial info for Windows systems:";
          m += "\nThe generated jars can be used out of the box with Java 32-Bit and Java 64-Bit as well.";
          m += "\nThe Java version is detected at runtime and the native support is switched accordingly.";
        }
        break;
      case(3):
        om = "Package 3: ... in addition to Package 1 or Package 2 for use with Jython";
//              -------------------------------------------------------------
        m += "\nThis package is of interest, if you plan to develop Jython scripts outside of the"
             + " SikuliX environment using your own standalon Jython or other IDE's";
        m += "\nThe advantage: since it does not contain the Jython interpreter package, there"
             + " cannot be any collisions on the Python path.";
        m += "\n\nIt contains the Sikuli Jython API, adds itself to Python path at runtime"
             + "\nand exports the Sikuli Python modules to the folder libs/Libs"
             + " that helps to setup the auto-complete in IDE's like NetBeans, Eclipse ...";
        if (Settings.isWindows()) {
          m += "\n\nSpecial info for Windows systems:";
          m += "\nThe generated jars can be used out of the box with Java 32-Bit and Java 64-Bit as well.";
          m += "\nThe Java version is detected at runtime and the native support is switched accordingly.";
        }
        break;
      case(5):
        om = "Package 3: To support developement in Java or any Java aware scripting language"
                + "\n\n( ... make sure neither Option 1 (IDE) nor Option 2 (Script) is selected!"
                + "\nIf you want it additionally to IDE or Script, use the previous Option 3!)";
//              -------------------------------------------------------------
        m += "\nThe content of this package is stripped down to what is needed to develop in Java"
             + " or any Java aware scripting language \n(no IDE, no bundled script run support for Jython)";
        m += "\n\nHence this package is not runnable and must be in the class path to use it"
             + " for developement or at runtime";
        m += "\n\nSpecial info for usage with Jython: It contains the Sikuli Jython API ..."
             + "\n... and adds itself to Python path at runtime"
             + "\n... and exports the Sikuli Python modules to the folder libs/Libs at runtime"
             + "\nthat helps to setup the auto-complete in IDE's like NetBeans, Eclipse ...";
        if (Settings.isWindows()) {
          m += "\n\nSpecial info for Windows systems:";
          m += "\nThe generated jars can be used out of the box with Java 32-Bit and Java 64-Bit as well.";
          m += "\nThe Java version is detected at runtime and the native support is switched accordingly.";
        }
       break;
      case(4):
        om = "To get the additional Tesseract stuff into your packages to use the OCR engine";
//              -------------------------------------------------------------
        m += "\nOnly makes sense for Windows and Mac," + 
                "\nsince for Linux the complete install of Tesseract is your job.";
        m += "\nFeel free to add this to your packages, \n...but be aware of the restrictions, oddities "
             + "and bugs with the current OCR and text search feature.";
        m += "\nIt adds more than 10 MB to your jars and the libs folder at runtime."
             + "\nSo be sure, that you really want to use it!";
        m += "\n\nIt is NOT recommended for people new to Sikuli."
             + "\nYou might add this feature later after having gathered some experiences with Sikuli";
        break;
      case(6):
        om = "To prepare the selected packages to run on all supported systems";
//              -------------------------------------------------------------
        m += "\nWith this option NOT selected, the setup process will only add the system specific"
             + " native stuff \n(Windows: support for both Java 32-Bit and Java 64-Bit is added)";
        m += "\n\nSo as a convenience you might select this option to produce jars, that are"
             + " useable out of the box on Windows, Mac and Linux.";
        m += "\nThis is possible now, since the usage of Sikuli does not need any system specific"
             + " preparations any more. \nJust use the package (some restrictions on Linux though).";
        m += "\n\nSome scenarios for usages in different system environments:";
        m += "\n- download or use the jars from a central network place ";
        m += "\n- use the jars from a stick or similar mobile medium";
        m += "\n- deploying Sikuli apps to be used all over the place";
        break;        
    }
    if (option == 4 || option == 5) {
      option = option == 4 ? 5 : 4;
    }
    popInfo("asking for option " + option + ": " + om +"\n" + m);
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
  
  private static boolean download(String sDir, String tDir, String item, String jar) {
    boolean deleteDownloads = false;
    File downloaded = new File(workDir, "Downloads/" + item);
    if (downloaded.exists()) {
      if (popAsk("You already have this in your Setup/Downloads folder:\n"
                 + downloaded.getAbsolutePath()
                 + "\nClick YES, if you want to use this for setup processing\n\n"
                 + "... or click NO, to download a fresh copy\n"
                 + "(folder Download will be deleted on success in this case)")) {
        try {
          FileManager.xcopy(downloaded.getAbsolutePath(), jar, null);
        } catch (IOException ex) {
          terminate("Unable to copy from local Downloads: " + 
                    downloaded.getAbsolutePath() + "\n" + ex.getMessage());
        }
        log(lvl, "Copied form local Download: " + item);
        return true;
      } else {
        deleteDownloads = true;
      }
    }
    JFrame progress = new MultiFrame("download");
    String fname = FileManager.downloadURL(sDir + item, tDir, progress);
    progress.dispose();
    if (null == fname) {
      log1(-1, "Fatal error 001: not able to download: %s", item);
      return false;
    }
    if (!(new File(tDir, item)).renameTo(new File(jar))) {
      log1(-1, "rename to %s did not work", jar);
      return false;
    }
    if (deleteDownloads) {
      FileManager.deleteFileOrFolder(new File(workDir, "Downloads").getAbsolutePath());
    }
    return true;
  }
  
  private static void terminate(String msg) {
    log1(-1, msg);
    log1(-1, "... terminated abnormally :-(");
    popError("Something serious happened! Sikuli not useable!\n"
            + "Check the error log at " + logfile);
    System.exit(1);
  }
}
