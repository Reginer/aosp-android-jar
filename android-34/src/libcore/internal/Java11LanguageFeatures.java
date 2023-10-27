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
 * Proof of concept / fake code whose only purpose is to demonstrate that Java 11
 * language features are supported in libcore.
 */
public class Java11LanguageFeatures {

    public static String collectUpperCaseStrings(List<String> list) {
        // Language feature: local-variable syntax for lambda
        return list.stream()
            .map((var item) -> item.toUpperCase())
            .collect(Collectors.joining(" "));
    }

    public static String guessTheString(String guess) {
        // Language feature (Java 10): local-variable type inference
        var answer = "The answer to the universe, life and everything";
        if (!(answer instanceof String)) {
            return null;
        }
        if (!guess.equals(answer)) {
            return "";
        }
        return answer;
    }

    public static class Person {
        // Language feature: Reflection on private fields/methods of nest-mates
        // Note: This at the moment is not supported yet in Android and calling these methods will
        // result in exceptions (b/210843415).
        private final String name;
        private final Vocabulary vocabulary = new Vocabulary();

        public Person(String name) {
            this.name = name;
        }

        public String greet() throws NoSuchMethodException, InvocationTargetException,
               NoSuchFieldException, IllegalAccessException {
            Method greetFn = Vocabulary.class.getDeclaredMethod("greet");
            return (String) greetFn.invoke(vocabulary);
        }

        private class Vocabulary {

            private String greet() throws NoSuchFieldException, IllegalAccessException {
                Field nameField = Person.class.getDeclaredField("name");
                String nameValue = (String) nameField.get(Person.this);
                return "Hello " + nameValue;
            }
        }
    }
}
