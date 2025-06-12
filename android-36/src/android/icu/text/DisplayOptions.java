/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package android.icu.text;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Represents all the display options that are supported by CLDR such as grammatical case, noun
 * class, ... etc. It currently supports enums, but may be extended in the future to have other
 * types of data. It replaces a DisplayContext[] as a method parameter.
 * <p>
 * NOTE: This class is Immutable, and uses a Builder interface.
 * <p>For example:
 * {@code DisplayOptions x =
 *                DisplayOptions.builder()
 *                             .setNounClass(NounClass.DATIVE)
 *                             .setPluralCategory(PluralCategory.FEW)
 *                             .build();
 *                             }
 */
public final class DisplayOptions {
    private final GrammaticalCase grammaticalCase;
    private final NounClass nounClass;
    private final PluralCategory pluralCategory;
    private final Capitalization capitalization;
    private final NameStyle nameStyle;
    private final DisplayLength displayLength;
    private final SubstituteHandling substituteHandling;

    private DisplayOptions(Builder builder) {
        this.grammaticalCase = builder.grammaticalCase;
        this.nounClass = builder.nounClass;
        this.pluralCategory = builder.pluralCategory;
        this.capitalization = builder.capitalization;
        this.nameStyle = builder.nameStyle;
        this.displayLength = builder.displayLength;
        this.substituteHandling = builder.substituteHandling;
    }

    /**
     * Creates a builder with the {@code UNDEFINED} value for all the parameters.
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder with the same parameters from this object.
     *
     * @return Builder
     */
    public Builder copyToBuilder() {
        return new Builder(this);
    }

    /**
     * Gets the grammatical case.
     *
     * @return GrammaticalCase
     */
    public GrammaticalCase getGrammaticalCase() {
        return this.grammaticalCase;
    }

    /**
     * Gets the noun class.
     *
     * @return NounClass
     */
    public NounClass getNounClass() {
        return this.nounClass;
    }

    /**
     * Gets the plural category.
     *
     * @return PluralCategory
     */
    public PluralCategory getPluralCategory() {
        return this.pluralCategory;
    }

    /**
     * Gets the capitalization.
     *
     * @return Capitalization
     */
    public Capitalization getCapitalization() {
        return this.capitalization;
    }

    /**
     * Gets the name style.
     *
     * @return NameStyle
     */
    public NameStyle getNameStyle() {
        return this.nameStyle;
    }

    /**
     * Gets the display length.
     *
     * @return DisplayLength
     */
    public DisplayLength getDisplayLength() {
        return this.displayLength;
    }

    /**
     * Gets the substitute handling.
     *
     * @return SubstituteHandling
     */
    public SubstituteHandling getSubstituteHandling() {
        return this.substituteHandling;
    }

    /**
     * Responsible for building {@code DisplayOptions}.
     */
    public static class Builder {
        private GrammaticalCase grammaticalCase;
        private NounClass nounClass;
        private PluralCategory pluralCategory;
        private Capitalization capitalization;
        private NameStyle nameStyle;
        private DisplayLength displayLength;
        private SubstituteHandling substituteHandling;

        /**
         * Creates a {@code DisplayOptions.Builder} with the default values.
         */
        private Builder() {
            this.grammaticalCase = GrammaticalCase.UNDEFINED;
            this.nounClass = NounClass.UNDEFINED;
            this.pluralCategory = PluralCategory.UNDEFINED;
            this.capitalization = Capitalization.UNDEFINED;
            this.nameStyle = NameStyle.UNDEFINED;
            this.displayLength = DisplayLength.UNDEFINED;
            this.substituteHandling = SubstituteHandling.UNDEFINED;
        }

        /**
         * Creates a {@code Builder} with all the information from a {@code DisplayOptions}.
         *
         * @param displayOptions Options to be copied.
         */
        private Builder(DisplayOptions displayOptions) {
            this.grammaticalCase = displayOptions.grammaticalCase;
            this.nounClass = displayOptions.nounClass;
            this.pluralCategory = displayOptions.pluralCategory;
            this.capitalization = displayOptions.capitalization;
            this.nameStyle = displayOptions.nameStyle;
            this.displayLength = displayOptions.displayLength;
            this.substituteHandling = displayOptions.substituteHandling;
        }

        /**
         * Sets the grammatical case.
         *
         * @param grammaticalCase The grammatical case.
         * @return Builder
         */
        public Builder setGrammaticalCase(GrammaticalCase grammaticalCase) {
            this.grammaticalCase = grammaticalCase;
            return this;
        }

