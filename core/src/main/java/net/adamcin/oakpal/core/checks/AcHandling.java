/*
 * Copyright 2018 Mark Adamcin
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

package net.adamcin.oakpal.core.checks;

import static java.util.Optional.ofNullable;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.CLEAR;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.IGNORE;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.MERGE;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.MERGE_PRESERVE;
import static org.apache.jackrabbit.vault.fs.io.AccessControlHandling.OVERWRITE;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import net.adamcin.oakpal.core.PackageCheck;
import net.adamcin.oakpal.core.PackageCheckFactory;
import net.adamcin.oakpal.core.SimplePackageCheck;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;
import org.json.JSONObject;

/**
 * Limit package {@code acHandling} mode to prevent unforeseen changes to ACLs upon installation.
 * <p>
 * Example config using {@link ACHandlingLevelSet#ONLY_IGNORE}:
 * <pre>
 *     "config": {
 *         "levelSet": "only_ignore"
 *     }
 * </pre>
 * <p>
 * Example config requiring {@link AccessControlHandling#MERGE_PRESERVE}:
 * <pre>
 *     "config": {
 *         "allowedModes": ["merge_preserve"]
 *     }
 * </pre>
 */
public class AcHandling implements PackageCheckFactory {
    public static final String CONFIG_ALLOWED_MODES = "allowedModes";
    public static final String CONFIG_LEVEL_SET = "levelSet";

    /**
     * Encapsulation of incrementally wider sets of forbidden acHandling modes as discrete levels.
     */
    public enum ACHandlingLevelSet {
        /**
         * Marker set for explicit enumeration in {@code allowedModes} array.
         */
        EXPLICIT(Collections.emptyList()),

        /**
         * Allow all acHandling modes except for {@link AccessControlHandling#CLEAR}.
         */
        NO_CLEAR(Collections.singletonList(CLEAR)),

        /**
         * (Default levelSet) Prevent blindly destructive modes. Package can still overwrite existing permissions for
         * any principal identified in ACEs included in the package
         */
        NO_UNSAFE(Arrays.asList(CLEAR, OVERWRITE)),

        /**
         * Allow only {@link AccessControlHandling#MERGE_PRESERVE} or {@link AccessControlHandling#IGNORE}. This results
         * in only allowing additive ACE changes.
         */
        ONLY_ADD(Arrays.asList(CLEAR, OVERWRITE, MERGE)),

        /**
         * Prevent any ACL changes by requiring {@link AccessControlHandling#IGNORE}.
         */
        ONLY_IGNORE(Arrays.asList(CLEAR, OVERWRITE, MERGE, MERGE_PRESERVE));

        private final List<AccessControlHandling> forbiddenModes;

        ACHandlingLevelSet(final List<AccessControlHandling> forbiddenModes) {
            this.forbiddenModes = forbiddenModes;
        }

        public List<AccessControlHandling> getForbiddenModes() {
            return forbiddenModes;
        }
    }

    class Check extends SimplePackageCheck {
        final ACHandlingLevelSet levelSet;
        final List<AccessControlHandling> allowedModes;

        public Check(final ACHandlingLevelSet levelSet,
                     final List<AccessControlHandling> allowedModes) {
            this.levelSet = levelSet;
            this.allowedModes = allowedModes;
        }

        @Override
        public String getCheckName() {
            return AcHandling.this.getClass().getSimpleName();
        }

        @Override
        public void beforeExtract(final PackageId packageId, final PackageProperties packageProperties,
                                  final MetaInf metaInf, final List<PackageId> subpackages) {
            if (this.levelSet == null) {
                return;
            }

            AccessControlHandling packageMode = ofNullable(packageProperties.getACHandling()).orElse(IGNORE);
            if (this.levelSet == ACHandlingLevelSet.EXPLICIT) {
                if (!allowedModes.contains(packageMode)) {
                    reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("acHandling mode %s is forbidden. acHandling values in allowedModes are %s",
                                    packageMode, allowedModes), packageId));
                }
            } else {
                if (this.levelSet.getForbiddenModes().contains(packageMode)) {
                    reportViolation(new SimpleViolation(Violation.Severity.MAJOR,
                            String.format("acHandling mode %s is forbidden. forbidden acHandling values in levelSet:%s are %s",
                                    packageMode, this.levelSet.name().toLowerCase(), this.levelSet.getForbiddenModes())));
                }
            }
        }
    }

    @Override
    public PackageCheck newInstance(final JSONObject config) throws Exception {
        if (config.has(CONFIG_ALLOWED_MODES)) {
            List<String> jsonAllowedModes = StreamSupport
                    .stream(config.getJSONArray(CONFIG_ALLOWED_MODES).spliterator(), false)
                    .map(String::valueOf).collect(Collectors.toList());
            List<AccessControlHandling> allowedModes = new ArrayList<>();
            for (String mode : jsonAllowedModes) {
                allowedModes.add(AccessControlHandling.valueOf(mode.toUpperCase()));
            }
            return new Check(ACHandlingLevelSet.EXPLICIT, allowedModes);
        } else if (config.has(CONFIG_LEVEL_SET)) {
            ACHandlingLevelSet levelSet = ACHandlingLevelSet.valueOf(config.getString(CONFIG_LEVEL_SET).toUpperCase());
            return new Check(levelSet, Collections.emptyList());
        } else {
            return new Check(ACHandlingLevelSet.NO_UNSAFE, Collections.emptyList());
        }
    }
}
