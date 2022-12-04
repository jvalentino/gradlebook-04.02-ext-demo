## 4.2 Extensions

Most plugins need to obtain some configuration from the build script. One method for doing this is to use extension objects. The Gradle Project has an associated ExtensionContainer object that contains all the settings and properties for the plugins that have been applied to the project. You can provide configuration for your plugin by adding an extension object to this container. An extension object is simply a Java Bean compliant class. Groovy is a good language choice to implement an extension object because plain old Groovy objects contain all the getter and setter methods that a Java Bean requires.

\-    https://docs.gradle.org/current/userguide/custom_plugins.html#sec:getting_input_from_the_build

The purpose of this project is to demonstrate how to create an extension and how to unit test it, using a the third method for manual plugin testing. If you are running a version of Gradle prior to version 3.1, the other methods for manual plugin testing must be used. Of specific interest is the methodology for unit testing, which relies in an internal Gradle mechanism for instantiating the Extension via the text fixture project.

 

#### build.gradle

```properties
version = '1.0.0'
group = 'com.blogspot.jvalentino.gradle'
archivesBaseName = 'ext-demo'
```

The standard build is used, which includes JaCoCo, Codenarc, and and handles publishing to local Artifactory.

#### src/main/groovy/com/blogspot/jvalentino/gradle/HelloExtension.groovy

```groovy
package com.blogspot.jvalentino.gradle

/**
 * <p>A basic extension</p>
 * @author jvalentino2
 */
class HelloExtension {
    int alpha = 1
    int bravo = 2
    int sum = 0
}
```

An Extension is just a class, which can contain any number and variety of variables. It is the association within the plugin class that turns this class into an Extension.

 

#### src/main/groovy/com/blogspot/jvalentino/gradle/ExtDemoPlugin.groovy

```groovy
package com.blogspot.jvalentino.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>A basic gradle plugin.</p>
 * @author jvalentino2
 */
class ExtDemoPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.extensions.create 'hello', HelloExtension
        project.task('add', type:AddTask)
    }
}
```

**Line 12: The extension**

The association of the extension class and its instance includes the name that it will be referred to in the plugin test. The keyword “hello” will be available within the body of the consuming build.gradle.

 

#### src/main/groovy/com/blogspot/jvalentino/gradle/AddTask.groovy

```groovy
@SuppressWarnings(['Println'])
class AddTask extends DefaultTask {

    String group = 'demo'
    String description = 'Adds some numbers together'

    @TaskAction
    void perform() {
        HelloExtension ex = 
            project.extensions.findByType(HelloExtension)
        ex.sum = ex.alpha + ex.bravo
        println "${ex.alpha} + ${ex.bravo} = ${ex.sum}"
    }
}
```

**Lines 17-18: Using the extension**

Within the plugin, an extension by class has a single instance. That instance must be accessed via the project instance, in which the extension must be accessed by class type.

 

**Line 19: The function**

The function of this task is to add two numbers together from the extension and place the result back in the extension in another variable.

 

#### plugin-tests/local/build.gradle

```groovy
buildscript {
  repositories {
	jcenter()
  }
  dependencies {
    classpath 'com.blogspot.jvalentino.gradle:ext-demo:1.0.0'
  }
}

apply plugin: 'ext-demo'

hello {
    alpha = 5
    bravo = 6
}
```

**Lines 12: The extension by name**

The plugin class made the association of the name of “hello” to the backing class of **HelloExtension**.

 

**Lines 13-14: The values**

Sets the values to be used for the **alpha** and bravo **member** variables of the **HelloExtention** instance.

 

#### plugin-tests/local/setings.gradle

```groovy
rootProject.name='demo'
includeBuild '../../'
```

References the project that contains the plugin, so that we don’t have to manually construct the JAR and/or POM to test it.

 

#### Manual Testing

```bash
plugin-tests/local$ ./gradlew add

> Task :add 
5 + 6 = 11


BUILD SUCCESSFUL
```

Executing the task from the command-line shows that the values for **alpha** and **bravo** were taking from the build.gradle’s hello extension.

 

### 4.2.1 Unit Testing using project.apply

 

#### src/main/groovy/com/blogspot/jvalentino/gradle/ExtDemoPluginTestSpec.groovy

