package de.talentmatch.resume;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import de.talentmatch.resume.api.FileGroupDto;
import de.talentmatch.resume.api.GroupMatch;
import de.talentmatch.resume.api.InspectedFileDto;

/**
 * Fasst Dateien derselben Person zusammen: gleiche E-Mail, sonst gleiche Telefonnummer, sonst gleicher Name.
 * Zuordnungen, die nur auf dem Namen beruhen, werden zur Prüfung markiert.
 */
public final class DocumentGrouping {

    private DocumentGrouping() {
    }

    public static List<FileGroupDto> group(List<InspectedFileDto> files) {
        List<InspectedFileDto> usable = files.stream().filter(f -> f.error() == null).toList();
        UnionFind components = new UnionFind(files.size());
        for (Link link : links(usable)) {
            components.union(link.a(), link.b(), link.match());
        }
        Map<Integer, List<Integer>> grouped = new LinkedHashMap<>();
        for (InspectedFileDto file : usable) {
            grouped.computeIfAbsent(components.find(file.index()), root -> new ArrayList<>()).add(file.index());
        }
        return grouped.entrySet().stream()
                .map(entry -> toGroup(entry.getValue(), components.match(entry.getKey()),
                        components.reviewNeeded(entry.getKey()), files))
                .toList();
    }

    /**
     * Alle Paare mit gemeinsamem Merkmal, stärkste Merkmale zuerst.
     */
    private static List<Link> links(List<InspectedFileDto> files) {
        List<Link> links = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            for (int j = i + 1; j < files.size(); j++) {
                int a = files.get(i).index();
                int b = files.get(j).index();
                strongestMatch(files.get(i), files.get(j)).ifPresent(match -> links.add(new Link(a, b, match)));
            }
        }
        links.sort(Comparator.comparing(Link::match));
        return links;
    }

    static Optional<GroupMatch> strongestMatch(InspectedFileDto a, InspectedFileDto b) {
        if (a.email() != null && a.email().equalsIgnoreCase(b.email())) {
            return Optional.of(GroupMatch.EMAIL);
        }
        Optional<String> phoneA = ResumeHeuristics.normalizePhone(a.phone());
        if (phoneA.isPresent() && phoneA.equals(ResumeHeuristics.normalizePhone(b.phone()))) {
            return Optional.of(GroupMatch.PHONE);
        }
        if (knownName(a).isPresent() && knownName(a).equals(knownName(b))) {
            return Optional.of(GroupMatch.NAME);
        }
        return Optional.empty();
    }

    private static Optional<String> knownName(InspectedFileDto file) {
        ResumeHeuristics.Name name = new ResumeHeuristics.Name(file.firstName(), file.lastName());
        return name.isKnown() && file.firstName() != null
                ? Optional.of((file.firstName() + " " + file.lastName()).toLowerCase(Locale.GERMAN))
                : Optional.empty();
    }

    private static FileGroupDto toGroup(List<Integer> indexes, GroupMatch match, boolean nameOnly,
                                        List<InspectedFileDto> files) {
        Map<DocumentType, Integer> perType = new EnumMap<>(DocumentType.class);
        indexes.forEach(i -> perType.merge(files.get(i).type(), 1, Integer::sum));
        boolean duplicateTypes = perType.values().stream().anyMatch(count -> count > 1);
        boolean withoutResume = !perType.containsKey(DocumentType.RESUME);
        return new FileGroupDto(List.copyOf(indexes), indexes.size() == 1 ? GroupMatch.NONE : match,
                nameOnly || duplicateTypes || withoutResume);
    }

    private record Link(int a, int b, GroupMatch match) {
    }

    /**
     * Union-Find über Dateiindizes; merkt sich je Gruppe das stärkste Merkmal und ob ein reiner Namens-Treffer
     * zwei sonst getrennte Gruppen verbunden hat.
     */
    private static final class UnionFind {

        private final int[] parent;
        private final GroupMatch[] match;
        private final Set<Integer> nameOnly = new HashSet<>();

        UnionFind(int size) {
            parent = new int[size];
            match = new GroupMatch[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
                match[i] = GroupMatch.NONE;
            }
        }

        int find(int index) {
            int root = index;
            while (parent[root] != root) {
                root = parent[root];
            }
            return root;
        }

        void union(int a, int b, GroupMatch link) {
            int rootA = find(a);
            int rootB = find(b);
            if (rootA == rootB) {
                return;
            }
            boolean review = nameOnly.remove(rootA) | nameOnly.remove(rootB) || link == GroupMatch.NAME;
            parent[rootB] = rootA;
            match[rootA] = strongest(match[rootA], strongest(match[rootB], link));
            if (review) {
                nameOnly.add(rootA);
            }
        }

        GroupMatch match(int root) {
            return Objects.requireNonNullElse(match[root], GroupMatch.NONE);
        }

        boolean reviewNeeded(int root) {
            return nameOnly.contains(root);
        }

        private static GroupMatch strongest(GroupMatch a, GroupMatch b) {
            return a.compareTo(b) <= 0 ? a : b;
        }
    }
}
