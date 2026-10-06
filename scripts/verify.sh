#!/bin/sh
set -eu

repo_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
classes_dir="$repo_dir/target/classes"
rm -rf "$classes_dir"
mkdir -p "$classes_dir"
find "$repo_dir/src/main/java" "$repo_dir/src/test/java" -name '*.java' -print0 \
  | xargs -0 javac -d "$classes_dir"
java -cp "$classes_dir" dev.learning.notify.DeveloperNotificationTest
