package org.itsallcode.openfasttrace.lsp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.CompletionTriggerKind;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.InsertTextFormat;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.MarkupKind;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.PrepareRenameParams;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TypeHierarchyItem;
import org.eclipse.lsp4j.TypeHierarchyPrepareParams;
import org.eclipse.lsp4j.TypeHierarchySubtypesParams;
import org.eclipse.lsp4j.TypeHierarchySupertypesParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.itsallcode.openfasttrace.api.core.SpecificationItem;
import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.itsallcode.openfasttrace.lsp.codelens.OftCodeLensProvider;
import org.itsallcode.openfasttrace.lsp.completion.OftCompletionContext;
import org.itsallcode.openfasttrace.lsp.completion.OftCompletionSupport;
import org.itsallcode.openfasttrace.lsp.diagnostics.DiagnosticsProvider;
import org.itsallcode.openfasttrace.lsp.diagnostics.QuickFixProvider;
import org.itsallcode.openfasttrace.lsp.hierarchy.OftTypeHierarchyProvider;
import org.itsallcode.openfasttrace.lsp.highlighting.OftSemanticTokensProvider;
import org.itsallcode.openfasttrace.lsp.index.LocationConverter;
import org.itsallcode.openfasttrace.lsp.index.OftIdAtPosition;
import org.itsallcode.openfasttrace.lsp.index.OftWorkspaceIndex;
import org.itsallcode.openfasttrace.lsp.rename.OftRenameProvider;
import org.tinylog.Logger;

public class OftTextDocumentService implements TextDocumentService {

    private LanguageClient client;
    private volatile OftWorkspaceIndex index = OftWorkspaceIndex.empty();
    private final Set<String> openUris = ConcurrentHashMap.newKeySet();
    private final Map<String, List<String>> openDocumentBuffers = new ConcurrentHashMap<>();
    private Runnable onSaveCallback = null;

    private final DiagnosticsProvider diagnosticsProvider = new DiagnosticsProvider();
    private final QuickFixProvider quickFixProvider = new QuickFixProvider();

    void setOnSaveCallback(final Runnable callback) {
        this.onSaveCallback = callback;
    }

    void updateIndex(final OftWorkspaceIndex index) {
        this.index = index;
        openUris.forEach(this::publishDiagnostics);
    }

    void connect(final LanguageClient client) {
        this.client = client;
    }

    // [impl->req~hover-title-and-description~1]
    @Override
    public CompletableFuture<Hover> hover(final HoverParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        Logger.debug("hover: uri=" + uri + " line=" + line + " col=" + col);
        return CompletableFuture.supplyAsync(() -> {
            final String lineText = readLine(uri, line);
            return hoverForLine(lineText, col).orElse(null);
        });
    }

    Optional<Hover> hoverForLine(final String lineText, final int col) {
        return OftIdAtPosition.findAt(lineText, col)
                .flatMap(id -> index.findSpecItem(id))
                .map(this::toHover);
    }

    private Hover toHover(final SpecificationItem item) {
        final String markdown = "**" + item.getTitle() + "**\n\n" + item.getDescription();
        final var content = new MarkupContent(MarkupKind.MARKDOWN, markdown);
        return new Hover(content);
    }

    // [impl->req~goto-definition-spec-to-tags~1, req~goto-definition-tag-to-spec~1]
    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
            definition(final DefinitionParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        Logger.debug("definition: uri=" + uri + " line=" + line + " col=" + col);
        return CompletableFuture.supplyAsync(() -> {
            final String lineText = readLine(uri, line);
            final List<Location> locations = definitionForLine(lineText, col, uri);
            return Either.<List<? extends Location>, List<? extends LocationLink>>forLeft(locations);
        });
    }

    List<Location> definitionForLine(final String lineText, final int col,
            final String currentFileUri) {
        return OftIdAtPosition.findAt(lineText, col)
                .map(id -> {
                    final Optional<SpecificationItem> specItem = index.findSpecItem(id);
                    final boolean cursorIsInSpecFile = specItem
                            .map(SpecificationItem::getLocation)
                            .map(loc -> LocationConverter.toFileKey(loc.getPath())
                                    .equals(LocationConverter.toFileKey(currentFileUri)))
                            .orElse(false);

                    if (cursorIsInSpecFile) {
                        return index.findCoverageTags(id).stream()
                                .map(this::tightLocation)
                                .collect(Collectors.toList());
                    }
                    return specItem
                            .map(this::tightLocation)
                            .map(List::of)
                            .orElse(Collections.emptyList());
                })
                .orElse(Collections.emptyList());
    }