        /**
         * Sets the noun class.
         *
         * @param nounClass The noun class.
         * @return Builder
         */
        public Builder setNounClass(NounClass nounClass) {
            this.nounClass = nounClass;
            return this;
        }

        /**
         * Sets the plural category.
         *
         * @param pluralCategory The plural category.
         * @return Builder
         */
        public Builder setPluralCategory(PluralCategory pluralCategory) {
            this.pluralCategory = pluralCategory;
            return this;
        }

        /**
         * Sets the capitalization.
         *
         * @param capitalization The capitalization.
         * @return Builder
         */
        public Builder setCapitalization(Capitalization capitalization) {
            this.capitalization = capitalization;
            return this;
        }

        /**
         * Sets the name style.
         *
         * @param nameStyle The name style.
         * @return Builder
         */
        public Builder setNameStyle(NameStyle nameStyle) {
            this.nameStyle = nameStyle;
            return this;
        }

        /**
         * Sets the display length.
         *
         * @param displayLength The display length.
         * @return Builder
         */
        public Builder setDisplayLength(DisplayLength displayLength) {
            this.displayLength = displayLength;
            return this;
        }

        /**
         * Sets the substitute handling.
         *
         * @param substituteHandling The substitute handling.
         * @return Builder
         */
        public Builder setSubstituteHandling(SubstituteHandling substituteHandling) {
            this.substituteHandling = substituteHandling;
            return this;
        }

        /**
         * Builds the display options.
         *
         * @return DisplayOptions
         */
        public DisplayOptions build() {
            DisplayOptions displayOptions = new DisplayOptions(this);
            return displayOptions;
        }
    }

    /**
     * Represents all the grammatical noun classes that are supported by CLDR.
     */
    public enum NounClass {
        /**
         * A possible setting for NounClass. The noun class context to be used is unknown (this is the
         * default value).
         */
        UNDEFINED("undefined"),
        /**
         */
        OTHER("other"),
        /**
         */
        NEUTER("neuter"),
        /**
         */
        FEMININE("feminine"),
        /**
         */
        MASCULINE("masculine"),
        /**
         */
        ANIMATE("animate"),
        /**
         */
        INANIMATE("inanimate"),
        /**
         */
        PERSONAL("personal"),
        /**
         */
        COMMON("common");

        private final String identifier;

