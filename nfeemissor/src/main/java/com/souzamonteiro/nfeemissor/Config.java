/*
 * Classe de configuração para o emissor de NF-e
 * Suporta apenas os estados BA (Bahia) e AM (Amazonas) para simplificação
 * 
 * @author Professor - Para fins didáticos
 */
package com.souzamonteiro.nfeemissor;

import br.com.swconsultoria.certificado.Certificado;
import br.com.swconsultoria.certificado.CertificadoService;
import br.com.swconsultoria.certificado.exception.CertificadoException;
import br.com.swconsultoria.nfe.dom.enuns.EstadosEnum;
import br.com.swconsultoria.nfe.dom.enuns.AmbienteEnum;
import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.exception.NfeException;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.json.JSONObject;

/**
 * Classe responsável pelas configurações do emissor de NF-e
 * Configura certificado digital, ambiente e estados suportados
 */
public class Config {
    
    /**
     * Carrega o certificado A1 no formato PFX
     * @return Certificado digital carregado
     * @throws CertificadoException em caso de erro no certificado
     */
    private static Certificado certificadoA1Pfx() throws CertificadoException {
        // Obtém o diretório atual do projeto
        String caminhoNFeEmissor = System.getProperty("user.dir");
        
        // Define o caminho do arquivo de configuração
        String caminhoArquivoConfiguracoes = caminhoNFeEmissor + "/NFeEmissor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        try {
            // Obtém caminho e senha do certificado do arquivo de configuração
            String caminhoCertificado = configuracoes.get("caminhoCertificado").toString();
            String senhaCertificado = configuracoes.get("senhaCertificado").toString();

            // Carrega o certificado digital
            return CertificadoService.certificadoPfx(caminhoCertificado, senhaCertificado);
        } catch (Exception e)  {
            System.out.println("Erro ao carregar certificado: " + e);
            return null;
        }
    }
    
    /**
     * Inicializa as configurações para comunicação com a SEFAZ
     * @return Configurações da NF-e
     * @throws NfeException em caso de erro na configuração
     */
    public static ConfiguracoesNfe iniciaConfiguracoes() throws NfeException {
        String caminhoNFeEmissor = System.getProperty("user.dir");
        
        String caminhoArquivoConfiguracoes = caminhoNFeEmissor + "/NFeEmissor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        try {
            // Obtém configurações do arquivo JSON
            String webserviceUF = configuracoes.get("webserviceUF").toString();
            String webserviceAmbiente = configuracoes.get("webserviceAmbiente").toString();
            String caminhoSchemas = configuracoes.get("caminhoSchemas").toString();
            
            // Carrega o certificado digital
            Certificado certificado = certificadoA1Pfx();

            // Configurações simplificadas - apenas BA e AM
            // Para ambiente de produção (1) ou homologação (2)
            if (webserviceUF.equals("BA")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.BA, AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.BA, AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else if (webserviceUF.equals("AM")) {
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AM, AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.AM, AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            } else {
                // Estado padrão em caso de configuração inválida
                System.out.println("Estado não suportado. Usando BA como padrão.");
                if (webserviceAmbiente.equals("1")) {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.BA, AmbienteEnum.PRODUCAO, certificado, caminhoSchemas);
                } else {
                    return ConfiguracoesNfe.criarConfiguracoes(EstadosEnum.BA, AmbienteEnum.HOMOLOGACAO, certificado, caminhoSchemas);
                }
            }
        } catch (Exception e)  {
            System.out.println("Erro nas configurações: " + e);
            return null;
        }
    }
    
    /**
     * Obtém o estado configurado para emissão
     * @return Enum do estado
     */
    public static EstadosEnum getEstado() {
        String caminhoNFeEmissor = System.getProperty("user.dir");
        
        String caminhoArquivoConfiguracoes = caminhoNFeEmissor + "/NFeEmissor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        String emitenteUF = configuracoes.get("emitenteUF").toString();
        
        // Retorna apenas BA ou AM - simplificado para ensino
        if (emitenteUF.equals("BA")) {
            return EstadosEnum.BA;
        } else if (emitenteUF.equals("AM")) {
            return EstadosEnum.AM;
        } else {
            System.out.println("Estado não suportado. Usando BA como padrão.");
            return EstadosEnum.BA;
        }
    }
    
    /**
     * Lê o conteúdo de um arquivo
     * @param filePath Caminho do arquivo
     * @return Conteúdo do arquivo como String
     */
    static public String readFile(String filePath) {
        String data = new String();

        try {
            File file = new File(filePath);
            Scanner scanner = new Scanner(file);
            String line;
            
            while (scanner.hasNextLine()) {
                line = scanner.nextLine();
                data = data + line;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("Arquivo não encontrado: " + e);
        }
        
        return data;
    }
}