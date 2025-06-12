/*
 * Copyright (C) 2021 The Android Open Source Project
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
 * Proof of concept / fake code whose only purpose is to demonstrate that Java 17
 * language features are supported in libcore.
 */
public class Java17LanguageFeatures {

    public static String getTextBlock() {
        // Language feature (Java 15): JEP-378 - Text blocks
        return """
            This is a
            multiline
            string.""";
    }

    public static int calculateApproximateArea(Shape s) {
        // Language feature (Java 16): JEP-394 - Pattern matching instanceof
        if (s instanceof Triangle t) {
            return (t.base * t.height / 2);
        } else if (s instanceof Rectangle r) {
            return (r.length * r.width);
        } else if (s instanceof Circle c) {
            return (c.radius * c.radius * 3);
        } else {
            return 0;
        }
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

    public static Point buildPoint(int x, int y) {
        // Language feature (Java 16): JEP-395 - Records
        return new Point(x, y);
    }

    public record Point(int x, int y) { }

    public static int getSealedClassId(BaseSealedClass obj) {
        // Language feature (Java 17): JEP-409 - Sealed classes
        return obj.getId();
    }

    public static sealed class BaseSealedClass permits FinalDerivedClass, NonSealedDerivedClass {
        public int getId() {
            return 0;
        }
    }

    public static final class FinalDerivedClass extends BaseSealedClass {
        @Override
        public int getId() {
            return 1;
        }
    }

    public static non-sealed class NonSealedDerivedClass extends BaseSealedClass {
        @Override
        public int getId() {
            return 2;
        }
    }
}
