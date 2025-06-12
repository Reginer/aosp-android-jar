/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package libcore.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Proof of concept / fake code whose only purpose is to demonstrate that Java 21
 * language features are supported in libcore.
 */
public class Java21LanguageFeatures {

    public static int calculateApproximateArea(Shape s) {
        // Language feature (Java 21): JEP-441 - Pattern Matching for switch
        // Matching by type
        return switch (s) {
            case Triangle t -> (t.base * t.height / 2);
            case Rectangle r -> (r.length * r.width);
            case Circle c -> (c.radius * c.radius * 3);
            default -> 0;
        };
    }

    public static abstract class Shape {
    }

    public static class Triangle extends Shape {
        public final int base;
        public final int height;

        public Triangle(int base, int height) {
            this.base = base;
            this.height = height;
        }
    }

    public static class Rectangle extends Shape {
        public final int length;
        public final int width;

        public Rectangle(int length, int width) {
            this.length = length;
            this.width = width;
        }
    }

    public static class Circle extends Shape {
        public final int radius;

        public Circle(int radius) {
            this.radius = radius;
        }
    }

    public static String isDroid(String s) {
        // Language feature (Java 21): JEP-441 - Pattern Matching for switch
        // Matching with null check
        return switch (s) {
            case null -> "404";
            case "Android", "Marvin" -> "Yes";
            default -> "No";
        };
    }

    public static String isDroidIgnoreCase(String s) {
        // Language feature (Java 21): JEP-441 - Pattern Matching for switch
        // Matching with case refinement
        return switch (s) {
            case null -> "404";
            case String str when str.equalsIgnoreCase("android") -> "Yes";
            case String str when str.equalsIgnoreCase("marvin") -> "Yes";
            default -> "No";
        };
    }

    public static boolean hasManySides(ShapeType t) {
        // Language feature (Java 21): JEP-441 - Pattern Matching for switch
        // Matching enum constants
        return switch (t) {
            case PointyShapeType.TRIANGLE -> false;
            case PointyShapeType.RECTANGLE -> true;
            case RoundedShapeType.CIRCLE -> false;
        };
    }

    public sealed interface ShapeType permits PointyShapeType, RoundedShapeType {}
    public enum PointyShapeType implements ShapeType { TRIANGLE, RECTANGLE }
    public enum RoundedShapeType implements ShapeType { CIRCLE }

    public static int getX(Object obj) {
        // Language feature (Java 21): JEP-440 - Record Patterns
        if (obj instanceof Point(int x, int y)) {
            return x;
        } else {
            return 0;
        }
    }

    public static int getY(Object obj) {
        // Language feature (Java 21): JEP-440 - Record Patterns
        // Pattern match with type inference for components
        if (obj instanceof Point(var x, var y)) {
            return y;
        } else {
            return 0;
        }
    }

    public static boolean isLineVertical(Object obj) {
        // Language feature (Java 21): JEP-440 - Record Patterns
        // Nested record patterns
        if (obj instanceof Line(Point(int xa, int ya),
                                Point(int xb, int yb))) {
            return xa == xb;
        } else {
            return false;
        }
    }

    public record Point(int x, int y) { }

    public record Line(Point a, Point b) { }

    public static boolean isFirstItemLarger(Pair pair) {
        // Language feature (Java 21): JEP-440 - Record Patterns
        // Switch and record patterns
        return switch (pair) {
            case Pair(PairableInt(int xa), PairableInt(int xb)) -> xa > xb;
            case Pair(PairableString a, Pairable b) -> false;
            case Pair(PairableInt a, PairableString b) -> false;
        };
    }

    public sealed interface Pairable permits PairableInt, PairableString { }

    public record PairableInt(int x) implements Pairable { }

    public record PairableString(String s) implements Pairable { }

    public record Pair(Pairable a, Pairable b) { }

    public static int sumOfMembers(AnyPair<Integer, Integer> pair) {
        // Language feature (Java 21): JEP-440 - Record Patterns
        // Pattern match with type inference type arguments
        return switch (pair) {
            case AnyPair(var a, var b) -> a.intValue() + b.intValue();
        };
    }

    public record AnyPair<S, T>(S a, T b) { }


    public sealed interface Blob permits SquishyBlob, ComplexBlob { }

    public enum SquishyBlob implements Blob { SMALL, MEDIUM, LARGE }

    public sealed interface ComplexBlob extends Blob permits MultiBlob, GeometricBlob { }

    public enum Color { RED, GREEN, BLUE }

    public record MultiBlob(ShapeType type, Color color, Integer count) implements ComplexBlob { }

    public record GeometricBlob(int sides) implements ComplexBlob { }

    public static Object getMainCharacteristic(Blob blob) {
        return switch (blob) {
            case SquishyBlob squishy -> squishy;
            case MultiBlob (RoundedShapeType type, var color, var count)
                when type == RoundedShapeType.CIRCLE -> RoundedShapeType.CIRCLE;
            case MultiBlob (var type, Color color, var count)
                when color == Color.RED -> Color.RED;
            case MultiBlob (var type, var color, var count) -> count;
            case GeometricBlob (int sides) when sides > 1000 -> Integer.valueOf(1000);
            case GeometricBlob (int sides) -> Integer.valueOf(sides);
        };
    }
}
