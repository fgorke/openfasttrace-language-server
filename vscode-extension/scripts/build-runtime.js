// Builds a minimal platform-specific Java runtime with jlink and places it in runtime/, so the extension
// can run the language server without a JDK installed on the machine.

// [impl->adr~bundle-java-runtimes-instead-of-a-native-image~1]
const { execFileSync, spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const destDir = path.join(__dirname, "..", "runtime");
const markerFile = path.join(destDir, "oft-runtime.json");

const REQUIRED_MODULES = ["java.base", "java.management", "java.naming", "java.sql"];

function javaHome() {
  if (process.env.JAVA_HOME) {
    return process.env.JAVA_HOME;
  }
  const fromPath = javaHomeFromPath();
  if (fromPath !== undefined) {
    return fromPath;
  }
  throw new Error(
    "Could not locate a JDK. Set JAVA_HOME, or put a JDK (not just a JRE) on the PATH."
  );
}

function javaHomeFromPath() {
  const result = spawnSync("java", ["-XshowSettings:properties", "-version"], {
    encoding: "utf8",
  });
  if (result.error !== undefined) {
    return undefined;
  }
  const output = `${result.stdout ?? ""}${result.stderr ?? ""}`;
  return /java\.home = (.+)/.exec(output)?.[1].trim();
}

function toolPath(name) {
  const exe = process.platform === "win32" ? `${name}.exe` : name;
  const candidate = path.join(javaHome(), "bin", exe);
  if (!fs.existsSync(candidate)) {
    throw new Error(`Could not find ${exe} in ${path.join(javaHome(), "bin")}`);
  }
  return candidate;
}

function runtimeTarget() {
  const platform = process.platform === "win32" ? "win32" : process.platform;
  const arch = process.arch;
  return `${platform}-${arch}`;
}

function runJlink(compression) {
  fs.rmSync(destDir, { recursive: true, force: true });
  execFileSync(toolPath("jlink"), [
    "--add-modules", REQUIRED_MODULES.join(","),
    "--strip-debug",
    "--no-header-files",
    "--no-man-pages",
    "--compress", compression,
    "--output", destDir,
  ], { stdio: "inherit" });
}

function build() {
  try {
    runJlink("zip-6");
  } catch {
    runJlink("2");
  }

  const launcher = path.join(destDir, "bin", process.platform === "win32" ? "java.exe" : "java");
  if (!fs.existsSync(launcher)) {
    throw new Error(`jlink produced no launcher at ${launcher}`);
  }

  fs.writeFileSync(markerFile, JSON.stringify({ target: runtimeTarget() }, null, 2));

  console.log(`Bundled Java runtime for ${runtimeTarget()}: ${destDir}`);
}

build();
