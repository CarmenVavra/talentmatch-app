package de.talentmatch.resume;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.talentmatch.resume.api.FileGroupDto;
import de.talentmatch.resume.api.GroupMatch;
import de.talentmatch.resume.api.InspectedFileDto;

class DocumentGroupingTest {

    @Test
    void groupsByEmailAndKeepsOtherPeopleSeparate() {
        List<FileGroupDto> groups = DocumentGrouping.group(List.of(
                file(0, DocumentType.RESUME, "Carmen", "Gregor", "carmen@example.org", null),
                file(1, DocumentType.RESUME, "Max", "Muster", "max@example.org", null),
                file(2, DocumentType.SKILL_PROFILE, "Neu", "Bewerber:in", "CARMEN@example.org", null)));

        assertThat(groups).extracting(FileGroupDto::fileIndexes).containsExactly(List.of(0, 2), List.of(1));
        assertThat(groups.getFirst().match()).isEqualTo(GroupMatch.EMAIL);
        assertThat(groups.getFirst().needsReview()).isFalse();
        assertThat(groups.get(1).match()).isEqualTo(GroupMatch.NONE);
    }

    @Test
    void groupsByPhoneIgnoringFormatting() {
        List<FileGroupDto> groups = DocumentGrouping.group(List.of(
                file(0, DocumentType.RESUME, "Neu", "Bewerber:in", null, "+43 660 123 4567"),
                file(1, DocumentType.COVER_LETTER, "Neu", "Bewerber:in", null, "0660/1234567")));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.match()).isEqualTo(GroupMatch.PHONE);
            assertThat(group.needsReview()).isFalse();
        });
    }

    @Test
    void nameOnlyMatchesNeedReview() {
        List<FileGroupDto> groups = DocumentGrouping.group(List.of(
                file(0, DocumentType.RESUME, "Carmen", "Gregor", "carmen@example.org", null),
                file(1, DocumentType.SKILL_PROFILE, "carmen", "gregor", null, null)));

        assertThat(groups).singleElement().satisfies(group -> {
            assertThat(group.match()).isEqualTo(GroupMatch.NAME);
            assertThat(group.needsReview()).isTrue();
        });
    }

    @Test
    void placeholderNamesNeverMatch() {
        List<FileGroupDto> groups = DocumentGrouping.group(List.of(
                file(0, DocumentType.RESUME, "Neu", "Bewerber:in", null, null),
                file(1, DocumentType.RESUME, "Neu", "Bewerber:in", null, null)));

        assertThat(groups).hasSize(2);
    }

    @Test
    void flagsGroupsWithTwoResumesOrNoResume() {
        List<FileGroupDto> groups = DocumentGrouping.group(List.of(
                file(0, DocumentType.RESUME, "Anna", "Berg", "anna@example.org", null),
                file(1, DocumentType.RESUME, "Anna", "Berg", "anna@example.org", null),
                file(2, DocumentType.SKILL_PROFILE, "Tim", "Roth", null, null)));

        assertThat(groups).extracting(FileGroupDto::needsReview).containsExactly(true, true);
    }

    @Test
    void ignoresUnusableFiles() {
        InspectedFileDto broken = new InspectedFileDto(1, "bild.png", null, null, null, null, null, "nur PDF");
        List<FileGroupDto> groups = DocumentGrouping.group(List.of(
                file(0, DocumentType.RESUME, "Anna", "Berg", null, null), broken));

        assertThat(groups).extracting(FileGroupDto::fileIndexes).containsExactly(List.of(0));
    }

    private static InspectedFileDto file(int index, DocumentType type, String first, String last, String email,
                                         String phone) {
        return new InspectedFileDto(index, "datei" + index + ".pdf", type, first, last, email, phone, null);
    }
}
