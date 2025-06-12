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

package android.content.pm;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.app.Person;
import android.app.appsearch.AppSearchSchema;
import android.app.appsearch.GenericDocument;
import android.graphics.drawable.Icon;
import android.net.UriCodec;

import com.android.internal.annotations.VisibleForTesting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@link GenericDocument} representation of {@link Person} object.
 *
 * @hide
 */
public class AppSearchShortcutPerson extends GenericDocument {

    /**
     * The name of the schema type for {@link Person} documents.
     * @hide
     */
    public static final String SCHEMA_TYPE = "ShortcutPerson";

    private static final String KEY_NAME = "name";
    private static final String KEY_KEY = "key";
    private static final String KEY_IS_BOT = "isBot";
    private static final String KEY_IS_IMPORTANT = "isImportant";
    private static final String KEY_ICON = "icon";

    public AppSearchShortcutPerson(@NonNull GenericDocument document) {
        super(document);
    }

    public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
            .addProperty(new AppSearchSchema.StringPropertyConfig.Builder(KEY_NAME)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                    .build()

            ).addProperty(new AppSearchSchema.StringPropertyConfig.Builder(KEY_KEY)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
                    .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
                    .build()

            ).addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(KEY_IS_BOT)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                    .build()

            ).addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder(KEY_IS_IMPORTANT)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                    .build()

            ).addProperty(new AppSearchSchema.BytesPropertyConfig.Builder(KEY_ICON)
                    .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                    .build()

            ).build();

    /** @hide */
    @NonNull
    public static AppSearchShortcutPerson instance(@NonNull final Person person) {
        Objects.requireNonNull(person);
        final String id;
        if (person.getUri() != null) {
            id = person.getUri();
        } else {
            // NOTE: an identifier is required even when uri is null.
            id = UUID.randomUUID().toString();
        }
        return new Builder(id).setName(person.getName())
                .setKey(person.getKey()).setIsBot(person.isBot())
                .setIsImportant(person.isImportant())
                .setIcon(transformToByteArray(person.getIcon())).build();
    }

    /**
     * Convert this {@link GenericDocument} into {@link Person}.
     */
    @NonNull
    public Person toPerson() {
        String uri;
        try {
            uri = UriCodec.decode(
                    getId(), false /* convertPlus */, StandardCharsets.UTF_8,
                    true /* throwOnFailure */);
        } catch (IllegalArgumentException e) {
            uri = null;
        }
        return new Person.Builder().setName(getPropertyString(KEY_NAME))
                .setUri(uri).setKey(getPropertyString(KEY_KEY))
                .setBot(getPropertyBoolean(KEY_IS_BOT))
                .setImportant(getPropertyBoolean(KEY_IS_IMPORTANT))
                .setIcon(transformToIcon(getPropertyBytes(KEY_ICON)))
                .build();
    }

    /** @hide */
    @VisibleForTesting
    public static class Builder extends GenericDocument.Builder<Builder> {

        public Builder(@NonNull final String id) {
            super(/*namespace=*/ "", id, SCHEMA_TYPE);
        }

        /** @hide */
        @NonNull
        public Builder setName(@Nullable final CharSequence name) {
            if (name != null) {
                setPropertyString(KEY_NAME, name.toString());
            }
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setKey(@Nullable final String key) {
            if (key != null) {
                setPropertyString(KEY_KEY, key);
            }
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setIsBot(final boolean isBot) {
            setPropertyBoolean(KEY_IS_BOT, isBot);
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setIsImportant(final boolean isImportant) {
            setPropertyBoolean(KEY_IS_IMPORTANT, isImportant);
            return this;
        }

        /** @hide */
        @NonNull
        public Builder setIcon(@Nullable final byte[] icon) {
            if (icon != null) {
                setPropertyBytes(KEY_ICON, icon);
            }
            return this;
        }

        /** @hide */
        @NonNull
        @Override
        public AppSearchShortcutPerson build() {
            return new AppSearchShortcutPerson(super.build());
        }
    }

    /**
     * Convert {@link Icon} into byte[].
     */
    @Nullable
    private static byte[] transformToByteArray(@Nullable final Icon icon) {
        if (icon == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            icon.writeToStream(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Convert byte[] into {@link Icon}.
     */
    @Nullable
    private Icon transformToIcon(@Nullable final byte[] icon) {
        if (icon == null) {
            return null;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(icon)) {
            return Icon.createFromStream(bais);
        } catch (IOException e) {
            return null;
        }
    }
}
