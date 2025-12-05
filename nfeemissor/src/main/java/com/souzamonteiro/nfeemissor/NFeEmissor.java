/*
 * Emissor de NF-e para Simples Nacional
 * Projeto didático para alunos - Versão simplificada
 * 
 * @author Professor - Para fins educacionais
 */
package com.souzamonteiro.nfeemissor;

import br.com.swconsultoria.nfe.dom.ConfiguracoesNfe;
import br.com.swconsultoria.nfe.dom.enuns.*;
import br.com.swconsultoria.nfe.Nfe;
import br.com.swconsultoria.nfe.schema_4.consStatServ.TRetConsStatServ;
import br.com.swconsultoria.nfe.schema_4.consReciNFe.TRetConsReciNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.*;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.*;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.COFINS;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.COFINS.COFINSAliq;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.COFINS.COFINSOutr;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.ICMS;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.PIS;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.PIS.PISAliq;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Imposto.PIS.PISOutr;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Det.Prod;
import br.com.swconsultoria.nfe.schema_4.enviNFe.TNFe.InfNFe.Total.ICMSTot;
import br.com.swconsultoria.nfe.util.*;

// Importações para geração de PDF
import br.com.swconsultoria.nfe.impressao.ImpressaoDTO;
import br.com.swconsultoria.nfe.impressao.ImpressaoNfeUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.*;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Emissor de NF-e para Simples Nacional
 * 
 * @author Professor
 * @version 1.0 - Versão didática
 */
public class NFeEmissor {
    
    /**
     * Método principal - inicia o servidor HTTP
     */
    public static void main(String[] args) throws Exception {
        String caminhoNFeEmissor = System.getProperty("user.dir");
        
        // Carrega as configurações do arquivo JSON
        String caminhoArquivoConfiguracoes = caminhoNFeEmissor + "/NFeEmissor.json";
        String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
        
        JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
        
        // Obtém a porta do servidor HTTP
        int porta = Integer.parseInt(configuracoes.get("porta").toString());
            
        // Cria e configura o servidor HTTP
        HttpServer server = HttpServer.create(new InetSocketAddress(porta), 0);
        server.createContext("/NFeStatusServico", new NFeStatusServicoHandler());
        server.createContext("/NFeAutorizacao", new NFeAutorizacaoHandler());
        server.createContext("/NFeGerarPDF", new NFeGerarPDFHandler());
        server.setExecutor(null);
        server.start();
        
        System.out.println("Servidor NFeEmissor iniciado na porta: " + porta);
        System.out.println("Endpoints disponíveis:");
        System.out.println("- /NFeStatusServico");
        System.out.println("- /NFeAutorizacao");
        System.out.println("- /NFeGerarPDF");
    }
    
    /**
     * Lê o conteúdo de um arquivo
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
            System.out.println("Erro ao ler arquivo: " + e);
        }
        
        return data;
    }
    
    /**
     * Gera o DANFE em PDF
     */
    private static boolean gerarPDFDanfe(String xml, String caminhoPDF) {
        try {
            System.out.println("Iniciando geração do DANFE em PDF...");
            
            // Cria o DTO de impressão com layout padrão
            ImpressaoDTO impressao = ImpressaoNfeUtil.impressaoPadraoNFe(xml);
            
            // Gera o PDF e salva no arquivo
            ImpressaoNfeUtil.impressaoPdfArquivo(impressao, caminhoPDF);
            
            System.out.println("DANFE gerado com sucesso: " + caminhoPDF);
            return true;
            
        } catch (Exception e) {
            System.out.println("Erro ao gerar PDF: " + e.getMessage());
            return false;
        }
    }

    /**
     * Handler para autorização de NF-e
     * Baseado no exemplo funcional do Samuel Oliveira
     */
    static class NFeAutorizacaoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String caminhoNFeEmissor = System.getProperty("user.dir");
        
            // Carrega configurações
            String caminhoArquivoConfiguracoes = caminhoNFeEmissor + "/NFeEmissor.json";
            String jsonConfiguracoes = readFile(caminhoArquivoConfiguracoes);
            JSONObject configuracoes = new JSONObject(jsonConfiguracoes);
            
