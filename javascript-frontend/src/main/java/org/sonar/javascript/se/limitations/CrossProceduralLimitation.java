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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sonar.javascript.se.Constraint;
import org.sonar.javascript.se.ProgramState;
import org.sonar.javascript.se.sv.LogicalNotSymbolicValue;
import org.sonar.javascript.se.sv.SymbolicValue;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.tree.Tree;
import org.sonar.plugins.javascript.api.tree.expression.BinaryExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.CallExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ConditionalExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.ExpressionTree;
import org.sonar.plugins.javascript.api.tree.expression.IdentifierTree;
import org.sonar.plugins.javascript.api.tree.statement.DoWhileStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.ForStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.IfStatementTree;
import org.sonar.plugins.javascript.api.tree.statement.WhileStatementTree;
import org.sonar.plugins.javascript.api.visitors.DoubleDispatchVisitor;

public class CrossProceduralLimitation {

  public ProgramState prepareForBranching(Tree branchingTree, ProgramState currentState) {
    if (conditionIsAmbiguous(currentState)) {
      return dropConstraintsOnSymbols(currentState, retrieveFunctionArgumentSymbolsFromCondition(branchingTree));
    } else {
      return currentState;
    }
  }

  private ProgramState dropConstraintsOnSymbols(ProgramState incomingState, Set<Symbol> involvedSymbols) {
    ProgramState currentState = incomingState;
    for (Symbol symbol : involvedSymbols) {
      currentState = currentState.newSymbolicValue(symbol, Constraint.ANY_VALUE);
    }
    return currentState;
  }

  private static boolean conditionIsAmbiguous(ProgramState currentState) {
    final SymbolicValue value = currentState.peekStack();
    if (value instanceof LogicalNotSymbolicValue) {
      return currentState.getConstraint(((LogicalNotSymbolicValue) value).negatedValue()).equals(Constraint.ANY_VALUE);
    }
    return currentState.getConstraint(value).equals(Constraint.ANY_VALUE);
  }

  private Set<Symbol> retrieveFunctionArgumentSymbolsFromCondition(Tree branchingTree) {
    return ConditionRetriever.condition(branchingTree).map(FunctionArgumentsCollector::collect).orElse(new HashSet<>());
  }

  static class ConditionRetriever extends DoubleDispatchVisitor {

    private ExpressionTree condition;

    public static Optional<ExpressionTree> condition(Tree branchingTree) {
      final ConditionRetriever collector = new ConditionRetriever();
      branchingTree.accept(collector);
      return collector.condition();
    }

    public Optional<ExpressionTree> condition() {
      return Optional.ofNullable(condition);
    }

    @Override
    public void visitIfStatement(IfStatementTree tree) {
      condition = tree.condition();
    }

    @Override
    public void visitConditionalExpression(ConditionalExpressionTree tree) {
      condition = tree.condition();
    }

    @Override
    public void visitWhileStatement(WhileStatementTree tree) {
      condition = tree.condition();
    }

    @Override
    public void visitDoWhileStatement(DoWhileStatementTree tree) {
      condition = tree.condition();
    }

    @Override
    public void visitForStatement(ForStatementTree tree) {
      condition = tree.condition();
    }

    @Override
    public void visitBinaryExpression(BinaryExpressionTree tree) {
      if (tree.is(Tree.Kind.CONDITIONAL_AND, Tree.Kind.CONDITIONAL_OR)) {
        condition = tree;
      }
      super.visitBinaryExpression(tree);
    }

  }

  static class FunctionArgumentsCollector extends DoubleDispatchVisitor {

    private Set<Symbol> symbols = new HashSet<>();

    public static Set<Symbol> collect(Tree tree) {
      final FunctionArgumentsCollector collector = new FunctionArgumentsCollector();
      tree.accept(collector);
      collector.symbols.remove(null);
      return collector.symbols;
    }

    @Override
    public void visitCallExpression(CallExpressionTree tree) {
      for (Tree parameter : tree.arguments().parameters()) {
        if (parameter instanceof IdentifierTree) {
          symbols.add(((IdentifierTree) parameter).symbol());
        }
      }
      super.visitCallExpression(tree);
    }
  }
}
