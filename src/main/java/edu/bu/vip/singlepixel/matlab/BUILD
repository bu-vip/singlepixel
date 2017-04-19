java_binary(
    name = "matlabmqtt",
    srcs = [
        "Main.java",
        "MatlabMqtt.java",
    ],
    main_class = "edu.bu.vip.singlepixel.matlab.Main",
    visibility = ["//visibility:public"],
    deps = [
        "//:com_google_guava_guava",
        "//:com_google_protobuf_protobuf_java",
        "//:org_eclipse_paho_client_mqttv3",
        "//src/main/proto:singlepixel_java",
    ],
)

"""
java_toolchain(
    name = "toolchain",
    bootclasspath = ["@bazel_tools//tools/jdk:bootclasspath"],
    encoding = "UTF-8",
    extclasspath = ["@bazel_tools//tools/jdk:extclasspath"],
    forcibly_disable_header_compilation = 1,
    genclass = ["@bazel_tools//tools/jdk:GenClass_deploy.jar"],
    header_compiler = ["@bazel_tools//tools/jdk:turbine_deploy.jar"],
    ijar = ["@bazel_tools//tools/jdk:ijar"],
    javabuilder = ["@bazel_tools//tools/jdk:JavaBuilder_deploy.jar"],
    javac = ["@bazel_tools//third_party/java/jdk/langtools:javac_jar"],
    javac_supports_workers = 0,
    jvm_opts = [
        "-XX:+TieredCompilation",
        "-XX:TieredStopAtLevel=1",
    ],
    misc = [
        "-XDskipDuplicateBridges=true",
    ],
    singlejar = ["@bazel_tools//tools/jdk:SingleJar_deploy.jar"],
    source_version = "7",
    target_version = "7",
    visibility = ["//visibility:public"],
)"""
