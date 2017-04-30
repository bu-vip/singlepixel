java_binary(
    name = "demo",
    srcs = [
        "Algorithm2.java",
        "Main.java",
        "TensorFlowInterface.java",
    ],
    main_class = "edu.bu.vip.singlepixel.demo.Main",
    resources = [
        "//src/main/resources:log4jprops",
    ],
    runtime_deps = [
        "@org_pubref_rules_protobuf//java:netty_runtime_deps",
    ],
    deps = [
        "//:com_google_guava_guava",
        "//:com_google_inject_guice",
        "//:com_google_protobuf_protobuf_java",
        "//:com_google_protobuf_protobuf_java_util",
        "//:io_ratpack_ratpack_guice",
        "//:org_eclipse_paho_client_mqttv3",
        "//:org_slf4j_slf4j_api",
        "//:org_slf4j_slf4j_log4j12",
        "//:smartthings_ratpack_protobuf",
        "//src/main/java/edu/bu/vip/singlepixel/util:readingutils",
        "//src/main/proto:singlepixel_java",
        "@multikinect//src/main/proto:frame",
        "@org_pubref_rules_protobuf//java:grpc_compiletime_deps",
        "@tensorflow_java_api//jar",
        "@tensorflow_java_api_jni//:libtensorflow_jni",
    ],
)
