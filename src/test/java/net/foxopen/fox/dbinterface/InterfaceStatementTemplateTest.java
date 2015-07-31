package net.foxopen.fox.dbinterface;

import net.foxopen.fox.ContextUElem;
import net.foxopen.fox.database.parser.ParsedStatement;
import net.foxopen.fox.database.parser.StatementParser;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExParser;
import net.foxopen.fox.module.NodeInfoProvider;
import net.foxopen.fox.module.datanode.NodeInfo;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class InterfaceStatementTemplateTest {

  private final DOM mTestDOM = DOM.createDocumentFromXMLString("<ROOT>\n" +
                                                               "  <STRING>string_bind</STRING>\n" +
                                                               "  <ESCAPED_CHARACTER_STRING>&quot;double quotes&quot; &apos;single quotes&apos; &amp; &lt; &gt; -</ESCAPED_CHARACTER_STRING>\n" +
                                                               "  <DATE>2015-01-01</DATE>\n" +
                                                               "  <EMPTY_NODE/>\n" +
                                                               "  <STRING_LIST>\n" +
                                                               "    <STRING>string1</STRING>\n" +
                                                               "    <STRING>string2</STRING>\n" +
                                                               "  </STRING_LIST>\n" +
                                                               "  <EMPTY_STRING_LIST>\n" +
                                                               "    <EMPTY_STRING/>\n" +
                                                               "    <EMPTY_STRING/>\n" +
                                                               "  </EMPTY_STRING_LIST>\n" +
                                                               "  <PARTIAL_EMPTY_STRING_LIST>\n" +
                                                               "    <EMPTY_STRING/>\n" +
                                                               "    <STRING>string1</STRING>\n" +
                                                               "  </PARTIAL_EMPTY_STRING_LIST>\n" +
                                                               "</ROOT>");

  private ContextUElem mContextUElem = new ContextUElem();

  private NodeInfoProvider mNodeInfoProvider = new NodeInfoProvider() {
    @Override
    public NodeInfo getNodeInfo(String pAbsolutePath) {
      return null;
    }

    @Override
    public NodeInfo getNodeInfo(DOM pNode) {
      return null;
    }
  };

  private static class ParamProvider implements StatementBindProvider.InterfaceParameterProvider {

    private final Map<String, InterfaceParameter> mParams = new HashMap<>();

    private ParamProvider() {
      mParams.put("string", TemplateVariableInterfaceParameter.create("string", "./STRING"));
      mParams.put("escaped_character_string", TemplateVariableInterfaceParameter.create("escaped_character_string", "./ESCAPED_CHARACTER_STRING"));
      mParams.put("date_string", TemplateVariableInterfaceParameter.create("date_string", "./DATE"));
      mParams.put("empty_node", TemplateVariableInterfaceParameter.create("empty_node", "./EMPTY_NODE"));
      mParams.put("missing_node", TemplateVariableInterfaceParameter.create("missing_node", "./NOT_A_NODE"));
      mParams.put("string_list", TemplateVariableInterfaceParameter.create("string_list", "./STRING_LIST/STRING"));
      mParams.put("empty_string_list", TemplateVariableInterfaceParameter.create("empty_string_list", "./EMPTY_STRING_LIST/EMPTY_STRING"));
      mParams.put("partial_empty_string_list", TemplateVariableInterfaceParameter.create("partial_empty_string_list", "./PARTIAL_EMPTY_STRING_LIST/*"));
      mParams.put("xpath_string_list", TemplateVariableInterfaceParameter.create("string_list", "./STRING_LIST/STRING[2] | ./EMPTY_STRING_LIST/*"));
      mParams.put("list_container", TemplateVariableInterfaceParameter.create("list_container", "./STRING_LIST"));

      mParams.put("xpath_string", TemplateVariableInterfaceParameter.create("xpath_string", "'xpath_' || 'string'"));
      mParams.put("boolean_true", TemplateVariableInterfaceParameter.create("boolean_true", "true()"));
      mParams.put("boolean_false", TemplateVariableInterfaceParameter.create("boolean_false", "false()"));
      mParams.put("boolean_true_list", TemplateVariableInterfaceParameter.create("boolean_true_list", "(true(), true(), true())"));
      mParams.put("number_1", TemplateVariableInterfaceParameter.create("number_1", "1"));
      mParams.put("integer_1", TemplateVariableInterfaceParameter.create("integer_1", "xs:integer(1)"));
      mParams.put("date", TemplateVariableInterfaceParameter.create("date", "xs:date('2015-02-02')"));
    }

    @Override
    public InterfaceParameter getParamForBindNameOrNull(String pBindName) {
      return mParams.get(pBindName);
    }
  }

  private StatementBindProvider mBindProvider = new StatementBindProvider(mTestDOM, mContextUElem, new ParamProvider(), mNodeInfoProvider, "test");

  private String getParsedStatement(String pString)
  throws ExParser, ExActionFailed {
    ParsedStatement lParsedStatement = StatementParser.parse(pString, "Test", true, true);
    return lParsedStatement.applyTemplates(mBindProvider).getParsedStatementString();
  }

  @Test
  public void testXPathResultHandling()
  throws ExParser, ExActionFailed {

    String lStatement = getParsedStatement("SELECT {{#string}}{{string}}{{/string}} FROM table");
    assertEquals("String variable in parsed statement converted correctly", "SELECT string_bind FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#date_string}}{{date_string}}{{/date_string}} FROM table");
    assertEquals("Date string variable in parsed statement converted correctly", "SELECT 2015-01-01 FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#empty_node}}empty: {{empty_node}}{{/empty_node}} FROM table");
    assertEquals("Empty node variable in parsed statement not converted", "SELECT  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{^empty_node}}empty: {{empty_node}}{{/empty_node}} FROM table");
    assertEquals("Empty node variable in parsed statement converted correctly (when condition inverted)", "SELECT empty:  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#missing_node}}{{missing_node}}{{/missing_node}} FROM table");
    assertEquals("Missing node variable in parsed statement converted correctly", "SELECT  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#string_list}}{{string_list}}{{/string_list}} FROM table");
    assertEquals("String list variable in parsed statement converted correctly (first item taken)", "SELECT string1 FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#empty_string_list}}empty: {{empty_string_list}}{{/empty_string_list}} FROM table");
    assertEquals("Empty string list variable in parsed statement not converted (first item is empty)", "SELECT  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{^empty_string_list}}empty: {{empty_string_list}}{{/empty_string_list}} FROM table");
    assertEquals("Empty string list variable in parsed statement converted correctly (when coniditon inverted)", "SELECT empty:  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#partial_empty_string_list}}empty: {{partial_empty_string_list}}{{/partial_empty_string_list}} FROM table");
    assertEquals("Partial empty string list variable in parsed statement not converted (first item is empty)", "SELECT  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{^partial_empty_string_list}}empty: {{partial_empty_string_list}}{{/partial_empty_string_list}} FROM table");
    assertEquals("Partial empty string list variable in parsed statement converted correctly (when condition inverted)", "SELECT empty:  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#xpath_string_list}}{{xpath_string_list}}{{/xpath_string_list}} FROM table");
    assertEquals("XPath string list variable in parsed statement converted correctly", "SELECT string2 FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#list_container}}bind_exists{{/list_container}} FROM table");
    assertEquals("List container parsed statement not converted", "SELECT  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{^list_container}}bind_not_exists{{/list_container}} FROM table");
    assertEquals("List container parsed statement converted correctly", "SELECT bind_not_exists FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#xpath_string}}{{xpath_string}}{{/xpath_string}} FROM table");
    assertEquals("XPath string variable in parsed statement converted correctly", "SELECT xpath_string FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#boolean_true}}{{boolean_true}}{{/boolean_true}} FROM table");
    assertEquals("True boolean variable in parsed statement converted correctly", "SELECT true FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#boolean_false}}{{boolean_false}}{{/boolean_false}} FROM table");
    assertEquals("False boolean variable in parsed statement converted correctly", "SELECT  FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{^boolean_false}}{{boolean_false}}{{/boolean_false}} FROM table");
    assertEquals("False boolean variable in parsed statement converted correctly (displayed when false and condition inverted)", "SELECT false FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#boolean_true_list}}{{boolean_true_list}}{{/boolean_true_list}} FROM table");
    assertEquals("True boolean list variable in parsed statement converted correctly (first item taken)", "SELECT true FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#number_1}}{{number_1}}{{/number_1}} FROM table");
    assertEquals("Number variable in parsed statement converted correctly", "SELECT 1.0 FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#integer_1}}{{integer_1}}{{/integer_1}} FROM table");
    assertEquals("Integer variable in parsed statement converted correctly", "SELECT 1 FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{#date}}{{date}}{{/date}} FROM table");
    assertEquals("Date variable in parsed statement converted correctly", "SELECT 2015-02-02 FROM table", lStatement);
  }

  @Test
  public void testCharacterEscaping()
  throws ExParser, ExActionFailed {

    String lStatement = getParsedStatement("SELECT {{escaped_character_string}} FROM table");
    assertEquals("Special characters in double braces are escaped", "SELECT <DQUOTE>double quotes<DQUOTE> <SQUOTE>single quotes<SQUOTE> <AMP> < > - FROM table", lStatement);

    lStatement = getParsedStatement("SELECT {{{escaped_character_string}}} FROM table");
    assertEquals("Special characters in triple braces are not escaped", "SELECT \"double quotes\" 'single quotes' & < > - FROM table", lStatement);
  }

  @Test
  public void testReflectedMethodsNotAvailable()
  throws ExParser, ExActionFailed {
    String lStatement = getParsedStatement("SELECT {{#string}}{{value}}{{/string}} FROM table");
    assertEquals("Mustache cannot invoke value() DOM method", "SELECT  FROM table", lStatement);
  }

  @Test(expected = Throwable.class)
  public void testPartialIncludesNotAvailable()
  throws ExParser, ExActionFailed {
    getParsedStatement("SELECT {{> some_include}} FROM table");
  }


  @Test(expected = Throwable.class)
  public void testMalformedSyntaxNotConverted_unmatchedOpenTag()
  throws ExParser, ExActionFailed {
    getParsedStatement("SELECT {{#string}}string FROM table");
  }

  @Test(expected = Throwable.class)
  public void testMalformedSyntaxNotConverted_unmatchedCloseTag()
  throws ExParser, ExActionFailed {
    getParsedStatement("SELECT string{{/string}} FROM table");
  }

  @Test(expected = Throwable.class)
  public void testMalformedSyntaxNotConverted_unClosedTag()
  throws ExParser, ExActionFailed {
    getParsedStatement("SELECT {{string FROM table");
  }
}