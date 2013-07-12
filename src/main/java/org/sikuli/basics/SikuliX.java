/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * added RaiMan 2013
 */
package org.sikuli.basics;

/**
 *
 * Only used as anchor for the preferences store
 */
public class SikuliX {
  
  public static void terminate(int n) {
    Debug.error("Terminating SikuliX after a fatal error" +
            (n == 0 ? "" : "(%d)" ) +
            "! Sorry, but it makes no sense to continue!\n" +
            "If you do not have any idea about the error cause or solution, run again\n" +
            "with a Debug level of 3. You might paste the output to the Q&A board.", n);
    cleanup();
    System.exit(1);
  }

  private static void cleanup() {
    
  }
}
