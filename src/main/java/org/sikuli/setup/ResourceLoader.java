/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.sikuli.script.Debug;
import org.sikuli.script.FileManager;
import org.sikuli.script.IResourceLoader;
import org.sikuli.script.Settings;

public class ResourceLoader implements IResourceLoader {

  //<editor-fold defaultstate="collapsed" desc="new logging concept">
  private String me = "ResourceLoaderBasic";
  private String mem = "...";
  private int lvl = 3;

  private void log(int level, String message, Object... args) {
    Debug.logx(level, level < 0 ? "error" : "debug",
            me + ": " + mem + ": " + message, args);
  }
  //</editor-fold>
  private String loaderName = "basic";
  //"HKCU\Environment /v PATH /t REG_EXPAND_SZ /f /d  ";
  // "^%USERPROFILE^%\AppData\Roaming\SikuliX\libs;^%PATH^
  private static final String cmdRegQuery = "reg QUERY %s /v %s";
  private static final String cmdRegAdd = "reg ADD %s /v %s /t %s /f /d %s ";
  private static String cmdRegQueryPath = String.format(cmdRegQuery, "HKCU\\Environment", "PATH");
  private String cmdRegKey;
  private String cmdRegTyp;
  private String cmdRegValue;
  private StringBuffer alreadyLoaded = new StringBuffer("");
  private ClassLoader cl;
  private String jarParentPath = null;
  private String jarPath = null;
  private List<String[]> libsList = new ArrayList<String[]>();
  private String fileList = "/filelist.txt";
  private static final String sikhomeEnv = System.getenv("SIKULIX_HOME");
  private static final String sikhomeProp = System.getProperty("sikuli.Home");
  private static final String userdir = System.getProperty("user.dir");
  private static final String userhome = System.getProperty("user.home");
  private String libPath = null;
  private String libPathFallBack = null;
  private File libsDir = null;
  private static final String checkFileNameAll = "MadeForSikuliX";
  private String checkFileNameMac = checkFileNameAll + "64M.txt";
  private String checkFileNameW32 = checkFileNameAll + "32W.txt";
  private String checkFileNameW64 = checkFileNameAll + "64W.txt";
  private String checkFileNameL32 = checkFileNameAll + "32L.txt";
  private String checkFileNameL64 = checkFileNameAll + "64L.txt";
  private String checkFileName = null;
  private String checkLib = null;
  private static final String prefixSikuli = "SikuliX";
  private static final String suffixLibs = "/libs";
  private static final String libSub = prefixSikuli + suffixLibs;
  private String userSikuli = null;
  /**
   * Mac: standard place for native libs
   */
  private static String libPathMac = "/Applications/SikuliX-IDE.app/Contents/libs";
  /**
   * Win: standard place for native libs
   */
  private static final String libPathWin = FileManager.slashify(System.getenv("ProgramFiles"), true) + libSub;
  private static final String libPathWin32 = FileManager.slashify(System.getenv("ProgramFiles(x86)"), true) + libSub;
  /**
   * in-jar folder to load other ressources from
   */
  private static String jarResources = "META-INF/res/";
  /**
   * in-jar folder to load native libs from
   */
  private static String libSource32 = "META-INF/libs/libs32/";
  private static String libSource64 = "META-INF/libs/libs64/";
  private String libSource;
  private String osarch;
  private String javahome;