            String caminhoXML = configuracoes.get("caminhoXML").toString();
            String gerarPDF = configuracoes.has("gerarPDF") ? configuracoes.get("gerarPDF").toString() : "0";
            
            InputStream inputStream = httpExchange.getRequestBody(); 
            Scanner scanner = new Scanner(inputStream);
            String dados = scanner.useDelimiter("\\A").next();
            scanner.close();
            
            System.out.print("INFORMAÇÕES: [JSON]: ");
            System.out.println(dados);
            
            JSONObject json = new JSONObject(dados);
            
            String cStat = "";
            String xMotivo = "";
            String dhRecbto = "";
            String nProt = "";
            String chave = "";
            String xml = "";
            
            // Inicia as configurações
            ConfiguracoesNfe config;
            
            try {
                config = Config.iniciaConfiguracoes();
            } catch (Exception e) {
                System.out.println("Erro nas configurações: " + e.getMessage());
                enviarResposta(httpExchange, "000", e.getMessage(), "", "", "", "");
                return;
            }
            
            try {
                // Extrai dados do JSON
                JSONObject jsonInfNFe = json.getJSONObject("infNFe");
                JSONObject jsonIde = jsonInfNFe.getJSONObject("ide");
                JSONObject jsonEmit = jsonInfNFe.getJSONObject("emit");
                JSONObject jsonEnderEmit = jsonEmit.getJSONObject("enderEmit");
                JSONObject jsonDest = jsonInfNFe.getJSONObject("dest");
                JSONArray jsonDet = jsonInfNFe.getJSONArray("det");
                JSONObject jsonTotal = jsonInfNFe.getJSONObject("total");
                JSONObject jsonICMSTot = jsonTotal.getJSONObject("ICMSTot");
                JSONObject jsonTransp = jsonInfNFe.getJSONObject("transp");
                JSONObject jsonPag = jsonInfNFe.getJSONObject("pag");
                JSONArray jsonDetPag = jsonPag.getJSONArray("detPag");
                
                // Dados básicos da NF-e
                String nNF = jsonIde.getString("nNF");
                String cNF = jsonIde.getString("cNF");
                int numeroNFe = Integer.parseInt(nNF);
                
                // CNPJ do emitente
                String cnpj = jsonEmit.getString("CNPJ");
                
                // Data de emissão
                LocalDateTime dataEmissao = LocalDateTime.now();
                
                // Modelo da NF-e (55 = NF-e normal)
                String modelo = "55";
                
                // Série da NF-e
                int serie = Integer.parseInt(jsonIde.getString("serie"));
                
                // Tipo de ambiente
                String tpAmb = config.getAmbiente().getCodigo();
                
                // Tipo de emissão
                String tpEmis = jsonIde.getString("tpEmis");
                
                // Gera a chave da NF-e
                ChaveUtil chaveUtil = new ChaveUtil(config.getEstado(), cnpj, modelo, serie, numeroNFe, tpEmis, cNF, dataEmissao);
                chave = chaveUtil.getChaveNF();
                String cdv = chaveUtil.getDigitoVerificador();

                System.out.println("Gerando NF-e com chave: " + chave);

                // CRIA O OBJETO InfNFe
                InfNFe infNFe = new InfNFe();
                infNFe.setId(chave);
                infNFe.setVersao(ConstantesUtil.VERSAO.NFE);

                // PREENCHE A IDE (Identificação da NF-e)
                Ide ide = new Ide();
                ide.setCUF(config.getEstado().getCodigoUF());
                ide.setCNF(cNF);
                ide.setNatOp(jsonIde.getString("natOp"));
                ide.setMod(modelo);
                ide.setSerie(String.valueOf(serie));
                ide.setNNF(String.valueOf(numeroNFe));
                ide.setDhEmi(XmlNfeUtil.dataNfe(dataEmissao));
                ide.setTpNF(jsonIde.getString("tpNF"));
                ide.setIdDest(jsonIde.getString("idDest"));
                ide.setCMunFG(jsonIde.getString("cMunFG"));
                ide.setTpImp(jsonIde.getString("tpImp"));
                ide.setTpEmis(tpEmis);
                ide.setCDV(cdv);
                ide.setTpAmb(tpAmb);
                ide.setFinNFe(jsonIde.getString("finNFe"));
                ide.setIndFinal(jsonIde.getString("indFinal"));
                ide.setIndPres(jsonIde.getString("indPres"));
                ide.setProcEmi(jsonIde.getString("procEmi"));
                ide.setVerProc(jsonIde.getString("verProc"));
                
                infNFe.setIde(ide);

                // PREENCHE O EMITENTE
                Emit emit = new Emit();
                emit.setCNPJ(cnpj);
                emit.setIE(jsonEmit.getString("IE"));
                emit.setXNome(jsonEmit.getString("xNome"));

                TEnderEmi enderEmit = new TEnderEmi();
                enderEmit.setXLgr(jsonEnderEmit.getString("xLgr"));
                enderEmit.setNro(jsonEnderEmit.getString("nro"));
                if (jsonEnderEmit.has("xCpl")) {
                    enderEmit.setXCpl(jsonEnderEmit.getString("xCpl"));
                }
                enderEmit.setXBairro(jsonEnderEmit.getString("xBairro"));
                enderEmit.setCMun(jsonEnderEmit.getString("cMun"));
                enderEmit.setXMun(jsonEnderEmit.getString("xMun"));
                enderEmit.setUF(TUfEmi.valueOf(jsonEnderEmit.getString("UF")));
                enderEmit.setCEP(jsonEnderEmit.getString("CEP"));
                enderEmit.setCPais("1058"); // Código do Brasil
                enderEmit.setXPais("Brasil");
                if (jsonEnderEmit.has("fone")) {
                    enderEmit.setFone(jsonEnderEmit.getString("fone"));
                }
                emit.setEnderEmit(enderEmit);
                emit.setCRT(jsonEmit.getString("CRT"));

                infNFe.setEmit(emit);

                // PREENCHE O DESTINATÁRIO
                Dest dest = new Dest();
                if (jsonDest.has("CNPJ")) {
                    dest.setCNPJ(jsonDest.getString("CNPJ"));
                }
                if (jsonDest.has("CPF")) {
                    dest.setCPF(jsonDest.getString("CPF"));
                }
                dest.setXNome(jsonDest.getString("xNome"));
                
                if (jsonDest.has("enderDest")) {
                    JSONObject jsonEnderDest = jsonDest.getJSONObject("enderDest");
                    TEndereco enderDest = new TEndereco();
                    enderDest.setXLgr(jsonEnderDest.getString("xLgr"));
                    enderDest.setNro(jsonEnderDest.getString("nro"));
                    if (jsonEnderDest.has("xCpl")) {
                        enderDest.setXCpl(jsonEnderDest.getString("xCpl"));
                    }
                    enderDest.setXBairro(jsonEnderDest.getString("xBairro"));
                    enderDest.setCMun(jsonEnderDest.getString("cMun"));
                    enderDest.setXMun(jsonEnderDest.getString("xMun"));
                    enderDest.setUF(TUf.valueOf(jsonEnderDest.getString("UF")));
                    if (jsonEnderDest.has("CEP")) {
                        enderDest.setCEP(jsonEnderDest.getString("CEP"));
                    }
                    dest.setEnderDest(enderDest);
                }
                
                if (jsonDest.has("indIEDest")) {
                    dest.setIndIEDest(jsonDest.getString("indIEDest"));
                } else {
                    dest.setIndIEDest("9");
                }
                if (jsonDest.has("IE")) {
                    dest.setIE(jsonDest.getString("IE"));
                }
                
                infNFe.setDest(dest);
                
                // PREENCHE OS AUTORIZADOS A TER ACESSO AO XML
                if (jsonInfNFe.has("autXML")) {
                    JSONObject jsonAutXML = jsonInfNFe.getJSONObject("autXML");

                    AutXML autXML = new AutXML();
                    if (jsonAutXML.has("CPF")) {
                        autXML.setCPF(jsonAutXML.get("CPF").toString());
                    }
                    if (jsonAutXML.has("CNPJ")) {
                        autXML.setCNPJ(jsonAutXML.get("CNPJ").toString());
                    }
                    infNFe.getAutXML().add(autXML);
                }
                
                // PREENCHE OS PRODUTOS
                for (int i = 0; i < jsonDet.length(); i++) {
                    JSONObject itemDet = jsonDet.getJSONObject(i);
                    JSONObject jsonProd = itemDet.getJSONObject("prod");
                    JSONObject jsonImposto = itemDet.getJSONObject("imposto");

                    int numeroItem = i + 1;

                    Det det = new Det();
                    det.setNItem(Integer.toString(numeroItem));

                    // Preenche dados do produto
                    Prod prod = new Prod();
                    prod.setCProd(jsonProd.getString("cProd"));
                    if (jsonProd.has("cEAN")) {
                        prod.setCEAN(jsonProd.getString("cEAN"));
                    }
                    prod.setXProd(jsonProd.getString("xProd"));
                    prod.setNCM(jsonProd.getString("NCM"));
                    if (jsonProd.has("CEST")) {
                        prod.setCEST(jsonProd.getString("CEST"));
                    }
                    prod.setCFOP(jsonProd.getString("CFOP"));
                    prod.setUCom(jsonProd.getString("uCom"));
                    prod.setQCom(jsonProd.getString("qCom"));
                    prod.setVUnCom(jsonProd.getString("vUnCom"));
                    prod.setVProd(jsonProd.getString("vProd"));
                    if (jsonProd.has("cEANTrib")) {
                        prod.setCEANTrib(jsonProd.getString("cEANTrib"));
                    }
                    prod.setUTrib(jsonProd.getString("uTrib"));
                    prod.setQTrib(jsonProd.getString("qTrib"));
                    prod.setVUnTrib(jsonProd.getString("vUnTrib"));
                    prod.setIndTot(jsonProd.getString("indTot"));
                    
                    det.setProd(prod);

                    // Preenche impostos
                    Imposto imposto = new Imposto();
                    
                    // ICMS
                    ICMS icms = new ICMS();
                    JSONObject jsonICMS = jsonImposto.getJSONObject("ICMS");
                    
                    // Verifica qual tipo de ICMS está presente
                    if (jsonICMS.has("ICMS00")) {
                        JSONObject jsonICMS00 = jsonICMS.getJSONObject("ICMS00");
                        ICMS.ICMS00 icms00 = new ICMS.ICMS00();
                        icms00.setOrig(jsonICMS00.getString("orig"));
                        icms00.setCST(jsonICMS00.getString("CST"));
                        icms00.setModBC(jsonICMS00.getString("modBC"));
                        icms00.setVBC(jsonICMS00.getString("vBC"));
                        icms00.setPICMS(jsonICMS00.getString("pICMS"));
                        icms00.setVICMS(jsonICMS00.getString("vICMS"));
                        icms.setICMS00(icms00);
                    }
                    else if (jsonICMS.has("ICMSSN101")) {
                        JSONObject jsonICMSSN101 = jsonICMS.getJSONObject("ICMSSN101");
                        ICMS.ICMSSN101 icmsSN101 = new ICMS.ICMSSN101();
                        icmsSN101.setOrig(jsonICMSSN101.getString("orig"));
                        icmsSN101.setCSOSN(jsonICMSSN101.getString("CSOSN"));
                        icmsSN101.setPCredSN(jsonICMSSN101.getString("pCredSN"));
                        icmsSN101.setVCredICMSSN(jsonICMSSN101.getString("vCredICMSSN"));
                        icms.setICMSSN101(icmsSN101);
                    }
                    else if (jsonICMS.has("ICMSSN102")) {
                        JSONObject jsonICMSSN102 = jsonICMS.getJSONObject("ICMSSN102");
                        ICMS.ICMSSN102 icmsSN102 = new ICMS.ICMSSN102();
                        icmsSN102.setOrig(jsonICMSSN102.getString("orig"));
                        icmsSN102.setCSOSN(jsonICMSSN102.getString("CSOSN"));
                        icms.setICMSSN102(icmsSN102);
                    }
                    
                    JAXBElement<ICMS> icmsElement = new JAXBElement<>(new QName("ICMS"), ICMS.class, icms);
                    imposto.getContent().add(icmsElement);

                    // PIS
                    if (jsonImposto.has("PIS")) {
                        JSONObject jsonPIS = jsonImposto.getJSONObject("PIS");
                        PIS pis = new PIS();
                        
                        if (jsonPIS.has("PISAliq")) {
                            JSONObject jsonPISAliq = jsonPIS.getJSONObject("PISAliq");
                            PISAliq pisAliq = new PISAliq();
                            pisAliq.setCST(jsonPISAliq.getString("CST"));
                            pisAliq.setVBC(jsonPISAliq.getString("vBC"));
                            pisAliq.setPPIS(jsonPISAliq.getString("pPIS"));
                            pisAliq.setVPIS(jsonPISAliq.getString("vPIS"));
                            pis.setPISAliq(pisAliq);
                        }
                        else if (jsonPIS.has("PISOutr")) {
                            JSONObject jsonPISOutr = jsonPIS.getJSONObject("PISOutr");
                            PISOutr pisOutr = new PISOutr();
                            pisOutr.setCST(jsonPISOutr.getString("CST"));
                            pisOutr.setVBC(jsonPISOutr.getString("vBC"));
                            pisOutr.setPPIS(jsonPISOutr.getString("pPIS"));
                            pisOutr.setVPIS(jsonPISOutr.getString("vPIS"));
                            pis.setPISOutr(pisOutr);
                        }
                        
                        JAXBElement<PIS> pisElement = new JAXBElement<>(new QName("PIS"), PIS.class, pis);
                        imposto.getContent().add(pisElement);
                    }
                    
                    // COFINS
                    if (jsonImposto.has("COFINS")) {
                        JSONObject jsonCOFINS = jsonImposto.getJSONObject("COFINS");
                        COFINS cofins = new COFINS();
                        
                        if (jsonCOFINS.has("COFINSAliq")) {
                            JSONObject jsonCOFINSAliq = jsonCOFINS.getJSONObject("COFINSAliq");
                            COFINSAliq cofinsAliq = new COFINSAliq();
                            cofinsAliq.setCST(jsonCOFINSAliq.getString("CST"));
                            cofinsAliq.setVBC(jsonCOFINSAliq.getString("vBC"));
                            cofinsAliq.setPCOFINS(jsonCOFINSAliq.getString("pCOFINS"));
                            cofinsAliq.setVCOFINS(jsonCOFINSAliq.getString("vCOFINS"));
                            cofins.setCOFINSAliq(cofinsAliq);
                        }
                        else if (jsonCOFINS.has("COFINSOutr")) {
                            JSONObject jsonCOFINSOutr = jsonCOFINS.getJSONObject("COFINSOutr");
                            COFINSOutr cofinsOutr = new COFINSOutr();
                            cofinsOutr.setCST(jsonCOFINSOutr.getString("CST"));
                            cofinsOutr.setVBC(jsonCOFINSOutr.getString("vBC"));
                            cofinsOutr.setPCOFINS(jsonCOFINSOutr.getString("pCOFINS"));
                            cofinsOutr.setVCOFINS(jsonCOFINSOutr.getString("vCOFINS"));
                            cofins.setCOFINSOutr(cofinsOutr);
                        }
                        
                        JAXBElement<COFINS> cofinsElement = new JAXBElement<>(new QName("COFINS"), COFINS.class, cofins);
                        imposto.getContent().add(cofinsElement);
                    }
                    
                    det.setImposto(imposto);
                    infNFe.getDet().add(det);
                }

                // PREENCHE OS TOTAIS
                Total total = new Total();
                ICMSTot icmstot = new ICMSTot();
                icmstot.setVBC(jsonICMSTot.getString("vBC"));
                icmstot.setVICMS(jsonICMSTot.getString("vICMS"));
                icmstot.setVICMSDeson(jsonICMSTot.getString("vICMSDeson"));
                icmstot.setVFCP(jsonICMSTot.getString("vFCP"));
                icmstot.setVBCST(jsonICMSTot.getString("vBCST"));
                icmstot.setVST(jsonICMSTot.getString("vST"));
                icmstot.setVFCPST(jsonICMSTot.getString("vFCPST"));
                icmstot.setVFCPSTRet(jsonICMSTot.getString("vFCPSTRet"));
                icmstot.setVProd(jsonICMSTot.getString("vProd"));
                icmstot.setVFrete(jsonICMSTot.getString("vFrete"));
                icmstot.setVSeg(jsonICMSTot.getString("vSeg"));
                icmstot.setVDesc(jsonICMSTot.getString("vDesc"));
                icmstot.setVII(jsonICMSTot.getString("vII"));
                icmstot.setVIPI(jsonICMSTot.getString("vIPI"));
                icmstot.setVIPIDevol(jsonICMSTot.getString("vIPIDevol"));
                icmstot.setVPIS(jsonICMSTot.getString("vPIS"));
                icmstot.setVCOFINS(jsonICMSTot.getString("vCOFINS"));
                icmstot.setVOutro(jsonICMSTot.getString("vOutro"));
                icmstot.setVNF(jsonICMSTot.getString("vNF"));
                
                total.setICMSTot(icmstot);
                infNFe.setTotal(total);

                // PREENCHE TRANSPORTE
                Transp transp = new Transp();
                transp.setModFrete(jsonTransp.getString("modFrete"));
                infNFe.setTransp(transp);

                // PREENCHE PAGAMENTO
                Pag pag = new Pag();
                for (int i = 0; i < jsonDetPag.length(); i++) {
                    JSONObject jsonItemDetPag = jsonDetPag.getJSONObject(i);
                    Pag.DetPag detPag = new Pag.DetPag();
                    detPag.setTPag(jsonItemDetPag.getString("tPag"));
                    detPag.setVPag(jsonItemDetPag.getString("vPag"));
                    pag.getDetPag().add(detPag);
                }
                infNFe.setPag(pag);

                // PREENCHE INFORMAÇÕES ADICIONAIS
                if (jsonInfNFe.has("infAdic")) {
                    JSONObject jsonInfAdic = jsonInfNFe.getJSONObject("infAdic");
                    InfAdic infAdic = new InfAdic();
                    infAdic.setInfCpl(jsonInfAdic.getString("infCpl"));
                    infNFe.setInfAdic(infAdic);
                }

                // CRIA O OBJETO TNFe
                TNFe nfe = new TNFe();
                nfe.setInfNFe(infNFe);

                // CRIA O ENVI NFe (Lote)
                TEnviNFe enviNFe = new TEnviNFe();
                enviNFe.setVersao(ConstantesUtil.VERSAO.NFE);
                enviNFe.setIdLote("1");
                enviNFe.setIndSinc("1");
                enviNFe.getNFe().add(nfe);
                
                /*
                 * ENVIO PARA SEFAZ
                 */
                
                // Monta e Assina o XML
                enviNFe = Nfe.montaNfe(config, enviNFe, true);
                
                // Envia a NF-e para a SEFAZ
                TRetEnviNFe retorno = Nfe.enviarNfe(config, enviNFe, DocumentoEnum.NFE);
                
                // Verifica se o retorno é assíncrono
                if (RetornoUtil.isRetornoAssincrono(retorno)) {
                    // Processamento assíncrono (com recibo)
                    String recibo = retorno.getInfRec().getNRec();
                    int tentativa = 0;
                    TRetConsReciNFe retornoNfe = null;

                    // Consulta o recibo várias vezes até obter resposta
                    while (tentativa < 10) {
                        retornoNfe = Nfe.consultaRecibo(config, recibo, DocumentoEnum.NFE);
                        if (retornoNfe.getCStat().equals(StatusEnum.LOTE_EM_PROCESSAMENTO.getCodigo())) {
                            System.out.println("INFO: Lote Em Processamento, tentativa " + (tentativa + 1));
                            Thread.sleep(1000);
                            tentativa++;
                        } else {
                            break;
                        }
                    }

                    RetornoUtil.validaAssincrono(retornoNfe);
                    
                    cStat = retornoNfe.getProtNFe().get(0).getInfProt().getCStat();
                    xMotivo = retornoNfe.getProtNFe().get(0).getInfProt().getXMotivo();
                    
                    System.out.println("cStat: " + cStat + " - " + xMotivo);
                    
                    if (cStat.equals("100")) {
                        nProt = retornoNfe.getProtNFe().get(0).getInfProt().getNProt();
                        dhRecbto = retornoNfe.getProtNFe().get(0).getInfProt().getDhRecbto();
                        xml = XmlNfeUtil.criaNfeProc(enviNFe, retornoNfe.getProtNFe().get(0));
                        
                        // Salva o XML da NF-e
                        salvarXML(xml, caminhoXML, chave);
                        
                        // Gera PDF do DANFE se configurado
                        if ("1".equals(gerarPDF)) {
                            String caminhoPDF = caminhoXML + "/" + chave + ".pdf";
                            gerarPDFDanfe(xml, caminhoPDF);
                        }
                    }
                } else {
                    // Retorno síncrono
                    RetornoUtil.validaSincrono(retorno);
                    
                    cStat = retorno.getProtNFe().getInfProt().getCStat();
                    xMotivo = retorno.getProtNFe().getInfProt().getXMotivo();
                    
                    System.out.println("cStat: " + cStat + " - " + xMotivo);
                    
                    if (cStat.equals("100")) {
                        nProt = retorno.getProtNFe().getInfProt().getNProt();
                        dhRecbto = retorno.getProtNFe().getInfProt().getDhRecbto();
                        xml = XmlNfeUtil.criaNfeProc(enviNFe, retorno.getProtNFe());
                        
                        // Salva o XML da NF-e
                        salvarXML(xml, caminhoXML, chave);
                        
                        // Gera PDF do DANFE se configurado
                        if ("1".equals(gerarPDF)) {
                            String caminhoPDF = caminhoXML + "/" + chave + ".pdf";
                            gerarPDFDanfe(xml, caminhoPDF);
                        }
                    }
                }
                
            } catch (Exception e) {
                System.out.println("Erro no envio para SEFAZ: " + e.getMessage());
                e.printStackTrace();
                cStat = "000";
                xMotivo = e.getMessage();
            }
            
