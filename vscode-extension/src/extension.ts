import * as fs from "fs";
import * as path from "path";
import * as vscode from "vscode";
import {
  LanguageClient,
  LanguageClientOptions,
  ServerOptions,
  TransportKind,
} from "vscode-languageclient/node";

const SUPPORTED_EXTENSIONS = [
  "md", "markdown", "rst",
  "ads", "adb",
  "bat",
  "c", "cc", "cpp", "c++", "h", "hh", "hpp", "h++",
  "dox",
  "c#", "cs",
  "cfg", "conf", "ini",
  "feature",
  "go",
  "groovy",
  "htm", "html", "xhtml", "xml", "fxml", "json", "yaml", "yml", "toml",
  "java", "clj", "kt", "kts", "scala",
  "js", "mjs", "cjs", "ejs", "ts",
  "lua",
  "m", "mm",
  "php",
  "pl", "pm",
  "proto",
  "pu", "puml", "plantuml",
  "py",
  "r",
  "robot",
  "rs",
  "sh", "bash", "zsh",
  "sv", "v", "inc",
  "swift",
  "tf", "tfvars",
  "sql", "pls",
];

const BUNDLED_JAR_NAME = "openfasttrace-language-server.jar";

let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const jarPath = resolveServerJar(context);
  if (jarPath === undefined) {
    vscode.window.showErrorMessage(
      "OpenFastTrace: no server JAR found. Run 'mvn package' in the project root " +
        "and recompile this extension."
    );
    return;
  }

  const javaPath = resolveJavaExecutable(context);

  const serverOptions: ServerOptions = {
    command: javaPath,
    args: ["-jar", jarPath],
    transport: TransportKind.stdio,
  };

  const clientOptions: LanguageClientOptions = {
    documentSelector: [
      { scheme: "file", pattern: `**/*.{${SUPPORTED_EXTENSIONS.join(",")}}` },
    ],
    outputChannelName: "OpenFastTrace Language Server",
  };

  client = new LanguageClient(
    "openfasttraceLsp",
    "OpenFastTrace Language Server",
    serverOptions,
    clientOptions
  );

  try {
    await client.start();
  } catch (error) {
    vscode.window.showErrorMessage(startupErrorMessage(javaPath, error));
  }
}

function startupErrorMessage(javaPath: string, error: unknown): string {
  const usesPathJava = javaPath === "java";
  const isMissingExecutable = String(error).includes("ENOENT");
  if (usesPathJava && isMissingExecutable) {
    return (
      "OpenFastTrace: no Java found. This build of the extension does not bundle a Java " +
      "runtime, so it needs Java 17 or later on the PATH, or a path configured in " +
      "'oft.java.path'. Installing the platform-specific build of this extension avoids that."
    );
  }
  return `OpenFastTrace: failed to start language server using '${javaPath}': ${error}`;
}

export function deactivate(): Thenable<void> | undefined {
  return client?.stop();
}

function resolveServerJar(context: vscode.ExtensionContext): string | undefined {
  const bundledJar = path.join(context.extensionPath, "server", BUNDLED_JAR_NAME);
  return fs.existsSync(bundledJar) ? bundledJar : undefined;
}

function resolveJavaExecutable(context: vscode.ExtensionContext): string {
  const setting = vscode.workspace.getConfiguration("oft").inspect<string>("java.path");
  const configured =
    setting?.workspaceFolderValue ?? setting?.workspaceValue ?? setting?.globalValue;
  if (configured !== undefined && configured.trim() !== "") {
    return configured;
  }

  const bundled = bundledJavaExecutable(context);
  if (bundled !== undefined) {
    return bundled;
  }

  return "java";
}

function bundledJavaExecutable(context: vscode.ExtensionContext): string | undefined {
  const runtimeDir = path.join(context.extensionPath, "runtime");
  const launcher = path.join(
    runtimeDir,
    "bin",
    process.platform === "win32" ? "java.exe" : "java"
  );
  if (!fs.existsSync(launcher)) {
    return undefined;
  }
  if (!matchesCurrentPlatform(runtimeDir)) {
    console.warn("Bundled Java runtime was built for a different platform, ignoring it.");
    return undefined;
  }
  ensureExecutable(launcher);
  return launcher;
}

function ensureExecutable(file: string): void {
  if (process.platform === "win32") {
    return;
  }
  try {
    fs.accessSync(file, fs.constants.X_OK);
  } catch {
    try {
      fs.chmodSync(file, 0o755);
    } catch (error) {
      console.warn(`Could not make the bundled Java runtime executable: ${error}`);
    }
  }
}

function matchesCurrentPlatform(runtimeDir: string): boolean {
  const markerFile = path.join(runtimeDir, "oft-runtime.json");
  if (!fs.existsSync(markerFile)) {
    return true;
  }
  try {
    const marker = JSON.parse(fs.readFileSync(markerFile, "utf8")) as { target?: string };
    const platform = process.platform === "win32" ? "win32" : process.platform;
    return marker.target === undefined || marker.target === `${platform}-${process.arch}`;
  } catch {
    return true;
  }
}
