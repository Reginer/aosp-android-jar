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
@android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a builder with the same parameters from this object.
     *
     * @return Builder
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public Builder copyToBuilder() {
        return new Builder(this);
    }

    /**
     * Gets the grammatical case.
     *
     * @return GrammaticalCase
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public GrammaticalCase getGrammaticalCase() {
        return this.grammaticalCase;
    }

    /**
     * Gets the noun class.
     *
     * @return NounClass
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public NounClass getNounClass() {
        return this.nounClass;
    }

    /**
     * Gets the plural category.
     *
     * @return PluralCategory
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public PluralCategory getPluralCategory() {
        return this.pluralCategory;
    }

    /**
     * Gets the capitalization.
     *
     * @return Capitalization
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public Capitalization getCapitalization() {
        return this.capitalization;
    }

    /**
     * Gets the name style.
     *
     * @return NameStyle
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public NameStyle getNameStyle() {
        return this.nameStyle;
    }

    /**
     * Gets the display length.
     *
     * @return DisplayLength
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public DisplayLength getDisplayLength() {
        return this.displayLength;
    }

    /**
     * Gets the substitute handling.
     *
     * @return SubstituteHandling
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public SubstituteHandling getSubstituteHandling() {
        return this.substituteHandling;
    }

    /**
     * Responsible for building {@code DisplayOptions}.
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public Builder setSubstituteHandling(SubstituteHandling substituteHandling) {
            this.substituteHandling = substituteHandling;
            return this;
        }

        /**
         * Builds the display options.
         *
         * @return DisplayOptions
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public DisplayOptions build() {
            DisplayOptions displayOptions = new DisplayOptions(this);
            return displayOptions;
        }
    }

    /**
     * Represents all the grammatical noun classes that are supported by CLDR.
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum NounClass {
        /**
         * A possible setting for NounClass. The noun class context to be used is unknown (this is the
         * default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED("undefined"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        OTHER("other"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        NEUTER("neuter"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        FEMININE("feminine"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        MASCULINE("masculine"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        ANIMATE("animate"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        INANIMATE("inanimate"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        PERSONAL("personal"),
        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        COMMON("common");

        private final String identifier;

        private NounClass(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all noun classes constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<NounClass> VALUES =
                Collections.unmodifiableList(Arrays.asList(NounClass.values()));

        /**
         * @return the lowercase CLDR keyword string for the noun class.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "feminine" or "masculine"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum NameStyle {
        /**
         * A possible setting for NameStyle. The NameStyle context to be used is unknown (this is the
         * default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED,
        /**
         * Use standard names when generating a locale name, e.g. en_GB displays as 'English (United
         * Kingdom)'.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        STANDARD_NAMES,

        /**
         * Use dialect names, when generating a locale name, e.g. en_GB displays as 'British English'.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        DIALECT_NAMES;

        /**
         * Unmodifiable List of all name styles constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<NameStyle> VALUES =
                Collections.unmodifiableList(Arrays.asList(NameStyle.values()));
    }

    /**
     * Represents all the substitute handlings.
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum SubstituteHandling {
        /**
         * A possible setting for SubstituteHandling. The SubstituteHandling context to be used is
         * unknown (this is the default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED,
        /**
         * Returns a fallback value (e.g., the input code) when no data is available. This is the
         * default behaviour.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        SUBSTITUTE,

        /**
         * Returns a null value when no data is available.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        NO_SUBSTITUTE;

        /**
         * Unmodifiable List of all substitute handlings constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<SubstituteHandling> VALUES =
                Collections.unmodifiableList(Arrays.asList(SubstituteHandling.values()));
    }

    /**
     * Represents all the display lengths.
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum DisplayLength {
        /**
         * A possible setting for DisplayLength. The DisplayLength context to be used is unknown (this
         * is the default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED,
        /**
         * Uses full names when generating a locale name, e.g. "United States" for US.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        LENGTH_FULL,

        /**
         * Use short names when generating a locale name, e.g. "U.S." for US.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        LENGTH_SHORT;

        /**
         * Unmodifiable List of all display lengths constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<DisplayLength> VALUES =
                Collections.unmodifiableList(Arrays.asList(DisplayLength.values()));
    }

    /**
     * Represents all the capitalization options.
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum Capitalization {
        /**
         * A possible setting for Capitalization. The capitalization context to be used is unknown (this
         * is the default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for the beginning of a sentence.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        BEGINNING_OF_SENTENCE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for the middle of a sentence.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        MIDDLE_OF_SENTENCE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for stand-alone usage such as an isolated name on a calendar
         * page.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        STANDALONE,

        /**
         * The capitalization context if a date, date symbol or display name is to be formatted with
         * capitalization appropriate for a user-interface list or menu item.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UI_LIST_OR_MENU;

        /**
         * Unmodifiable List of all the capitalizations constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<Capitalization> VALUES =
                Collections.unmodifiableList(Arrays.asList(Capitalization.values()));
    }

    /**
     * Standard CLDR plural category constants. See http://www.unicode.org/reports/tr35/tr35-numbers.html#Language_Plural_Rules
     */
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum PluralCategory {
        /**
         * A possible setting for PluralCategory. The plural category context to be used is unknown
         * (this is the default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED("undefined"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        ZERO("zero"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        ONE("one"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        TWO("two"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        FEW("few"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        MANY("many"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        OTHER("other");

        private final String identifier;

        private PluralCategory(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all plural categories constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<PluralCategory> VALUES =
                Collections.unmodifiableList(Arrays.asList(PluralCategory.values()));

        /**
         * @return the lowercase CLDR keyword string for the plural category
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "few" or "other"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
    @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
    public enum GrammaticalCase {
        /**
         * A possible setting for GrammaticalCase. The grammatical case context to be used is unknown
         * (this is the default value).
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        UNDEFINED("undefined"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        ABLATIVE("ablative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        ACCUSATIVE("accusative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        COMITATIVE("comitative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        DATIVE("dative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        ERGATIVE("ergative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        GENITIVE("genitive"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        INSTRUMENTAL("instrumental"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        LOCATIVE("locative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        LOCATIVE_COPULATIVE("locative_copulative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        NOMINATIVE("nominative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        OBLIQUE("oblique"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        PREPOSITIONAL("prepositional"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        SOCIATIVE("sociative"),

        /**
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        VOCATIVE("vocative");

        private final String identifier;

        private GrammaticalCase(String identifier) {
            this.identifier = identifier;
        }

        /**
         * Unmodifiable List of all grammatical cases constants. List version of {@link #values()}.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public static final List<GrammaticalCase> VALUES =
                Collections.unmodifiableList(Arrays.asList(GrammaticalCase.values()));

        /**
         * @return the lowercase CLDR keyword string for the grammatical case.
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
        public final String getIdentifier() {
            return this.identifier;
        }

        /**
         * @param identifier in lower case such as "dative" or "nominative"
         * @return the plural category corresponding to the identifier, or {@code UNDEFINED}
         */
        @android.annotation.FlaggedApi(com.android.icu.Flags.FLAG_ICU_V_API)
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