    // [impl->req~coverage-code-lens~1]
    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(final CodeLensParams params) {
        final String uri = params.getTextDocument().getUri();
        Logger.debug("codeLens: uri=" + uri);
        return CompletableFuture.supplyAsync(
                () -> OftCodeLensProvider.codeLenses(index.linkedItemsInFile(uri)));
    }

    // [impl->req~coverage-hierarchy~1]
    @Override
    public CompletableFuture<List<TypeHierarchyItem>> prepareTypeHierarchy(
            final TypeHierarchyPrepareParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        Logger.debug("prepareTypeHierarchy: uri=" + uri + " line=" + line + " col=" + col);
        return CompletableFuture.supplyAsync(
                () -> OftTypeHierarchyProvider.prepareAt(readLine(uri, line), col, index));
    }

    @Override
    public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySupertypes(
            final TypeHierarchySupertypesParams params) {
        return CompletableFuture.supplyAsync(
                () -> OftTypeHierarchyProvider.supertypesOf(params.getItem(), index));
    }

    @Override
    public CompletableFuture<List<TypeHierarchyItem>> typeHierarchySubtypes(
            final TypeHierarchySubtypesParams params) {
        return CompletableFuture.supplyAsync(
                () -> OftTypeHierarchyProvider.subtypesOf(params.getItem(), index));
    }

    // [impl->req~find-references-covering-tags~1]
    @Override
    public CompletableFuture<List<? extends Location>> references(
            final ReferenceParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        Logger.debug("references: uri=" + uri + " line=" + line + " col=" + col);
        return CompletableFuture.supplyAsync(() -> {
            final String lineText = readLine(uri, line);
            return referencesForLine(lineText, col);
        });
    }

    List<Location> referencesForLine(final String lineText, final int col) {
        return OftIdAtPosition.findAt(lineText, col)
                .map(id -> index.findCoverageTags(id).stream()
                        .map(this::tightLocation)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    // [impl->req~precise-ranges-from-oft~1]
    private Location tightLocation(final SpecificationItem item) {
        final String uri = LocationConverter.pathToUri(item.getLocation().getPath());
        return LocationConverter.rangeOfDeclaredId(item)
                .map(range -> new Location(uri, range))
                .orElseGet(() -> tightLocation(item.getLocation()));
    }

    private Location tightLocation(final org.itsallcode.openfasttrace.api.core.Location oftLocation) {
        final String uri = LocationConverter.pathToUri(oftLocation.getPath());
        final String lineText = readLine(uri, Math.max(0, oftLocation.getLine() - 1));
        return LocationConverter.toLspLocation(oftLocation, lineText);
    }

    // [impl->req~quickfix-updates-version~1, req~diagnostic-outdated-version~1]
    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
            final CodeActionParams params) {
        final String uri = params.getTextDocument().getUri();
        return CompletableFuture.supplyAsync(() -> params.getContext().getDiagnostics().stream()
                .filter(d -> "openfasttrace-lsp".equals(d.getSource()))
                .flatMap(d -> quickFixProvider
                        .quickFixesForDiagnostic(d, uri).stream())
                .map(action -> Either.<Command, CodeAction>forRight(action))
                .collect(Collectors.toList()));
    }

    private static final List<String> COVERAGE_TAG_ARTIFACT_TYPES =
            List.of("impl", "utest", "itest", "stest");

    // [impl->req~complete-specification-item-id-in-covers-section~1]
    // [impl->req~complete-specification-item-id-in-coverage-tag-target~1]
    // [impl->req~complete-closing-bracket-for-coverage-tag~1]
    // [impl->req~suggest-coverage-tag-start-in-comment~1]
    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
            final CompletionParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        // TriggerCharacter requests come from typing, everything else counts as manual.
        final boolean suggestTagStart = params.getContext() == null
                || params.getContext().getTriggerKind() != CompletionTriggerKind.TriggerCharacter;
        Logger.debug("completion: uri=" + uri + " line=" + line + " col=" + col);
        return CompletableFuture.supplyAsync(() -> {
            final List<CompletionItem> items =
                    completionForPosition(readAllLines(uri), line, col, suggestTagStart);
            return Either.<List<CompletionItem>, CompletionList>forLeft(items);
        });
    }

