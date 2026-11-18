package dev.survivalcommands.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

final class CommandSearch {
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private CommandSearch() {}

    static Index index(List<String> terms) {
        List<String> literalKeys = new ArrayList<>();
        List<Word> pinyinKeys = new ArrayList<>();
        for (String term : terms) {
            literalKeys.add(normalize(term));
            if (term.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint)
                    == Character.UnicodeScript.HAN)) {
                pinyinKeys.add(new Word(term.codePoints().filter(Character::isLetterOrDigit)
                        .mapToObj(CommandSearch::spellingsForCharacter).toList()));
            }
        }
        return new Index(List.copyOf(literalKeys), List.copyOf(pinyinKeys));
    }

    static String normalize(String text) {
        String normalized = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD);
        return SEPARATORS.matcher(MARKS.matcher(normalized).replaceAll("")).replaceAll("");
    }

    static boolean matches(Index index, String query) {
        if (query.isEmpty() || index.literalKeys().stream().anyMatch(key -> key.contains(query))) {
            return true;
        }
        return index.pinyinKeys().stream().anyMatch(word -> matchesPinyin(word, query));
    }

    private static boolean matchesPinyin(Word word, String query) {
        // Track how much of the query can be matched at each syllable boundary.
        BitSet matched = new BitSet(query.length());
        for (List<String> options : word.syllables()) {
            matched.set(0);
            BitSet next = new BitSet(query.length());
            for (int offset = matched.nextSetBit(0); offset >= 0; offset = matched.nextSetBit(offset + 1)) {
                int remaining = query.length() - offset;
                for (String spelling : options) {
                    if (query.regionMatches(offset, spelling, 0, Math.min(remaining, spelling.length()))) {
                        if (remaining <= spelling.length()) {
                            return true;
                        }
                        next.set(offset + spelling.length());
                    }
                }
            }
            matched = next;
        }
        return false;
    }

    private static List<String> spellingsForCharacter(int codePoint) {
        List<String> spellings = Pinyin.READINGS.get(codePoint);
        if (spellings != null) {
            return spellings;
        }
        return List.of(normalize(Character.toString(codePoint)));
    }

    record Index(List<String> literalKeys, List<Word> pinyinKeys) {}

    private record Word(List<List<String>> syllables) {}

    private static final class Pinyin {
        private static final Map<Integer, List<String>> READINGS = loadReadings();

        private static Map<Integer, List<String>> loadReadings() {
            var resource = CommandSearch.class.getResourceAsStream("/assets/survivalcommands/search/pinyin.tsv");
            if (resource == null) {
                return Map.of();
            }
            Map<Integer, List<String>> result = new HashMap<>();
            try (var reader = new BufferedReader(new InputStreamReader(resource, StandardCharsets.US_ASCII))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.startsWith("#")) {
                        continue;
                    }
                    String[] row = line.split("\t", 2);
                    if (row.length < 2 || row[1].isBlank()) {
                        continue;
                    }
                    var spellings = new LinkedHashSet<String>();
                    for (String reading : row[1].split(",")) {
                        spellings.add(reading);
                        spellings.add(reading.replace('v', 'u'));
                        spellings.add(reading.substring(0, 1));
                        if (reading.startsWith("zh") || reading.startsWith("ch") || reading.startsWith("sh")) {
                            spellings.add(reading.substring(0, 2));
                        }
                    }
                    result.put(Integer.parseInt(row[0], 16), List.copyOf(spellings));
                }
            } catch (IOException | IllegalArgumentException exception) {
                System.getLogger(CommandSearch.class.getName())
                        .log(System.Logger.Level.WARNING, "Unable to load pinyin search data", exception);
            }
            return Map.copyOf(result);
        }
    }
}
