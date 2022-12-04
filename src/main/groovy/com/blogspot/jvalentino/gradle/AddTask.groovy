package com.blogspot.jvalentino.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * <p>A Task.</p>
 * @author jvalentino2
 */
@SuppressWarnings(['Println'])
class AddTask extends DefaultTask {

    String group = 'demo'
    String description = 'Adds some numbers together'
    AddTask instance = this

    @TaskAction
    void perform() {
        HelloExtension ex =
                instance.project.extensions.findByType(HelloExtension)
        ex.sum = ex.alpha + ex.bravo
        println "${ex.alpha} + ${ex.bravo} = ${ex.sum}"
    }
}
