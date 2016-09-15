package com.roeper.bu.consolefilestream;

import com.beust.jcommander.Parameter;

public class ConsoleStreamArgs {

  public enum FileType {
    CSV
  }

  @Parameter(names = {"--type", "-t"}, required = true, description = "Type of file.")
  private FileType fileType;

  @Parameter(names = {"--file", "-f"}, required = true, description = "Input file name")
  private String fileName;

  @Parameter(names = {"--has-labels"},
      description = "For CSV files, if the file has labels as the first row")
  private boolean hasLabels;
  
  @Parameter(names = "--help", help = true, description = "Displays usage info.")
  private boolean help = false;
  
  public FileType getFileType() {
    return fileType;
  }

  public String getFileName() {
    return fileName;
  }

  public boolean getHasLabels() {
    return hasLabels;
  }

  public boolean isHelp() {
    return help;
  }
}