  public ResourceLoader() {
    cl = this.getClass().getClassLoader();
    CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
    if (src.getLocation() != null) {
      jarPath = src.getLocation().getPath();
      jarParentPath = FileManager.slashify((new File(jarPath)).getParent(), true);
    } else {
      log(-1, "No access to the jar files!");
      System.exit(1);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(String[] args) {
    //Debug.log(lvl, "%s: %s: init", me, loaderName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void check(String what) {
    mem = "check";
    if (!what.equals(Settings.SIKULI_LIB)) {
      log(-1, "Currently only Sikuli libs supported!");
      return;
    }
    if (libPath == null || libsDir == null) {
      libPath = null;
      libsDir = null;
      File libsfolder;
      String libspath;

      // check the bit-arch
      osarch = System.getProperty("os.arch");
      log(lvl, "we are running on arch: " + osarch);
      javahome = FileManager.slashify(System.getProperty("java.home"), true);
      log(lvl, "using Java at: " + javahome);

      if (userhome != null) {
        if (Settings.isWindows()) {
          userSikuli = System.getenv("APPDATA");
          if (userSikuli != null) {
            userSikuli = FileManager.slashify(userSikuli, true) + prefixSikuli;
          }
        } else {
          userSikuli = FileManager.slashify(userhome, true) + prefixSikuli;
        }
      }

      //  Mac specific 
      if (Settings.isMac()) {
        if (!osarch.contains("64")) {
          log(-1, "Mac: only 64-Bit supported");
          System.exit(1);
        }
        libSource = libSource64;
        checkFileName = checkFileNameMac;
        checkLib = "MacUtil";
        if ((new File(libPathMac)).exists()) {
          libPathFallBack = libPathMac;
        }
      }

      // Windows specific 
      if (Settings.isWindows()) {
        if (osarch.contains("64")) {
          libSource = libSource64;
          checkFileName = checkFileNameW64;
          if ((new File(libPathWin)).exists()) {
            libPathFallBack = libPathWin;
          }
        } else {
          libSource = libSource32;
          checkFileName = checkFileNameW32;
          if ((new File(libPathWin)).exists()) {
            libPathFallBack = libPathWin;
          } else if ((new File(libPathWin32)).exists()) {
            libPathFallBack = libPathWin32;
          }
        }
        checkLib = "WinUtil";
      }

      // Linux specific
      if (Settings.isLinux()) {
        if (!osarch.contains("64")) {
          libSource = libSource64;
          checkFileName = checkFileNameL64;
        } else {
          libSource = libSource32;
          checkFileName = checkFileNameL32;
        }
        checkLib = "JXGrabKey";
      }

      // check Java property sikuli.home
      if (sikhomeProp != null) {
        libspath = (new File(FileManager.slashify(sikhomeProp, true) + "libs")).getAbsolutePath();
        if ((new File(libspath)).exists()) {
          libPath = libspath;
        }
        log(lvl, "Exists Property.sikuli.Home? %s: %s", libPath == null ? "NO" : "YES", libspath);
        libsDir = checkLibsDir(libPath);
      }

      // check environmenet SIKULIX_HOME
      if (libPath == null && sikhomeEnv != null) {
        libspath = FileManager.slashify(sikhomeEnv, true) + "libs";
        if ((new File(libspath)).exists()) {
          libPath = libspath;
        }
        log(lvl, "Exists Environment.SIKULIX_HOME? %s: %s", libPath == null ? "NO" : "YES", libspath);
        libsDir = checkLibsDir(libPath);
      }

      // check the users home folder
      if (libPath == null && userSikuli != null) {
        File ud = new File(userSikuli + suffixLibs);
        if (ud.exists()) {
          libPath = ud.getAbsolutePath();
        }
        log(lvl, "Exists libs folder in user home folder? %s: %s", libPath == null ? "NO" : "YES",
                ud.getAbsolutePath());
        libsDir = checkLibsDir(libPath);
      }

      // check parent folder of jar file
      if (libPath == null && jarPath != null) {
        if (jarPath.endsWith(".jar")) {
          String lfp = jarParentPath + "libs";
          libsfolder = (new File(lfp));
          if (libsfolder.exists()) {
            libPath = lfp;
          }
          log(lvl, "Exists libs folder at location of jar? %s: %s", libPath == null ? "NO" : "YES", jarParentPath);
          libsDir = checkLibsDir(libPath);
        } else {
          log(lvl, "not running from jar: " + jarParentPath);
        }
      }

      // check the working directory and its parent
      if (libPath == null && userdir != null) {
        File wd = new File(userdir);
        File wdp = new File(userdir).getParentFile();
        File wdl = new File(FileManager.slashify(wd.getAbsolutePath(), true) + libSub);
        File wdpl = new File(FileManager.slashify(wdp.getAbsolutePath(), true) + libSub);
        if (wdl.exists()) {
          libPath = wdl.getAbsolutePath();
        } else if (wdpl.exists()) {
          libPath = wdpl.getAbsolutePath();
        }
        log(lvl, "Exists libs folder in working folder or its parent? %s: %s", libPath == null ? "NO" : "YES",
                wd.getAbsolutePath());
        libsDir = checkLibsDir(libPath);
      }

      if (libPath == null && libPathFallBack != null) {
        libPath = libPathFallBack;
        log(lvl, "Checking available fallback for libs folder: " + libPath);
        libsDir = checkLibsDir(libPath);
        if (libsDir == null) {
          libPath = null; // non-valid fallback makes no sense
          log(lvl, "We do not update the fallback libs folder");
        }
      }
    }

    if (libsDir == null && libPath != null) {
      log(-1, "libs dir is empty, has wrong content or is outdated - extracting libs to: " + libPath);
      File dir = new File(libPath);
      File[] dirList = dir.listFiles();
      boolean success = true;
      if (dirList.length > 0) {
        for (File f : dirList) {
          if (f.isFile() && !f.delete()) {
            success = false;
          }
        }
        if (!success) {
          log(-1, "not possible to empty libs dir");
          System.exit(1);
        }
      }
      if (extractLibs(dir.getParent(), libSource) == null) {
        log(-1, "not possible!");
        libPath = null;
      }
      libsDir = checkLibsDir(libPath);
    }

    //<editor-fold defaultstate="collapsed" desc="libs dir finally invalid">
    if (libPath == null) {
      log(lvl, "No valid libs path available until now!");
      if (libPath == null && jarParentPath != null && jarParentPath.endsWith(".jar")) {
        log(lvl, "Trying to extract libs to jar parent folder: " + jarParentPath);
        File jarPathLibs = extractLibs((new File(jarParentPath)).getAbsolutePath(), libSource);
        if (jarPathLibs == null) {
          log(-1, "not possible!");
        } else {
          libPath = jarPathLibs.getAbsolutePath();
        }
      }
      if (libPath == null && userSikuli != null) {
        log(lvl, "Trying to extract libs to user home: " + userSikuli);
        File userhomeLibs = extractLibs((new File(userSikuli)).getAbsolutePath(), libSource);
        if (userhomeLibs == null) {
          log(-1, "not possible!");
        } else {
          libPath = userhomeLibs.getAbsolutePath();
        }
      }
      libsDir = checkLibsDir(libPath);
      if (libPath == null || libsDir == null) {
        log(-1, "No valid native libraries folder available - giving up!");
        System.exit(1);
      }
    }

    if (Settings.OcrDataPath == null) {
      if (Settings.isWindows() || Settings.isMac()) {
        log(lvl, "Using this as OCR directory (tessdata) too");
        Settings.OcrDataPath = libPath;
      } else {
        Settings.OcrDataPath = "/usr/local/share";
      }
    }
    //</editor-fold>
  }

  private File checkLibsDir(String path) {
    String memx = mem;
    mem = "checkLibsDir";
    File dir = null;
    if (path != null) {
      log(lvl, path);
      File checkFile = (new File(FileManager.slashify(path, true) + checkFileName));
      if (checkFile.exists()) {
        if ((new File(jarPath)).lastModified() > checkFile.lastModified()) {
          log(-1, "libs folder outdated - trying to reload from jar");
        } else {
          //TODO check outdated against jar date last modified
          if (Settings.isWindows()) {
            // is on system path?
            String syspath = System.getenv("PATH");
            if (!syspath.contains((new File(path).getAbsolutePath()))) {
              log(-1, "Fatal error: libs dir is not on system path (envionment: PATH)");
              log(lvl, "Trying to set systempath");
              String winpath = runcmd(new String[]{cmdRegQueryPath});
              if (winpath.startsWith("*** error ***")) {
                log(-1, "Not possible to access registry!");
              } else {
                log(lvl, winpath);
                System.exit(1);
              }
            }

            //convenience: jawt.dll in libsdir avoids need for java/bin in system path
            String lib = "jawt.dll";
            try {
              extractResource(javahome + "bin/" + lib, new File(path, lib), false);
            } catch (IOException ex) {
              log(-1, "Fatal error: problem copying " + lib + "\n" + ex.getMessage());
              System.exit(1);
            }
          }
          loadLib(checkLib);
          log(lvl, "Using libs at: " + path);
          dir = new File(path);
        }
      } else {
        if (Settings.isWindows()) {
          // might be wrong arch
          if ((new File(FileManager.slashify(path, true) + checkFileNameW32)).exists()
                  || (new File(FileManager.slashify(path, true) + checkFileNameW64)).exists()) {
            log(-1, "libs dir contains wrong arch for " + osarch);
          }
        } else {
          log(-1, "Not a valid libs dir for SikuliX (" + osarch + "): " + path);
        }
      }
    }
    mem = memx;
    return dir;
  }

  //<editor-fold defaultstate="collapsed" desc="overwritten">
  /**
   * {@inheritDoc}
   */
  @Override
  public void export(String res, String target) {
    mem = "export";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(String[] args) {
    mem = "install";
    log(lvl, "entered");
    //extractLibs(args[0]);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean doSomethingSpecial(String action, Object[] args) {
    if ("loadLib".equals(action)) {
      loadLib((String) args[0]);
      return true;
    } else if ("runcmd".equals(action)) {
      String retval = runcmd((String[]) args);
      args[0] = retval;
      return true;
    } else {
      return false;
    }
  }

  private String runcmd(String args[]) {
    String memx = mem;
    mem = "runcmd";
    String result = "";
    String error = "*** error ***" + System.lineSeparator();
    try {
      log(lvl, args[0]);
      Process process = Runtime.getRuntime().exec(args[0]);
      BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
      BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
      String s;
      while ((s = stdInput.readLine()) != null) {
        result += s + System.lineSeparator();
      }
      if ((s = stdError.readLine()) != null) {
        result = error + result;
      }
    } catch (Exception e) {
      e.printStackTrace(System.err);
    }
    mem = memx;
    return System.lineSeparator() + result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return loaderName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getResourceTypes() {
    return Settings.SIKULI_LIB;
  }

  /**
   * make sure, a native library is available and loaded
   *
   * @param libname System.loadLibrary() compatible library name
   * @return the extracted File object
   * @throws IOException
   */
  public void loadLib(String libname) {
    String memx = mem;
    mem = "loadLib";
    if (libname == null || "".equals(libname)) {
      log(-1, "libname == null");
      mem = memx;
      return;
    }
    if (alreadyLoaded.indexOf("*" + libname) < 0) {
      alreadyLoaded.append("*").append(libname);
    } else {
      log(lvl, "Is already loaded: " + libname);
      mem = memx;
      return;
    }
    log(lvl, libname);
    if (libPath == null) {
      log(-1, "Fatal error: No libs directory available");
      System.exit(1);
    }
    String mappedlib = System.mapLibraryName(libname);
    File lib = new File(libPath, mappedlib);
    if (!lib.exists()) {
      log(-1, "Fatal error: not found: " + lib.getAbsolutePath());
      System.exit(1);
    }
    log(lvl, "Found: " + libname);
    try {
      System.load(lib.getAbsolutePath());
    } catch (Error e) {
      log(-1, "Fatal error loading: " + mappedlib);
      log(-1, "Since native library was found, it might be a problem with needed dependent libraries\n%s",
              e.getMessage());
      System.exit(1);
    }
    log(lvl, "Now loaded: " + libname);
    mem = memx;
  }
  //</editor-fold>

  private File extractLibs(String targetDir, String libSource) {
    String memx = mem;
    mem = "extractLibs";
    log(lvl, "Trying to access package");
    CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
    int iDir = 0;
    int iFile = 0;
    URL jar;
    boolean isJar = false;
    if (src != null) {
      jar = src.getLocation();
      if (jar == null) {
        log(-1, "Not running from jar");
        mem = memx;
        return null;
      } else if (!jar.getPath().endsWith(".jar")) {
        File folder = new File(jar.getPath(), libSource);
        log(lvl, "accessing folder: " + folder.getAbsolutePath());
        File[] flist = folder.listFiles();
        for (File f : flist) {
          log(lvl + 2, "file: " + f.getAbsolutePath());
          if (f.isFile()) {
            libsList.add(new String[]{FileManager.slashify(f.getAbsolutePath(), false),
              String.format("%d", f.lastModified())});
            iFile++;
          }
        }
        log(lvl, "Found in %s: %d files", libSource, iFile);
      } else {
        try {
          ZipInputStream zip = new ZipInputStream(jar.openStream());
          ZipEntry ze;
          log(lvl, "Accessing jar: " + jar.toString());
          while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();
            if (entryName.startsWith(libSource)
                    && !entryName.endsWith("/")) {
              log(lvl + 2, "%d: %s", iFile, entryName);
              libsList.add(new String[]{FileManager.slashify(entryName, false),
                String.format("%d", ze.getTime())});
              iFile++;
            }
          }
          log(lvl, "Found %d Files in %s", iFile, libSource);
          isJar = true;
        } catch (IOException e) {
          log(-1, "Did not work!\n%s", e.getMessage());
          mem = memx;
          return null;
        }
      }
    } else {
      Debug.error("Cannot access jar");
      mem = memx;
      return null;
    }
    targetDir = FileManager.slashify(targetDir, true) + "libs";
    (new File(targetDir)).mkdirs();
    String targetName = null;
    File targetFile;
    long targetDate;
    for (String[] e : libsList) {
      try {
        targetName = e[0].substring(e[0].lastIndexOf("/") + 1);
        targetFile = new File(targetDir, targetName);
        if (targetFile.exists()) {
          targetDate = targetFile.lastModified();
        } else {
          targetDate = 0;
        }
        if (targetDate == 0 || targetDate < Long.valueOf(e[1])) {
          extractResource(e[0], targetFile, isJar);
          log(lvl + 2, "is from: %s (%d)", e[1], targetDate);
        } else {
          log(lvl + 2, "already in place: " + targetName);
        }
      } catch (IOException ex) {
        log(lvl, "IO-problem extracting: %s\n%s", targetName, ex.getMessage());
        mem = memx;
        return null;
      }
    }
    mem = memx;
    return new File(targetDir);
  }

  /**
   * extract a resource to a writable file
   *
   * @param resourcename the name of the resource on the classpath
   * @param outputfile the file to copy to
   * @return the extracted file
   * @throws IOException
   */
  private File extractResource(String resourcename, File outputfile, boolean isJar) throws IOException {
    InputStream in;
    if (isJar) {
      in = cl.getResourceAsStream(resourcename);
    } else {
      in = new FileInputStream(resourcename);
    }
    if (in == null) {
      throw new IOException("Resource " + resourcename + " not on classpath");
    }
    if (!outputfile.getParentFile().exists()) {
      outputfile.getParentFile().mkdirs();
    }
    log(lvl + 2, "Extracting from: " + resourcename);
    log(lvl + 2, "Extracting to: " + outputfile.getAbsolutePath());
    copyResource(in, outputfile);
    return outputfile;
  }

  private void copyResource(InputStream in, File outputfile) throws IOException {
    OutputStream out = null;
    try {
      out = new FileOutputStream(outputfile);
      copy(in, out);
    } catch (IOException e) {
      log(-1, "Not possible: " + e.getMessage());
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Extract files from a jar using a list of files in a file (def. filelist.txt)
   *
   * @param srcPath from here
   * @param localPath to there (if null, create a default in temp folder)
   * @return the local path to the extracted resources
   * @throws IOException
   */
  private String extractWithList(String srcPath, String localPath) throws IOException {
    mem = "extractWithList";
    if (localPath == null) {
      localPath = Settings.BaseTempPath + File.separator + "sikuli" + File.separator + srcPath;
      new File(localPath).mkdirs();
    }
    log(lvl, "From " + srcPath + " to " + localPath);
    localPath = FileManager.slashify(localPath, true);
    BufferedReader r = new BufferedReader(new InputStreamReader(
            cl.getResourceAsStream(srcPath + fileList)));
    if (r == null) {
      log(-1, "File containing file list not found: " + fileList);
      return null;
    }
    String line;
    InputStream in;
    while ((line = r.readLine()) != null) {
      String fullpath = localPath + line;
      log(lvl, "extracting: " + fullpath);
      File outf = new File(fullpath);
      outf.getParentFile().mkdirs();
      in = cl.getResourceAsStream(srcPath + line);
      if (in != null) {
        copyResource(in, outf);
      } else {
        log(-1, "Not found");
      }
    }
    return localPath;
  }

  /**
   * copy an InputStream to an OutputStream.
   *
   * @param in InputStream to copy from
   * @param out OutputStream to copy to
   * @throws IOException if there's an error
   */
  private void copy(InputStream in, OutputStream out) throws IOException {
    byte[] tmp = new byte[8192];
    int len = 0;
    while (true) {
      len = in.read(tmp);
      if (len <= 0) {
        break;
      }
      out.write(tmp, 0, len);
    }
  }
}