```groovy
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
```

**Lines 11-18: Standard setup**

The **project** instance and the subject of the test are best kept as member variables, as they will likely be needed in every test. The **ProjectBuilder** is required in to construct the **Project** instance.

 

**Lines 20-25: Standard plugin task test**

After calling the plugin’s apply method, it can be asserted that **add** task was added.

 

**Line 26: Verify the extension creation**

The project extension variable, after the plugin instance, can be used to assert that the appropriate extension was created, by looking it up by class type. This is the same way that the task gains access to the specific extension instance.

 

#### src/main/groovy/com/blogspot/jvalentino/gradle/AddTaskTestSpec.groovy

```groovy
package com.blogspot.jvalentino.gradle

import org.gradle.api.Project
import org.gradle.internal.impldep.com.google.common.collect.ImmutableMap
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification
import spock.lang.Subject

class AddTaskTestSpec extends Specification {

    @Subject
    AddTask task
    Project p
    HelloExtension extension
    
    def setup() {
        p = ProjectBuilder.builder().build()
        p.apply(ImmutableMap.of("plugin", ExtDemoPlugin.class))
        task = p.tasks['add']
        extension = p.extensions.findByType(HelloExtension)
    }

```

**Lines 12-15: Standard task setup**

The subject of the test along with the project instance and extension will likely be needed in every test case. 

 

**Line 18: The project instance**

The **ProjectBuilder** must be used to get an instance of **Project**. That instance of **Project** is then needed to construct other test fixtures.

 

**Line 19: Using Gradle internals to create the extension**

