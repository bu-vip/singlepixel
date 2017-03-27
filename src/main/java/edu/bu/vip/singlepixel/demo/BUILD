java_binary(
    name = "demo",
    srcs = [
        "LocalizationAlgorithm.java",
        "Main.java",
        "TensorflowLocalizationAlgorithm.java",
    ],
    main_class = "edu.bu.vip.singlepixel.demo.Main",
    deps = [
        "@tensorflow_java_api//jar",
        "@tensorflow_java_api_jni//:libtensorflow_jni",
        "//:com_google_guava_guava",
        "//src/main/proto:singlepixel",
    ],
)
