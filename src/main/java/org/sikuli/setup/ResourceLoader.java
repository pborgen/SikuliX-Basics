/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2012
 */
package org.sikuli.setup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;
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
  private StringBuffer alreadyLoaded = new StringBuffer("");
  private ClassLoader cl;
  private List<String[]> libsList = new ArrayList<String[]>();
  private String fileList = "/filelist.txt";
  private static final String sikhomeEnv = System.getenv("SIKULIX_HOME");
  private static final String sikhomeProp = System.getProperty("sikuli.Home");
  private static final String userdir = System.getProperty("user.dir");
  private static final String userhome = System.getProperty("user.home");
  private String libPath = null;
  private String libPathFallBack = null;
  private File libsDir = null;
  private String jarPath = null;
  private String checkFileName = null;
  private String checkLib = null;
  private static final String libSub = FileManager.slashify("SikuliX/libs", false);
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

  public ResourceLoader() {
    cl = this.getClass().getClassLoader();
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
      String osarch = System.getProperty("os.arch");
      log(lvl, "we are running on arch: " + osarch);

      //  Mac specific 
      if (Settings.isMac()) {
        if (!osarch.contains("64")) {
          log(-1, "Mac: only 64-Bit supported");
          System.exit(1);
        }
        libSource = libSource64;
        checkFileName = "MadeForSikuliX64M.txt";
        checkLib = "MacUtil";
        if ((new File(libPathMac)).exists()) {
          libPathFallBack = libPathMac;
        }
      }

      // Windows specific 
      if (Settings.isWindows()) {
        if (!osarch.contains("64")) {
          libSource = libSource64;
          checkFileName = "MadeForSikuliX64W.txt";
          checkLib = "WinUtil";
          if ((new File(libPathWin)).exists()) {
            libPathFallBack = libPathWin;
          }
        } else {
          libSource = libSource32;
          checkFileName = "MadeForSikuliX32W.txt";
          if ((new File(libPathWin)).exists()) {
            libPathFallBack = libPathWin;
          } else if ((new File(libPathWin32)).exists()) {
            libPathFallBack = libPathWin32;
          }
        }
      }

      // Linux specific
      if (Settings.isLinux()) {
        if (!osarch.contains("64")) {
          libSource = libSource64;
        } else {
          libSource = libSource32;
        }
        checkLib = "JXGrabKey";
      }

      // check Java property sikuli.home
      if (sikhomeProp != null) {
        libspath = (new File(FileManager.slashify(sikhomeProp, true) + "libs")).getAbsolutePath();
        if ((new File(libspath)).exists()) {
          libPath = libspath;
        }
        log(lvl, "Libs in Property.sikuli.Home? %s: %s", libPath == null ? "NO" : "YES", libspath);
        libsDir = checkLibsDir(libPath);
      }

      // check environmenet SIKULIX_HOME
      if (libPath == null && sikhomeEnv != null) {
        libspath = FileManager.slashify(sikhomeEnv, true) + "libs";
        if ((new File(libspath)).exists()) {
          libPath = libspath;
        }
        log(lvl, "Libs in Environment.SIKULIX_HOME? %s: %s", libPath == null ? "NO" : "YES", libspath);
        libsDir = checkLibsDir(libPath);
      }

      // check parent folder of jar file
      if (libPath == null) {
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
        String lfp = null;
        if (src.getLocation() != null) {
          String srcParent = (new File(src.getLocation().getPath())).getParent();
          jarPath = srcParent;
          lfp = FileManager.slashify(srcParent, true) + "libs";
          libsfolder = (new File(lfp));
          if (libsfolder.exists()) {
            libPath = lfp;
          }
          log(lvl, "Libs at location of jar? %s: %s", libPath == null ? "NO" : "YES", jarPath);
          libsDir = checkLibsDir(libPath);
        }
      }

      // check the users home folder
      if (libPath == null && userhome != null) {
        File wd = new File(FileManager.slashify(userhome, true) + libSub);
        if (wd.exists()) {
          libPath = wd.getAbsolutePath();
        }
        log(lvl, "Libs in user home folder? %s: %s", libPath == null ? "NO" : "YES", 
                wd.getAbsolutePath());
        libsDir = checkLibsDir(libPath);
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
        log(lvl, "Libs in working folder or its parent? %s: %s", libPath == null ? "NO" : "YES", 
                wd.getAbsolutePath());
        libsDir = checkLibsDir(libPath);
      }
    }

    if (libPath == null) {
      log(lvl, "No valid libs path available - trying to extract libs from jar");
      if (libPathFallBack != null) {
        libPath = libPathFallBack;
        log(lvl, "Checking available fallback" + libPath);
        libsDir = checkLibsDir(libPath);
      }
      if (libPath == null && jarPath != null) {
        log(lvl, "Trying to extract libs to jar parent folder: " + jarPath);
        File jarPathLibs = extractLibs((new File(jarPath)).getAbsolutePath(), libSource);
        if (jarPathLibs == null) {
          log(-1, "not possible!");
        } else {
          libPath = jarPathLibs.getAbsolutePath();
        }
      }
      if (libPath == null && userhome != null) {
        String userhomeLibsDir = FileManager.slashify(userhome, true) + "SikuliX";
        log(lvl, "Trying to extract libs to user home: " + userhomeLibsDir);
        File userhomeLibs = extractLibs((new File(userhomeLibsDir)).getAbsolutePath(), libSource);
        if (userhomeLibs == null) {
          log(-1, "not possible!");
        } else {
          libPath = userhomeLibs.getAbsolutePath();
        }
      }
      if (libPath == null) {
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
  }

  private File checkLibsDir(String path) {
    File dir = null;
    String memx = mem;
    mem = "checkLibsDir";
    if (path != null) {
      log(lvl, path);
      dir = new File(path);
      if ((new File(FileManager.slashify(path, true) + checkFileName)).exists()) {
        //TODO check outdated against jar date last modified 
        loadLib(checkLib);
        log(lvl, "Using libs at: " + path);
      } else {
        log(-1, "Not a valid libs dir for SikuliX:" + path);
        dir = null;
        libPath = null; // reset libPath
      }
      if (Settings.isWindows()) {
        //TODO check Windows system path
      }
    }
    mem = memx;
    return dir;
  }

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
    } else if ("convertSrcToHtml".equals(action)) {
      return true;
    } else {
      return false;
    }
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
    boolean libFound = false;
    String mappedlib = System.mapLibraryName(libname);
    File lib = new File(libPath, mappedlib);
    if (!lib.exists()) {
      log(-1, "Fatal error: not found: " + lib.getAbsolutePath());
      System.exit(1);
    }
    log(lvl, "Found: " + libname);
    libFound = true;
    try {
      System.load(lib.getAbsolutePath());
    } catch (Error e) {
      log(-1, "Fatal error loading: " + libname);
      if (libFound) {
        log(-1, "Since native library was found, it might be a problem with needed dependent libraries");
        e.getMessage();
      }
      System.exit(1);
    }
    log(lvl, "Now loaded: " + libname);
    mem = memx;
  }

  /**
   * extract a resource to a writable file
   *
   * @param resourcename the name of the resource on the classpath
   * @param outputfile the file to copy to
   * @return the extracted file
   * @throws IOException
   */
  private File extractResource(String resourcename, File outputfile) throws IOException {
    InputStream in = cl.getResourceAsStream(resourcename);
    if (in == null) {
      throw new IOException("Resource " + resourcename + " not on classpath");
    }
    if (!outputfile.getParentFile().exists()) {
      outputfile.getParentFile().mkdirs();
    }
    log(lvl, "Extracting to %s", outputfile.getAbsolutePath());
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

  private File extractLibs(String targetDir, String libSource) {
    String memx = mem;
    mem = "extractLibs";
    log(lvl, "Trying to acces jar");
    CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
    int iDir = 0;
    int iFile = 0;
    if (src != null) {
      URL jar = src.getLocation();
      if (jar == null) {
        log(-1, "Not running from jar");
        mem = memx;
        return null;
      } else {
        try {
          ZipInputStream zip = new ZipInputStream(jar.openStream());
          ZipEntry ze;
          log(lvl, "Accessing jar: " + jar.toString());
          while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();
            if (entryName.startsWith(libSource)) {
              if (entryName.endsWith(File.separator)) {
                iDir++;
              } else {
                libsList.add(new String[]{FileManager.slashify(entryName, false),
                  String.format("%d", ze.getTime())});
                iFile++;
              }
            }
          }
          log(lvl, "Found in %s: Dirs: %d Files: %d", libSource, iDir, iFile);
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
          extractResource(e[0], targetFile);
          log(lvl, "is from: %s (%d)", e[1], targetDate);
        } else {
          log(lvl, "already in place: " + targetName);
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
