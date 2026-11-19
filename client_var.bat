@echo off
rem Executa o cliente MCP passando o diretório autorizado pela
rem variável de ambiente MCP_FILES_ROOT.
rem Uso: client_var.bat DIRETORIO_AUTORIZADO
setlocal

if "%~1"=="" (
   echo Uso: %~nx0 DIRETORIO_AUTORIZADO
   exit /b 1
)

set "MCP_FILES_ROOT=%~1"
cd /d "%~dp0target\dist-client"
java -jar mcp_server01_client.jar

endlocal
