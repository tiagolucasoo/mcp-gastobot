package br.edu.ifpr.mcp_server.services.pdfs.model;

import java.util.regex.Pattern;

public record SearchOptions(String targetText, boolean useRegex,
                            boolean caseSensitive) {
   public Pattern toPattern() {
      String regex = useRegex ? targetText : Pattern.quote(targetText);
//      int    flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE |
//                                         Pattern.UNICODE_CASE;
      // faz com que classes como \w, \W, \b e outras propriedades relacionadas
      // utilizem o comportamento Unicode
      int    flags = Pattern.UNICODE_CHARACTER_CLASS;

      if (!caseSensitive)
         flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;

      return Pattern.compile(regex,flags);
   }
}