In older version of Gradle, the **task.project** member variable was not read-only, and developers could directly mock the **Project** instance to return whatever was needed. The removal of this ability to directly mock **Project** required some creative solutions to drive tests, and one of these solutions was to use the **PluginAware** interface (https://docs.gradle.org/current/javadoc/org/gradle/api/plugins/PluginAware.html) via the **Project** instance, to cause the plugin to get created along with the extension instance. The result is that **project.extensions** becomes populated with all of the listed extension declarations.

 

**Line 20: The task instance**

The previous method of task instantiation using **project.task** can no longer be used, because the task of name “add” already exists as a result of Line 19. Since the task already exists though, the instance can be obtained via **projects.tasks**.

 

**Line 21: The extension instance**

Another result of Line 19 is that the instance of the extension is available, via the same mechanism that the task used.

```groovy
    void "Test peform (default)"() {
        when:
        task.perform()
        
        then:
        extension.sum == 3
    }
    
    void "Test peform with some values"() {
        given:
        extension.alpha = 3
        extension.bravo = 4
        
        when:
        task.perform()
        
        then:
        extension.sum == 7
    }


```

**Lines 24-30: Testing the default extension values**

The purpose of this test is to verify the default values for **alpha** and **bravo** within the extension. This equates to what happen if the using **build.gradle** does not define **alpha** and **bravo** in the **hello** extension.

 

**Lines 32-41: Testing the extension with different values**

The purpose of this test is show what happens when the using **build.gradle** changes the values of **alpha** and **bravo** in the extension.

 

### 4.2.2 Unit Testing using mocks

Using the **project.apply** method for instantiating a plugin within the **Project** instance, may not always be an option. Reasons can range from the plugin class requiring special logic handling that you don’t want to couple with the logic of testing an individual task, to dealing with **Project** variables that are impossible to change. 

Dealing with manipulating the **Project** instance also isn’t the only concern in the interactions that can occur within a Gradle plugin. The File System, other plugins, and other services can be involved. If one were to use the actual instances of these interactions, the test would stop being a unit test and become either an integration or functional test. The objective of a unit test is to test method level (or class level) behavior, which requires mocking these external class interactions.

The concept of mocking applies to Gradle plugins and tasks as well. For the purpose of unit testing, external interactions should be mocked. All that needs to be determined is how to accomplish that mocking. By replacing the needed external interfaces with Spock Mock objects, their behavior can be directly controlled at the method level, without having to rely on the complexities of the Gradle or other Test Fixture frameworks.

 

#### build.gradle

```groovy
dependencies {
    compile gradleApi()
    compile 'org.codehaus.groovy:groovy-all:2.4.12'
    
    testCompile 'org.spockframework:spock-core:1.1-groovy-2.4'
    testCompile 'cglib:cglib:3.2.6'
    testCompile 'org.objenesis:objenesis:2.6'
}

```

**Lines 47-48: New Dependencies**

To use the mocking abilities of Spock, specifically **Mock** and later **GroovyMock**, dependencies for CGLIB and Objenesis must be added.

 

#### src/main/groovy/com/blogspot/jvalentino/gradle/AddTask.groovy

```groovy
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

```

**Line 15: Replacing “this”**

An issue with the **Task** class is the general dependency on member variables and methods such as **project**, which are read-only or that cannot be directly used without requiring special knowledge and handling. A mechanism for handling the mocking of internal calls, is accomplished by using a member variable for representing the reference to “this”, which can be substituted by the test class with a mock.

 

**Line 20: The change**

Instance of referring to the Project instance using **this.project**, **instance.project** is used instead. While when running as a task within the applying build.gradle the **instance** member variable simply references “this”, when in the test is will be a Mock of the task class. This allows the test case to control the behavior of **project**, which is a shortcut to **getProject().**

 

#### src/test/groovy/com/blogspot/jvalentino/gradle/AddTaskTestSpecMethod2.groovy

```groovy
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

```

**Lines 3-10: Imports**

General imports for performing the test and its mocking.

 

**Line 15: The test subject**

The subject of the test will be used in every test case, so it is best to keep it as a member variable.

 

**Lines 16-17: The project and extension**

The **Project** instance will additionally be needed in every test case, as well as the extension instance that will now have to be manually instantiated.

 

**Line 18: The extension container**

The **ExtensionContainer** class is the implementation behind **project.extensions**. To mock **project.extensions.findByType**, **project.extensions** must first be mocked.

 

**Lines 21-22: Task instantiation**

The **ProjectBuilder** fixture bust me used to instantiate a **Project** instance, so that it can then be used to create an instance of the **AddTask** class to be the subject of the test. 

 

**Line 23: The mock of “this”**

The setting of the **instance** member variable occurs here, so that all calls to **instance** within the task under test now refer to a mock.

 

**Line 24: Mocking the project**

**ProjectInternal** is the actual implementation class behind a task’s project **member** variable. If you don’t use this class, you will get a **ClassCastException**. This will be the mock by which we are able to return a mocked instance of the **ExtensionContainer**.

 

**Line 25: The extension instance**

The extension instance needs to be manually instantiated, as we are no longer relying on the plugin class to handle its instantiation via the **Project** class. 

 

**Line 26: The extension container**

In older versions of Gradle, **ExtensionContainer** can be directly mocked. However, after Gradle 3.2 one has to mock **ExtensionContainerInternal**. The intention of mocking this class is to that it can be provided as the result of **project.getExtensions()**, so that we can return our instance of the extension.

```groovy
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

```

**Line 33: Then came mocking**

Within the **then** clause, Spock can use statements in the following form to tell a Mock object to both expect a call to a method, and what that method is to return if wanted:

 

Number of method calls * mock object.method(parameters) >> return object

 

As an additional note, in Groovy it is not necessary to call getter and setting method for public member variable access.

 

object.variable = “foo”

 

…is the same as:

 

object.setVariable(“foo”)

 

and…

 

object.variable

 

…is the same as

 

object.getVariable()

 

**Line 34: The project mock**

The purpose of this line is to account for the first part of Line 20 of AddTask.groovy, specifically:

 

instance.project

 

Since the **instance** variable is a mock of **AddTask**, this mocking statement says that when **instance.project** is called, we return the **project** variable. That project variable is our mock of **ProjectInternal**.

 

**Line 35: The extension container**

The purpose of this line is to account for the next part of Line 20 of AddTask.groovy, specifically:

 

instance.project.**extensions**

 

Since the **project** variable is a mock, we tell it to return the mock instance of **ExtensionContainerInternal** when **project.extensions** is called.

 

**Line 36: The extension**

The purpose of this line is to account for the rest of Line 20 of AddTask.groovy, specifically:

 

instance.project.extensions.**findByType(HelloExtension)**

 

Since the **extensions** variable is a mock, we tell it to return the instance of **HelloExtension** when **extensions.findByType** is called with the parameter of **HelloExtension**.