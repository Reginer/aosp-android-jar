/**
 * Copyright 2021, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package android.app.appsearch.aidl;

/** {@hide} */
oneway interface IAppSearchObserverProxy {
    void onSchemaChanged(
            in String packageName, in String databaseName, in List<String> changedSchemaNames);

    void onDocumentChanged(
            in String packageName,
            in String databaseName,
            in String namespace,
            in String schemaName,
            in List<String> changedDocumentIds);
}
