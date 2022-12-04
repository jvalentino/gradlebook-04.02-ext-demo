package com.blogspot.jvalentino.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification
import spock.lang.Subject

class ExtDemoPluginTestSpec extends Specification {

    Project project
    @Subject
    ExtDemoPlugin plugin

    def setup() {
        project = ProjectBuilder.builder().build()
        plugin = new ExtDemoPlugin()
    }

    void "test plugin"() {
        when:
        plugin.apply(project)

        then:
        project.tasks.getAt(0).toString() == "task ':add'"
        project.getExtensions().findByType(HelloExtension) != null
    }
}
