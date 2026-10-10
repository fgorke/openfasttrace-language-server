// Puts the native language server binary for a target platform into server/, so the extension
// starts the server without any Java installation.
//
// Usage: node scripts/copy-server-binary.js [target]   (target defaults to this machine)
//
// The binary is taken from the first of:
//   1. server/openfasttrace-language-server-<version>-<target>[.exe]: a CI build artifact or a
//      release download, which is how packages for other platforms are built.
//   2. ../target/openfasttrace-language-server[.exe]: the output of `mvn -Pnative package`,
//      accepted only for this machine's target since Native Image builds for the build host.
//   3. server/openfasttrace-language-server[.exe] left by an earlier run for the same target.
//
// [impl->adr~ship-the-native-binary-in-the-vs-code-extension~1]
const fs = require("fs");
const path = require("path");

const serverDir = path.join(__dirname, "..", "server");
const mavenTargetDir = path.join(__dirname, "..", "..", "target");
const markerFile = path.join(serverDir, "oft-server.json");

function currentTarget() {
  const platform = process.platform === "win32" ? "win32" : process.platform;
  return `${platform}-${process.arch}`;
}

function binaryName(target) {
  return target.startsWith("win32") ? "openfasttrace-language-server.exe" : "openfasttrace-language-server";
}

function artifactInServerDir(target) {
  if (!fs.existsSync(serverDir)) {
    return undefined;
  }
  const suffix = target.startsWith("win32") ? `-${target}.exe` : `-${target}`;
  const pattern = new RegExp(`^openfasttrace-language-server-.+${suffix.replace(/[.]/g, "\.")}$`);
  const matches = fs.readdirSync(serverDir).filter((name) => pattern.test(name));
  if (matches.length === 0) {
    return undefined;
  }
  return matches
    .map((name) => ({ fullPath: path.join(serverDir, name), mtime: fs.statSync(path.join(serverDir, name)).mtimeMs }))
    .sort((a, b) => b.mtime - a.mtime)[0].fullPath;
}

function binaryInMavenTarget(target) {
  if (target !== currentTarget()) {
    return undefined;
  }
  const candidate = path.join(mavenTargetDir, binaryName(target));
  return fs.existsSync(candidate) ? candidate : undefined;
}

function binaryAlreadyInPlace(target) {
  const candidate = path.join(serverDir, binaryName(target));
  if (!fs.existsSync(candidate) || !fs.existsSync(markerFile)) {
    return undefined;
  }
  try {
    const marker = JSON.parse(fs.readFileSync(markerFile, "utf8"));
    return marker.target === target ? candidate : undefined;
  } catch {
    return undefined;
  }
}

const target = process.argv[2] ?? currentTarget();
const source = artifactInServerDir(target) ?? binaryInMavenTarget(target) ?? binaryAlreadyInPlace(target);
if (source === undefined) {
  console.error(
    `No native server binary for ${target} found.\n` +
      `Either run 'mvn -Pnative -DskipTests package' in the project root (builds for ${currentTarget()}),\n` +
      `or put openfasttrace-language-server-<version>-${target}${target.startsWith("win32") ? ".exe" : ""} into ${serverDir}.`
  );
  process.exit(1);
}

fs.mkdirSync(serverDir, { recursive: true });
const destination = path.join(serverDir, binaryName(target));
if (path.resolve(source) === path.resolve(destination)) {
  // already in place
} else if (path.dirname(path.resolve(source)) === path.resolve(serverDir)) {
  // An artifact dropped into server/ is renamed, so the package does not carry the binary twice.
  fs.renameSync(source, destination);
} else {
  fs.copyFileSync(source, destination);
}
if (!target.startsWith("win32")) {
  fs.chmodSync(destination, 0o755);
}
fs.writeFileSync(markerFile, JSON.stringify({ target }, null, 2));
console.log(`Bundled native server for ${target}: ${source} -> ${destination}`);
