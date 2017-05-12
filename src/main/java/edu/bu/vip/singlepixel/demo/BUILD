java_binary(
    name = "main",
    srcs = [
        "Main.java",
        "MainArgs.java",
    ],
    main_class = "edu.bu.vip.singlepixel.demo.Main",
    resources = [
        "//src/main/resources:log4jprops",
    ],
    runtime_deps = [
        "@org_pubref_rules_protobuf//java:netty_runtime_deps",
    ],
    deps = [
        ":algorithm",
        ":demo",
        ":webconsole",
        "//:com_beust_jcommander",
        "//:com_google_guava_guava",
        "//:com_google_inject_guice",
        "//:org_slf4j_slf4j_api",
        "//:org_slf4j_slf4j_log4j12",
        "@org_pubref_rules_protobuf//java:grpc_compiletime_deps",
    ],
)

java_library(
    name = "demo",
    srcs = [
        "Demo.java",
    ],
    deps = [
        ":algorithm",
        ":locationpredictor",
        "//:com_google_guava_guava",
        "//:com_google_protobuf_protobuf_java",
        "//:com_google_protobuf_protobuf_java_util",
        "//:org_eclipse_paho_client_mqttv3",
        "//:org_slf4j_slf4j_api",
        "//src/main/java/edu/bu/vip/singlepixel/filter:occupantkalmanfilter",
        "//src/main/java/edu/bu/vip/singlepixel/tensorflow:predictor",
        "//src/main/proto:demo_java",
        "//src/main/proto:singlepixel_java",
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller",
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller/calibration:calibrationdatadb",
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller/calibration:manager",
        "@multikinect//src/main/java/edu/bu/vip/multikinect/controller/realtime:syncedframebus",
        "@multikinect//src/main/java/edu/bu/vip/multikinect/sync:protoutils",
        "@multikinect//src/main/proto:calibration",
        "@multikinect//src/main/proto:frame",
        "@multikinect//src/main/proto:realtime",
    ],
)

java_library(
    name = "webconsole",
    srcs = [
        "WebConsole.java",
    ],
    deps = [
        ":demo",
        "//:com_google_guava_guava",
        "//:io_ratpack_ratpack_guice",
        "//:smartthings_ratpack_protobuf",
        "//src/main/proto:demo_java",
    ],
)

java_library(
    name = "algorithm",
    srcs = [
        "LocalizationAlgorithm.java",
    ],
    deps = [
        ":locationpredictor",
        "//:com_google_guava_guava",
        "//:org_slf4j_slf4j_api",
        "//src/main/java/edu/bu/vip/singlepixel/util:readingutils",
        "//src/main/proto:singlepixel_java",
        "@multikinect//src/main/proto:frame",
    ],
)

java_library(
    name = "locationpredictor",
    srcs = [
        "LocationPredictor.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//:com_google_guava_guava",
        "//:org_slf4j_slf4j_api",
        "@multikinect//src/main/proto:frame",
    ],
)
