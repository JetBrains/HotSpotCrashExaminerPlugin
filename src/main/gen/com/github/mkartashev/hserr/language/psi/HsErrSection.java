// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.ItemPresentation;

public interface HsErrSection extends PsiElement {

  @NotNull
  List<HsErrSubsection> getSubsectionList();

  @NotNull
  List<HsErrToken> getTokenList();

  @Nullable
  String getName();

  @NotNull
  ItemPresentation getPresentation();

}
