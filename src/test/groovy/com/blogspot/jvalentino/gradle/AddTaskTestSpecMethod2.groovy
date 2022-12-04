package com.blogspot.jvalentino.gradle

import org.gradle.api.Project
import org.gradle.api.internal.plugins.ExtensionContainerInternal
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification
import spock.lang.Subject

class AddTaskTestSpecMethod2 extends Specification {

    @Subject
    AddTask task
    Project project
    HelloExtension extension
    ExtensionContainer extensions
    
    def setup() {
        Project p = ProjectBuilder.builder().build()
        task = p.task('add', type: AddTask)
        task.instance = Mock(AddTask)
        project = Mock(ProjectInternal)
        extension = new HelloExtension()
        extensions = Mock(ExtensionContainerInternal)
    }
    
    void "Test peform (default)"() { 
        when:
        task.perform()
        
        then:
        1 * task.instance.project >> project
        1 * project.extensions >> extensions
        1 * extensions.findByType(HelloExtension) >> extension
        
        and:
        extension.sum == 3
    }
    
    void "Test peform with some values"() {
        given:
        extension.alpha = 3
        extension.bravo = 4
        
        when:
        task.perform()
        
        then:
        1 * task.instance.project >> project
        1 * project.extensions >> extensions
        1 * extensions.findByType(HelloExtension) >> extension
        
        and:
        extension.sum == 7
    }
}
