/* GENERATED SOURCE. DO NOT MODIFY. */
// © 2022 and later: Unicode, Inc. and others.
// License & terms of use: https://www.unicode.org/copyright.html

package android.icu.message2;

import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.icu.message2.MFDataModel.Annotation;
import android.icu.message2.MFDataModel.CatchallKey;
import android.icu.message2.MFDataModel.Declaration;
import android.icu.message2.MFDataModel.Expression;
import android.icu.message2.MFDataModel.FunctionAnnotation;
import android.icu.message2.MFDataModel.FunctionExpression;
import android.icu.message2.MFDataModel.InputDeclaration;
import android.icu.message2.MFDataModel.Literal;
import android.icu.message2.MFDataModel.LiteralExpression;
import android.icu.message2.MFDataModel.LiteralOrCatchallKey;
import android.icu.message2.MFDataModel.LiteralOrVariableRef;
import android.icu.message2.MFDataModel.LocalDeclaration;
import android.icu.message2.MFDataModel.Option;
import android.icu.message2.MFDataModel.Pattern;
import android.icu.message2.MFDataModel.SelectMessage;
import android.icu.message2.MFDataModel.StringPart;
import android.icu.message2.MFDataModel.VariableRef;
import android.icu.message2.MFDataModel.Variant;
import android.icu.message2.MessageFormatter.ErrorHandlingBehavior;
import android.icu.util.Calendar;
import android.icu.util.CurrencyAmount;

/**
 * Takes an {@link MFDataModel} and formats it to a {@link String}
 * (and later on we will also implement formatting to a {@code FormattedMessage}).
 */
// TODO: move this in the MessageFormatter?
class MFDataModelFormatter {
    private final Locale locale;
    private final ErrorHandlingBehavior errorHandlingBehavior;
    private final MFDataModel.Message dm;

    private final MFFunctionRegistry standardFunctions;
    private final MFFunctionRegistry customFunctions;
    private static final MFFunctionRegistry EMPTY_REGISTY = MFFunctionRegistry.builder().build();

    MFDataModelFormatter(
            MFDataModel.Message dm,
            Locale locale,
            ErrorHandlingBehavior errorHandlingBehavior,
            MFFunctionRegistry customFunctionRegistry) {
        this.locale = locale;
        this.errorHandlingBehavior = errorHandlingBehavior == null
                ? ErrorHandlingBehavior.BEST_EFFORT : errorHandlingBehavior;
        this.dm = dm;
        this.customFunctions =
                customFunctionRegistry == null ? EMPTY_REGISTY : customFunctionRegistry;

        standardFunctions =
                MFFunctionRegistry.builder()
                        // Date/time formatting
                        .setFormatter("datetime", new DateTimeFormatterFactory("datetime"))
                        .setFormatter("date", new DateTimeFormatterFactory("date"))
                        .setFormatter("time", new DateTimeFormatterFactory("time"))
                        .setDefaultFormatterNameForType(Date.class, "datetime")
                        .setDefaultFormatterNameForType(Calendar.class, "datetime")
                        .setDefaultFormatterNameForType(java.util.Calendar.class, "datetime")
                        .setDefaultFormatterNameForType(Temporal.class, "datetime")

                        // Number formatting
                        .setFormatter("number", new NumberFormatterFactory("number"))
                        .setFormatter("integer", new NumberFormatterFactory("integer"))
                        .setDefaultFormatterNameForType(Integer.class, "number")
                        .setDefaultFormatterNameForType(Double.class, "number")
                        .setDefaultFormatterNameForType(Number.class, "number")
                        .setDefaultFormatterNameForType(CurrencyAmount.class, "number")

                        // Format that returns "to string"
                        .setFormatter("string", new IdentityFormatterFactory())
                        .setDefaultFormatterNameForType(String.class, "string")
                        .setDefaultFormatterNameForType(CharSequence.class, "string")

                        // Register the standard selectors
                        .setSelector("number", new NumberFormatterFactory("number"))
                        .setSelector("integer", new NumberFormatterFactory("integer"))
                        .setSelector("string", new TextSelectorFactory())
                        .setSelector("icu:gender", new TextSelectorFactory())
                        .build();
    }

