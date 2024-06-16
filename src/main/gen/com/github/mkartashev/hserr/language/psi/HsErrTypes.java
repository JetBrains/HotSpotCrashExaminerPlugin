// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language.psi;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.PsiElement;
import com.intellij.lang.ASTNode;
import com.github.mkartashev.hserr.language.HsErrElementType;
import com.github.mkartashev.hserr.language.psi.impl.*;

public interface HsErrTypes {

  IElementType INTRO = new HsErrElementType("INTRO");
  IElementType SECTION = new HsErrElementType("SECTION");
  IElementType SUBSECTION = new HsErrElementType("SUBSECTION");
  IElementType TOKEN = new HsErrElementType("TOKEN");

  IElementType IDENTIFIER = new HsErrTokenType("IDENTIFIER");
  IElementType KEYWORD = new HsErrTokenType("KEYWORD");
  IElementType NUMBER = new HsErrTokenType("NUMBER");
  IElementType PUNCT = new HsErrTokenType("PUNCT");
  IElementType REGISTER = new HsErrTokenType("REGISTER");
  IElementType SECTION_1_0_2_0 = new HsErrTokenType("section_1_0_2_0");
  IElementType SECTION_HDR = new HsErrTokenType("SECTION_HDR");
  IElementType SIGNAL = new HsErrTokenType("SIGNAL");
  IElementType STRING = new HsErrTokenType("STRING");
  IElementType SUBTITLE = new HsErrTokenType("SUBTITLE");
  IElementType URL = new HsErrTokenType("URL");
  IElementType WHITE_SPACE = new HsErrTokenType("WHITE_SPACE");
  IElementType WORD = new HsErrTokenType("WORD");

  class Factory {
    public static PsiElement createElement(ASTNode node) {
      IElementType type = node.getElementType();
      if (type == INTRO) {
        return new HsErrIntroImpl(node);
      }
      else if (type == SECTION) {
        return new HsErrSectionImpl(node);
      }
      else if (type == SUBSECTION) {
        return new HsErrSubsectionImpl(node);
      }
      else if (type == TOKEN) {
        return new HsErrTokenImpl(node);
      }
      throw new AssertionError("Unknown element type: " + type);
    }
  }
}
