package edu.bu.vip.singlepixel.multikinect;

import com.beust.jcommander.JCommander;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import edu.bu.vip.multikinect.Protos.Frame;
import edu.bu.vip.multikinect.controller.Controller;
import edu.bu.vip.multikinect.controller.camera.FrameReceivedEvent;
import edu.bu.vip.multikinect.controller.webconsole.WebConsole;
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
      Controller controller = new Controller(mainArgs.getDataDirectory());
      WebConsole webConsole = new WebConsole(controller, mainArgs.getDataDirectory());

      SinglePixelSensorPlugin sensorPlugin = new SinglePixelSensorPlugin(
          mainArgs.getMqttTopicPrefix(), mainArgs.getMqttHost());
      controller.registerPlugin("singlepixel", sensorPlugin);

      // Must start the controller before the web console
      controller.start();
      webConsole.start();

      System.out.println("Press enter to stop");
      Scanner scanner = new Scanner(System.in);
      scanner.nextLine();
      scanner.close();

      webConsole.stop();
      controller.stop();
    }
  }
}