    String format(Map<String, Object> arguments) {
        MFDataModel.Pattern patternToRender = null;
        if (arguments == null) {
            arguments = new HashMap<>();
        }

        Map<String, Object> variables;
        if (dm instanceof MFDataModel.PatternMessage) {
            MFDataModel.PatternMessage pm = (MFDataModel.PatternMessage) dm;
            variables = resolveDeclarations(pm.declarations, arguments);
            if (pm.pattern == null) {
                fatalFormattingError("The PatternMessage is null.");
            }
            patternToRender = pm.pattern;
        } else if (dm instanceof MFDataModel.SelectMessage) {
            MFDataModel.SelectMessage sm = (MFDataModel.SelectMessage) dm;
            variables = resolveDeclarations(sm.declarations, arguments);
            patternToRender = findBestMatchingPattern(sm, variables, arguments);
            if (patternToRender == null) {
                fatalFormattingError("Cannor find a match for the selector.");
            }
        } else {
            fatalFormattingError("Unknown message type.");
            // formattingError throws, so the return does not actually happen
            return "ERROR!";
        }

        StringBuilder result = new StringBuilder();
        for (MFDataModel.PatternPart part : patternToRender.parts) {
            if (part instanceof MFDataModel.StringPart) {
                MFDataModel.StringPart strPart = (StringPart) part;
                result.append(strPart.value);
            } else if (part instanceof MFDataModel.Expression) {
                FormattedPlaceholder formattedExpression =
                        formatExpression((Expression) part, variables, arguments);
                result.append(formattedExpression.getFormattedValue().toString());
            } else if (part instanceof MFDataModel.Markup) {
                // Ignore
            } else {
                fatalFormattingError("Unknown part type: " + part);
            }
        }
        return result.toString();
    }