            enviarResposta(httpExchange, cStat, xMotivo, dhRecbto, nProt, chave, xml);
        }
        
        private void salvarXML(String xml, String caminhoXML, String chave) throws IOException {
            FileWriter writer = new FileWriter(caminhoXML + "/" + chave + ".xml");
            writer.write(xml);
            writer.close();
            System.out.println("XML Final salvo: " + chave + ".xml");
        }
        
        private void enviarResposta(HttpExchange httpExchange, String cStat, String xMotivo, 
                                   String dhRecbto, String nProt, String chave, String xml) throws IOException {
            JSONObject responseJSON = new JSONObject();
            responseJSON.put("cStat", cStat);
            responseJSON.put("xMotivo", xMotivo);
            responseJSON.put("dhRecbto", dhRecbto);
            responseJSON.put("nProt", nProt);
            responseJSON.put("chave", chave);
            responseJSON.put("xml", xml);

            System.out.println("Resposta autorização: " + responseJSON.toString());
            
            String response = responseJSON.toString();
            
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }

    // Outros handlers...
    static class NFeGerarPDFHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            // Implementação do serviço de geração de PDF
            String response = "{\"cStat\":\"100\",\"xMotivo\":\"Serviço de PDF disponível\"}";
            
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
    
    static class NFeStatusServicoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            try {
                // Inicia As Configurações
                ConfiguracoesNfe config = Config.iniciaConfiguracoes();

                try {
                    //Efetua Consulta
                    TRetConsStatServ retorno = Nfe.statusServico(config, DocumentoEnum.NFE);

                    //Resultado
                    System.out.println();
                    System.out.println("# Status: " + retorno.getCStat() + " - " + retorno.getXMotivo());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("# Erro: " + e.getMessage());
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("# Erro: " + e.getMessage());
            }
            
            String response = "{\"cStat\":\"107\",\"xMotivo\":\"Serviço em Operação\"}";
            
            httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            httpExchange.getResponseHeaders().add("Content-Type", "application/json");
            httpExchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream outputStream = httpExchange.getResponseBody();
            outputStream.write(response.getBytes());
            outputStream.close();
        }
    }
}