java_binary(
    name = "main",
    srcs = [
        "Main.java",
    ],
    main_class = "edu.bu.vip.singlepixel.multikinect.Main",
    resources = [
        "//src/main/resources:log4jprops",
    ],
    runtime_deps = [
        "@org_pubref_rules_protobuf//java:netty_runtime_deps",
    ],
    deps = [
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller",
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller/webconsole",
        ":singlepixelsensorplugin",
        "//:com_google_guava_guava",
        "//:org_slf4j_slf4j_api",
        "//:org_slf4j_slf4j_log4j12",
    ],
)

java_library(
    name = "singlepixelsensorplugin",
    srcs = [
        "SinglePixelSensorPlugin.java",
    ],
    deps = [
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller/plugin:plugin",
        "//:com_google_guava_guava",
        "//:com_google_protobuf_protobuf_java",
        "//:org_eclipse_paho_client_mqttv3",
        "//:org_slf4j_slf4j_api",
        "//src/main/proto:singlepixel",
    ],
)