    List<CompletionItem> completionForPosition(final List<String> lines, final int line, final int col,
            final boolean suggestTagStart) {
        final Optional<OftCompletionContext> context = OftCompletionContext.findAt(lines, line, col);
        if (context.isPresent()) {
            return OftCompletionSupport
                    .findMatching(index, context.get().prefix(), context.get().coveringArtifactType()).stream()
                    .map(item -> toCompletionItem(item, context.get(), line, col))
                    .collect(Collectors.toList());
        }
        if (suggestTagStart && line >= 0 && line < lines.size()
                && OftCompletionContext.isInsideCommentWithoutOpenTag(lines.get(line), col)) {
            return tagStartSnippets(line, col);
        }
        return Collections.emptyList();
    }

    private CompletionItem toCompletionItem(final SpecificationItem item, final OftCompletionContext context,
            final int line, final int col) {
        final String id = item.getId().toString();
        final String newText = context.appendClosingBracket() ? id + "]" : id;
        final var completionItem = new CompletionItem(id);
        completionItem.setKind(CompletionItemKind.Reference);
        completionItem.setDetail(item.getId().getArtifactType());
        completionItem.setSortText(OftCompletionSupport.sortTextFor(item, context.prefix()));
        final var range =
                new Range(new Position(line, col - context.prefix().length()), new Position(line, col));
        completionItem.setTextEdit(Either.forLeft(new TextEdit(range, newText)));
        return completionItem;
    }

    private List<CompletionItem> tagStartSnippets(final int line, final int col) {
        return COVERAGE_TAG_ARTIFACT_TYPES.stream()
                .map(type -> toTagStartSnippet(type, line, col))
                .collect(Collectors.toList());
    }

    private CompletionItem toTagStartSnippet(final String artifactType, final int line, final int col) {
        final var completionItem = new CompletionItem("[" + artifactType + "->...]");
        completionItem.setKind(CompletionItemKind.Snippet);
        completionItem.setDetail("OFT coverage tag");
        completionItem.setInsertTextFormat(InsertTextFormat.Snippet);
        final var range = new Range(new Position(line, col), new Position(line, col));
        completionItem.setTextEdit(Either.forLeft(new TextEdit(range, "[" + artifactType + "->$0]")));
        return completionItem;
    }

    // [impl->req~prepare-rename~1]
    @Override
    public CompletableFuture<Either3<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>>
            prepareRename(final PrepareRenameParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        Logger.debug("prepareRename: uri=" + uri + " line=" + line + " col=" + col);
        return CompletableFuture.supplyAsync(() -> prepareRenameAt(readLine(uri, line), line, col)
                .map(Either3::<Range, PrepareRenameResult, PrepareRenameDefaultBehavior>forSecond)
                .orElse(null));
    }

    Optional<PrepareRenameResult> prepareRenameAt(final String lineText, final int line, final int col) {
        return OftRenameProvider.nameRangeAt(lineText, line, col)
                .map(range -> new PrepareRenameResult(range,
                        lineText.substring(range.getStart().getCharacter(),
                                range.getEnd().getCharacter())));
    }

    // [impl->req~rename-name-part-only~1, req~rename-specification-item~1]
    @Override
    public CompletableFuture<WorkspaceEdit> rename(final RenameParams params) {
        final String uri = params.getTextDocument().getUri();
        final int line = params.getPosition().getLine();
        final int col = params.getPosition().getCharacter();
        Logger.debug("rename: uri=" + uri + " line=" + line + " col=" + col
                + " newName=" + params.getNewName());
        try {
            return CompletableFuture
                    .completedFuture(renameEdits(uri, line, col, params.getNewName()));
        } catch (final ResponseErrorException exception) {
            return CompletableFuture.failedFuture(exception);
        }
    }

    WorkspaceEdit renameEdits(final String uri, final int line, final int col,
            final String requestedName) {
        final SpecificationItemId id = OftIdAtPosition.findAt(readLine(uri, line), col)
                .orElseThrow(() -> renameError("There is no specification item ID at the cursor."));
        final String newName = OftRenameProvider.extractItemName(requestedName);
        if (!OftRenameProvider.isValidItemName(newName)) {
            throw renameError("'" + newName + "' is not a valid specification item name. "
                    + "A name starts with a letter and continues with letters, digits, "
                    + "underscores, hyphens or dots.");
        }
        // [impl->req~rename-conflict-check~1]
        if (!newName.equals(id.getName())
                && index.findSpecItemByTypeAndName(id.getArtifactType(), newName).isPresent()) {
            throw renameError("'" + id.getArtifactType() + "~" + newName
                    + "' already exists. Choose a different name.");
        }
        final Map<String, List<TextEdit>> changes = new LinkedHashMap<>();
        for (final String fileUri : filesToSearch(uri)) {
            final List<TextEdit> edits = renameEditsInFile(fileUri, id, newName);
            if (!edits.isEmpty()) {
                changes.put(fileUri, edits);
            }
        }
        Logger.info("rename " + id + " to '" + newName + "': " + changes.size() + " file(s)");
        return new WorkspaceEdit(changes);
    }