        private NounClass(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all noun classes constants. List version of {@link #values()}.
         */
        public static final List<NounClass> VALUES =
                Collections.unmodifiableList(Arrays.asList(NounClass.values()));

        /**
         * @return the lowercase CLDR keyword string for the noun class.
         */
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "feminine" or "masculine"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         */
        public static final NounClass fromIdentifier(String identifier) {
            if (identifier == null) {
                return NounClass.UNDEFINED;
            }

            for (NounClass nounClass : VALUES) {
                if (identifier.equals(nounClass.getIdentifier())) {
                    return nounClass;
                }
            }

            return NounClass.UNDEFINED;
        }
    }

    /**
     * Represents all the name styles.
     */
    public enum NameStyle {
        /**
         * A possible setting for NameStyle. The NameStyle context to be used is unknown (this is the
         * default value).
         */
        UNDEFINED,
        /**
         * Use standard names when generating a locale name, e.g. en_GB displays as 'English (United
         * Kingdom)'.
         */
        STANDARD_NAMES,

        /**
         * Use dialect names, when generating a locale name, e.g. en_GB displays as 'British English'.
         */
        DIALECT_NAMES;

        /**
         * Unmodifiable List of all name styles constants. List version of {@link #values()}.
         */
        public static final List<NameStyle> VALUES =
                Collections.unmodifiableList(Arrays.asList(NameStyle.values()));
    }

    /**
     * Represents all the substitute handlings.
     */
    public enum SubstituteHandling {
        /**
         * A possible setting for SubstituteHandling. The SubstituteHandling context to be used is
         * unknown (this is the default value).
         */
        UNDEFINED,
        /**
         * Returns a fallback value (e.g., the input code) when no data is available. This is the
         * default behaviour.
         */
        SUBSTITUTE,

        /**
         * Returns a null value when no data is available.
         */
        NO_SUBSTITUTE;

        /**
         * Unmodifiable List of all substitute handlings constants. List version of {@link #values()}.
         */
        public static final List<SubstituteHandling> VALUES =
                Collections.unmodifiableList(Arrays.asList(SubstituteHandling.values()));
    }

    /**
     * Represents all the display lengths.
     */
    public enum DisplayLength {
        /**
         * A possible setting for DisplayLength. The DisplayLength context to be used is unknown (this
         * is the default value).
         */
        UNDEFINED,
        /**
         * Uses full names when generating a locale name, e.g. "United States" for US.
         */
        LENGTH_FULL,

        /**
         * Use short names when generating a locale name, e.g. "U.S." for US.
         */
        LENGTH_SHORT;

        /**
         * Unmodifiable List of all display lengths constants. List version of {@link #values()}.
         */
        public static final List<DisplayLength> VALUES =
                Collections.unmodifiableList(Arrays.asList(DisplayLength.values()));
    }

    /**
     * Represents all the capitalization options.
     */
    public enum Capitalization {
        /**
         * A possible setting for Capitalization. The capitalization context to be used is unknown (this
         * is the default value).
         */
        UNDEFINED,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for the beginning of a sentence.
         */
        BEGINNING_OF_SENTENCE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for the middle of a sentence.
         */
        MIDDLE_OF_SENTENCE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for stand-alone usage such as an isolated name on a calendar
         * page.
         */
        STANDALONE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for a user-interface list or menu item.
         */
        UI_LIST_OR_MENU;

        /**
         * Unmodifiable List of all the capitalizations constants. List version of {@link #values()}.
         */
        public static final List<Capitalization> VALUES =
                Collections.unmodifiableList(Arrays.asList(Capitalization.values()));
    }

    /**
     * Standard CLDR plural category constants. See http://www.unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules
     */
    public enum PluralCategory {
        /**
         * A possible setting for PluralCategory. The plural category context to be used is unknown
         * (this is the default value).
         */
        UNDEFINED("undefined"),

        /**
         */
        ZERO("zero"),

        /**
         */
        ONE("one"),

        /**
         */
        TWO("two"),

        /**
         */
        FEW("few"),

        /**
         */
        MANY("many"),

        /**
         */
        OTHER("other");

        private final String identifier;

        private PluralCategory(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all plural categories constants. List version of {@link #values()}.
         */
        public static final List<PluralCategory> VALUES =
                Collections.unmodifiableList(Arrays.asList(PluralCategory.values()));

        /**
         * @return the lowercase CLDR keyword string for the plural category
         */
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "few" or "other"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         */
        public static final PluralCategory fromIdentifier(String identifier) {
            if (identifier == null) {
                return PluralCategory.UNDEFINED;
            }

            for (PluralCategory pluralCategory : VALUES) {
                if (identifier.equals(pluralCategory.getIdentifier())) {
                    return pluralCategory;
                }
            }

            return PluralCategory.UNDEFINED;
        }
    }

    /**
     * Represents all the grammatical cases that are supported by CLDR.
     */
    public enum GrammaticalCase {
        /**
         * A possible setting for GrammaticalCase. The grammatical case context to be used is unknown
         * (this is the default value).
         */
        UNDEFINED("undefined"),

        /**
         */
        ABLATIVE("ablative"),

        /**
         */
        ACCUSATIVE("accusative"),

        /**
         */
        COMITATIVE("comitative"),

        /**
         */
        DATIVE("dative"),

        /**
         */
        ERGATIVE("ergative"),

        /**
         */
        GENITIVE("genitive"),

        /**
         */
        INSTRUMENTAL("instrumental"),

        /**
         */
        LOCATIVE("locative"),

        /**
         */
        LOCATIVE_COPULATIVE("locative_copulative"),

        /**
         */
        NOMINATIVE("nominative"),

        /**
         */
        OBLIQUE("oblique"),

        /**
         */
        PREPOSITIONAL("prepositional"),

        /**
         */
        SOCIATIVE("sociative"),

        /**
         */
        VOCATIVE("vocative");

        private final String identifier;

        private GrammaticalCase(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all grammatical cases constants. List version of {@link #values()}.
         */
        public static final List<GrammaticalCase> VALUES =
                Collections.unmodifiableList(Arrays.asList(GrammaticalCase.values()));

        /**
         * @return the lowercase CLDR keyword string for the grammatical case.
         */
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "dative" or "nominative"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         */
        public static final GrammaticalCase fromIdentifier(String identifier) {
            if (identifier == null) {
                return GrammaticalCase.UNDEFINED;
            }

            for (GrammaticalCase grammaticalCase : VALUES) {
                if (identifier.equals(grammaticalCase.getIdentifier())) {
                    return grammaticalCase;
                }
            }

            return GrammaticalCase.UNDEFINED;
        }
    }
}
