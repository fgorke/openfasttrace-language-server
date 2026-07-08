#!/usr/bin/env bash

set -o errexit
set -o nounset
set -o pipefail

base_dir="$( cd "$(dirname "$0")/../.." >/dev/null 2>&1 ; pwd -P )"
readonly base_dir
pom_file="$base_dir/pom.xml"
readonly pom_file

if [[ ! -f "$pom_file" ]]; then
    echo "Could not find pom.xml at $pom_file" >&2
    exit 1
fi

# Extract the first <version>...</version> occurrence from the root pom.xml.
project_version=$(grep -m1 -oP '(?<=<version>).*?(?=</version>)' "$pom_file" | tr -d '\r')
readonly project_version
echo "Read project version '$project_version' from $pom_file"

if [[ -z "$project_version" ]]; then
    echo "Could not extract version from $pom_file" >&2
    exit 1
fi

if [[ "$project_version" == *-SNAPSHOT ]]; then
    echo "Release version must not end with -SNAPSHOT: $project_version" >&2
    exit 1
fi

readonly changes_file="$base_dir/doc/changes/changes_${project_version}.md"
if [[ ! -f "$changes_file" ]]; then
    echo "Could not find release notes file $changes_file" >&2
    exit 1
fi

artifact_path=$(find "$base_dir/intellij-plugin/build/distributions" -maxdepth 1 -type f -name '*.zip' | sort | head -n 1)
readonly artifact_path
if [[ -z "$artifact_path" ]]; then
    echo "Could not find plugin distribution archive in $base_dir/intellij-plugin/build/distributions" >&2
    exit 1
fi

server_jar=$(find "$base_dir/target" -maxdepth 1 -type f -name '*-standalone.jar' | sort | head -n 1)
readonly server_jar
if [[ -z "$server_jar" ]]; then
    echo "Could not find standalone server JAR in $base_dir/target" >&2
    exit 1
fi

echo "Calculate sha256sum for plugin archive and server JAR"

# checksum for plugin
file_dir="$(dirname "$artifact_path")"
file_name="$(basename "$artifact_path")"
cd "$file_dir"
checksum_plugin_name="${file_name}.sha256"
sha256sum "$file_name" > "$checksum_plugin_name"
checksum_plugin_path="$file_dir/$checksum_plugin_name"
cd "$base_dir"

# checksum for server JAR
server_dir="$(dirname "$server_jar")"
server_name="$(basename "$server_jar")"
cd "$server_dir"
checksum_server_name="${server_name}.sha256"
sha256sum "$server_name" > "$checksum_server_name"
checksum_server_path="$server_dir/$checksum_server_name"
cd "$base_dir"

readonly title="Release $project_version"
readonly tag="$project_version"
echo "Creating release:"
echo "Git tag      : $tag"
echo "Title        : $title"
echo "Changes file : $changes_file"
echo "Plugin file  : $artifact_path"
echo "Plugin sha256: $checksum_plugin_path"
echo "Server JAR   : $server_jar"
echo "Server sha256: $checksum_server_path"

release_url=$(gh release create --latest --title "$title" --notes-file "$changes_file" --target main "$tag" \
    "$artifact_path" "$checksum_plugin_path" "$server_jar" "$checksum_server_path")
readonly release_url
echo "Release URL: $release_url"


