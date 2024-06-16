// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.github.mkartashev.hserr.language.psi.HsErrTypes.*;
import com.github.mkartashev.hserr.language.psi.*;

public class HsErrTokenImpl extends HsErrNamedElementImpl implements HsErrToken {

  public HsErrTokenImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HsErrVisitor visitor) {
    visitor.visitToken(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HsErrVisitor) accept((HsErrVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement setName(@NotNull String name) {
    return HsErrPsiImplUtil.setName(this, name);
  }

  @Override
  @Nullable
  public PsiElement getNameIdentifier() {
    return HsErrPsiImplUtil.getNameIdentifier(this);
  }

}
