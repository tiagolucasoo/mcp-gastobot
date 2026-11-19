package br.edu.ifpr.mcp_server.mcp.utils;

public final class McpConsts {
   // info sobre o servidor MCP
   public static final String APP_NAME        = "Servidor MCP Java";
   public static final String APP_VERSION     = "1.1.0";
   public static final String APP_DESCRIPTION =
                                 "Descrição: Servidor MCP Java de demonstração";

   // nome e atributo(s) (propriedades nos esquemas JSON) das ferramentas
   // ("tools")
   // AuthPathTool
   public static final String APT_TOOL_NAME = "auth_path";
   // FileSearchTool
   public static final String FST_TOOL_NAME      = "buscar_arquivos";
   public static final String FST_ATTR_PATTERN   = "pattern";
   public static final String FST_ATTR_RECURSIVE = "recursive";
   public static final String FST_ATTR_MAX_RES   = "max_results";
   // MSWordParagraphCounterTool
   public static final String WPC_TOOL_NAME       = "contar_paragrafos_msword";
   public static final String WPC_ATTR_INPUT_FILE = "input_file";
   // MSWordWordCounterTool
   public static final String WWC_TOOL_NAME       = "contar_palavras_msword";
   public static final String WWC_ATTR_INPUT_FILE = "input_file";
   // PdfMetadataTool
   public static final String PDFMT_TOOL_NAME       = "pdf_metadata";
   public static final String PDFMT_ATTR_INPUT_FILE = "input_file";
   // PdfSearchTool
   public static final String PDFST_TOOL_NAME           = "buscar_texto_pdf";
   public static final String PDFST_ATTR_INPUT_FILE     = "input_file";
   public static final String PDFST_ATTR_SEARCH         = "search";
   public static final String PDFST_ATTR_USE_REGEX      = "use_regex";
   public static final String PDFST_ATTR_CASE_SENSITIVE = "case_sensitive";
   // PdfSearchAndReplaceTool
   public static final String PDFSRT_TOOL_NAME        = "buscar_substituir_pdf";
   public static final String PDFSRT_ATTR_INPUT_FILE     = "input_file";
   public static final String PDFSRT_ATTR_OUTPUT_FILE    = "output_file";
   public static final String PDFSRT_ATTR_SEARCH         = "search";
   public static final String PDFSRT_ATTR_REPLACE        = "replace";
   public static final String PDFSRT_ATTR_USE_REGEX      = "use_regex";
   public static final String PDFSRT_ATTR_CASE_SENSITIVE = "case_sensitive";
   // SimplReportToolIT
   public static final String SRT_TOOL_NAME              = "obter_documento";
   public static final String SRT_ATTR_ID                = "id";

   // nome e atributo(s) dos prompts
   // AnalyzePdfPrompt
   public static final String ANALYZE_PDF_PROMPT        = "analisar_pdf";
   public static final String ANALYZE_PDF_ATTR_ARQUIVO  = "arquivo";
   public static final String ANALYZE_PDF_ATTR_TERMO    = "termo";
   public static final String ANALYZE_PDF_ATTR_CONTEXTO = "contexto";
   // GreetingPrompt
   public static final String GREETING_PROMPT    = "saudacao";
   public static final String GREETING_ATTR_NOME = "nome";

   // URIs dos recursos ("resources")
   // AboutResource
   public static final String RESOURCE_ABOUT_NAME = "about";
   public static final String RESOURCE_ABOUT_URI  = "app://" +
                                                    RESOURCE_ABOUT_NAME;
   // RootInfoResource
   public static final String RESOURCE_ROOT_INFO_NAME = "root-info";
   public static final String RESOURCE_ROOT_INFO_URI  = "app://" +
                                                        RESOURCE_ROOT_INFO_NAME;
}
