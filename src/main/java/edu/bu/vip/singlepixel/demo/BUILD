java_binary(
    name = "demo",
    srcs = [
        "LocalizationAlgorithm.java",
        "Main.java",
        "TensorflowLocalizationAlgorithm.java",
    ],
    main_class = "edu.bu.vip.singlepixel.demo.Main",
    deps = [
        "@org_pubref_rules_protobuf//java:grpc_compiletime_deps",
        "@tensorflow_java_api//jar",
        "@tensorflow_java_api_jni//:libtensorflow_jni",
        "//:com_google_guava_guava",
        "//:com_google_inject_guice",
        "//:com_google_protobuf_protobuf_java",
        "//:io_ratpack_ratpack_guice",
        "//:org_eclipse_paho_client_mqttv3",
        "//:org_slf4j_slf4j_api",
        "//:smartthings_ratpack_protobuf",
        "//src/main/proto:singlepixel_java",
    ],
)
