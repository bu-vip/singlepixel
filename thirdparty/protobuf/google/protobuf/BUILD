load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_library")

package(
    default_visibility = ["//visibility:public"],
)

# TODO(doug) - remove when imports from real package are fixed
java_proto_library(
    name = "well_known_types",
    protos = glob(["*.proto"]),
)
