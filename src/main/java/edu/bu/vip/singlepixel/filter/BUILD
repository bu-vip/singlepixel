java_library(
    name = "kalmanfilter",
    srcs = [
        "KalmanFilter.java",
    ],
    deps = [
        "//:org_ejml_all",
        "//:org_slf4j_slf4j_api",
    ],
)

java_library(
    name = "occupantkalmanfilter",
    srcs = [
        "OccupantKalmanFilter.java",
    ],
    visibility = ["//src:__subpackages__"],
    deps = [
        ":kalmanfilter",
        "//:org_ejml_all",
        "//:org_slf4j_slf4j_api",
        "@multikinect//src/main/proto:frame",
    ],
)
