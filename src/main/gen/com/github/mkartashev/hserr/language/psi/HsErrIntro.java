// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;

public interface HsErrIntro extends PsiElement {

  @NotNull
  List<HsErrToken> getTokenList();

  @NotNull
  String getName();

  @NotNull
  ItemPresentation getPresentation();

}
