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

package net.adamcin.oakpal.maven.checks;

import java.util.List;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import net.adamcin.oakpal.core.SimplePackageCheck;
import net.adamcin.oakpal.core.SimpleViolation;
import net.adamcin.oakpal.core.Violation;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackageProperties;

public class SimpleJavaCheck extends SimplePackageCheck {

    @Override
    public void beforeExtract(final PackageId packageId,
                              final PackageProperties packageProperties,
                              final MetaInf metaInf,
                              final List<PackageId> subpackages) {

        reportViolation(new SimpleViolation(Violation.Severity.MINOR,
                packageProperties.getACHandling().toString(), packageId));
    }
}
