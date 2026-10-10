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
const BUNDLED_BINARY_NAME =
  process.platform === "win32" ? "openfasttrace-language-server.exe" : "openfasttrace-language-server";
const BUNDLED_SERVER_MARKER = "oft-server.json";

/** How the language server process is started. */
interface ServerLaunch {
  command: string;
  args: string[];
  /** Where the command came from, for the error message when it does not start. */
  source: "bundled-binary" | "configured-java" | "path-java";
}

let client: LanguageClient | undefined;

export async function activate(context: vscode.ExtensionContext): Promise<void> {
  const launch = resolveServerLaunch(context);
  if (launch === undefined) {
    vscode.window.showErrorMessage(
      "OpenFastTrace: this extension carries no language server. Run 'mvn -Pnative package' " +
        "(native binary) or 'mvn package' (JAR) in the project root and recompile the extension."
    );
    return;
  }

  const serverOptions: ServerOptions = {
    command: launch.command,
    args: launch.args,
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

  registerGenerateReportCommand(context);
  registerCoverageTagSuggestions(context);

  try {
    await client.start();
  } catch (error) {
    vscode.window.showErrorMessage(startupErrorMessage(launch, error));
  }
}

function startupErrorMessage(launch: ServerLaunch, error: unknown): string {
  const isMissingExecutable = String(error).includes("ENOENT");
  if (launch.source === "path-java" && isMissingExecutable) {
    return (
      "OpenFastTrace: no Java found. This build of the extension bundles no native language " +
      "server for this platform, so it needs Java 25 or later on the PATH, or a path configured " +
      "in 'oft.java.path'. The platform-specific builds of this extension need no Java."
    );
  }
  return `OpenFastTrace: failed to start language server using '${launch.command}': ${error}`;
}

export function deactivate(): Thenable<void> | undefined {
  return client?.stop();
}

const CODE_COMMENT_MARKERS = ["//", "#", "--", ";", "/*", "<!--"];

function registerCoverageTagSuggestions(context: vscode.ExtensionContext): void {
  const listener = vscode.workspace.onDidChangeTextDocument(event => {
    const lastChange = event.contentChanges[event.contentChanges.length - 1];
    if (lastChange === undefined || lastChange.text.length === 0) {
      return;
    }
    setTimeout(() => suggestInsideOpenTag(event.document), 0);
  });
  context.subscriptions.push(listener);
}

function suggestInsideOpenTag(document: vscode.TextDocument): void {
  const editor = vscode.window.activeTextEditor;
  if (editor === undefined || editor.document !== document) {
    return;
  }
  const cursor = editor.selection.active;
  const linePrefix = document.lineAt(cursor.line).text.slice(0, cursor.character);
  if (isInOpenCoverageTag(linePrefix)) {
    void vscode.commands.executeCommand("editor.action.triggerSuggest");
  }
}

function isInOpenCoverageTag(linePrefix: string): boolean {
  const bracketStart = linePrefix.lastIndexOf("[");
  if (bracketStart < 0) {
    return false;
  }
  if (!containsCodeCommentMarker(linePrefix.slice(0, bracketStart))) {
    return false;
  }
  const afterBracket = linePrefix.slice(bracketStart);
  return afterBracket.includes("->") && !afterBracket.includes("]");
}

function containsCodeCommentMarker(text: string): boolean {
  return CODE_COMMENT_MARKERS.some(marker => text.includes(marker));
}

const GENERATE_TRACE_REPORT_COMMAND = "oft.generateTraceReport";

const REPORT_PRESETS: { id: string; label: string; description: string }[] = [
  { id: "html", label: "HTML report", description: "the full trace as a web page" },
  { id: "plain-all", label: "Plain text, every item", description: "" },
  { id: "plain-failures", label: "Plain text, defects only", description: "" },
  {
    id: "plain-direct-failures",
    label: "Plain text, defects only",
    description: "without those inherited from covered items",
  },
  { id: "plain-summary", label: "Plain text, summary", description: "a single line" },
];

function registerGenerateReportCommand(context: vscode.ExtensionContext): void {
  const command = vscode.commands.registerCommand("oft.showTraceReport", async () => {
    if (client === undefined) {
      vscode.window.showErrorMessage("OpenFastTrace: the language server is not running.");
      return;
    }
    const picked = await vscode.window.showQuickPick(
      REPORT_PRESETS.map(preset => ({ label: preset.label, detail: preset.description, id: preset.id })),
      { title: "Generate OpenFastTrace report", placeHolder: "Choose a report" }
    );
    if (picked === undefined) {
      return;
    }
    await generateAndOpen(picked.id);
  });
  context.subscriptions.push(command);
}

async function generateAndOpen(preset: string): Promise<void> {
  try {
    const path = await vscode.window.withProgress(
      { location: vscode.ProgressLocation.Notification, title: "Generating OpenFastTrace report" },
      () =>
        client!.sendRequest<string | null>("workspace/executeCommand", {
          command: GENERATE_TRACE_REPORT_COMMAND,
          arguments: [preset],
        })
    );
    if (!path) {
      vscode.window.showErrorMessage("OpenFastTrace: the server returned no report.");
      return;
    }
    await openReport(vscode.Uri.file(path), preset);
  } catch (error) {
    vscode.window.showErrorMessage(`OpenFastTrace: could not generate the report: ${error}`);
  }
}

async function openReport(uri: vscode.Uri, preset: string): Promise<void> {
  if (preset === "html") {
    showRenderedReport(uri);
    return;
  }
  const document = await vscode.workspace.openTextDocument(uri);
  await vscode.window.showTextDocument(document);
}

function showRenderedReport(uri: vscode.Uri): void {
  const panel = vscode.window.createWebviewPanel(
    "oftTraceReport",
    "OpenFastTrace Report",
    vscode.ViewColumn.Active,
    { enableFindWidget: true, retainContextWhenHidden: true }
  );
  panel.webview.html = fs.readFileSync(uri.fsPath, "utf8");
}

/**
 * Picks how to start the server, in this order:
 * 1. the JAR with the Java configured in `oft.java.path`, when both exist,
 * 2. the native binary bundled with the platform-specific package,
 * 3. the JAR with `java` from the PATH, for a package built without a binary.
 */
// [impl->adr~ship-the-native-binary-in-the-vs-code-extension~1]
function resolveServerLaunch(context: vscode.ExtensionContext): ServerLaunch | undefined {
  const jarPath = resolveServerJar(context);
  const configuredJava = configuredJavaExecutable();
  if (configuredJava !== undefined && jarPath !== undefined) {
    return { command: configuredJava, args: ["-jar", jarPath], source: "configured-java" };
  }
  if (configuredJava !== undefined) {
    console.warn("'oft.java.path' is set but this extension carries no server JAR, using the bundled binary.");
  }

  const binary = bundledServerBinary(context);
  if (binary !== undefined) {
    return { command: binary, args: [], source: "bundled-binary" };
  }

  if (jarPath !== undefined) {
    return { command: "java", args: ["-jar", jarPath], source: "path-java" };
  }
  return undefined;
}

function resolveServerJar(context: vscode.ExtensionContext): string | undefined {
  const bundledJar = path.join(context.extensionPath, "server", BUNDLED_JAR_NAME);
  return fs.existsSync(bundledJar) ? bundledJar : undefined;
}

function configuredJavaExecutable(): string | undefined {
  const setting = vscode.workspace.getConfiguration("oft").inspect<string>("java.path");
  const configured =
    setting?.workspaceFolderValue ?? setting?.workspaceValue ?? setting?.globalValue;
  if (configured !== undefined && configured.trim() !== "") {
    return configured;
  }
  return undefined;
}

function bundledServerBinary(context: vscode.ExtensionContext): string | undefined {
  const serverDir = path.join(context.extensionPath, "server");
  const binary = path.join(serverDir, BUNDLED_BINARY_NAME);
  if (!fs.existsSync(binary)) {
    return undefined;
  }
  if (!matchesCurrentPlatform(serverDir)) {
    console.warn("Bundled language server was built for a different platform, ignoring it.");
    return undefined;
  }
  ensureExecutable(binary);
  return binary;
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
      console.warn(`Could not make the bundled language server executable: ${error}`);
    }
  }
}

function matchesCurrentPlatform(serverDir: string): boolean {
  const markerFile = path.join(serverDir, BUNDLED_SERVER_MARKER);
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
