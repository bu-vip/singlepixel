package edu.bu.vip.singlepixel.demo;

import com.beust.jcommander.JCommander;
import java.util.Scanner;

public class Main {

  public static void main(String[] args) throws Exception {
    // Parse command line args
    MainArgs mainArgs = new MainArgs();
    JCommander commander = new JCommander();
    commander.addObject(mainArgs);
    commander.parse(args);

    // Check if help
    if (mainArgs.isHelp()) {
      commander.usage();
    } else {
      // Run
      Demo demo = new Demo(mainArgs.getModelFile(),
          mainArgs.getRecordedDataDirectory(),
          mainArgs.getMultiKinectDirectory(),
          mainArgs.getCalibrationId(),
          mainArgs.getMqttHost()
      );
      demo.start();

      WebConsole console = new WebConsole(demo);
      console.start();

      System.out.println("Press enter to stop");
      Scanner scanner = new Scanner(System.in);
      scanner.nextLine();
      scanner.close();

      console.stop();
      demo.stop();
    }

  }
}
