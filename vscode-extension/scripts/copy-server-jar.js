const fs = require("fs");
const path = require("path");

const SERVER_JAR_PATTERN = /^openfasttrace-language-server-.*-standalone\.jar$/;
const targetDir = path.join(__dirname, "..", "..", "target");
const destDir = path.join(__dirname, "..", "server");
const destFile = path.join(destDir, "openfasttrace-language-server.jar");

function findNewestServerJar(dir) {
  if (!fs.existsSync(dir)) {
    return undefined;
  }
  const matches = fs.readdirSync(dir).filter((name) => SERVER_JAR_PATTERN.test(name));
  if (matches.length === 0) {
    return undefined;
  }
  return matches
    .map((name) => {
      const fullPath = path.join(dir, name);
      return { fullPath, mtime: fs.statSync(fullPath).mtimeMs };
    })
    .sort((a, b) => b.mtime - a.mtime)[0].fullPath;
}

const sourceJar = findNewestServerJar(targetDir);
if (sourceJar === undefined) {
  if (fs.existsSync(destFile)) {
    console.warn(`No build in ../target. Keeping the existing bundled JAR: ${destFile}`);
  } else {
    console.warn(
      "No server JAR available yet. Run 'mvn package' in the project root, then compile again."
    );
  }
  process.exit(0);
}

fs.mkdirSync(destDir, { recursive: true });
fs.copyFileSync(sourceJar, destFile);
console.log(`Bundled server JAR: ${sourceJar} -> ${destFile}`);
