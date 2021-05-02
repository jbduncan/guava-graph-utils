package org.jbduncan.rewrite

import org.junit.jupiter.api.Test
import org.openrewrite.Parser
import org.openrewrite.Recipe
import org.openrewrite.RecipeTest
import org.openrewrite.java.JavaParser

class SayHelloRecipeTests : RecipeTest {
  override val parser: Parser<*>
    get() = JavaParser.fromJavaVersion().build()
  override val recipe: Recipe
    get() = SayHelloRecipe().apply { fullyQualifiedClassName = "com.yourorg.A" }

  @Test
  fun addsHelloToA() =
      assertChanged(
          before =
              """
                package com.yourorg;
                
                class A {
                }
              """.trimIndent(),
          after =
              """
                package com.yourorg;
                
                class A {
                    public String hello() {
                        return "Hello from com.yourorg.A!";
                    }
                }
              """.trimIndent())

  @Test
  fun doesNotChangeExistingHello() =
      assertUnchanged(
          before =
              """
                package com.yourorg;
    
                class A {
                    public String hello() { return ""; }
                }
              """.trimIndent())

  @Test
  fun doesNotChangeOtherClass() =
      assertUnchanged(
          before =
              """
                package com.yourorg;
    
                class B {
                }
              """.trimIndent())
}
