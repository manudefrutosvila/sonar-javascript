/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.javascript.se.limitations;

import com.sonar.sslr.api.typed.ActionParser;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.sonar.javascript.parser.JavaScriptParserBuilder;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.expression.LiteralTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.javascript.se.limitations.CrossProceduralLimitation.ConditionRetriever;

public class ConditionRetrieverTest {

  private ActionParser<Tree> parser;

  @Before
  public void setUp() throws Exception {
    parser = JavaScriptParserBuilder.createParser();
  }

  @Test
  public void should_retrieve_condition_from_if() throws Exception {
    Optional<ExpressionTree> condition = ConditionRetriever.condition(parser.parse("if(a){}"));
    assertThat(((IdentifierTree) condition.get()).name()).isEqualTo("a");
  }

  @Test
  public void should_retrieve_condition_from_conditional_expression() throws Exception {
    Optional<ExpressionTree> condition = ConditionRetriever.condition(parser.parse("e ? 1 : 0"));
    assertThat(((IdentifierTree) condition.get()).name()).isEqualTo("e");
  }

  @Test
  public void should_retrieve_condition_from_loops() throws Exception {
    Optional<ExpressionTree> condition = ConditionRetriever.condition(parser.parse("while(true){}"));
    assertThat(((LiteralTree) condition.get()).value()).isEqualTo("true");

    condition = ConditionRetriever.condition(parser.parse("do{}while('')"));
    assertThat(((LiteralTree) condition.get()).value()).isEqualTo("''");

    condition = ConditionRetriever.condition(parser.parse("for(var i; i < 3; i++){}"));
    assertThat(condition.get().toString().trim()).isEqualTo("i < 3");
  }

  @Test
  public void should_retrieve_nothing_from_degraded_for() throws Exception {
    Optional<ExpressionTree> condition = ConditionRetriever.condition(parser.parse("for(var x;;){}"));
    assertThat(condition.isPresent()).isFalse();
  }

  @Test
  public void should_retrieve_composite_condition() throws Exception {
    Optional<ExpressionTree> condition = ConditionRetriever.condition(parser.parse("if(a || b){}"));
    assertThat(condition.get().toString().trim()).isEqualTo("a || b");

    condition = ConditionRetriever.condition(parser.parse("a && b"));
    assertThat(condition.get().toString().trim()).isEqualTo("a && b");

    condition = ConditionRetriever.condition(parser.parse("a || b"));
    assertThat(condition.get().toString().trim()).isEqualTo("a || b");
  }

  @Test
  public void should_retrieve_nothing_from_non_conditional_statement() throws Exception {
    Optional<ExpressionTree> condition = ConditionRetriever.condition(parser.parse("a = 3"));
    assertThat(condition.isPresent()).isFalse();
  }

}
