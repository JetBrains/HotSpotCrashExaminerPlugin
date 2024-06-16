// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.mkartashev.hserr.language.psi.HsErrTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.github.mkartashev.hserr.language.psi.*;
import com.intellij.navigation.ItemPresentation;

public class HsErrSubsectionImpl extends ASTWrapperPsiElement implements HsErrSubsection {

  public HsErrSubsectionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HsErrVisitor visitor) {
    visitor.visitSubsection(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HsErrVisitor) accept((HsErrVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HsErrToken> getTokenList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HsErrToken.class);
  }

  @Override
  @Nullable
  public String getName() {
    return HsErrPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public ItemPresentation getPresentation() {
    return HsErrPsiImplUtil.getPresentation(this);
  }

}