    private Pattern findBestMatchingPattern(
            SelectMessage sm, Map<String, Object> variables, Map<String, Object> arguments) {
        Pattern patternToRender = null;

        // ====================================
        // spec: ### Resolve Selectors
        // ====================================

        // Collect all the selector functions in an array, to reuse
        List<Expression> selectors = sm.selectors;
        // spec: Let `res` be a new empty list of resolved values that support selection.
        List<ResolvedSelector> res = new ArrayList<>(selectors.size());
        // spec: For each _selector_ `sel`, in source order,
        for (Expression sel : selectors) {
            // spec: Let `rv` be the resolved value of `sel`.
            FormattedPlaceholder fph = formatExpression(sel, variables, arguments);
            String functionName = null;
            Object argument = null;
            Map<String, Object> options = new HashMap<>();
            if (fph.getInput() instanceof ResolvedExpression) {
                ResolvedExpression re = (ResolvedExpression) fph.getInput();
                argument = re.argument;
                functionName = re.functionName;
                options.putAll(re.options);
            } else if (fph.getInput() instanceof MFDataModel.VariableExpression) {
                MFDataModel.VariableExpression ve = (MFDataModel.VariableExpression) fph.getInput();
                argument = resolveLiteralOrVariable(ve.arg, variables, arguments);
                if (ve.annotation instanceof FunctionAnnotation) {
                    functionName = ((FunctionAnnotation) ve.annotation).name;
                }
            } else if (fph.getInput() instanceof LiteralExpression) {
                LiteralExpression le = (LiteralExpression) fph.getInput();
                argument = le.arg;
                if (le.annotation instanceof FunctionAnnotation) {
                    functionName = ((FunctionAnnotation) le.annotation).name;
                }
            }
            SelectorFactory funcFactory = standardFunctions.getSelector(functionName);
            if (funcFactory == null) {
                funcFactory = customFunctions.getSelector(functionName);
            }
            // spec: If selection is supported for `rv`:
            if (funcFactory != null) {
                Selector selectorFunction = funcFactory.createSelector(locale, options);
                ResolvedSelector rs = new ResolvedSelector(argument, options, selectorFunction);
                // spec: Append `rv` as the last element of the list `res`.
                res.add(rs);
            } else {
                fatalFormattingError("Unknown selector type: " + functionName);
            }
        }

        // This should not be possible, we added one function for each selector,
        // or we have thrown an exception.
        // But just in case someone removes the throw above?
        if (res.size() != selectors.size()) {
            fatalFormattingError(
                    "Something went wrong, not enough selector functions, "
                            + res.size() + " vs. " + selectors.size());
        }

        // ====================================
        // spec: ### Resolve Preferences
        // ====================================

        // spec: Let `pref` be a new empty list of lists of strings.
        List<List<String>> pref = new ArrayList<>();
        // spec: For each index `i` in `res`:
        for (int i = 0; i < res.size(); i++) {
            // spec: Let `keys` be a new empty list of strings.
            List<String> keys = new ArrayList<>();
            // spec: For each _variant_ `var` of the message:
            for (Variant var : sm.variants) {
                // spec: Let `key` be the `var` key at position `i`.
                LiteralOrCatchallKey key = var.keys.get(i);
                // spec: If `key` is not the catch-all key `'*'`:
                if (key instanceof CatchallKey) {
                    keys.add("*");
                } else if (key instanceof Literal) {
                    // spec: Assert that `key` is a _literal_.
                    // spec: Let `ks` be the resolved value of `key`.
                    String ks = ((Literal) key).value;
                    // spec: Append `ks` as the last element of the list `keys`.
                    keys.add(ks);
                } else {
                    fatalFormattingError("Literal expected, but got " + key);
                }
            }
            // spec: Let `rv` be the resolved value at index `i` of `res`.
            ResolvedSelector rv = res.get(i);
            // spec: Let `matches` be the result of calling the method MatchSelectorKeys(`rv`, `keys`)
            List<String> matches = matchSelectorKeys(rv, keys);
            // spec: Append `matches` as the last element of the list `pref`.
            pref.add(matches);
        }

        // ====================================
        // spec: ### Filter Variants
        // ====================================

        // spec: Let `vars` be a new empty list of _variants_.
        List<Variant> vars = new ArrayList<>();
        // spec: For each _variant_ `var` of the message:
        for (Variant var : sm.variants) {
            // spec: For each index `i` in `pref`:
            int found = 0;
            for (int i = 0; i < pref.size(); i++) {
                // spec: Let `key` be the `var` key at position `i`.
                LiteralOrCatchallKey key = var.keys.get(i);
                // spec: If `key` is the catch-all key `'*'`:
                if (key instanceof CatchallKey) {
                    // spec: Continue the inner loop on `pref`.
                    found++;
                    continue;
                }
                // spec: Assert that `key` is a _literal_.
                if (!(key instanceof Literal)) {
                    fatalFormattingError("Literal expected");
                }
                // spec: Let `ks` be the resolved value of `key`.
                String ks = ((Literal) key).value;
                // spec: Let `matches` be the list of strings at index `i` of `pref`.
                List<String> matches = pref.get(i);
                // spec: If `matches` includes `ks`:
                if (matches.contains(ks)) {
                    // spec: Continue the inner loop on `pref`.
                    found++;
                    continue;
                } else {
                    // spec: Else:
                    // spec: Continue the outer loop on message _variants_.
                    break;
                }
            }
            if (found == pref.size()) {
                // spec: Append `var` as the last element of the list `vars`.
                vars.add(var);
            }
        }

        // ====================================
        // spec: ### Sort Variants
        // ====================================
        // spec: Let `sortable` be a new empty list of (integer, _variant_) tuples.
        List<IntVarTuple> sortable = new ArrayList<>();
        // spec: For each _variant_ `var` of `vars`:
        for (Variant var : vars) {
            // spec: Let `tuple` be a new tuple (-1, `var`).
            IntVarTuple tuple = new IntVarTuple(-1, var);
            // spec: Append `tuple` as the last element of the list `sortable`.
            sortable.add(tuple);
        }
        // spec: Let `len` be the integer count of items in `pref`.
        int len = pref.size();
        // spec: Let `i` be `len` - 1.
        int i = len - 1;
        // spec: While `i` >= 0:
        while (i >= 0) {
            // spec: Let `matches` be the list of strings at index `i` of `pref`.
            List<String> matches = pref.get(i);
            // spec: Let `minpref` be the integer count of items in `matches`.
            int minpref = matches.size();
            // spec: For each tuple `tuple` of `sortable`:
            for (IntVarTuple tuple : sortable) {
                // spec: Let `matchpref` be an integer with the value `minpref`.
                int matchpref = minpref;
                // spec: Let `key` be the `tuple` _variant_ key at position `i`.
                LiteralOrCatchallKey key = tuple.variant.keys.get(i);
                // spec: If `key` is not the catch-all key `'*'`:
                if (!(key instanceof CatchallKey)) {
                    // spec: Assert that `key` is a _literal_.
                    if (!(key instanceof Literal)) {
                        fatalFormattingError("Literal expected");
                    }
                    // spec: Let `ks` be the resolved value of `key`.
                    String ks = ((Literal) key).value;
                    // spec: Let `matchpref` be the integer position of `ks` in `matches`.
                    matchpref = matches.indexOf(ks);
                }
                // spec: Set the `tuple` integer value as `matchpref`.
                tuple.integer = matchpref;
            }
            // spec: Set `sortable` to be the result of calling the method `SortVariants(sortable)`.
            sortable.sort(MFDataModelFormatter::sortVariants);
            // spec: Set `i` to be `i` - 1.
            i--;
        }
        // spec: Let `var` be the _variant_ element of the first element of `sortable`.
        IntVarTuple var = sortable.get(0);
        // spec: Select the _pattern_ of `var`.
        patternToRender = var.variant.value;

        // And should do that only once, when building the data model.
        if (patternToRender == null) {
            // If there was a case with all entries in the keys `*` this should not happen
            fatalFormattingError("The selection went wrong, cannot select any option.");
        }

        return patternToRender;
    }

