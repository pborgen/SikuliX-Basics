/*
 * Copyright 2010-2013, Sikuli.org
 * Released under the MIT License.
 *
 * modified RaiMan 2013
 */
package org.sikuli.basics;

import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class CommandArgs {

  private static String _callerType = "";
  Options _options;
  ArrayList<String> userArgs = new ArrayList<String>();
  ArrayList<String> sikuliArgs = new ArrayList<String>();

  private static boolean isIDE(String callerType) {
    return ("IDE".equals(callerType));
  }
  
  public static boolean isIDE() {
    return ("IDE".equals(_callerType));
  }

  private static boolean isScript(String callerType) {
    return ("SCRIPT".equals(callerType));
  }

  public static boolean isScript() {
    return ("SCRIPT".equals(_callerType));
  }

  private static boolean isOther(String callerType) {
    return (!isIDE(callerType) && !isScript(callerType));
  }

  public CommandArgs(String type) {
    if (!isIDE(type) && !isScript(type)) {
      Debug.error("Commandline Parser not configured for " + type);
      _callerType = "OTHER";
    } else {
      _callerType = type;
    }
    init();
  }

  public CommandLine getCommandLine(String[] args) {
    CommandLineParser parser = new PosixParser();
    CommandLine cmd = null;
    
    boolean isUserArg = false;
    for (int i=0; i < args.length; i++) {
      Debug.log(3, "arg %d: %s", i + 1, args[i]);
      if (!isUserArg && args[i].startsWith("--")) {
        isUserArg = true;
        continue;
      }
      if (isUserArg) {
        userArgs.add(args[i]);
      } else {
        sikuliArgs.add(args[i]);
      }
    }
    try {
      cmd = parser.parse(_options, sikuliArgs.toArray(new String[]{}), true);
    } catch (ParseException exp) {
      Debug.error(exp.getMessage());
    }
    return cmd;
  }
  
  public String[] getUserArgs() {
    return userArgs.toArray(new String[]{});
  }

  /**
   * Adds all options to the Options object
   */
  @SuppressWarnings("static-access")
  private void init() {
    _options = new Options();
    _options.addOption(CommandArgsEnum.HELP.shortname(), 
            CommandArgsEnum.HELP.longname(), false, CommandArgsEnum.HELP.description());

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.DEBUG.longname())
            .hasArg()
            .withArgName(CommandArgsEnum.DEBUG.argname())
            .withDescription(CommandArgsEnum.DEBUG.description())
            .create(CommandArgsEnum.DEBUG.shortname().charAt(0)));

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.LOGFILE.longname())
            .hasOptionalArg()
            .withArgName(CommandArgsEnum.LOGFILE.argname())
            .withDescription(CommandArgsEnum.LOGFILE.description())
            .create(CommandArgsEnum.LOGFILE.shortname().charAt(0)));

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.USERLOGFILE.longname())
            .hasOptionalArg()
            .withArgName(CommandArgsEnum.USERLOGFILE.argname())
            .withDescription(CommandArgsEnum.USERLOGFILE.description())
            .create(CommandArgsEnum.USERLOGFILE.shortname().charAt(0)));

    _options.addOption(CommandArgsEnum.CONSOLE.shortname(), 
            CommandArgsEnum.CONSOLE.longname(), false, CommandArgsEnum.CONSOLE.description());

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.LOAD.longname())
            .withDescription(CommandArgsEnum.LOAD.description())
            .hasOptionalArgs()
            .withArgName(CommandArgsEnum.LOAD.argname())
            .create(CommandArgsEnum.LOAD.shortname().charAt(0)));

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.INTERACTIVE.longname())
            .hasOptionalArg()
            .withArgName(CommandArgsEnum.INTERACTIVE.argname())
            .withDescription(CommandArgsEnum.INTERACTIVE.description())
            .create(CommandArgsEnum.INTERACTIVE.shortname().charAt(0)));

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.TEST.longname())
            .hasArg()
            .withArgName(CommandArgsEnum.TEST.argname())
            .withDescription(CommandArgsEnum.TEST.description())
            .create(CommandArgsEnum.TEST.shortname().charAt(0)));

    _options.addOption(
            OptionBuilder.withLongOpt(CommandArgsEnum.RUN.longname())
            .hasArg()
            .withArgName(CommandArgsEnum.RUN.argname())
            .withDescription(CommandArgsEnum.RUN.description())
            .create(CommandArgsEnum.RUN.shortname().charAt(0)));
  }

  /**
   * Prints the help
   */
  public void printHelp() {
    HelpFormatter formatter = new HelpFormatter();
    if (isScript(_callerType)) {
      formatter.printHelp(80, "\n",
              "----- Running Sikuli script using sikuli-script.jar "
              + "---------------------------",
              _options,
              "-----\n<foobar.sikuli>\n"
              + "path relative to current working directory or absolute path\n"
              + "though deprecated: so called executables .skl can be used too\n"
              + "-------------------------------------------------------------",
              true);
    } else if (isIDE(_callerType)) {
      formatter.printHelp("Sikuli-IDE", _options, true);
    } else {
      formatter.printHelp("--?????--", _options, true);
    }
  }
}
