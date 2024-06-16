// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class HsErrVisitor extends PsiElementVisitor {

  public void visitIntro(@NotNull HsErrIntro o) {
    visitPsiElement(o);
  }

  public void visitSection(@NotNull HsErrSection o) {
    visitPsiElement(o);
  }

  public void visitSubsection(@NotNull HsErrSubsection o) {
    visitPsiElement(o);
  }

  public void visitToken(@NotNull HsErrToken o) {
    visitNamedElement(o);
  }

  public void visitNamedElement(@NotNull HsErrNamedElement o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