    /* spec:
     * `SortVariants` is a method whose single argument is
     * a list of (integer, _variant_) tuples.
     * It returns a list of (integer, _variant_) tuples.
     * Any implementation of `SortVariants` is acceptable
     * as long as it satisfies the following requirements:
     *
     * 1. Let `sortable` be an arbitrary list of (integer, _variant_) tuples.
     * 1. Let `sorted` be `SortVariants(sortable)`.
     * 1. `sorted` is the result of sorting `sortable` using the following comparator:
     *    1. `(i1, v1)` <= `(i2, v2)` if and only if `i1 <= i2`.
     * 1. The sort is stable (pairs of tuples from `sortable` that are equal
     *    in their first element have the same relative order in `sorted`).
     */
    private static int sortVariants(IntVarTuple o1, IntVarTuple o2) {
        int result = Integer.compare(o1.integer, o2.integer);
        if (result != 0) {
            return result;
        }
        List<LiteralOrCatchallKey> v1 = o1.variant.keys;
        List<LiteralOrCatchallKey> v2 = o1.variant.keys;
        if (v1.size() != v2.size()) {
            fatalFormattingError("The number of keys is not equal.");
        }
        for (int i = 0; i < v1.size(); i++) {
            LiteralOrCatchallKey k1 = v1.get(i);
            LiteralOrCatchallKey k2 = v2.get(i);
            String s1 = k1 instanceof Literal ? ((Literal) k1).value : "*";
            String s2 = k2 instanceof Literal ? ((Literal) k2).value : "*";
            int cmp = s1.compareTo(s2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /**
     * spec:
     * The method MatchSelectorKeys is determined by the implementation.
     * It takes as arguments a resolved _selector_ value `rv` and a list of string keys `keys`,
     * and returns a list of string keys in preferential order.
     * The returned list MUST contain only unique elements of the input list `keys`.
     * The returned list MAY be empty.
     * The most-preferred key is first,
     * with each successive key appearing in order by decreasing preference.
     */
    @SuppressWarnings("static-method")
    private List<String> matchSelectorKeys(ResolvedSelector rv, List<String> keys) {
        return rv.selectorFunction.matches(rv.argument, keys, rv.options);
    }

    private static class ResolvedSelector {
        final Object argument;
        final Map<String, Object> options;
        final Selector selectorFunction;

        public ResolvedSelector(
                Object argument, Map<String, Object> options, Selector selectorFunction) {
            this.argument = argument;
            this.options = new HashMap<>(options);
            this.selectorFunction = selectorFunction;
        }
    }

    private static void fatalFormattingError(String message) throws IllegalArgumentException {
        throw new IllegalArgumentException(message);
    }

    private FormatterFactory getFormattingFunctionFactoryByName(
            Object toFormat, String functionName) {
        // Get a function name from the type of the object to format
        if (functionName == null || functionName.isEmpty()) {
            if (toFormat == null) {
                // The object to format is null, and no function provided.
                return null;
            }
            Class<?> clazz = toFormat.getClass();
            functionName = standardFunctions.getDefaultFormatterNameForType(clazz);
            if (functionName == null) {
                functionName = customFunctions.getDefaultFormatterNameForType(clazz);
            }
            if (functionName == null) {
                fatalFormattingError(
                        "Object to format without a function, and unknown type: "
                                + toFormat.getClass().getName());
            }
        }

        FormatterFactory func = standardFunctions.getFormatter(functionName);
        if (func == null) {
            func = customFunctions.getFormatter(functionName);
        }
        return func;
    }

    private static Object resolveLiteralOrVariable(
            LiteralOrVariableRef value,
            Map<String, Object> localVars,
            Map<String, Object> arguments) {
        if (value instanceof Literal) {
            String val = ((Literal) value).value;
            // "The resolution of a text or literal MUST resolve to a string."
            // https://github.com/unicode-org/message-format-wg/blob/main/spec/formatting.md#literal-resolution
            return val;
        } else if (value instanceof VariableRef) {
            String varName = ((VariableRef) value).name;
            Object val = localVars.get(varName);
            if (val == null) {
                val = localVars.get(varName);
            }
            if (val == null) {
                val = arguments.get(varName);
            }
            return val;
        }
        return value;
    }

    private static Map<String, Object> convertOptions(
            Map<String, Option> options,
            Map<String, Object> localVars,
            Map<String, Object> arguments) {
        Map<String, Object> result = new HashMap<>();
        for (Option option : options.values()) {
            result.put(option.name, resolveLiteralOrVariable(option.value, localVars, arguments));
        }
        return result;
    }

    /**
     * Formats an expression.
     *
     * @param expression the expression to format
     * @param variables local variables, created from declarations (`.input` and `.local`)
     * @param arguments the arguments passed at runtime to be formatted (`mf.format(arguments)`)
     */
    private FormattedPlaceholder formatExpression(
            Expression expression, Map<String, Object> variables, Map<String, Object> arguments) {

        Annotation annotation = null; // function name
        String functionName = null;
        Object toFormat = null;
        Map<String, Object> options = new HashMap<>();
        String fallbackString = "{\uFFFD}";

        if (expression instanceof MFDataModel.VariableExpression) {
            MFDataModel.VariableExpression varPart = (MFDataModel.VariableExpression) expression;
            fallbackString = "{$" + varPart.arg.name + "}";
            annotation = varPart.annotation; // function name & options
            Object resolved = resolveLiteralOrVariable(varPart.arg, variables, arguments);
            if (resolved instanceof FormattedPlaceholder) {
                Object input = ((FormattedPlaceholder) resolved).getInput();
                if (input instanceof ResolvedExpression) {
                    ResolvedExpression re = (ResolvedExpression) input;
                    toFormat = re.argument;
                    functionName = re.functionName;
                    options.putAll(re.options);
                } else {
                    toFormat = input;
                }
            } else {
                toFormat = resolved;
            }
        } else if (expression
                instanceof MFDataModel.FunctionExpression) { // Function without arguments
            MFDataModel.FunctionExpression fe = (FunctionExpression) expression;
            fallbackString = "{:" + fe.annotation.name + "}";
            annotation = fe.annotation;
        } else if (expression instanceof MFDataModel.LiteralExpression) {
            MFDataModel.LiteralExpression le = (LiteralExpression) expression;
            annotation = le.annotation;
            fallbackString = "{|" + le.arg.value + "|}";
            toFormat = resolveLiteralOrVariable(le.arg, variables, arguments);
        } else if (expression instanceof MFDataModel.Markup) {
            // No output on markup, for now (we only format to string)
            return new FormattedPlaceholder(expression, new PlainStringFormattedValue(""));
        } else {
            if (expression == null) {
                fatalFormattingError("unexpected null expression");
            } else {
                fatalFormattingError("unknown expression type "
                        + expression.getClass().getName());
            }
        }

        if (annotation instanceof FunctionAnnotation) {
            FunctionAnnotation fa = (FunctionAnnotation) annotation;
            if (functionName != null && !functionName.equals(fa.name)) {
                fatalFormattingError(
                        "invalid function overrides, '" + functionName + "' <> '" + fa.name + "'");
            }
            functionName = fa.name;
            Map<String, Object> newOptions = convertOptions(fa.options, variables, arguments);
            options.putAll(newOptions);
        }

        FormatterFactory funcFactory = getFormattingFunctionFactoryByName(toFormat, functionName);
        if (funcFactory == null) {
            if (errorHandlingBehavior == ErrorHandlingBehavior.STRICT) {
                fatalFormattingError("unable to find function at " + fallbackString);
            }
            return new FormattedPlaceholder(expression, new PlainStringFormattedValue(fallbackString));
        }
        Formatter ff = funcFactory.createFormatter(locale, options);
        String res = ff.formatToString(toFormat, arguments);
        if (res == null) {
            if (errorHandlingBehavior == ErrorHandlingBehavior.STRICT) {
                fatalFormattingError("unable to format string at " + fallbackString);
            }
            res = fallbackString;
        }

        ResolvedExpression resExpression = new ResolvedExpression(toFormat, functionName, options);
        return new FormattedPlaceholder(resExpression, new PlainStringFormattedValue(res));
    }

    static class ResolvedExpression implements Expression {
        final Object argument;
        final String functionName;
        final Map<String, Object> options;

        public ResolvedExpression(
                Object argument, String functionName, Map<String, Object> options) {
            this.argument = argument;
            this.functionName = functionName;
            this.options = options;
        }
    }

    private Map<String, Object> resolveDeclarations(
            List<MFDataModel.Declaration> declarations, Map<String, Object> arguments) {
        Map<String, Object> variables = new HashMap<>();
        String name;
        Expression value;
        if (declarations != null) {
            for (Declaration declaration : declarations) {
                if (declaration instanceof InputDeclaration) {
                    name = ((InputDeclaration) declaration).name;
                    value = ((InputDeclaration) declaration).value;
                } else if (declaration instanceof LocalDeclaration) {
                    name = ((LocalDeclaration) declaration).name;
                    value = ((LocalDeclaration) declaration).value;
                } else {
                    continue;
                }
                try {
                    // There it no need to succeed in solving everything.
                    // For example there is no problem is `$b` is not defined below:
                    // .local $a = {$b :number}
                    // {{ Hello {$user}! }}
                    FormattedPlaceholder fmt = formatExpression(value, variables, arguments);
                    // If it works, all good
                    variables.put(name, fmt);
                } catch (Exception e) {
                    // It's OK to ignore the failure in this context, see comment above.
                }
            }
        }
        return variables;
    }

    private static class IntVarTuple {
        int integer;
        final Variant variant;

        public IntVarTuple(int integer, Variant variant) {
            this.integer = integer;
            this.variant = variant;
        }
    }
}
