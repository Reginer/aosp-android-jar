/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: http://www.unicode.org/copyright.html

package android.icu.message2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

/**
 * This maps closely to the official specification.
 * Since it is not final, we will not add javadoc everywhere.
 *
 * <p>See <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/syntax.md">the
 * description of the syntax with examples and use cases</a> and the corresponding
 * <a target="github" href="https://github.com/unicode-org/message-format-wg/blob/main/spec/message.ebnf">EBNF</a>.</p>
 *
 * @deprecated This API is for technology preview only.
 * @hide Only a subset of ICU is exposed in Android
 * @hide draft / provisional / internal are hidden on Android
 */
@Deprecated
@SuppressWarnings("javadoc")
public class Mf2DataModel {

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class SelectorKeys {
        private final List<String> keys;

        private SelectorKeys(Builder builder) {
            keys = new ArrayList<>();
            keys.addAll(builder.keys);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public List<String> getKeys() {
            return Collections.unmodifiableList(keys);
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        @Override
        public String toString() {
            StringJoiner result = new StringJoiner(" ");
            for (String key : keys) {
                result.add(key);
            }
            return result.toString();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide Only a subset of ICU is exposed in Android
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static class Builder {
            private final List<String> keys = new ArrayList<>();

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder add(String key) {
                keys.add(key);
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder addAll(Collection<String> otherKeys) {
                this.keys.addAll(otherKeys);
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public SelectorKeys build() {
                return new SelectorKeys(this);
            }
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Pattern {
        private final List<Part> parts;

        private Pattern(Builder builder) {
            parts = new ArrayList<>();
            parts.addAll(builder.parts);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public List<Part> getParts() {
            return parts;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("{");
            for (Part part : parts) {
                result.append(part);
            }
            result.append("}");
            return result.toString();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide Only a subset of ICU is exposed in Android
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static class Builder {
            private final List<Part> parts = new ArrayList<>();

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder add(Part part) {
                parts.add(part);
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder addAll(Collection<Part> otherParts) {
                parts.addAll(otherParts);
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Pattern build() {
                return new Pattern(this);
            }

        }
    }

    /**
     * No functional role, this is only to be able to say that a message is a sequence of Part(s),
     * and that plain text {@link Text} and {@link Expression} are Part(s).
     *
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public interface Part {
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Text implements Part {
        private final String value;

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        private Text(Builder builder) {
            this(builder.value);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Text(String value) {
            this.value = value;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public String getValue() {
            return value;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        @Override
        public String toString() {
            return value;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide Only a subset of ICU is exposed in Android
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static class Builder {
            private String value;

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder setValue(String value) {
                this.value = value;
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Text build() {
                return new Text(this);
            }
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Expression implements Part {
        private final Value operand; // Literal | Variable
        private final String functionName;
        private final Map<String, Value> options;
        Formatter formatter = null;

        private Expression(Builder builder) {
            this.operand = builder.operand;
            this.functionName = builder.functionName;
            this.options = builder.options;
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Value getOperand() {
            return operand;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public String getFunctionName() {
            return functionName;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Map<String, Value> getOptions() {
            return options;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("{");
            if (operand != null) {
                result.append(operand);
            }
            if (functionName != null) {
                result.append(" :").append(functionName);
            }
            for (Entry<String, Value> option : options.entrySet()) {
                result.append(" ").append(option.getKey()).append("=").append(option.getValue());
            }
            result.append("}");
            return result.toString();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide Only a subset of ICU is exposed in Android
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static class Builder {
            private Value operand = null;
            private String functionName = null;
            private final OrderedMap<String, Value> options = new OrderedMap<>();

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder setOperand(Value operand) {
                this.operand = operand;
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder setFunctionName(String functionName) {
                this.functionName = functionName;
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder addOption(String key, Value value) {
                options.put(key, value);
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder addOptions(Map<String, Value> otherOptions) {
                options.putAll(otherOptions);
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Expression build() {
                return new Expression(this);
            }
        }
    }

//    public static class Placeholder extends Expression implements Part {
//        public Placeholder(Builder builder) {
//            super(builder);
//        }
//    }

    /**
     * A Value can be either a Literal, or a Variable, but not both.
     *
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Value {
        private final String literal;
        private final String variableName;

        private Value(Builder builder) {
            this.literal = builder.literal;
            this.variableName = builder.variableName;
//            this(builder.literal, builder.variableName);
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public String getLiteral() {
            return literal;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public String getVariableName() {
            return variableName;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public boolean isLiteral() {
            return literal != null;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public boolean isVariable() {
            return variableName != null;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        @Override
        public String toString() {
            return isLiteral() ? "(" + literal + ")" : "$" + variableName;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide Only a subset of ICU is exposed in Android
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static class Builder {
            private String literal;
            private String variableName;

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder setLiteral(String literal) {
                this.literal = literal;
                this.variableName = null;
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder setVariableName(String variableName) {
                this.variableName = variableName;
                this.literal = null;
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Value build() {
                return new Value(this);
            }
        }
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Variable {
        private final String name;

        private Variable(Builder builder) {
            this.name = builder.name;
        }

        /**
         * Creates a builder.
         *
         * @return the Builder.
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static Builder builder() {
            return new Builder();
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public String getName() {
            return name;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide Only a subset of ICU is exposed in Android
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public static class Builder {
            private String name;

            // Prevent direct creation
            private Builder() {
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Builder setName(String name) {
                this.name = name;
                return this;
            }

            /**
             * @deprecated This API is for technology preview only.
             * @hide draft / provisional / internal are hidden on Android
             */
            @Deprecated
            public Variable build() {
                return new Variable(this);
            }
        }
    }

    /**
     * This is only to not force LinkedHashMap on the public API.
     *
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class OrderedMap<K, V> extends LinkedHashMap<K, V> {
        private static final long serialVersionUID = -7049361727790825496L;

        /**
         * {@inheritDoc}
         *
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public OrderedMap() {
            super();
        }
    }

    private final OrderedMap<String, Expression> localVariables;
    private final List<Expression> selectors;
    private final OrderedMap<SelectorKeys, Pattern> variants;
    private final Pattern pattern;

    private Mf2DataModel(Builder builder) {
        this.localVariables = builder.localVariables;
        this.selectors = builder.selectors;
        this.variants = builder.variants;
        this.pattern = builder.pattern;
    }

    /**
     * Creates a builder.
     *
     * @return the Builder.
     *
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public OrderedMap<String, Expression> getLocalVariables() {
        return localVariables;
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public List<Expression>  getSelectors() {
        return selectors;
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public OrderedMap<SelectorKeys, Pattern>  getVariants() {
        return variants;
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public Pattern getPattern() {
        return pattern;
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (Entry<String, Expression> lv : localVariables.entrySet()) {
            result.append("let $").append(lv.getKey());
            result.append(" = ");
            result.append(lv.getValue());
            result.append("\n");
        }
        if (!selectors.isEmpty()) {
            result.append("match");
            for (Expression e : this.selectors) {
                result.append(" ").append(e);
            }
            result.append("\n");
            for (Entry<SelectorKeys, Pattern> variant : variants.entrySet()) {
                result.append("  when ").append(variant.getKey());
                result.append(" ");
                result.append(variant.getValue());
                result.append("\n");
            }
        } else {
            result.append(pattern);
        }
        return result.toString();
    }

    /**
     * @deprecated This API is for technology preview only.
     * @hide Only a subset of ICU is exposed in Android
     * @hide draft / provisional / internal are hidden on Android
     */
    @Deprecated
    public static class Builder {
        private final OrderedMap<String, Expression> localVariables = new OrderedMap<>(); // declaration*
        private final List<Expression> selectors = new ArrayList<>();
        private final OrderedMap<SelectorKeys, Pattern> variants = new OrderedMap<>();
        private Pattern pattern = Pattern.builder().build();

        // Prevent direct creation
        private Builder() {
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder addLocalVariable(String variableName, Expression expression) {
            this.localVariables.put(variableName, expression);
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder addLocalVariables(OrderedMap<String, Expression> otherLocalVariables) {
            this.localVariables.putAll(otherLocalVariables);
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder addSelector(Expression otherSelector) {
            this.selectors.add(otherSelector);
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder addSelectors(List<Expression> otherSelectors) {
            this.selectors.addAll(otherSelectors);
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder addVariant(SelectorKeys keys, Pattern newPattern) {
            this.variants.put(keys, newPattern);
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder addVariants(OrderedMap<SelectorKeys, Pattern> otherVariants) {
            this.variants.putAll(otherVariants);
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Builder setPattern(Pattern pattern) {
            this.pattern = pattern;
            return this;
        }

        /**
         * @deprecated This API is for technology preview only.
         * @hide draft / provisional / internal are hidden on Android
         */
        @Deprecated
        public Mf2DataModel build() {
            return new Mf2DataModel(this);
        }
    }
}
