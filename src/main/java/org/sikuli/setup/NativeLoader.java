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
import org.sikuli.script.INativeLoader;
import org.sikuli.script.Settings;

public class NativeLoader implements INativeLoader {

  private static String me = "NativeLoader"; //NativeLoader.class.getName();
  public static String SIKULI_LIB = "sikuli_lib";
  private static String loaderName = "basic";
  private static StringBuffer alreadyLoaded = new StringBuffer("");
  private static String libSource = Settings.libSource;
  private static ClassLoader cl;
  private static File libsDir = null;
  private static final ArrayList<String> libPaths = new ArrayList<String>();
  private static List<String> libsList = new ArrayList<String>();

  /**
   * {@inheritDoc}
   */
  @Override
  public void init(String[] args) {
    //Debug.log(2, "%s: %s: init", me, loaderName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void check(String what) {
    if (!what.equals(getLibType())) {
      Debug.error("%s: check: Currently only Sikuli libs supported!", me);
      return;
    }
    if (libPaths.isEmpty()) {
      libPaths.add(Settings.libPath);
    }
    if (libsDir == null) {
      Debug.log(2, "NativeLoader: %s: check: Trying to get the libs directory", loaderName);
      File dir;
      for (String path : libPaths) {
        if (path == null || "".equals(path)) {
          continue;
        }
        dir = new File(path);
        if (dir.exists()) {
          System.setProperty("java.library.tmpdir", path);
          libsDir = dir;
          break;
        }
      }
      if (libsDir == null) {
        String libTmpDir = Settings.BaseTempPath + File.separator + "tmplib";
        System.setProperty("java.library.tmpdir", libTmpDir);
        libsDir = new File(libTmpDir);
        Debug.log(2, "NativeLoader: %s: check: Trying to create a temp libs directory", loaderName);
      }
      if (!libsDir.exists()) {
        if (!libsDir.mkdirs()) {
          Debug.log(2, "NativeLoader: %s: check: Not possible to create libs directory: %s",
                  loaderName, libsDir.getAbsolutePath());
          libsDir = null;
        }
      }
      if (libsDir != null) {
        Debug.log(2, "NativeLoader: %s: check: Using as libs directory: %s",
                loaderName, libsDir.getAbsolutePath());
        if (Settings.OcrDataPath == null) {
          Debug.log(2, "NativeLoader: %s: check: Using this as OCR directory (tessdata) too", loaderName);
          Settings.OcrDataPath = libsDir.getAbsolutePath();
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void export(String res, String target) {
    if (res == null || "".equals(res)) {
      Debug.error("%s: export: ressource == null", me);
      return;
    }
    if (!res.endsWith(getLibType())) {
      Debug.error("%s: export: currently only Sikuli libs supported!", me);
      return;
    }
    res = res.substring(0, res.indexOf(getLibType()));
    Debug.log(2, "%s: %s: export: %s", me, loaderName, res);
    if (libsDir == null) {
      Debug.error("%s: %s: export: Not possible: No libs directory available", me, loaderName);
      return;
    }
    File lib = null;
    boolean libFound = false;
    try {
      lib = exportLib(res);
      if (lib == null) {
        Debug.log(2, "%s: %s: export: %s already loaded", me, loaderName, res);
        return;
      }
      Debug.log(2, "%s: %s: export: %s found", me, loaderName, res);
      libFound = true;
    } catch (IOException ex) {
      Debug.error("%s: %s: export: %s could not be extracted nor found", me, loaderName, res);
      System.exit(1);
    }
    try {
      System.load(lib.getAbsolutePath());
    } catch (Error e) {
      Debug.error("%s: %s: export: %s could not be loaded", me, loaderName, res);
      if (libFound) {
        Debug.error("Since native library was found, it might be a problem with needed dependent libraries");
        e.printStackTrace();
      }
      System.exit(1);
    }
    Debug.log(2, "%s: %s: export: %s loaded", me, loaderName, res);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void install(String[] args) {
    Debug.log(2, "NativeLoader: install: %s", loaderName);
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
  public String getLibType() {
    return SIKULI_LIB;
  }

  /**
   * extract a JNI library from the classpath <br /> Mac: default is .jnilib (.dylib as fallback)
   *
   * @param libname System.loadLibrary() compatible library name
   * @return the extracted File object
   * @throws IOException
   */
  private static File exportLib(String libname) throws IOException {
    if (alreadyLoaded.indexOf("*" + libname) < 0) {
      alreadyLoaded.append("*").append(libname);
    } else {
      return null;
    }
    String mappedlib = System.mapLibraryName(libname);
    File outfile = new File(libsDir, mappedlib);
    if (!outfile.exists()) {
      URL res = cl.getResource(libSource + mappedlib);
      if (res == null) {
        if (mappedlib.endsWith(".jnilib")) {
          mappedlib = mappedlib.substring(0, mappedlib.length() - 7) + ".dylib";
          String jnilib = mappedlib.toString();
          outfile = new File(libsDir, jnilib);
          if (!outfile.exists()) {
            if (cl.getResource(libSource + mappedlib) == null) {
              throw new IOException("Library " + mappedlib + " not on classpath nor in default location");
            }
          } else {
            return outfile;
          }
        } else {
          throw new IOException("Library " + mappedlib + " not on classpath nor in default location");
        }
      }
    } else {
      return outfile;
    }
    File ret = extractJniResource(libSource + mappedlib, outfile);
    return ret;
  }

  /**
   * extract a resource to a writable file
   *
   * @param resourcename the name of the resource on the classpath
   * @param outputfile the file to copy to
   * @return the extracted file
   * @throws IOException
   */
  private static File extractJniResource(String resourcename, File outputfile) throws IOException {
    InputStream in = cl.getResourceAsStream(resourcename);
    if (in == null) {
      throw new IOException("Resource " + resourcename + " not on classpath");
    }
    Debug.log(2, "Extracting '" + resourcename + "' to '" + outputfile.getAbsolutePath() + "'");
    OutputStream out = new FileOutputStream(outputfile);
    FileManager.copy(in, out);
    out.close();
    in.close();
    return outputfile;
  }

  /**
   * extract a resource to an absolute path
   *
   * @param resourcename the name of the resource on the classpath
   * @param outputname the path of the file to copy to
   * @return the extracted file
   * @throws IOException
   */
  private static File extractJniResource(String resourcename, String outputname) throws IOException {
    return NativeLoader.extractJniResource(resourcename, new File(outputname));
  }

  private static void extractLibs() {
    Debug.log(2, "FileManager: trying to acces jar");
    CodeSource src = FileManager.class.getProtectionDomain().getCodeSource();
    int iDir = 0;
    int iFile = 0;
    if (src != null) {
      URL jar = src.getLocation();
      if (!jar.toString().endsWith(".jar")) {
        Debug.log(2, "FileManager: not running from jar");
      } else {
        try {
          ZipInputStream zip = new ZipInputStream(jar.openStream());
          ZipEntry ze;
          Debug.log(2, "FileManager: accessing jar: " + jar.toString());
          while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();
            if (entryName.startsWith("META-INF/libs")) {
              libsList.add(entryName);
              if (entryName.endsWith(File.separator)) {
                iDir++;
              } else {
                iFile++;
              }
            }
          }
          Debug.log(2, "FileManager: found in META-INF/libs: Dirs: " + iDir + " Files: " + iFile);
        } catch (IOException e) {
          Debug.error("FileManager: List jar did not work");
        }
      }
    } else {
      Debug.error("FileManager: cannot access jar");
    }
  }

  /**
   * Assume the list of resources can be found at path/filelist.txt
   *
   * @return the local path to the extracted resources
   */
  private static String extract(String path) throws IOException {
    InputStream in = NativeLoader.cl.getResourceAsStream(path + "/filelist.txt");
    String localPath = Settings.BaseTempPath + File.separator + "sikuli" + File.separator + path;
    new File(localPath).mkdirs();
    Debug.log(4, "extract resources " + path + " to " + localPath);
    writeFileList(in, path, localPath);
    return localPath + "/";
  }

  static void writeFile(String from, String to) throws IOException {
    Debug.log(7, "FileManager: JarResource: copy " + from + " to " + to);
    File toF = new File(to);
    toF.getParentFile().mkdirs();
    InputStream in = NativeLoader.cl.getResourceAsStream(from);
    if (in != null) {
      OutputStream out = null;
      try {
        out = new FileOutputStream(toF);
        FileManager.copy(in, out);
      } catch (IOException e) {
        Debug.log(7, "FileManager: JarResource: Can't extract " + from + ": " + e.getMessage());
      } finally {
        if (out != null) {
          out.close();
        }
      }
    } else {
      Debug.log(7, "FileManager: JarResource: not found: " + from);
    }
  }

  private static void writeFileList(InputStream ins, String fromPath, String outPath) throws IOException {
    BufferedReader r = new BufferedReader(new InputStreamReader(ins));
    String line;
    while ((line = r.readLine()) != null) {
      Debug.log(7, "write " + line);
      if (line.startsWith("./")) {
        line = line.substring(1);
      }
      String fullpath = outPath + line;
      File outf = new File(fullpath);
      outf.getParentFile().mkdirs();
      InputStream in = NativeLoader.cl.getResourceAsStream(fromPath + line);
      if (in != null) {
        OutputStream out = null;
        try {
          out = new FileOutputStream(outf);
          FileManager.copy(in, out);
        } catch (IOException e) {
          Debug.log("Can't extract " + fromPath + line + ": " + e.getMessage());
        } finally {
          if (out != null) {
            out.close();
          }
        }
      } else {
        Debug.log("Resource not found: " + fromPath + line);
      }
    }
  }
}
