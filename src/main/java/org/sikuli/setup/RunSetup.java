package org.sikuli.setup;

import org.sikuli.script.FileManager;
import org.sikuli.script.IResourceLoader;

public class RunSetup {

  public static void main(String[] args) {
    
    IResourceLoader loader = FileManager.getNativeLoader("basic", args);
    loader.install(args);

  }
}
