package br.edu.ifpr.mcp_server.utils.json.schema;

import java.io.InputStream;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public class SchemaLoader {
   private static final ObjectMapper MAPPER = new ObjectMapper();

   public static Map<String, Object> readFromFile(String resourcePath) {
      // garante que o caminho comece com '/' para buscar a partir da raiz do
      // classpath
      String normalizedPath = resourcePath.startsWith("/") ? resourcePath
                                                           : "/" + resourcePath;

      // carrega um arquivo JSON Schema da pasta src/main/resources e o
      // converte em um Map<String, Object> hierárquico.
      try (InputStream is = SchemaLoader.class
    			                        .getResourceAsStream(normalizedPath)) {
         if (is == null)
            throw new IllegalArgumentException("Arquivo de esquema JSON não " +
                                               "encontrado: " + resourcePath);
           
         // TypeReference garante a desserialização tipada da estrutura
         // aninhada
         return MAPPER.readValue(is,new TypeReference<Map<String, Object>>(){});
      }
      catch (Exception e) {
         throw new RuntimeException("Erro ao carregar e converter o esquema " +
                                    "JSON",e);
      }
   }

   public static Map<String, Object> readFromString(String jsonContent) {
      // converte diretamente uma String contendo o esquema JSON para
      // Map<String, Object>.
      try {
         return MAPPER.readValue(jsonContent,
                                 new TypeReference<Map<String, Object>>() {});
      }
      catch (Exception e) {
         throw new RuntimeException("Erro ao converter texto JSON",e);
      }
   }
}
/*
CABEÇALHO DO ESQUEMA JSON:
   "$schema": "https://json-schema.org/draft/2020-12/schema",
   "$id": "https://example.com/simple-report.schema.json",


OPÇÕES DE IMPLEMENTAÇÃO PARA O CÓDIGO ACIMA
1) org.json (JSON-Java):

JSONObject jsonObject = new JSONObject(jsonString);
Map<String, Object> schemaMap = jsonObject.toMap();

2) Google Gson:

Gson gson = new Gson();
Type type = new TypeToken<Map<String, Object>>(){}.getType();
Map<String, Object> schemaMap = gson.fromJson(jsonReader, type);
*/
