// This is a generated file. Not intended for manual editing.
package com.github.mkartashev.hserr.language;

import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import static com.github.mkartashev.hserr.language.psi.HsErrTypes.*;
import static com.intellij.lang.parser.GeneratedParserUtilBase.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.TokenSet;
import com.intellij.lang.PsiParser;
import com.intellij.lang.LightPsiParser;

@SuppressWarnings({"SimplifiableIfStatement", "UnusedAssignment"})
public class HsErrParser implements PsiParser, LightPsiParser {

  public ASTNode parse(IElementType t, PsiBuilder b) {
    parseLight(t, b);
    return b.getTreeBuilt();
  }

  public void parseLight(IElementType t, PsiBuilder b) {
    boolean r;
    b = adapt_builder_(t, b, this, null);
    Marker m = enter_section_(b, 0, _COLLAPSE_, null);
    r = parse_root_(t, b);
    exit_section_(b, 0, m, t, r, true, TRUE_CONDITION);
  }

  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    return parse_root_(t, b, 0);
  }

  static boolean parse_root_(IElementType t, PsiBuilder b, int l) {
    return hserrFile(b, l + 1);
  }

  /* ********************************************************** */
  // WHITE_SPACE|NUMBER|WORD|STRING|PUNCT|SUBTITLE|KEYWORD|SIGNAL|URL|IDENTIFIER|REGISTER
  static boolean all_(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "all_")) return false;
    boolean r;
    r = consumeToken(b, WHITE_SPACE);
    if (!r) r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, WORD);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, PUNCT);
    if (!r) r = consumeToken(b, SUBTITLE);
    if (!r) r = consumeToken(b, KEYWORD);
    if (!r) r = consumeToken(b, SIGNAL);
    if (!r) r = consumeToken(b, URL);
    if (!r) r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, REGISTER);
    return r;
  }

  /* ********************************************************** */
  // (token | WHITE_SPACE)+
  static boolean content(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "content")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = content_0(b, l + 1);
    while (r) {
      int c = current_position_(b);
      if (!content_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "content", c)) break;
    }
    exit_section_(b, m, null, r);
    return r;
  }

  // token | WHITE_SPACE
  private static boolean content_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "content_0")) return false;
    boolean r;
    r = token(b, l + 1);
    if (!r) r = consumeToken(b, WHITE_SPACE);
    return r;
  }

  /* ********************************************************** */
  // intro section* all_*
  static boolean hserrFile(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hserrFile")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = intro(b, l + 1);
    r = r && hserrFile_1(b, l + 1);
    r = r && hserrFile_2(b, l + 1);
    exit_section_(b, m, null, r);
    return r;
  }

  // section*
  private static boolean hserrFile_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hserrFile_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!section(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "hserrFile_1", c)) break;
    }
    return true;
  }

  // all_*
  private static boolean hserrFile_2(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "hserrFile_2")) return false;
    while (true) {
      int c = current_position_(b);
      if (!all_(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "hserrFile_2", c)) break;
    }
    return true;
  }

  /* ********************************************************** */
  // content*
  public static boolean intro(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "intro")) return false;
    Marker m = enter_section_(b, l, _NONE_, INTRO, "<intro>");
    while (true) {
      int c = current_position_(b);
      if (!content(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "intro", c)) break;
    }
    exit_section_(b, l, m, true, false, null);
    return true;
  }

  /* ********************************************************** */
  // SECTION_HDR (subsection | content | )*
  public static boolean section(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "section")) return false;
    if (!nextTokenIs(b, SECTION_HDR)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SECTION_HDR);
    r = r && section_1(b, l + 1);
    exit_section_(b, m, SECTION, r);
    return r;
  }

  // (subsection | content | )*
  private static boolean section_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "section_1")) return false;
    while (true) {
      int c = current_position_(b);
      if (!section_1_0(b, l + 1)) break;
      if (!empty_element_parsed_guard_(b, "section_1", c)) break;
    }
    return true;
  }

  // subsection | content | 
  private static boolean section_1_0(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "section_1_0")) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = subsection(b, l + 1);
    if (!r) r = content(b, l + 1);
    if (!r) r = consumeToken(b, SECTION_1_0_2_0);
    exit_section_(b, m, null, r);
    return r;
  }

  /* ********************************************************** */
  // SUBTITLE [content]
  public static boolean subsection(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subsection")) return false;
    if (!nextTokenIs(b, SUBTITLE)) return false;
    boolean r;
    Marker m = enter_section_(b);
    r = consumeToken(b, SUBTITLE);
    r = r && subsection_1(b, l + 1);
    exit_section_(b, m, SUBSECTION, r);
    return r;
  }

  // [content]
  private static boolean subsection_1(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "subsection_1")) return false;
    content(b, l + 1);
    return true;
  }

  /* ********************************************************** */
  // NUMBER|WORD|STRING|PUNCT|KEYWORD|SIGNAL|URL|IDENTIFIER|REGISTER
  public static boolean token(PsiBuilder b, int l) {
    if (!recursion_guard_(b, l, "token")) return false;
    boolean r;
    Marker m = enter_section_(b, l, _NONE_, TOKEN, "<token>");
    r = consumeToken(b, NUMBER);
    if (!r) r = consumeToken(b, WORD);
    if (!r) r = consumeToken(b, STRING);
    if (!r) r = consumeToken(b, PUNCT);
    if (!r) r = consumeToken(b, KEYWORD);
    if (!r) r = consumeToken(b, SIGNAL);
    if (!r) r = consumeToken(b, URL);
    if (!r) r = consumeToken(b, IDENTIFIER);
    if (!r) r = consumeToken(b, REGISTER);
    exit_section_(b, l, m, r, false, null);
    return r;
  }

}
