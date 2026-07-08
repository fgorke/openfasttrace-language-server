package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.itsallcode.openfasttrace.api.core.SpecificationItemId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OftIdAtPositionTest {

    @Test
    void testGivenCursorOnIdInCoversLineWhenFindingIdThenIdIsReturned() {
        // given
        final String line = "Covers:  req~my-requirement~3  and more";

        // when
        final var result = OftIdAtPosition.findAt(line, 12);

        // then
        assertThat(result).contains(SpecificationItemId.parseId("req~my-requirement~3"));
    }

    @ParameterizedTest(name = "position {1} in ''{0}''")
    @CsvSource({
        "prefix req~x~1 suffix, 7, req~x~1",
        "prefix req~x~1 suffix, 13, req~x~1",
        "prefix req~x~1 suffix, 14, "
    })
    void testGivenIdWithinLineWhenFindingIdAtBoundaryPositionsThenIdIsResolvedOnlyInsideItsSpan(
            final String line, final int col, final String expectedId) {
        // given / when
        final var result = OftIdAtPosition.findAt(line, col);

        // then
        if (expectedId == null || expectedId.isBlank()) {
            assertThat(result).isEmpty();
        } else {
            assertThat(result).contains(SpecificationItemId.parseId(expectedId));
        }
    }
}
