package org.itsallcode.openfasttrace.lsp.index;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.lsp4j.Range;
import org.itsallcode.openfasttrace.api.core.Location;
import org.junit.jupiter.api.Test;

class LocationConverterTest {

    // [utest->req~goto-definition-spec-to-tags~1]
    @Test
    void testGivenLocationWithoutLineTextWhenConvertingThenRangeSpansFullLine() {
        // given
        final Location oftLocation = Location.create("/ws/a.md", 3);

        // when
        final Range range = LocationConverter.toLspLocation(oftLocation).getRange();

        // then
        assertThat(range.getStart().getLine()).isEqualTo(2);
        assertThat(range.getStart().getCharacter()).isZero();
        assertThat(range.getEnd().getCharacter()).isEqualTo(Integer.MAX_VALUE);
    }

    // [utest->req~goto-definition-spec-to-tags~1]
    @Test
    void testGivenCoverageTagLineWhenConvertingThenRangeIsTightenedToTag() {
        // given
        final String line = "    // [impl->req~login~1]";

        // when
        final Range range = LocationConverter
                .toLspLocation(Location.create("/ws/a.java", 10), line)
                .getRange();

        // then
        assertThat(range.getStart().getCharacter()).isEqualTo(line.indexOf('['));
        assertThat(range.getEnd().getCharacter()).isEqualTo(line.indexOf(']') + 1);
    }

    // [utest->req~goto-definition-tag-to-spec~1]
    @Test
    void testGivenBacktickedSpecItemLineWhenConvertingThenRangeIsTightenedToBareId() {
        // given
        final String line = "`req~login~1`";

        // when
        final Range range = LocationConverter
                .toLspLocation(Location.create("/ws/spec.md", 5), line)
                .getRange();

        // then
        assertThat(range.getStart().getCharacter()).isEqualTo(1); // after backtick
        assertThat(range.getEnd().getCharacter()).isEqualTo(1 + "req~login~1".length());
    }

}
