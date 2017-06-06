#!/usr/bin/python
"""
Generates python files. Should be called from the root of the repo:
  python scripts/gen_py_protos.py
"""

import os
import sys

proto_paths = [
  "//src/main/proto:singlepixel_python",
  "@multikinect//src/main/proto:frame_python",
  "@multikinect//src/main/proto:realtime_python",
  "@multikinect//src/main/proto:session_python",
]

proto_files = [
  "src/main/proto/singlepixel_pb2.py",
  "external/multikinect/src/main/proto/frame_pb2.py",
  "external/multikinect/src/main/proto/realtime_pb2.py",
  "external/multikinect/src/main/proto/session_pb2.py",
]

bazel_command = "bazel build"
for proto_path in proto_paths:
  bazel_command += " " + proto_path

print("Executing: ", bazel_command)
status = os.system(bazel_command)

if status != 0:
  print("An error occurred building the protos...")
  sys.exit(1)

if os.path.exists("srcgen"):
  user_input = input("Are you sure you want to get rid of old generated protobuf files? (Y/n)")
  accept_answers = ["", "Y", "y"]
  if user_input not in accept_answers:
    print("Generating of protobuf files aborted")
    sys.exit(1)
  status = os.system("rm -rf srcgen")
  if status != 0:
    print("An error occurred deleting old generated files...")
    sys.exit(1)

cp_command = "mkdir -p srcgen"
for proto_file in proto_files:
  cp_command += " && cp bazel-genfiles/" + proto_file + " srcgen/"

print("Executing: ", cp_command)
status = os.system(cp_command)

if status != 0:
  print("An error occurred copying the generated files...")
  sys.exit(1)

update_import_command = 'sed -i srcgen/realtime_pb2.py -e "s/src\.main\.proto/srcgen/"'
print("Executing: ", update_import_command)
status = os.system(update_import_command)
if status != 0:
  print("An error occured updating imports...")
  sys.exit(1)