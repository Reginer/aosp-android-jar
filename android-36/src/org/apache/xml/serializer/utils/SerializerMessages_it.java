/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the  "License");
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
/*
 * $Id: SerializerMessages_it.java 471981 2006-11-07 04:28:00Z minchau $
 */

package org.apache.xml.serializer.utils;

import java.util.ListResourceBundle;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * An instance of this class is a ListResourceBundle that
 * has the required getContents() method that returns
 * an array of message-key/message associations.
 * <p>
 * The message keys are defined in {@link MsgKey}. The
 * messages that those keys map to are defined here.
 * <p>
 * The messages in the English version are intended to be
 * translated.
 *
 * This class is not a public API, it is only public because it is
 * used in org.apache.xml.serializer.
 *
 * @xsl.usage internal
 */
public class SerializerMessages_it extends ListResourceBundle {

    /*
     * This file contains error and warning messages related to
     * Serializer Error Handling.
     *
     *  General notes to translators:

     *  1) A stylesheet is a description of how to transform an input XML document
     *     into a resultant XML document (or HTML document or text).  The
     *     stylesheet itself is described in the form of an XML document.

     *
     *  2) An element is a mark-up tag in an XML document; an attribute is a
     *     modifier on the tag.  For example, in <elem attr='val' attr2='val2'>
     *     "elem" is an element name, "attr" and "attr2" are attribute names with
     *     the values "val" and "val2", respectively.
     *
     *  3) A namespace declaration is a special attribute that is used to associate
     *     a prefix with a URI (the namespace).  The meanings of element names and
     *     attribute names that use that prefix are defined with respect to that
     *     namespace.
     *
     *
     */

