package org.itsallcode.openfasttrace.lsp.diagnostics;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;

class QuickFixProviderTest {

    private QuickFixProvider provider;

    @BeforeEach
    void setUp() {
        provider = new QuickFixProvider();
    }

    // [utest->req~quickfix-updates-version~1]
    @Test
    void testGivenOutdatedDiagnosticWhenBuildingQuickFixThenTextEditReplacesIdWithCurrentRevision() {
        // given
        final var range = new Range(new Position(2, 8), new Position(2, 20));
        final var diagnostic = new Diagnostic(range, "Outdated reference: 'req~my-req~1'",
                DiagnosticSeverity.Warning, "openfasttrace-lsp");
        diagnostic.setData("req~my-req~3");

        // when
        final var actions = provider.quickFixesForDiagnostic(
                diagnostic, "file:///workspace/impl.md");

        // then
        assertThat(actions).hasSize(1);
        final CodeAction action = actions.get(0);
        assertThat(action.getKind()).isEqualTo(CodeActionKind.QuickFix);
        assertThat(action.getTitle()).contains("req~my-req~3");
        final var edits = action.getEdit().getChanges()
                .get("file:///workspace/impl.md");
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).getNewText()).isEqualTo("req~my-req~3");
        assertThat(edits.get(0).getRange()).isEqualTo(range);
    }

    // [utest->req~quickfix-updates-version~1]
    @Test
    void testGivenDiagnosticDataAsJsonPrimitiveWhenBuildingQuickFixThenTextEditIsProduced() {
        // given
        final var range = new Range(new Position(0, 0), new Position(0, 12));
        final var diagnostic = new Diagnostic(range, "Outdated reference: 'req~my-req~1'",
                DiagnosticSeverity.Warning, "openfasttrace-lsp");
        diagnostic.setData(new JsonPrimitive("req~my-req~3"));

        // when
        final var actions = provider.quickFixesForDiagnostic(diagnostic, "file:///impl.md");

        // then
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).contains("req~my-req~3");
    }

    // [utest->req~quickfix-updates-version~1]
    @Test
    void testGivenDiagnosticWithoutDataWhenBuildingQuickFixThenNoActionIsProduced() {
        // given
        final var diagnostic = new Diagnostic(
                new Range(new Position(0, 0), new Position(0, 13)),
                "Outdated reference: 'req~unknown~1'",
                DiagnosticSeverity.Warning, "openfasttrace-lsp");

        // when
        final var actions = provider.quickFixesForDiagnostic(diagnostic, "file:///any.md");

        // then
        assertThat(actions).isEmpty();
    }
}
