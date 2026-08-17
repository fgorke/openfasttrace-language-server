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

  registerGenerateReportCommand(context);
  registerCoverageTagSuggestions(context);

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
