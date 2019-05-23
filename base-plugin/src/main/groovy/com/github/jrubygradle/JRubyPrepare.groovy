package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 */
@CompileStatic
class JRubyPrepare extends DefaultTask {

    JRubyPrepare() {
        outputs.dir({ JRubyPrepare t -> new File(t.getOutputDir(), 'gems') }.curry(this))
    }

    /** Target directory for GEMs. Extracted GEMs should end up in {@code outputDir + "/gems"}
     */
    File getOutputDir() {
        project.file(this.outputDir)
    }

    @InputFiles
    private FileCollection gemsAsFileCollection() {
        return GemUtils.getGems(project.files(this.dependencies))
    }

    /** Sets the output directory
     *
     * @param f Output directory
     */
    void outputDir(Object f) {
        this.outputDir = f
    }

    /** Sets the output directory
     *
     * @param f Output directory
     */
    void setOutputDir(Object f) {
        outputDir = f
    }

    @Internal
    List<Object> dependencies = []

    @Optional
    /** Adds dependencies from the given configuration to be prepared
     *
     * @param f A file, directory, configuration or list of gems
     */
    void dependencies(Object f) {
        this.dependencies.add(f)
    }

    @TaskAction
    void copy() {
        /* XXX: This is a bad idea, relying on the fact that 'jrubyExec' has JRuby inside
         * is not a guarantee (pretty close though)
         */
        File out = getOutputDir()
        File jrubyJar = JRubyExecUtils.jrubyJar(project.configurations.findByName(JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG))
        GemUtils.extractGems(project, jrubyJar, gemsAsFileCollection(), out, GemUtils.OverwriteAction.SKIP)

        if (!dependencies.isEmpty()) {
            dependencies.each {
                if (it instanceof Configuration) {
                    GemUtils.setupJars(it, out, GemUtils.OverwriteAction.SKIP)
                }
            }
        }
    }

    private Object outputDir = {
        project.extensions.getByType(JRubyPluginExtension).gemInstallDir
    }
}

