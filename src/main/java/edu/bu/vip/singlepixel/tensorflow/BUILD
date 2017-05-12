java_library(
    name = "predictor",
    srcs = [
        "TensorFlowLocationPredictor.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":interface",
        "//:com_google_guava_guava",
        "//:org_slf4j_slf4j_api",
        "//src/main/java/edu/bu/vip/singlepixel/demo:locationpredictor",
        "@multikinect//src/main/proto:frame",
    ],
)

java_library(
    name = "interface",
    srcs = [
        "TensorFlowInterface.java",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//:com_google_guava_guava",
        "//:org_slf4j_slf4j_api",
        "@tensorflow_java_api//jar",
        "@tensorflow_java_api_jni//:libtensorflow_jni",
    ],
)
