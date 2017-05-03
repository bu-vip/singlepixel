package edu.bu.vip.singlepixel.demo;

import ratpack.guice.Guice;
import ratpack.http.Status;
import ratpack.server.RatpackServer;
import smartthings.ratpack.protobuf.CacheConfig;
import smartthings.ratpack.protobuf.ProtobufModule;
import smartthings.ratpack.protobuf.ProtobufModule.Config;

public class WebConsole {

  private final Demo demo;
  private RatpackServer server;

  public WebConsole(Demo demo) {
    this.demo = demo;
  }

  public void start() throws Exception {

    server = RatpackServer.start(s -> {
      s.serverConfig(config -> {
        config.port(8080);
      });
      s.registry(Guice.registry(b -> {
        Config protoConfig = new Config();
        protoConfig.setCache(new CacheConfig());
        b.moduleConfig(ProtobufModule.class, protoConfig);
      }));
      s.handlers(chain -> {
        chain.all(handler -> {
          // TODO(doug) - This could be handled better
          handler.getResponse().getHeaders().set("Access-Control-Allow-Origin", "*");
          handler.getResponse().getHeaders()
              .set("Access-Control-Allow-Headers",
                  "x-requested-with, origin, content-type, accept");
          handler.next();
        });

        chain.get("_/state", context -> {
          Protos.Status.Builder builder = Protos.Status.newBuilder();
          builder.setCapturingBackground(demo.isCapturingBackground());
          builder.setRecording(demo.isRecording());
          builder.addAllOccupants(demo.getOccupants());
          builder.setBounds(demo.getBounds());

          context.render(builder.build());
        });

        chain.get("_/background", ctx -> {
          demo.captureBackground();
          ctx.getResponse().status(Status.OK).send();
        });

        chain.get("_/record", ctx -> {
          demo.toggleRecording();
          ctx.getResponse().status(Status.OK).send();
        });
      });
    });
  }

  public void stop() throws Exception {
    if (server != null) {
      server.stop();
    }
  }

}
