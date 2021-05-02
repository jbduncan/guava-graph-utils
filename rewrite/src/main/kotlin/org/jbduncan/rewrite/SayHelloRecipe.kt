package org.jbduncan.rewrite

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.Recipe
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J

class SayHelloRecipe : Recipe() {

  @field:Option(
      displayName = "Fully Qualified Class Name",
      description = "A fully-qualified class name indicating which class to add a hello() method.",
      example = "com.yourorg.FooBar")
  internal lateinit var fullyQualifiedClassName: String

  override fun getDisplayName(): String {
    return "Say Hello"
  }

  override fun getDescription(): String {
    return """Adds a "hello" method to the specified class"""
  }

  override fun getVisitor(): JavaIsoVisitor<ExecutionContext> {
    return SayHelloVisitor()
  }

  inner class SayHelloVisitor : JavaIsoVisitor<ExecutionContext>() {

    private val helloTemplate =
        template("public String hello() { return \"Hello from #{}!\"; }").build()

    override fun visitClassDeclaration(
        classDecl: J.ClassDeclaration,
        executionContext: ExecutionContext
    ): J.ClassDeclaration {
      // In any visit() method the call to super() is what causes sub-elements of to be visited
      var cd = super.visitClassDeclaration(classDecl, executionContext)

      if (classDecl.type == null || classDecl.type?.fullyQualifiedName != fullyQualifiedClassName) {
        // We aren't looking at the specified class so return without making any modifications
        return cd
      }

      // Check if the class already has a method named "hello" so we don't incorrectly add a second
      // "hello" method
      val helloMethodExists =
          classDecl //
              .body
              .statements
              .asSequence()
              .filterIsInstance<J.MethodDeclaration>()
              .any { methodDeclaration -> methodDeclaration.name.simpleName == "hello" }
      if (helloMethodExists) {
        return cd
      }

      // Interpolate the fullyQualifiedClassName into the template and use the resulting AST to
      // update the class body
      cd =
          cd.withBody(
              cd.body.withTemplate(
                  helloTemplate, cd.body.coordinates.lastStatement(), fullyQualifiedClassName))

      return cd
    }
  }
}
