/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.basics;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import javax.swing.JOptionPane;
import org.apache.commons.cli.CommandLine;

/**
 * Contains the main class
 */
public class SikuliScript {

  private static final String me = "SikuliScript: ";
  /**
   * The ScriptRunner that is used to execute the script.
   */
  private static IScriptRunner runner;
  private static File imagePath;
  private static Boolean runAsTest;

  /**
   * Main method
   *
   * @param args passed arguments
   */
  public static void main(String[] args) {

    CommandArgs cmdArgs = new CommandArgs("SCRIPT");
    CommandLine cmdLine = cmdArgs.getCommandLine(args);

    if (cmdLine == null || cmdLine.getOptions().length == 0) {
      Debug.error("Did not find any valid option on command line!");
      System.exit(1);
    }

    // check if any commandline args were loaded and print std help and runner specific help
    if (cmdLine == null) {
      Debug.error("Nothing to do! No valid arguments on commandline!");
      cmdArgs.printHelp();
      System.exit(1);
    }

    // print help
    if (cmdLine.hasOption(CommandArgsEnum.HELP.shortname())) {
      cmdArgs.printHelp();
      if (runner != null) {
        System.out.println(runner.getCommandLineHelp());
      }
      System.exit(1);
    }

    if (cmdLine.hasOption(CommandArgsEnum.DEBUG.shortname())) {
      Debug.setDebugLevel(cmdLine.getOptionValue(CommandArgsEnum.DEBUG.longname()));      
    }
    
    if (cmdLine.hasOption(CommandArgsEnum.LOGFILE.shortname())) {
      String val = cmdLine.getOptionValue(CommandArgsEnum.LOGFILE.longname());
      Debug.setLogFile(val == null ? "" : val);
    }
    
    if (cmdLine.hasOption(CommandArgsEnum.USERLOGFILE.shortname())) {
      String val = cmdLine.getOptionValue(CommandArgsEnum.USERLOGFILE.longname());      
      Debug.setUserLogFile(val == null ? "" : val);
    }
    
    Settings.showJavaInfo();

//TODO    if (cmdLine.hasOption(CommandArgsEnum.IMAGEPATH.shortname())) {
    if (false) {
//      imagePath = getScriptRunner(cmdLine.getOptionValue(CommandArgsEnum.IMAGEPATH.longname()), null, args);
    } else {
      imagePath = null;
    }

    // select script runner and/or start interactive session
    // option is overloaded - might specify runner for -r/-t
    if (cmdLine.hasOption(CommandArgsEnum.INTERACTIVE.shortname())) {
      int exitCode = 0;
      if (runner == null) {
        String givenRunnerName = cmdLine.getOptionValue(CommandArgsEnum.INTERACTIVE.longname());
        if (givenRunnerName == null) {
          runner = SikuliX.getScriptRunner("jython", null, args);
        } else {
          runner = SikuliX.getScriptRunner(givenRunnerName, null, args);
          if (runner == null) {
            System.exit(1);
          }
        }
      }
      if (!cmdLine.hasOption(CommandArgsEnum.RUN.shortname()) &&
              !cmdLine.hasOption(CommandArgsEnum.TEST.shortname())) {
        exitCode = runner.runInteractive(cmdArgs.getUserArgs());
        runner.close();
        SikuliX.endNormal(exitCode);
      }
    }

    // start script execution using scriptrunner (-i) or decide from contained scriptfile
    String givenScriptName = null;
    runAsTest = false;
    if (cmdLine.hasOption(CommandArgsEnum.RUN.shortname())) {
      givenScriptName = cmdLine.getOptionValue(CommandArgsEnum.RUN.longname());
    } else if (cmdLine.hasOption(CommandArgsEnum.TEST.shortname())) {
      givenScriptName = cmdLine.getOptionValue(CommandArgsEnum.TEST.longname());
      runAsTest = true;
    }
    if (givenScriptName != null) {
      File script = FileManager.getScriptFile(new File(givenScriptName), runner, args);
      if (script == null) {
        System.exit(1);
      }
      runner = SikuliX.getRunner();
      if (imagePath == null) {
        imagePath = FileManager.resolveImagePath(script);
      }
      ImageLocator.setBundlePath(imagePath.getAbsolutePath());
      int exitCode = runAsTest
              ? runner.runTest(script, imagePath, cmdArgs.getUserArgs(), null)
              : runner.runScript(script, imagePath, cmdArgs.getUserArgs(), null);
      runner.close();
      SikuliX.endNormal(exitCode);
    } else {
      Debug.error("Nothing to do according to the given commandline options!");
      cmdArgs.printHelp();
      if (runner != null) {
        System.out.println(runner.getCommandLineHelp());
      }
      System.exit(1);
    }
  }

  public static void popup(String message, String title) {
    JOptionPane.showMessageDialog(null, message,
            title, JOptionPane.PLAIN_MESSAGE);
  }

  public static void popup(String message) {
    popup(message, "Sikuli");
  }

  public static String input(String msg) {
    return JOptionPane.showInputDialog(msg);
  }

  public static String input(String msg, String preset) {
    return JOptionPane.showInputDialog(msg, preset);
  }

  public static String run(String cmdline) {
    //TODO: improve run command
    String lines = "";
    try {
      String line;
      Process p = Runtime.getRuntime().exec(cmdline);
      BufferedReader input =
              new BufferedReader(new InputStreamReader(p.getInputStream()));
      while ((line = input.readLine()) != null) {
        lines = lines + '\n' + line;
      }
    } catch (Exception err) {
      Debug.error(me +"run: Problems running command\n%s\nerror: %s", cmdline, err.getMessage());
    }
    return lines;
  }

  /**
   * Prints the interactive help from the ScriptRunner.
   */
  public static void shelp() {
    System.out.println(runner.getInteractiveHelp());
  }
  
  public static void cleanUp() {
    SikuliX.cleanUp(0);
  }
}
