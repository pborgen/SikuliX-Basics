package org.sikuli.setup;

import org.sikuli.script.FileManager;
import org.sikuli.script.INativeLoader;

public class RunNativeLoader {

  public static void main(String[] args) {
    
    INativeLoader loader = FileManager.getNativeLoader("basic", args);
    loader.install(args);

  }
}