    private List<TextEdit> renameEditsInFile(final String fileUri, final SpecificationItemId id,
            final String newName) {
        final List<String> lines = readAllLines(fileUri);
        final List<TextEdit> edits = new ArrayList<>();
        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            edits.addAll(OftRenameProvider.renameEditsInLine(lines.get(lineIndex), lineIndex,
                    id.getArtifactType(), id.getName(), newName));
        }
        return edits;
    }

    private Set<String> filesToSearch(final String currentUri) {
        final Map<String, String> uriByFile = new LinkedHashMap<>();
        uriByFile.put(LocationConverter.toFileKey(currentUri), currentUri);
        index.allSpecItems().stream()
                .map(SpecificationItem::getLocation)
                .filter(Objects::nonNull)
                .map(location -> LocationConverter.pathToUri(location.getPath()))
                .forEach(uri -> uriByFile.putIfAbsent(LocationConverter.toFileKey(uri), uri));
        return new LinkedHashSet<>(uriByFile.values());
    }

    private static ResponseErrorException renameError(final String message) {
        return new ResponseErrorException(
                new ResponseError(ResponseErrorCode.RequestFailed, message, null));
    }

    // [impl->req~highlight-specification-item~1]
    // [impl->req~highlight-keyword~1]
    // [impl->req~highlight-coverage-tag~1]
    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(final SemanticTokensParams params) {
        final String uri = params.getTextDocument().getUri();
        Logger.debug("semanticTokensFull: uri=" + uri);
        return CompletableFuture.supplyAsync(
                () -> new SemanticTokens(OftSemanticTokensProvider.computeTokens(readAllLines(uri))));
    }

    // [impl->req~index-refresh-on-save~1]
    @Override
    public void didSave(final DidSaveTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        Logger.debug("didSave: " + uri);
        if (onSaveCallback != null) {
            CompletableFuture.runAsync(onSaveCallback);
        } else {
            publishDiagnostics(uri);
        }
    }

    private void publishDiagnostics(final String uri) {
        if (client == null) {
            return;
        }
        final List<String> lines = readAllLines(uri);
        final List<Diagnostic> diagnostics = diagnosticsProvider.diagnoseFile(uri, lines, index);
        client.publishDiagnostics(new PublishDiagnosticsParams(uri, diagnostics));
    }

    @Override
    public void didOpen(final DidOpenTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        Logger.debug("didOpen: " + uri);
        openUris.add(uri);
        openDocumentBuffers.put(uri, splitLines(params.getTextDocument().getText()));
        publishDiagnostics(uri);
    }

    // [impl->req~live-document-buffer~1]
    @Override
    public void didChange(final DidChangeTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        final List<TextDocumentContentChangeEvent> changes = params.getContentChanges();
        if (!changes.isEmpty()) {
            openDocumentBuffers.put(uri, splitLines(changes.get(changes.size() - 1).getText()));
        }
    }

    @Override
    public void didClose(final DidCloseTextDocumentParams params) {
        final String uri = params.getTextDocument().getUri();
        Logger.debug("didClose: " + uri);
        openUris.remove(uri);
        openDocumentBuffers.remove(uri);
    }

    private String readLine(final String uri, final int lineIndex) {
        final List<String> lines = readAllLines(uri);
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            return lines.get(lineIndex);
        }
        return "";
    }

    private List<String> readAllLines(final String uri) {
        final List<String> buffered = openDocumentBuffers.get(uri);
        if (buffered != null) {
            return buffered;
        }
        try {
            return Files.readAllLines(Path.of(URI.create(uri)));
        } catch (final IOException | IllegalArgumentException e) {
            Logger.debug("Could not read file: " + uri + ": " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<String> splitLines(final String text) {
        final List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(text))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (final IOException e) {
            Logger.warn("Could not read file: " + text + ": " + e.getMessage());
        }
        return lines;
    }
}
