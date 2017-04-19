load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")
load("@org_pubref_rules_protobuf//python:rules.bzl", "py_proto_compile")

package(
    default_visibility = ["//visibility:public"],
)

java_proto_library(
    name = "singlepixel_java",
    imports = [
        "external/com_github_google_protobuf/src/",
    ],
    inputs = [
        "@com_github_google_protobuf//:well_known_protos",
    ],
    protos = [
        "singlepixel.proto",
    ],
)

py_proto_compile(
    name = "singlepixel_python",
    imports = [
        "external/com_github_google_protobuf/src/",
    ],
    inputs = [
        "@com_github_google_protobuf//:well_known_protos",
    ],
    protos = [
        "singlepixel.proto",
    ],
)