    /** The lookup table for error messages.   */
    public Object[][] getContents() {
        Object[][] contents = new Object[][] {
            {   MsgKey.BAD_MSGKEY,
                "La chiave messaggio ''{0}'' non si trova nella classe del messaggio ''{1}''" },

            {   MsgKey.BAD_MSGFORMAT,
                "Il formato del messaggio ''{0}'' nella classe del messaggio ''{1}'' non \u00e8 riuscito." },

            {   MsgKey.ER_SERIALIZER_NOT_CONTENTHANDLER,
                "La classe del serializzatore ''{0}'' non implementa org.xml.sax.ContentHandler." },

            {   MsgKey.ER_RESOURCE_COULD_NOT_FIND,
                    "Risorsa [ {0} ] non trovata.\n {1}" },

            {   MsgKey.ER_RESOURCE_COULD_NOT_LOAD,
                    "Impossibile caricare la risorsa [ {0} ]: {1} \n {2} \t {3}" },

            {   MsgKey.ER_BUFFER_SIZE_LESSTHAN_ZERO,
                    "Dimensione buffer <=0" },

            {   MsgKey.ER_INVALID_UTF16_SURROGATE,
                    "Rilevato surrogato UTF-16 non valido: {0} ?" },

            {   MsgKey.ER_OIERROR,
                "Errore IO" },

            {   MsgKey.ER_ILLEGAL_ATTRIBUTE_POSITION,
                "Impossibile aggiungere l''''attributo {0} dopo i nodi secondari o prima che sia prodotto un elemento.  L''''attributo verr\u00e0 ignorato." },

            /*
             * Note to translators:  The stylesheet contained a reference to a
             * namespace prefix that was undefined.  The value of the substitution
             * text is the name of the prefix.
             */
            {   MsgKey.ER_NAMESPACE_PREFIX,
                "Lo spazio nomi per il prefisso ''{0}'' non \u00e8 stato dichiarato." },

            /*
             * Note to translators:  This message is reported if the stylesheet
             * being processed attempted to construct an XML document with an
             * attribute in a place other than on an element.  The substitution text
             * specifies the name of the attribute.
             */
            {   MsgKey.ER_STRAY_ATTRIBUTE,
                "L''''attributo ''{0}'' al di fuori dell''''elemento." },

            /*
             * Note to translators:  As with the preceding message, a namespace
             * declaration has the form of an attribute and is only permitted to
             * appear on an element.  The substitution text {0} is the namespace
             * prefix and {1} is the URI that was being used in the erroneous
             * namespace declaration.
             */
            {   MsgKey.ER_STRAY_NAMESPACE,
                "Dichiarazione dello spazio nome ''{0}''=''{1}'' al di fuori dell''''elemento." },

            {   MsgKey.ER_COULD_NOT_LOAD_RESOURCE,
                "Impossibile caricare ''{0}'' (verificare CLASSPATH), verranno utilizzati i valori predefiniti" },

            {   MsgKey.ER_ILLEGAL_CHARACTER,
                "Tentare di generare l''''output del carattere di valor integrale {0} che non \u00e8 rappresentato nella codifica di output specificata di {1}." },

            {   MsgKey.ER_COULD_NOT_LOAD_METHOD_PROPERTY,
                "Impossibile caricare il file delle propriet\u00e0 ''{0}'' per il metodo di emissione ''{1}'' (verificare CLASSPATH)" },

            {   MsgKey.ER_INVALID_PORT,
                "Numero di porta non valido" },

            {   MsgKey.ER_PORT_WHEN_HOST_NULL,
                "La porta non pu\u00f2 essere impostata se l'host \u00e8 nullo" },

            {   MsgKey.ER_HOST_ADDRESS_NOT_WELLFORMED,
                "Host non \u00e8 un'indirizzo corretto" },

            {   MsgKey.ER_SCHEME_NOT_CONFORMANT,
                "Lo schema non \u00e8 conforme." },

            {   MsgKey.ER_SCHEME_FROM_NULL_STRING,
                "Impossibile impostare lo schema da una stringa nulla" },

            {   MsgKey.ER_PATH_CONTAINS_INVALID_ESCAPE_SEQUENCE,
                "Il percorso contiene sequenza di escape non valida" },

            {   MsgKey.ER_PATH_INVALID_CHAR,
                "Il percorso contiene un carattere non valido: {0}" },

            {   MsgKey.ER_FRAG_INVALID_CHAR,
                "Il frammento contiene un carattere non valido" },

            {   MsgKey.ER_FRAG_WHEN_PATH_NULL,
                "Il frammento non pu\u00f2 essere impostato se il percorso \u00e8 nullo" },

            {   MsgKey.ER_FRAG_FOR_GENERIC_URI,
                "Il frammento pu\u00f2 essere impostato solo per un URI generico" },

            {   MsgKey.ER_NO_SCHEME_IN_URI,
                "Non \u00e8 stato trovato alcuno schema nell'URI" },

            {   MsgKey.ER_CANNOT_INIT_URI_EMPTY_PARMS,
                "Impossibile inizializzare l'URI con i parametri vuoti" },

            {   MsgKey.ER_NO_FRAGMENT_STRING_IN_PATH,
                "Il frammento non pu\u00f2 essere specificato sia nel percorso che nel frammento" },

            {   MsgKey.ER_NO_QUERY_STRING_IN_PATH,
                "La stringa di interrogazione non pu\u00f2 essere specificata nella stringa di interrogazione e percorso." },

            {   MsgKey.ER_NO_PORT_IF_NO_HOST,
                "La porta non pu\u00f2 essere specificata se l'host non S specificato" },

            {   MsgKey.ER_NO_USERINFO_IF_NO_HOST,
                "Userinfo non pu\u00f2 essere specificato se l'host non S specificato" },
            {   MsgKey.ER_XML_VERSION_NOT_SUPPORTED,
                "Attenzione:  La versione del documento di emissione \u00e8 obbligatorio che sia ''{0}''.  Questa versione di XML non \u00e8 supportata.  La versione del documento di emissione sar\u00e0 ''1.0''." },

            {   MsgKey.ER_SCHEME_REQUIRED,
                "Lo schema \u00e8 obbligatorio." },

            /*
             * Note to translators:  The words 'Properties' and
             * 'SerializerFactory' in this message are Java class names
             * and should not be translated.
             */
            {   MsgKey.ER_FACTORY_PROPERTY_MISSING,
                "L''''oggetto Properties passato al SerializerFactory non ha una propriet\u00e0 ''{0}''." },

            {   MsgKey.ER_ENCODING_NOT_SUPPORTED,
                "Avvertenza:  La codifica ''{0}'' non \u00e8 supportata da Java runtime." },

             {MsgKey.ER_FEATURE_NOT_FOUND,
             "Il parametro ''{0}'' non \u00e8 riconosciuto."},

             {MsgKey.ER_FEATURE_NOT_SUPPORTED,
             "Il parametro ''{0}'' \u00e8 riconosciuto ma non \u00e8 possibile impostare il valore richiesto."},

             {MsgKey.ER_STRING_TOO_LONG,
             "La stringa risultante \u00e8 troppo lunga per essere inserita in DOMString: ''{0}''."},

             {MsgKey.ER_TYPE_MISMATCH_ERR,
             "Il tipo di valore per questo nome di parametro non \u00e8 compatibile con il tipo di valore previsto."},

             {MsgKey.ER_NO_OUTPUT_SPECIFIED,
             "La destinazione di output in cui scrivere i dati era nulla."},

             {MsgKey.ER_UNSUPPORTED_ENCODING,
             "\u00c8 stata rilevata una codifica non supportata."},

             {MsgKey.ER_UNABLE_TO_SERIALIZE_NODE,
             "Impossibile serializzare il nodo."},

             {MsgKey.ER_CDATA_SECTIONS_SPLIT,
             "La Sezione CDATA contiene uno o pi\u00f9 markers di termine ']]>'."},

             {MsgKey.ER_WARNING_WF_NOT_CHECKED,
                 "Impossibile creare un'istanza del controllore Well-Formedness.  Il parametro well-formed \u00e8 stato impostato su true ma non \u00e8 possibile eseguire i controlli well-formedness."
             },

             {MsgKey.ER_WF_INVALID_CHARACTER,
                 "Il nodo ''{0}'' contiene caratteri XML non validi."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_COMMENT,
                 "Trovato un carattere XML non valido (Unicode: 0x{0}) nel commento."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_PI,
                 "Carattere XML non valido (Unicode: 0x{0}) rilevato nell''elaborazione di instructiondata."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_CDATA,
                 "Carattere XML non valido (Unicode: 0x{0}) rilevato nel contenuto di CDATASection."
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_TEXT,
                 "Carattere XML non valido (Unicode: 0x{0}) rilevato nel contenuto dati di caratteri del nodo. "
             },

             { MsgKey.ER_WF_INVALID_CHARACTER_IN_NODE_NAME,
                 "Carattere XML non valido rilevato nel nodo {0} denominato ''{1}''."
             },

             { MsgKey.ER_WF_DASH_IN_COMMENT,
                 "La stringa \"--\" non \u00e8 consentita nei commenti."
             },

             {MsgKey.ER_WF_LT_IN_ATTVAL,
                 "Il valore dell''''attributo \"{1}\" associato con un tipo di elemento \"{0}\" non deve contenere il carattere ''<''."
             },

             {MsgKey.ER_WF_REF_TO_UNPARSED_ENT,
                 "Il riferimento entit\u00e0 non analizzata \"&{0};\" non \u00e8 permesso."
             },

             {MsgKey.ER_WF_REF_TO_EXTERNAL_ENT,
                 "Il riferimento all''''entit\u00e0 esterna \"&{0};\" non \u00e8 permesso in un valore attributo."
             },

             {MsgKey.ER_NS_PREFIX_CANNOT_BE_BOUND,
                 "Il prefisso \"{0}\" non pu\u00f2 essere associato allo spazio nome \"{1}\"."
             },

             {MsgKey.ER_NULL_LOCAL_ELEMENT_NAME,
                 "Il nome locale dell''''elemento \"{0}\" \u00e8 null."
             },

             {MsgKey.ER_NULL_LOCAL_ATTR_NAME,
                 "Il nome locale dell''''attributo \"{0}\" \u00e8  null."
             },

             { MsgKey.ER_ELEM_UNBOUND_PREFIX_IN_ENTREF,
                 "Il testo di sostituzione del nodo di entit\u00e0 \"{0}\" contiene un nodo di elemento \"{1}\" con un prefisso non associato \"{2}\"."
             },

             { MsgKey.ER_ATTR_UNBOUND_PREFIX_IN_ENTREF,
                 "Il testo di sostituzione del nodo di entit\u00e0 \"{0}\" contiene un nodo di attributo \"{1}\" con un prefisso non associato \"{2}\"."
             },

        };

        return contents;
    }
}
