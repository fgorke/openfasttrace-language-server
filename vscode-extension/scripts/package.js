// Builds a platform-specific VSIX that carries the language server as a native binary.
//
// Usage: node scripts/package.js [target]   (target defaults to this machine)
//
// The binary comes from scripts/copy-server-binary.js. The server JAR is bundled as well, as the
// fallback for a configured `oft.java.path`.

const { spawnSync } = require("child_process");
const fs = require("fs");
const path = require("path");

const SUPPORTED_TARGETS = [
  "win32-x64",
  "linux-x64",
  "linux-arm64",
  "darwin-x64",
  "darwin-arm64",
];

function currentTarget() {
  const platform = process.platform === "win32" ? "win32" : process.platform;
  return `${platform}-${process.arch}`;
}

function run(args) {
  const result = spawnSync(process.execPath, args, {
    stdio: "inherit",
    cwd: path.join(__dirname, ".."),
  });
  if (result.error !== undefined) {
    console.error(`Could not run node ${args[0]}: ${result.error.message}`);
    process.exit(1);
  }
  if (result.status !== 0) {
    process.exit(result.status ?? 1);
  }
}

function vsceEntryPoint() {
  const manifestPath = require.resolve("@vscode/vsce/package.json");
  const manifest = require(manifestPath);
  const relative = typeof manifest.bin === "string" ? manifest.bin : manifest.bin.vsce;
  return path.join(path.dirname(manifestPath), relative);
}

const target = process.argv[2] ?? currentTarget();
if (!SUPPORTED_TARGETS.includes(target)) {
  console.error(
    `Unsupported target '${target}'. Supported: ${SUPPORTED_TARGETS.join(", ")}.\n` +
      "Run 'npx vsce package' to build a package without a native binary that needs Java on the PATH."
  );
  process.exit(1);
}

// Left over from builds before the native binary replaced the bundled Java runtime.
fs.rmSync(path.join(__dirname, "..", "runtime"), { recursive: true, force: true });

console.log(`Building VS Code extension for ${target}`);
run([path.join(__dirname, "copy-server-binary.js"), target]);
run([vsceEntryPoint(), "package", "--target", target]);
