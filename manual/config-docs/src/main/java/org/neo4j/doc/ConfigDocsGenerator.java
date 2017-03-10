/*
 * Licensed to Neo Technology under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Neo Technology licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.neo4j.doc;

import org.neo4j.configuration.ConfigValue;
import org.neo4j.kernel.configuration.Config;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ConfigDocsGenerator {

    private static final Pattern CONFIG_SETTING_PATTERN = Pattern.compile( "\\+?[a-z0-9]+((\\.|_)[a-z0-9]+)+\\+?" );
    public static final String IFDEF_HTMLOUTPUT = String.format("ifndef::nonhtmloutput[]%n");
    public static final String IFDEF_NONHTMLOUTPUT = String.format("ifdef::nonhtmloutput[]%n");
    public static final String ENDIF = String.format("endif::nonhtmloutput[]%n%n");
    private final Config config;
    List<SettingDescription> settingDescriptions;
    private PrintStream out;

    public ConfigDocsGenerator() {
        config = Config.serverDefaults();
    }

    public String document(Predicate<ConfigValue> filter) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        out = new PrintStream( baos );
        settingDescriptions = config.getConfigValues().values().stream()
                .filter(filter)
                .sorted((cv1, cv2) -> cv1.name().compareTo(cv2.name()))
                .map(c -> new SettingDescriptionImpl(
                        "config_" + (c.name().replace("(", "").replace(")", "")),
                        c.name(),
                        c.description(),
                        "dummy deprecated description",
                        c.valueDescription(),
                        (c.documentedDefaultValue().isPresent() ? c.documentedDefaultValue() : valueAsString(c)).get(),
                        c.deprecated(),
                        true
//                        c.value(),
//                        c.internal(),
//                        c.replacement()
                ))
                .collect(Collectors.toList());
        out.print(documentSummary(settingDescriptions));
        settingDescriptions.forEach(this::documentForAllOutputs);
        out.flush();
        return baos.toString();
    }

    private Optional<String> valueAsString(ConfigValue configValue) {
        try {
            return Optional.of(String.valueOf(configValue.value().get()));
        } catch (NoSuchElementException ex) {
            System.out.printf("    [x] failed to get value for setting `%s`%n", configValue.name());
            return Optional.empty();
        }
    }

    private String documentSummary(List<SettingDescription> settingDescriptions) {
        return new AsciiDocListGenerator("settings-all", "Settings", true).generateListAndTableCombo(settingDescriptions);
    }

    private void documentForAllOutputs(SettingDescription item) {
        document( item.formatted( (p) -> formatParagraph( item.name(), p, this::settingReferenceForHTML ) )  );
    }

    private void documentForHTML( SettingDescription item ) {
        out.print( IFDEF_HTMLOUTPUT );
        document( item.formatted( (p) -> formatParagraph( item.name(), p, this::settingReferenceForHTML ) )  );
        out.print( ENDIF );
    }

    private void documentForPDF( SettingDescription item ) {
        out.print( IFDEF_NONHTMLOUTPUT );
        document( item.formatted( (p) -> formatParagraph( item.name(), p, this::settingReferenceForPDF ) ) );
        out.print( ENDIF );
    }

    private void document(SettingDescription item) {
        out.printf("[[%s]]%n" +
                        ".%s%n" +
                        "[cols=\"<1h,<4\"]%n" +
                        "|===%n" +
                        "|Description a|%s%n" +
                        "|Valid values a|%s%n",
                item.id(), item.name(),
                item.description().orElse("No description available."), item.validationMessage() );

        if (item.hasDefault()) {
            out.printf("|Default value m|%s%n", item.defaultValue() );
        }

        if (item.isDeprecated()) {
            out.printf( "|Deprecated a|%s%n", item.deprecationMessage() );
            if (item.hasReplacement()) {
                out.printf("|Replaced by a|%s%n", settingReferenceForHTML(item.replacedBy()));
            }
        }
        if (item.isInternal()) {
            out.printf("|Internal a|%s is an internal, unsupported setting.%n", item.name());
        }

        out.printf("|===%n%n");
    }
    private String formatParagraph( String settingName, String paragraph, Function<String, String> renderReferenceToOtherSetting ) {
        return ensureEndsWithPeriod( transformSettingNames( paragraph, settingName, renderReferenceToOtherSetting ) );
    }

    private boolean shouldCreateCrossReference(String candidateSettingName) {
        return settingDescriptions.stream().map(SettingDescription::name).anyMatch(p -> p.equals(candidateSettingName));
    }

    private String transformSettingNames( String text, String settingBeingRendered, Function<String, String> transform ) {
        Matcher matcher = CONFIG_SETTING_PATTERN.matcher( text );
        StringBuffer result = new StringBuffer( 256 );
        while ( matcher.find() ) {
            String match = matcher.group();
            if ( match.endsWith( ".log" ) ) {
                // a filenamne
                match = "_" + match + "_";
            }
            else if ( match.startsWith( "+" ) && match.endsWith( "+" ) ) {
                // marked as passthrough, strip the mark but otherwise do nothing
                match = match.replaceAll( "^\\+|\\+$", "" );
            }
            else if ( match.equals( settingBeingRendered ) ) {
                // don't link to the settings we're describing
                match = "`" + match + "`";
            }
            else if (!shouldCreateCrossReference(match)) {
                // it's not a setting name, so do nothing
            }
            else {
                // If all fall through, assume this key refers to a setting name,
                // and render it as requested by the caller.
                match = transform.apply( match );
            }
            matcher.appendReplacement( result, match );
        }
        matcher.appendTail( result );
        return result.toString();
    }

    private String ensureEndsWithPeriod(String message) {
        if (!message.endsWith( "." ) && !message.endsWith( ". ")) {
            message += ".";
        }
        return message;
    }

    private String settingReferenceForHTML(String settingName) {
        return "<<config_" + settingName + "," + settingName + ">>";
    }

    private String settingReferenceForPDF( String settingName ) {
        return "`" + settingName + "`";
    }

}
