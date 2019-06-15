/*
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.internal.gems

import com.github.jrubygradle.api.gems.GemVersion
import com.github.jrubygradle.api.gems.GemGroups
import groovy.transform.PackageScope
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.github.jrubygradle.api.gems.GemVersion.gemVersionFromGradleIvyRequirement

/**
 * Resolver to compute gem versions
 *
 * @author Schalk W. Cronjé
 * @author Christian Meier
 *
 * @since 2.0
 */
class GemVersionResolver {
    static void addGemResolver(Configuration cfg, GemGroups gemGroups, GemVersionResolver versionResolver) {
        Action<DependencyResolveDetails> gemResolveRule = {
            GemGroups gemgrp, GemVersionResolver resolver, DependencyResolveDetails drd ->
                if (gemgrp.isGemGroup(drd.requested.group)) {
                    resolver.resolve(drd)
                }
        }.curry(gemGroups, versionResolver)
        cfg.resolutionStrategy.eachDependency(gemResolveRule)
    }

    GemVersionResolver(GemGroups gemGroups, Logger logger, Configuration configuration) {
        this.gemGroups = gemGroups
        this.logger = logger
        this.configuration = configuration
    }

    void resolve(DependencyResolveDetails details) {
        if (versions == null) {
            firstRun()
        }

        logger.debug("${configuration}: gem ${details.requested.name} ${details.requested.version}")

        GemVersion version = versions[details.requested.name]

        if (version != null) {
            GemVersion next = version.intersect(details.requested.version)

            if (next.conflict()) {
                throw new GradleException("there is no overlap for ${versions[details.requested.name]} and ${details.requested.version}")
            }
            versions[details.requested.name] = next

            logger.debug("${configuration}      collected ${version}")
            logger.debug("${configuration}      resolved  ${next}")

            details.useVersion(next.toString())
        } else {
            GemVersion next = gemVersionFromGradleIvyRequirement(details.requested.version)
            versions[details.requested.name] = next
            logger.debug("${configuration}      nothing collected")
            logger.debug("${configuration}      resolved  ${next}")
        }
    }

    String toString() {
        return "GemVersionResolver${versions}"
    }

    protected void firstRun() {
        debugWithSeparator('collect version range info')
        Configuration config = configuration.copyRecursive()
        versions.clear()
        addGemResolver(config, gemGroups, this)
        config.resolvedConfiguration
        debugWithSeparator('apply version range info')
    }

    protected GemVersionResolver() {
        this.logger = Logging.getLogger(GemVersionResolver)
    }

    private void debugWithSeparator(final String text) {
        logger.debug(
            "${configuration.name}\n${DBG_SEPARATOR}\n" +
                "                       ${text}\n${DBG_SEPARATOR}"
        )
    }

    @PackageScope
    static final GemVersionResolver NULL_RESOLVER = new GemVersionResolver()

    private static final String DBG_SEPARATOR = '                       ------------------------'
    private final Map versions = [:]
    private final Configuration configuration
    private final Logger logger
    private final GemGroups gemGroups
}
