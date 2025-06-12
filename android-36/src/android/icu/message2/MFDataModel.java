/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package android.icu.message2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This maps closely to the official specification.
 * Since it is not final, we will not add javadoc everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/data-model/README.md">the
 * latest description</a>.</p>
 *
 * @deprecated This API is for technology preview only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
@SuppressWarnings("javadoc")
public class MFDataModel {

    private MFDataModel() {
        // Prevent instantiation
    }

    // Messages

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface Message {
        // Provides a common type for PatternMessage and SelectMessage.
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class PatternMessage implements Message {
        public final List<Declaration> declarations;
        public final Pattern pattern;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public PatternMessage(List<Declaration> declarations, Pattern pattern) {
            this.declarations = declarations;
            this.pattern = pattern;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class SelectMessage implements Message {
        public final List<Declaration> declarations;
        public final List<Expression> selectors;
        public final List<Variant> variants;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public SelectMessage(
                List<Declaration> declarations,
                List<Expression> selectors,
                List<Variant> variants) {
            this.declarations = declarations;
            this.selectors = selectors;
            this.variants = variants;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface Declaration {
        // Provides a common type for InputDeclaration, and LocalDeclaration
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class InputDeclaration implements Declaration {
        public final String name;
        public final VariableExpression value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public InputDeclaration(String name, VariableExpression value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class LocalDeclaration implements Declaration {
        public final String name;
        public final Expression value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public LocalDeclaration(String name, Expression value) {
            this.name = name;
            this.value = value;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface LiteralOrCatchallKey {
        // Provides a common type for the selection keys: Variant, Literal, or CatchallKey.
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Variant implements LiteralOrCatchallKey {
        public final List<LiteralOrCatchallKey> keys;
        public final Pattern value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Variant(List<LiteralOrCatchallKey> keys, Pattern value) {
            this.keys = keys;
            this.value = value;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class CatchallKey implements LiteralOrCatchallKey {
        // String value; // Always '*' in MF2
    }

    // Patterns

    // type Pattern = Array<string | Expression | Markup>;
    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Pattern {
        public final List<PatternPart> parts;

        Pattern() {
            this.parts = new ArrayList<>();
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface PatternPart {
        // Provides a common type for StringPart and Expression.
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class StringPart implements PatternPart {
        public final String value;

        StringPart(String value) {
            this.value = value;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface Expression extends PatternPart {
        // Provides a common type for all kind of expressions:
        // LiteralExpression, VariableExpression, FunctionExpression, UnsupportedExpression, Markup
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class LiteralExpression implements Expression {
        public final Literal arg;
        public final Annotation annotation;
        public final List<Attribute> attributes;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public LiteralExpression(Literal arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class VariableExpression implements Expression {
        public final VariableRef arg;
        public final Annotation annotation;
        public final List<Attribute> attributes;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public VariableExpression(
                VariableRef arg, Annotation annotation, List<Attribute> attributes) {
            this.arg = arg;
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface Annotation {
        // Provides a common type for FunctionAnnotation, UnsupportedAnnotation
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class FunctionExpression implements Expression {
        public final FunctionAnnotation annotation;
        public final List<Attribute> attributes;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public FunctionExpression(FunctionAnnotation annotation, List<Attribute> attributes) {
            this.annotation = annotation;
            this.attributes = attributes;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Attribute {
        public final String name;
        public final LiteralOrVariableRef value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Attribute(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Expressions

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface LiteralOrVariableRef {
        // Provides a common type for Literal and VariableRef,
        // to represent things like `foo` / `|foo|` / `1234` (literals)
        // and `$foo` (VariableRef), as argument for placeholders or value in options.
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Literal implements LiteralOrVariableRef, LiteralOrCatchallKey {
        public final String value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Literal(String value) {
            this.value = value;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class VariableRef implements LiteralOrVariableRef {
        public final String name;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public VariableRef(String name) {
            this.name = name;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class FunctionAnnotation implements Annotation {
        public final String name;
        public final Map<String, Option> options;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public FunctionAnnotation(String name, Map<String, Option> options) {
            this.name = name;
            this.options = options;
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Option {
        public final String name;
        public final LiteralOrVariableRef value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Option(String name, LiteralOrVariableRef value) {
            this.name = name;
            this.value = value;
        }
    }

    // Markup

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Markup implements Expression {
        enum Kind {
            OPEN,
            CLOSE,
            STANDALONE
        }

        public final Kind kind;
        public final String name;
        public final Map<String, Option> options;
        public final List<Attribute> attributes;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Markup(
                Kind kind, String name, Map<String, Option> options, List<Attribute> attributes) {
            this.kind = kind;
            this.name = name;
            this.options = options;
            this.attributes = attributes;
        }
    }
}
